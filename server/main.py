from fastapi import FastAPI, HTTPException, BackgroundTasks
from pydantic import BaseModel
import yt_dlp
import uvicorn
import os
import uuid
import threading

app = FastAPI()

# In-memory storage for download status
# {task_id: {"status": "pending/downloading/completed/error", "progress": 0, "title": ""}}
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
    except Exception as e:
        download_tasks[task_id]["status"] = "error"
        download_tasks[task_id]["message"] = str(e)

@app.post("/download")
def download_media(request: DownloadRequest, background_tasks: BackgroundTasks):
    task_id = str(uuid.uuid4())
    download_tasks[task_id] = {"status": "pending", "progress": 0, "title": ""}
    background_tasks.add_task(run_download, request.url, request.format, task_id)
    return {"task_id": task_id}

@app.get("/status/{task_id}")
def get_status(task_id: str):
    if task_id not in download_tasks:
        raise HTTPException(status_code=404, detail="Task not found")
    return download_tasks[task_id]

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
