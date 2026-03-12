package com.example.googlemusic.data.auth

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var userAccount by mutableStateOf<GoogleSignInAccount?>(null)
        private set
    
    var uploadStatus by mutableStateOf<String?>(null)
        private set

    init {
        userAccount = authRepository.getSignedInAccount()
    }

    fun getSignInIntent(): Intent {
        return authRepository.getSignInIntent()
    }

    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        userAccount = authRepository.handleSignInResult(completedTask)
    }

    suspend fun getAccessToken(): String? {
        val account = userAccount ?: return null
        return authRepository.getAccessToken(account)
    }

    fun signOut() {
        authRepository.signOut {
            userAccount = null
        }
    }

    fun uploadFileToDrive(context: android.content.Context, filePath: String) {
        val account = userAccount?.account ?: return
        viewModelScope.launch {
            uploadStatus = "Preparing upload..."
            try {
                withContext(Dispatchers.IO) {
                    val credential = GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton(DriveScopes.DRIVE_FILE)
                    ).setSelectedAccount(account)

                    val googleDriveService = Drive.Builder(
                        com.google.api.client.extensions.android.http.AndroidHttp.newCompatibleTransport(),
                        com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                        credential
                    ).setApplicationName("Google Music").build()

                    val fileToUpload = java.io.File(filePath)
                    val fileMetadata = File().apply {
                        name = fileToUpload.name
                    }
                    val mediaContent = FileContent(null, fileToUpload)

                    googleDriveService.files().create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute()
                }
                uploadStatus = "Success! Uploaded to Drive."
            } catch (e: Exception) {
                e.printStackTrace()
                uploadStatus = "Error: ${e.message}"
            }
        }
    }
}
