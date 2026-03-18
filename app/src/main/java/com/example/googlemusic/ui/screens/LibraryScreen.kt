package com.example.googlemusic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.googlemusic.data.auth.AuthViewModel
import com.example.googlemusic.ui.library.LibraryViewModel
import com.example.googlemusic.ui.theme.GoogleMusicTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel, 
    authViewModel: AuthViewModel,
    onPlayMedia: (list: List<com.example.googlemusic.ui.library.LocalMedia>, index: Int) -> Unit
) {
    val context = LocalContext.current
    val userAccount = authViewModel.userAccount
    val uploadStatus = authViewModel.uploadStatus
    val selectedTab = viewModel.selectedTab
    
    val currentList = if (selectedTab == 0) viewModel.musicList else viewModel.videoList

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.refreshLibrary()
    }

    LaunchedEffect(uploadStatus) {
        uploadStatus?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Library") },
                    actions = {
                        IconButton(onClick = { viewModel.refreshLibrary() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectedTab = 0 },
                        text = { Text("Music (MP3)") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectedTab = 1 },
                        text = { Text("Video (MP4)") }
                    )
                }
            }
        }
    ) { padding ->
        if (currentList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No ${if (selectedTab == 0) "music" else "videos"} found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                itemsIndexed(currentList) { index, media ->
                    ListItem(
                        modifier = Modifier.clickable {
                            onPlayMedia(currentList, index)
                        },
                        headlineContent = { Text(media.name) },
                        supportingContent = { Text(if (media.isVideo) "Video" else "Audio") },
                        leadingContent = {
                            Icon(
                                imageVector = if (media.isVideo) Icons.Default.VideoLibrary else Icons.Default.Audiotrack,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            val isUploading = uploadStatus == "Preparing upload..."
                            IconButton(
                                onClick = { authViewModel.uploadFileToDrive(context, media.path) },
                                enabled = userAccount != null && !isUploading
                            ) {
                                if (isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF34A853)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Upload to Drive",
                                        tint = if (userAccount != null) Color(0xFF34A853) else Color.Gray
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
