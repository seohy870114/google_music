package com.example.googlemusic.ui.library

import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.io.File

data class LocalMedia(
    val name: String,
    val path: String,
    val isVideo: Boolean,
    val subtitlePath: String? = null
)

class LibraryViewModel : ViewModel() {
    var localMediaList by mutableStateOf<List<LocalMedia>>(emptyList())
        private set

    var selectedTab by mutableIntStateOf(0)

    val musicList: List<LocalMedia>
        get() = localMediaList.filter { !it.isVideo }

    val videoList: List<LocalMedia>
        get() = localMediaList.filter { it.isVideo }

    fun refreshLibrary() {
        val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "google_music")
        if (!musicDir.exists()) musicDir.mkdirs()
        
        val files = musicDir.listFiles { file ->
            file.extension.lowercase() in listOf("mp3", "mp4")
        }
        
        localMediaList = files?.map { file ->
            val subtitleFile = File(musicDir, file.nameWithoutExtension + ".srt")
            LocalMedia(
                name = file.name,
                path = file.absolutePath,
                isVideo = file.extension.lowercase() == "mp4",
                subtitlePath = if (subtitleFile.exists()) subtitleFile.absolutePath else null
            )
        }?.sortedByDescending { File(it.path).lastModified() } ?: emptyList()
    }
}
