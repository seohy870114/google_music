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
            
            val ip = settingsRepository.serverIpFlow.first()
            val apiService = networkRepository.getApiService(ip) ?: return@launch

            try {
                val response = apiService.downloadMedia(DownloadRequest(url, format))
                pollStatus(context, response.task_id, apiService)
            } catch (e: Exception) {
                errorMessage = "Download failed: ${e.message}"
                downloadStatus = null
            }
        }
    }

    private suspend fun pollStatus(context: Context, taskId: String, apiService: com.example.googlemusic.data.network.ApiService) {
        while (true) {
            try {
                val statusResponse = apiService.getStatus(taskId)
                downloadProgress = statusResponse.progress / 100f
                downloadStatus = "Server: ${statusResponse.status} (${statusResponse.progress.toInt()}%)"
                
                if (statusResponse.status == "completed") {
                    val filename = statusResponse.filename
                    if (filename != null) {
                        downloadToDevice(context, filename, apiService)
                    } else {
                        downloadStatus = "Success! (Filename missing)"
                    }
                    break
                } else if (statusResponse.status == "error") {
                    errorMessage = statusResponse.message ?: "Server error during download"
                    break
                }
            } catch (e: Exception) {
                errorMessage = "Status check failed: ${e.message}"
                break
            }
            delay(1000)
        }
    }

    private fun downloadToDevice(context: Context, filename: String, apiService: com.example.googlemusic.data.network.ApiService) {
        viewModelScope.launch {
            downloadStatus = "Downloading to phone..."
            downloadProgress = 0f
            
            try {
                val response = apiService.downloadFile(filename)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        withContext(Dispatchers.IO) {
                            val file = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                filename
                            )
                            val inputStream = body.byteStream()
                            val outputStream = FileOutputStream(file)
                            val buffer = ByteArray(4096)
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
                        }
                        downloadStatus = "Success! Saved to Downloads"
                        Toast.makeText(context, "휴대폰 저장 완료: $filename", Toast.LENGTH_LONG).show()
                    }
                } else {
                    errorMessage = "File transfer failed: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Transfer failed: ${e.message}"
            }
        }
    }
}
