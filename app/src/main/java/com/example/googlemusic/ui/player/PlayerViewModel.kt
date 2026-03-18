package com.example.googlemusic.ui.player

import android.content.Context
import kotlin.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.googlemusic.ui.library.LocalMedia
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import java.io.File

data class SubtitleLine(
    val start: Long,
    val end: Long,
    val text: String
)

class PlayerViewModel : ViewModel() {
    private var exoPlayer: ExoPlayer? = null
    
    var isPlaying by mutableStateOf(false)
        private set
    
    var currentPosition by mutableLongStateOf(0L)
        private set
    
    var duration by mutableLongStateOf(0L)
        private set

    var currentPlaylist by mutableStateOf<List<LocalMedia>>(emptyList())
        private set

    var currentMediaIndex by mutableIntStateOf(-1)
        private set

    var currentSubtitle by mutableStateOf("")
        private set

    var subtitleLines by mutableStateOf<List<SubtitleLine>>(emptyList())
        private set

    var currentSubtitleIndex by mutableIntStateOf(-1)
        private set

    @OptIn(UnstableApi::class)
    fun getPlayer(context: Context): Player {
        return exoPlayer ?: ExoPlayer.Builder(context).build().also {
            exoPlayer = it
            it.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        duration = it.duration
                    }
                }

                @OptIn(UnstableApi::class)
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    duration = it.duration
                    currentMediaIndex = it.currentMediaItemIndex
                    loadSubtitlesForCurrentMedia()
                }

                override fun onCues(cueGroup: androidx.media3.common.text.CueGroup) {
                    currentSubtitle = cueGroup.cues.firstOrNull()?.text?.toString() ?: ""
                }
            })
            startPositionTracker()
        }
    }

    private fun startPositionTracker() {
        viewModelScope.launch {
            while (isActive) {
                exoPlayer?.let {
                    val pos = it.currentPosition
                    currentPosition = pos
                    updateActiveSubtitleIndex(pos)
                }
                delay(200)
            }
        }
    }

    private fun updateActiveSubtitleIndex(position: Long) {
        val index = subtitleLines.indexOfLast { it.start <= position }
        if (index != currentSubtitleIndex) {
            currentSubtitleIndex = index
        }
    }

    private fun loadSubtitlesForCurrentMedia() {
        val media = currentPlaylist.getOrNull(currentMediaIndex)
        subtitleLines = if (media?.subtitlePath != null) {
            parseSrt(File(media.subtitlePath))
        } else {
            emptyList()
        }
        currentSubtitleIndex = -1
    }

    private fun parseSrt(file: File): List<SubtitleLine> {
        val lines = mutableListOf<SubtitleLine>()
        try {
            val content = file.readText()
            val blocks = content.split(Regex("(\\r?\\n){2,}")).filter { it.isNotBlank() }
            
            for (block in blocks) {
                val subLines = block.lines().filter { it.isNotBlank() }
                if (subLines.size >= 3) {
                    val timeLine = subLines[1]
                    val times = timeLine.split(" --> ")
                    if (times.size == 2) {
                        val start = parseSrtTime(times[0])
                        val end = parseSrtTime(times[1])
                        val text = subLines.drop(2).joinToString("\n")
                        lines.add(SubtitleLine(start, end, text))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lines
    }

    private fun parseSrtTime(time: String): Long {
        val cleanTime = time.trim().replace(",", ".")
        val parts = cleanTime.split(":")
        var millis = 0L
        if (parts.size == 3) {
            millis += parts[0].toLong() * 3600000
            millis += parts[1].toLong() * 60000
            millis += (parts[2].toDouble() * 1000).toLong()
        }
        return millis
    }

    @OptIn(UnstableApi::class)
    fun setPlaylist(mediaList: List<LocalMedia>, startIndex: Int) {
        if (currentPlaylist == mediaList && currentMediaIndex == startIndex) return
        
        currentPlaylist = mediaList
        currentMediaIndex = startIndex
        
        exoPlayer?.let { player ->
            val mediaItems = mediaList.map { localMedia ->
                val builder = MediaItem.Builder()
                    .setUri(localMedia.path)
                    .setMediaId(localMedia.path)
                
                localMedia.subtitlePath?.let { subPath ->
                    val subtitle = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subPath))
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage("ko")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                    builder.setSubtitleConfigurations(listOf(subtitle))
                }
                
                builder.build()
            }
            player.setMediaItems(mediaItems)
            player.prepare()
            player.seekTo(startIndex, 0L)
            player.playWhenReady = true
            loadSubtitlesForCurrentMedia()
        }
    }

    fun playAtIndex(index: Int) {
        exoPlayer?.let {
            if (index in currentPlaylist.indices) {
                it.seekTo(index, 0L)
                it.playWhenReady = true
            }
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }

    fun seekForward(amount: Long = 5000L) {
        exoPlayer?.let {
            it.seekTo(it.currentPosition + amount)
        }
    }

    fun seekBack(amount: Long = 5000L) {
        exoPlayer?.let {
            it.seekTo((it.currentPosition - amount).coerceAtLeast(0L))
        }
    }

    fun playNext() {
        exoPlayer?.seekToNextMediaItem()
    }

    fun playPrevious() {
        exoPlayer?.seekToPreviousMediaItem()
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}
