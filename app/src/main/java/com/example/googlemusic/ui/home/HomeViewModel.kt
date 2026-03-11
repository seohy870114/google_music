package com.example.googlemusic.ui.home

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.googlemusic.data.network.AnalyzeRequest
import com.example.googlemusic.data.network.DownloadRequest
import com.example.googlemusic.data.network.DriveUploadRequest
import com.example.googlemusic.data.network.NetworkRepository
import com.example.googlemusic.data.network.VideoInfoResponse
import com.example.googlemusic.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val networkRepository: NetworkRepository = NetworkRepository()
) : ViewModel() {

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

    fun analyzeUrl(url: String) {
        viewModelScope.launch {
            isAnalyzing = true
            errorMessage = null
            videoInfo = null
            
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

    fun startDownload(context: Context, url: String, format: String) {
        viewModelScope.launch {
            errorMessage = null
            downloadProgress = 0f
            downloadStatus = "Starting download on server..."
            currentTaskId = null
            
            val ip = settingsRepository.serverIpFlow.first()
            val apiService = networkRepository.getApiService(ip) ?: return@launch

            try {
                val response = apiService.downloadMedia(DownloadRequest(url, format))
                currentTaskId = response.task_id
                pollStatus(context, response.task_id, apiService)
            } catch (e: Exception) {
                errorMessage = "Download failed: ${e.message}"
                downloadStatus = null
            }
        }
    }

    private suspend fun pollStatus(context: Context, taskId: String, apiService: com.example.googlemusic.data.network.ApiService) {
        var retryCount = 0
        val maxRetries = 5

        while (true) {
            try {
                val statusResponse = apiService.getStatus(taskId)
                retryCount = 0 
                
                downloadProgress = statusResponse.progress / 100f
                downloadStatus = if (statusResponse.status == "processing") {
                    "Server: Processing (FFmpeg)..."
                } else if (statusResponse.status == "uploading_to_drive") {
                    "Server: Uploading to Drive..."
                } else if (statusResponse.status == "drive_completed") {
                    "Success! Uploaded to Google Drive."
                } else {
                    "Server: ${statusResponse.status} (${statusResponse.progress.toInt()}%)"
                }
                
                if (statusResponse.status == "completed") {
                    // Stop polling if we just wanted local download
                    // But if we want to support drive upload, we might continue
                    // For now, let's say "completed" is a stopping point for status checks
                    // unless a drive upload is explicitly triggered.
                    downloadStatus = "Server: Download completed."
                    break
                } else if (statusResponse.status == "drive_completed") {
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

    fun downloadToDevice(
        context: Context, 
        taskId: String, 
        serverFilename: String, 
        userTitle: String,
        ext: String
    ) {
        viewModelScope.launch {
            val ip = settingsRepository.serverIpFlow.first()
            val apiService = networkRepository.getApiService(ip) ?: return@launch
            
            downloadStatus = "Downloading to phone..."
            downloadProgress = 0f
            
            try {
                val response = apiService.downloadFile(taskId, serverFilename)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val finalFile = withContext(Dispatchers.IO) {
                            val sanitizedTitle = userTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            
                            var file = File(downloadsDir, "$sanitizedTitle.$ext")
                            var count = 1
                            while (file.exists()) {
                                file = File(downloadsDir, "$sanitizedTitle ($count).$ext")
                                count++
                            }

                            val inputStream = body.byteStream()
                            val outputStream = FileOutputStream(file)
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            val fileSize = body.contentLength()
                            var totalBytesRead: Long = 0

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                if (fileSize > 0) {
                                    withContext(Dispatchers.Main) {
                                        downloadProgress = totalBytesRead.toFloat() / fileSize
                                    }
                                }
                            }
                            outputStream.close()
                            inputStream.close()
                            file
                        }
                        downloadStatus = "Success! Saved to Downloads"
                        Toast.makeText(context, "휴대폰 저장 완료: ${finalFile.name}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    errorMessage = "File transfer failed: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Transfer failed: ${e.message}"
            }
        }
    }

    fun uploadToDrive(context: Context, taskId: String, accessToken: String) {
        viewModelScope.launch {
            downloadStatus = "Requesting Drive upload..."
            val ip = settingsRepository.serverIpFlow.first()
            val apiService = networkRepository.getApiService(ip) ?: return@launch

            try {
                apiService.uploadToDrive(DriveUploadRequest(taskId, accessToken))
                pollStatus(context, taskId, apiService)
            } catch (e: Exception) {
                errorMessage = "Drive upload request failed: ${e.message}"
            }
        }
    }
    
    fun fetchStatusManually(context: Context, taskId: String) {
        viewModelScope.launch {
            val ip = settingsRepository.serverIpFlow.first()
            val apiService = networkRepository.getApiService(ip) ?: return@launch
            pollStatus(context, taskId, apiService)
        }
    }
}
