package com.example.googlemusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.googlemusic.data.auth.AuthViewModel
import com.example.googlemusic.data.network.VideoInfoResponse
import com.example.googlemusic.ui.home.HomeViewModel
import com.example.googlemusic.ui.theme.GoogleMusicTheme
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(homeViewModel: HomeViewModel, authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // ViewModel의 상태를 관찰
    val videoInfo = homeViewModel.videoInfo
    val isAnalyzing = homeViewModel.isAnalyzing
    val downloadProgress = homeViewModel.downloadProgress
    val downloadStatus = homeViewModel.downloadStatus
    val errorMessage = homeViewModel.errorMessage
    val currentTaskId = homeViewModel.currentTaskId
    val isServerDownloadComplete = homeViewModel.isServerDownloadComplete
    val userAccount = authViewModel.userAccount

    HomeContent(
        urlInput = homeViewModel.urlInput,
        videoInfo = videoInfo,
        isAnalyzing = isAnalyzing,
        downloadProgress = downloadProgress,
        downloadStatus = downloadStatus,
        errorMessage = errorMessage,
        // 서버 다운로드가 완료되고, 로그인된 상태일 때만 업로드 버튼 활성화
        canUploadToDrive = isServerDownloadComplete && currentTaskId != null && userAccount != null,
        onUrlChange = { homeViewModel.urlInput = it },
        onAnalyze = { homeViewModel.analyzeUrl(it) },
        onDownload = { url, format -> homeViewModel.startDownload(url, format) },
        onUploadToDrive = {
            scope.launch {
                val token = authViewModel.getAccessToken()
                if (token != null && currentTaskId != null) {
                    homeViewModel.uploadToDrive(currentTaskId, token)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    urlInput: String,
    videoInfo: VideoInfoResponse?,
    isAnalyzing: Boolean,
    downloadProgress: Float,
    downloadStatus: String?,
    errorMessage: String?,
    canUploadToDrive: Boolean,
    onUrlChange: (String) -> Unit,
    onAnalyze: (String) -> Unit,
    onDownload: (String, String) -> Unit,
    onUploadToDrive: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Download YouTube Media", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        // [URL 입력창 개선]: TrailingIcon으로 Clear 버튼 추가
        OutlinedTextField(
            value = urlInput,
            onValueChange = onUrlChange,
            label = { Text("YouTube URL") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Paste link here...") },
            singleLine = true,
            trailingIcon = {
                if (urlInput.isNotEmpty()) {
                    IconButton(onClick = { onUrlChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { if (urlInput.isNotEmpty()) onAnalyze(urlInput) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAnalyzing && urlInput.isNotEmpty()
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Analyze URL")
            }
        }

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        videoInfo?.let { info ->
            Spacer(modifier = Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AsyncImage(
                        model = info.thumbnail,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = info.title ?: "No Title", style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(text = info.uploader ?: "Unknown Uploader", style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { info.url?.let { onDownload(it, "mp3") } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("MP3")
                        }
                        Button(
                            onClick = { info.url?.let { onDownload(it, "mp4") } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("MP4")
                        }
                    }
                    
                    // [업로드 버튼 조건부 노출]: AnimatedVisibility 사용
                    AnimatedVisibility(
                        visible = canUploadToDrive,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onUploadToDrive,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853))
                            ) {
                                Text("Upload from Server to Drive")
                            }
                        }
                    }
                }
            }
        }

        downloadStatus?.let { status ->
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = status, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = downloadProgress,
                modifier = Modifier.fillMaxWidth()
            )
            Text(text = "${(downloadProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
