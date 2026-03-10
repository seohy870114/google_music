package com.example.googlemusic.ui.library

import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.io.File

data class LocalMedia(
    val name: String,
    val path: String,
    val isVideo: Boolean
)

class LibraryViewModel : ViewModel() {
    var localMediaList by mutableStateOf<List<LocalMedia>>(emptyList())
        private set

    fun refreshLibrary() {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val files = downloadsDir.listFiles { file ->
            file.extension.lowercase() in listOf("mp3", "mp4")
        }
        
        localMediaList = files?.map { file ->
            LocalMedia(
                name = file.name,
                path = file.absolutePath,
                isVideo = file.extension.lowercase() == "mp4"
            )
        }?.sortedByDescending { File(it.path).lastModified() } ?: emptyList()
    }
}
