package com.example.googlemusic.ui.home

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    fun startDownload(url: String, format: String) {
        viewModelScope.launch {
            errorMessage = null
            downloadProgress = 0f
            downloadStatus = "Starting download..."
            
            val ip = settingsRepository.serverIpFlow.first()
            val apiService = networkRepository.getApiService(ip) ?: return@launch

            try {
                val response = apiService.downloadMedia(DownloadRequest(url, format))
                pollStatus(response.task_id, apiService)
            } catch (e: Exception) {
                errorMessage = "Download failed: ${e.message}"
                downloadStatus = null
            }
        }
    }

    private suspend fun pollStatus(taskId: String, apiService: com.example.googlemusic.data.network.ApiService) {
        while (true) {
            try {
                val statusResponse = apiService.getStatus(taskId)
                downloadProgress = statusResponse.progress / 100f
                downloadStatus = "Status: ${statusResponse.status}"
                
                if (statusResponse.status == "completed") {
                    downloadStatus = "Success! Download complete."
                    break
                } else if (statusResponse.status == "error") {
                    errorMessage = statusResponse.message ?: "Server error during download"
                    break
                }
            } catch (e: Exception) {
                errorMessage = "Status check failed: ${e.message}"
                break
            }
            delay(1000) // Poll every 1 second
        }
    }
}
