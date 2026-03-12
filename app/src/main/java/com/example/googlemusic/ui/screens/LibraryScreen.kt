package com.example.googlemusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.googlemusic.data.auth.AuthViewModel
import com.example.googlemusic.ui.library.LibraryViewModel
import com.example.googlemusic.ui.theme.GoogleMusicTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel, authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val mediaList = viewModel.localMediaList
    val userAccount = authViewModel.userAccount
    val uploadStatus = authViewModel.uploadStatus

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.refreshLibrary()
    }

    LaunchedEffect(uploadStatus) {
        uploadStatus?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    IconButton(onClick = { viewModel.refreshLibrary() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (mediaList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No media found in Downloads")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(mediaList) { media ->
                    ListItem(
                        headlineContent = { Text(media.name) },
                        supportingContent = { Text(if (media.isVideo) "Video (MP4)" else "Audio (MP3)") },
                        leadingContent = {
                            Icon(
                                imageVector = if (media.isVideo) Icons.Default.VideoLibrary else Icons.Default.Audiotrack,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            IconButton(
                                onClick = { authViewModel.uploadFileToDrive(context, media.path) },
                                enabled = userAccount != null
                            ) {
                                Icon(
                                    Icons.Default.CloudUpload, 
                                    contentDescription = "Upload to Drive",
                                    tint = if (userAccount != null) Color(0xFF34A853) else Color.Gray
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
