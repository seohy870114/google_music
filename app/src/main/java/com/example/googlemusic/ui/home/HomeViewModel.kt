package com.example.googlemusic.ui.home

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.googlemusic.data.network.*
import com.example.googlemusic.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class HomeViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val networkRepository: NetworkRepository = NetworkRepository()
) : ViewModel() {

    var urlInput by mutableStateOf("")
    
    var videoInfo by mutableStateOf<VideoInfoResponse?>(null)
        private set

    var isAnalyzing by mutableStateOf(false)
        private set

    var downloadProgress by mutableStateOf(0f)
        private set

    var downloadStatus by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var currentTaskId by mutableStateOf<String?>(null)
        private set

    var isServerDownloadComplete by mutableStateOf(false)
        private set

    fun analyzeUrl(url: String) {
        viewModelScope.launch {
            isAnalyzing = true
            errorMessage = null
            videoInfo = null
            isServerDownloadComplete = false
            
            val ip = settingsRepository.serverIpFlow.first()
            val apiService = networkRepository.getApiService(ip)
            
            if (apiService == null) {
                errorMessage = "Server IP not configured"
                isAnalyzing = false
                return@launch
            }

            try {
                videoInfo = apiService.analyzeUrl(AnalyzeRequest(url))
            } catch (e: Exception) {
                errorMessage = "Analysis failed: ${e.message}"
            } finally {
                isAnalyzing = false
            }
        }
    }

    fun startDownload(url: String, format: String) {
        viewModelScope.launch {
            errorMessage = null
            downloadProgress = 0f
            downloadStatus = "Starting download on server..."
            currentTaskId = null
            isServerDownloadComplete = false
            
            val ip = settingsRepository.serverIpFlow.first()
            val apiService = networkRepository.getApiService(ip) ?: return@launch

            try {
                val response = apiService.downloadMedia(DownloadRequest(url, format))
                currentTaskId = response.task_id
                pollStatus(response.task_id, apiService)
            } catch (e: Exception) {
                errorMessage = "Download failed: ${e.message}"
                downloadStatus = null
            }
        }
    }

    private suspend fun pollStatus(taskId: String, apiService: ApiService) {
        var retryCount = 0
        val maxRetries = 5

        while (true) {
            try {
                val statusResponse = apiService.getStatus(taskId)
                retryCount = 0 
                
                downloadProgress = statusResponse.progress / 100f
                downloadStatus = when (statusResponse.status) {
                    "processing" -> "Server: Processing (FFmpeg)..."
                    "uploading_to_drive" -> "Server: Uploading to Drive..."
                    "drive_completed" -> "Success! Uploaded to Google Drive."
                    else -> "Server: ${statusResponse.status} (${statusResponse.progress.toInt()}%)"
                }
                
                // Automatic trigger when finished
                if (statusResponse.status == "completed") {
                    isServerDownloadComplete = true
                    val serverFilename = statusResponse.filename
                    val ext = statusResponse.ext ?: "mp4"
                    val userTitle = videoInfo?.title ?: "Downloaded_File"

                    if (serverFilename != null) {
                        // Automatically bring file to device
                        downloadToDevice(
                            taskId, 
                            serverFilename, 
                            userTitle, 
                            ext, 
                            apiService,
                            hasSubtitles = statusResponse.has_subtitles,
                            subtitleFilename = statusResponse.subtitle_filename
                        )
                    }
                    break
                }
 else if (statusResponse.status == "drive_completed") {
                    break
                } else if (statusResponse.status == "error" || statusResponse.status == "drive_error") {
                    errorMessage = statusResponse.message ?: "Server error during task"
                    break
                }
            } catch (e: Exception) {
                if (e is IOException && retryCount < maxRetries) {
                    retryCount++
                    downloadStatus = "Connection lost. Retrying ($retryCount/$maxRetries)..."
                    delay(2000)
                    continue
                } else {
                    errorMessage = "Status check failed: ${e.message}"
                    break
                }
            }
            delay(1500)
        }
    }

    private fun downloadToDevice(
        taskId: String, 
        serverFilename: String, 
        userTitle: String,
        ext: String,
        apiService: ApiService,
        hasSubtitles: Boolean = false,
        subtitleFilename: String? = null
    ) {
        viewModelScope.launch {
            downloadStatus = "Downloading to phone..."
            downloadProgress = 0f
            
            try {
                val sanitizedTitle = userTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                
                // 1. Download Media File
                val response = apiService.downloadFile(taskId, serverFilename)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        saveToMediaStore(
                            fileName = "$sanitizedTitle.$ext",
                            mimeType = if (ext == "mp3") "audio/mpeg" else "video/mp4",
                            body = body
                        )
                    }
                }

                // 2. Download Subtitle File (if exists)
                if (hasSubtitles && subtitleFilename != null) {
                    downloadStatus = "Downloading subtitles..."
                    val subResponse = apiService.downloadFile(taskId, subtitleFilename)
                    if (subResponse.isSuccessful) {
                        val body = subResponse.body()
                        if (body != null) {
                            saveToMediaStore(
                                fileName = "$sanitizedTitle.srt",
                                mimeType = "application/x-subrip",
                                body = body,
                                isSubtitle = true
                            )
                        }
                    }
                }

                downloadStatus = "Success! Saved to Downloads"
            } catch (e: Exception) {
                errorMessage = "Device download failed: ${e.message}"
            }
        }
    }

    private suspend fun saveToMediaStore(
        fileName: String,
        mimeType: String,
        body: ResponseBody,
        isSubtitle: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Change directory to Music/google_music
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/google_music")
            }
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mimeType.startsWith("video")) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else if (isSubtitle) {
                // Subtitles can be stored in Downloads or Files collection
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI // Fallback
        }

        val uri = resolver.insert(collectionUri, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                val inputStream = body.byteStream()
                val buffer = ByteArray(8192)
                var bytesRead: Int
                val fileSize = body.contentLength()
                var totalBytesRead: Long = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (!isSubtitle && fileSize > 0) {
                        withContext(Dispatchers.Main) {
                            downloadProgress = totalBytesRead.toFloat() / fileSize
                        }
                    }
                }
            }
            
            // For older API or direct file access scenarios, help media scanner
            try {
                val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "google_music")
                if (!musicDir.exists()) musicDir.mkdirs()
                val actualFile = File(musicDir, fileName)
                MediaScannerConnection.scanFile(context, arrayOf(actualFile.absolutePath), arrayOf(mimeType), null)
            } catch (e: Exception) {
                // Ignore scanner errors
            }
            
        } else {
            throw IOException("Failed to create MediaStore entry for $fileName")
        }
    }

    fun uploadToDrive(taskId: String, accessToken: String) {
        viewModelScope.launch {
            val ip = settingsRepository.serverIpFlow.first()
            val apiService = networkRepository.getApiService(ip) ?: return@launch
            try {
                apiService.uploadToDrive(DriveUploadRequest(taskId, accessToken))
                pollStatus(taskId, apiService)
            } catch (e: Exception) {
                errorMessage = "Drive upload failed: ${e.message}"
            }
        }
    }
}
