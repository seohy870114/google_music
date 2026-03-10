package com.example.googlemusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.googlemusic.ui.library.LibraryViewModel
import com.example.googlemusic.ui.theme.GoogleMusicTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    val viewModel = remember { LibraryViewModel() }
    val mediaList = viewModel.localMediaList

    LaunchedEffect(Unit) {
        viewModel.refreshLibrary()
    }

    Scaffold(
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
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
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
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LibraryScreenPreview() {
    GoogleMusicTheme {
        LibraryScreen()
    }
}
