package com.example.googlemusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.googlemusic.data.repository.SettingsRepository
import com.example.googlemusic.ui.settings.SettingsViewModel
import com.example.googlemusic.ui.theme.GoogleMusicTheme

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    // For now, initializing manually (will be replaced by DI later)
    val viewModel = remember {
        SettingsViewModel(SettingsRepository(context))
    }
    
    val savedServerIp by viewModel.serverIp.collectAsState()
    val connectionStatus = viewModel.connectionStatus
    
    SettingsContent(
        savedServerIp = savedServerIp,
        connectionStatus = connectionStatus,
        onSaveIp = { viewModel.updateServerIp(it) },
        onCheckConnection = { viewModel.checkServerConnection() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    savedServerIp: String,
    connectionStatus: String?,
    onSaveIp: (String) -> Unit,
    onCheckConnection: () -> Unit
) {
    var serverIpInput by remember { mutableStateOf("") }
    
    // Update local input when saved value changes (initially)
    LaunchedEffect(savedServerIp) {
        if (serverIpInput.isEmpty() && savedServerIp.isNotEmpty()) {
            serverIpInput = savedServerIp
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Server Configuration", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = serverIpInput,
            onValueChange = { serverIpInput = it },
            label = { Text("Server IP Address") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. 192.168.0.10") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSaveIp(serverIpInput) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save IP")
            }
            OutlinedButton(
                onClick = onCheckConnection,
                modifier = Modifier.weight(1f)
            ) {
                Text("Check Connection")
            }
        }
        
        if (connectionStatus != null) {
            Text(
                text = connectionStatus,
                style = MaterialTheme.typography.bodySmall,
                color = if (connectionStatus.startsWith("Success")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Download Quality", style = MaterialTheme.typography.titleMedium)
        // TODO: Dropdown for quality selection
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Account", style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = { /* TODO: Google Login/Logout */ },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Google Sign-In")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    GoogleMusicTheme {
        SettingsContent(
            savedServerIp = "192.168.0.10",
            connectionStatus = "Success: Google Music Server is running",
            onSaveIp = {},
            onCheckConnection = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenDarkPreview() {
    GoogleMusicTheme {
        SettingsContent(
            savedServerIp = "192.168.0.10",
            connectionStatus = "Error: Connection refused",
            onSaveIp = {},
            onCheckConnection = {}
        )
    }
}
