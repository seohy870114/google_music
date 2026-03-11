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

def run_download(url: str, format_type: str, task_id: str):
    task_dir = os.path.join(DOWNLOAD_ROOT, task_id)
    os.makedirs(task_dir, exist_ok=True)
    
    if format_type == "mp3":
        ydl_opts = {
            'format': 'bestaudio/best',
            'postprocessors': [{
                'key': 'FFmpegExtractAudio',
                'preferredcodec': 'mp3',
                'preferredquality': '192',
            }],
            'outtmpl': f'{task_dir}/%(id)s.%(ext)s',
            'overwrites': True,
        }
    else:
        ydl_opts = {
            'format': 'bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best',
            'outtmpl': f'{task_dir}/%(id)s.%(ext)s',
            'overwrites': True,
        }
    
    ydl_opts['progress_hooks'] = [lambda d: progress_hook(d, task_id)]
    
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            video_id = info.get('id')
            ext = "mp3" if format_type == "mp3" else info.get('ext', 'mp4')
            
            download_tasks[task_id].update({
                "status": "completed",
                "progress": 100,
                "title": info.get('title'),
                "filename": f"{video_id}.{ext}",
                "ext": ext
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
                    if os.stat(path).st_mtime < now - 3600:
                        try:
                            shutil.rmtree(path)
                            if task_id in download_tasks:
                                del download_tasks[task_id]
                        except:
                            pass
        time.sleep(600)

threading.Thread(target=cleanup_loop, daemon=True).start()

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
