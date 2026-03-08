package com.example.googlemusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.googlemusic.ui.theme.GoogleMusicTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var url by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Download YouTube Media", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("YouTube URL") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Paste link here...") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { /* TODO: Analyze URL */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Analyze / Download")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Downloading List", style = MaterialTheme.typography.titleMedium)
        // TODO: LinearProgressIndicator List
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    GoogleMusicTheme {
        HomeScreen()
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenDarkPreview() {
    GoogleMusicTheme {
        HomeScreen()
    }
}
