from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.responses import FileResponse
from pydantic import BaseModel
import yt_dlp
import uvicorn
import os
import uuid
import time
import shutil

app = FastAPI()

# In-memory storage for download status
download_tasks = {}

DOWNLOAD_DIR = "downloads"
if not os.path.exists(DOWNLOAD_DIR):
    os.makedirs(DOWNLOAD_DIR)

class AnalyzeRequest(BaseModel):
    url: str

class DownloadRequest(BaseModel):
    url: str
    format: str # "mp3" or "mp4"

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
            }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

def progress_hook(d, task_id):
    if d['status'] == 'downloading':
        p = d.get('_percent_str', '0%').replace('%','')
        try:
            download_tasks[task_id]["progress"] = float(p)
            download_tasks[task_id]["status"] = "downloading"
        except:
            pass
    elif d['status'] == 'finished':
        download_tasks[task_id]["progress"] = 100
        download_tasks[task_id]["status"] = "completed"
        # Store the actual filename
        download_tasks[task_id]["filename"] = os.path.basename(d['info_dict']['get_path'])

def run_download(url: str, format_type: str, task_id: str):
    if format_type == "mp3":
        ydl_opts = {
            'format': 'bestaudio/best',
            'postprocessors': [{
                'key': 'FFmpegExtractAudio',
                'preferredcodec': 'mp3',
                'preferredquality': '192',
            }],
            'outtmpl': f'{DOWNLOAD_DIR}/%(title)s.%(ext)s',
        }
    else:
        ydl_opts = {
            'format': 'bestvideo+bestaudio/best',
            'outtmpl': f'{DOWNLOAD_DIR}/%(title)s.%(ext)s',
        }
    
    ydl_opts['progress_hooks'] = [lambda d: progress_hook(d, task_id)]
    
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            download_tasks[task_id]["title"] = info.get('title', 'Unknown')
            # If filename wasn't set in hook (e.g. for post-processed files)
            if "filename" not in download_tasks[task_id]:
                # yt-dlp internal logic for final filename
                ext = "mp3" if format_type == "mp3" else info.get('ext')
                download_tasks[task_id]["filename"] = f"{info.get('title')}.{ext}"
    except Exception as e:
        download_tasks[task_id]["status"] = "error"
        download_tasks[task_id]["message"] = str(e)

@app.post("/download")
def download_media(request: DownloadRequest, background_tasks: BackgroundTasks):
    task_id = str(uuid.uuid4())
    download_tasks[task_id] = {"status": "pending", "progress": 0, "title": "", "filename": ""}
    background_tasks.add_task(run_download, request.url, request.format, task_id)
    return {"task_id": task_id}

@app.get("/status/{task_id}")
def get_status(task_id: str):
    if task_id not in download_tasks:
        raise HTTPException(status_code=404, detail="Task not found")
    return download_tasks[task_id]

def delete_file_later(path: str):
    # Wait a bit to ensure the file handle is closed
    time.sleep(10)
    if os.path.exists(path):
        os.remove(path)

@app.get("/files/{filename}")
async def get_file(filename: str, background_tasks: BackgroundTasks):
    file_path = os.path.join(DOWNLOAD_DIR, filename)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="File not found")
    
    # Schedule deletion after transfer
    background_tasks.add_task(delete_file_later, file_path)
    return FileResponse(path=file_path, filename=filename)

# Periodic cleanup for old files (older than 1 hour)
@app.on_event("startup")
async def startup_event():
    def cleanup():
        while True:
            now = time.time()
            for f in os.listdir(DOWNLOAD_DIR):
                path = os.path.join(DOWNLOAD_DIR, f)
                if os.stat(path).st_mtime < now - 3600:
                    os.remove(path)
            time.sleep(600) # Run every 10 minutes
    
    import threading
    threading.Thread(target=cleanup, daemon=True).start()

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
