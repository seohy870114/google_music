package com.example.googlemusic.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.googlemusic.data.network.NetworkRepository
import com.example.googlemusic.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val networkRepository: NetworkRepository = NetworkRepository()
) : ViewModel() {
    
    val serverIp: StateFlow<String> = repository.serverIpFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    var connectionStatus by mutableStateOf<String?>(null)
        private set

    fun updateServerIp(ip: String) {
        viewModelScope.launch {
            repository.saveServerIp(ip)
        }
    }

    fun checkServerConnection() {
        viewModelScope.launch {
            val ip = serverIp.value
            if (ip.isEmpty()) {
                connectionStatus = "IP address is empty"
                return@launch
            }

            connectionStatus = "Connecting..."
            val apiService = networkRepository.getApiService(ip)
            if (apiService == null) {
                connectionStatus = "Invalid URL or setup error"
                return@launch
            }

            try {
                val response = apiService.checkHealth()
                connectionStatus = "Success: ${response.message}"
            } catch (e: Exception) {
                connectionStatus = "Error: ${e.message}"
            }
        }
    }
}
