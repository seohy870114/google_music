package com.example.googlemusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.googlemusic.data.network.VideoInfoResponse
import com.example.googlemusic.data.repository.SettingsRepository
import com.example.googlemusic.ui.home.HomeViewModel
import com.example.googlemusic.ui.theme.GoogleMusicTheme

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val viewModel = remember {
        HomeViewModel(SettingsRepository(context))
    }
    
    val videoInfo = viewModel.videoInfo
    val isAnalyzing = viewModel.isAnalyzing
    val downloadProgress = viewModel.downloadProgress
    val downloadStatus = viewModel.downloadStatus
    val errorMessage = viewModel.errorMessage

    HomeContent(
        videoInfo = videoInfo,
        isAnalyzing = isAnalyzing,
        downloadProgress = downloadProgress,
        downloadStatus = downloadStatus,
        errorMessage = errorMessage,
        onAnalyze = { viewModel.analyzeUrl(it) },
        onDownload = { url, format -> viewModel.startDownload(url, format) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    videoInfo: VideoInfoResponse?,
    isAnalyzing: Boolean,
    downloadProgress: Float,
    downloadStatus: String?,
    errorMessage: String?,
    onAnalyze: (String) -> Unit,
    onDownload: (String, String) -> Unit
) {
    var url by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Download YouTube Media", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("YouTube URL") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Paste link here...") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { if (url.isNotEmpty()) onAnalyze(url) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAnalyzing && url.isNotEmpty()
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    GoogleMusicTheme {
        HomeContent(
            videoInfo = VideoInfoResponse(
                url = "https://youtube.com/...",
                title = "Sample Video Title that might be very long and wrap",
                thumbnail = null,
                duration = 300,
                uploader = "Sample Artist"
            ),
            isAnalyzing = false,
            downloadProgress = 0.45f,
            downloadStatus = "Status: downloading",
            errorMessage = null,
            onAnalyze = {},
            onDownload = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenDarkPreview() {
    GoogleMusicTheme {
        HomeContent(
            videoInfo = null,
            isAnalyzing = true,
            downloadProgress = 0f,
            downloadStatus = null,
            errorMessage = "Analysis failed: connection refused",
            onAnalyze = {},
            onDownload = { _, _ -> }
        )
    }
}
