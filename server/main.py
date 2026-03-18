from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.responses import FileResponse
from pydantic import BaseModel
import yt_dlp
import uvicorn
import os
import uuid
import time
import shutil
import threading
from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

app = FastAPI()

# In-memory storage for download status
download_tasks = {}

DOWNLOAD_ROOT = "downloads"
if not os.path.exists(DOWNLOAD_ROOT):
    os.makedirs(DOWNLOAD_ROOT)

class AnalyzeRequest(BaseModel):
    url: str

class DownloadRequest(BaseModel):
    url: str
    format: str # "mp3" or "mp4"

class DriveUploadRequest(BaseModel):
    task_id: str
    access_token: str

@app.get("/")
def read_root():
    return {"status": "ok", "message": "Google Music Server is running"}

@app.post("/analyze")
def analyze_url(request: AnalyzeRequest):
    ydl_opts = {'quiet': True, 'no_warnings': True}
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(request.url, download=False)
            return {
                "url": request.url,
                "title": info.get('title'),
                "thumbnail": info.get('thumbnail'),
                "duration": info.get('duration'),
                "uploader": info.get('uploader'),
                "video_id": info.get('id')
            }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

def progress_hook(d, task_id):
    if d['status'] == 'downloading':
        p = d.get('_percent_str', '0%').replace('%','').strip()
        try:
            download_tasks[task_id]["progress"] = float(p)
            download_tasks[task_id]["status"] = "downloading"
        except:
            pass
    elif d['status'] == 'finished':
        download_tasks[task_id]["progress"] = 100
        download_tasks[task_id]["status"] = "processing"

import whisper
import datetime

# Load Whisper model globally to avoid reloading (Warning: consumes memory)
# Using "base" for a balance between speed and quality. 
# "tiny" is faster but less accurate.
whisper_model = None

def get_whisper_model():
    global whisper_model
    if whisper_model is None:
        try:
            whisper_model = whisper.load_model("base")
        except Exception as e:
            print(f"Error loading whisper model: {e}")
    return whisper_model

def format_timestamp(seconds: float):
    td = datetime.timedelta(seconds=seconds)
    total_seconds = int(td.total_seconds())
    hours = total_seconds // 3600
    minutes = (total_seconds % 3600) // 60
    secs = total_seconds % 60
    millis = int(td.microseconds / 1000)
    return f"{hours:02}:{minutes:02}:{secs:02},{millis:03}"

def generate_srt(segments):
    srt_content = ""
    for i, segment in enumerate(segments, start=1):
        start = format_timestamp(segment['start'])
        end = format_timestamp(segment['end'])
        text = segment['text'].strip()
        srt_content += f"{i}\n{start} --> {end}\n{text}\n\n"
    return srt_content

def run_download(url: str, format_type: str, task_id: str):
    task_dir = os.path.join(DOWNLOAD_ROOT, task_id)
    os.makedirs(task_dir, exist_ok=True)
    
    # Initialize ydl_opts
    ydl_opts = {
        'outtmpl': f'{task_dir}/%(id)s.%(ext)s',
        'overwrites': True,
        'progress_hooks': [lambda d: progress_hook(d, task_id)],
    }

    postprocessors = []

    if format_type == "mp3":
        ydl_opts.update({
            'format': 'bestaudio/best',
        })
        postprocessors.append({
            'key': 'FFmpegExtractAudio',
            'preferredcodec': 'mp3',
            'preferredquality': '192',
        })
    else:
        ydl_opts.update({
            'format': 'bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best',
            'writesubs': True,
            'writeautomaticsubs': True,
            'subtitleslangs': ['ko', 'en'],
        })
        postprocessors.append({
            'key': 'FFmpegSubtitlesConvertor',
            'format': 'srt',
        })

    ydl_opts['postprocessors'] = postprocessors
    
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            video_id = info.get('id')
            
            # Determine final extension and filename
            # For MP3, it's always .mp3 after post-processing
            # For MP4, it's usually .mp4 but let's be safe
            if format_type == "mp3":
                ext = "mp3"
            else:
                ext = info.get('ext', 'mp4')
            
            filename = f"{video_id}.{ext}"
            file_path = os.path.join(task_dir, filename)

            # Verification: If the expected file doesn't exist, check for alternatives
            if not os.path.exists(file_path):
                # Check if it stayed as webm or m4a
                for alt_ext in ['webm', 'm4a', 'mp4', 'mp3']:
                    alt_path = os.path.join(task_dir, f"{video_id}.{alt_ext}")
                    if os.path.exists(alt_path):
                        file_path = alt_path
                        filename = f"{video_id}.{alt_ext}"
                        ext = alt_ext
                        break
            
            # Subtitle check
            srt_filename = f"{video_id}.ko.srt"
            if not os.path.exists(os.path.join(task_dir, srt_filename)):
                srt_filename = f"{video_id}.en.srt"
            
            has_subtitles = os.path.exists(os.path.join(task_dir, srt_filename))
            
            # AI Fallback: Whisper (Works for both MP3 and MP4)
            if not has_subtitles:
                download_tasks[task_id]["status"] = "generating_ai_subtitles"
                model = get_whisper_model()
                if model and os.path.exists(file_path):
                    try:
                        result = model.transcribe(file_path)
                        srt_content = generate_srt(result['segments'])
                        srt_filename = f"{video_id}.srt"
                        with open(os.path.join(task_dir, srt_filename), "w", encoding="utf-8") as f:
                            f.write(srt_content)
                        has_subtitles = True
                    except Exception as whisper_e:
                        print(f"Whisper failed: {whisper_e}")

            download_tasks[task_id].update({
                "status": "completed",
                "progress": 100,
                "title": info.get('title'),
                "filename": filename,
                "ext": ext,
                "has_subtitles": has_subtitles,
                "subtitle_filename": srt_filename if has_subtitles else None
            })
    except Exception as e:
        download_tasks[task_id]["status"] = "error"
        download_tasks[task_id]["message"] = str(e)

@app.post("/download")
def download_media(request: DownloadRequest, background_tasks: BackgroundTasks):
    task_id = str(uuid.uuid4())
    download_tasks[task_id] = {
        "status": "pending", 
        "progress": 0, 
        "title": "", 
        "filename": "",
        "ext": ""
    }
    background_tasks.add_task(run_download, request.url, request.format, task_id)
    return {"task_id": task_id}

@app.get("/status/{task_id}")
def get_status(task_id: str):
    if task_id not in download_tasks:
        raise HTTPException(status_code=404, detail="Task not found")
    
    task = download_tasks[task_id]
    if task["status"] == "completed":
        return task
        
    if task["filename"]:
        file_path = os.path.join(DOWNLOAD_ROOT, task_id, task["filename"])
        if os.path.exists(file_path) and task["status"] == "processing":
            task["status"] = "completed"
            task["progress"] = 100
            
    return task

@app.get("/files/{task_id}/{filename}")
async def get_file(task_id: str, filename: str):
    file_path = os.path.join(DOWNLOAD_ROOT, task_id, filename)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="File not found")
    return FileResponse(path=file_path, filename=filename)

def run_drive_upload(task_id: str, access_token: str):
    task = download_tasks.get(task_id)
    if not task or task["status"] != "completed":
        return

    file_path = os.path.join(DOWNLOAD_ROOT, task_id, task["filename"])
    if not os.path.exists(file_path):
        return

    try:
        download_tasks[task_id]["status"] = "uploading_to_drive"
        creds = Credentials(token=access_token)
        service = build('drive', 'v3', credentials=creds)

        file_metadata = {'name': task["title"] + "." + task["ext"]}
        media = MediaFileUpload(file_path, resumable=True)
        
        file = service.files().create(body=file_metadata, media_body=media, fields='id').execute()
        download_tasks[task_id]["status"] = "drive_completed"
        download_tasks[task_id]["drive_file_id"] = file.get('id')
    except Exception as e:
        download_tasks[task_id]["status"] = "drive_error"
        download_tasks[task_id]["message"] = str(e)

@app.post("/upload/drive")
def upload_to_drive(request: DriveUploadRequest, background_tasks: BackgroundTasks):
    if request.task_id not in download_tasks:
        raise HTTPException(status_code=404, detail="Task not found")
    
    background_tasks.add_task(run_drive_upload, request.task_id, request.access_token)
    return {"status": "upload_started"}

def cleanup_loop():
    while True:
        now = time.time()
        if os.path.exists(DOWNLOAD_ROOT):
            for task_id in os.listdir(DOWNLOAD_ROOT):
                path = os.path.join(DOWNLOAD_ROOT, task_id)
                if os.path.isdir(path):
                    # Keep files for 30 minutes (1800 seconds)
                    if os.stat(path).st_mtime < now - 1800:
                        try:
                            shutil.rmtree(path)
                            if task_id in download_tasks:
                                del download_tasks[task_id]
                        except:
                            pass
        time.sleep(600) # Run every 10 minutes

threading.Thread(target=cleanup_loop, daemon=True).start()

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
