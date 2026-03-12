package com.example.googlemusic.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.googlemusic.data.auth.AuthViewModel
import com.example.googlemusic.data.repository.SettingsRepository
import com.example.googlemusic.ui.settings.SettingsViewModel
import com.example.googlemusic.ui.theme.GoogleMusicTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn

@Composable
fun SettingsScreen(settingsRepository: SettingsRepository, authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val settingsViewModel = remember {
        SettingsViewModel(settingsRepository)
    }
    
    val savedServerIp by settingsViewModel.serverIp.collectAsState()
    val connectionStatus = settingsViewModel.connectionStatus
    val userAccount = authViewModel.userAccount

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        authViewModel.handleSignInResult(task)
    }

    SettingsContent(
        savedServerIp = savedServerIp,
        connectionStatus = connectionStatus,
        userEmail = userAccount?.email,
        onSaveIp = { settingsViewModel.updateServerIp(it) },
        onCheckConnection = { settingsViewModel.checkServerConnection() },
        onSignIn = { launcher.launch(authViewModel.getSignInIntent()) },
        onSignOut = { authViewModel.signOut() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    savedServerIp: String,
    connectionStatus: String?,
    userEmail: String?,
    onSaveIp: (String) -> Unit,
    onCheckConnection: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    var serverIpInput by remember { mutableStateOf("") }
    
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
        Text(text = "Account", style = MaterialTheme.typography.titleMedium)
        
        if (userEmail != null) {
            Text(text = "Signed in as: $userEmail", style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = onSignOut,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Sign Out")
            }
        } else {
            Button(
                onClick = onSignIn,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Google Sign-In")
            }
        }
    }
}
