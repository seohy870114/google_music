package com.example.googlemusic.data.auth

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var userAccount by mutableStateOf<GoogleSignInAccount?>(null)
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
}
