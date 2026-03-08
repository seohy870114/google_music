package com.example.googlemusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.googlemusic.ui.theme.GoogleMusicTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Library") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Placeholder for media items
            items(10) { index ->
                ListItem(
                    headlineContent = { Text("Song Title $index") },
                    supportingContent = { Text("Artist Name $index") },
                    leadingContent = {
                        Icon(
                            imageVector = if (index % 2 == 0) Icons.Default.PhoneAndroid else Icons.Default.CloudDone,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
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

@Preview(showBackground = true, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LibraryScreenDarkPreview() {
    GoogleMusicTheme {
        LibraryScreen()
    }
}
