package com.example.googlemusic.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import kotlin.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.googlemusic.ui.player.PlayerViewModel
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val player = remember { playerViewModel.getPlayer(context) }

    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // UI state for brightness/volume feedback
    var gestureInfo by remember { mutableStateOf<String?>(null) }
    var gestureProgress by remember { mutableFloatStateOf(0f) }
    var gestureTimer by remember { mutableLongStateOf(0L) }
    
    // Seek animation state
    var seekAction by remember { mutableStateOf<String?>(null) }
    var seekTimer by remember { mutableLongStateOf(0L) }

    val subtitleListState = rememberLazyListState()
    var showPlaylistByMode by remember { mutableStateOf(false) } // false = Lyrics, true = Playlist

    // Auto-scroll subtitles with centering
    LaunchedEffect(playerViewModel.currentSubtitleIndex) {
        val index = playerViewModel.currentSubtitleIndex
        if (index >= 0 && !showPlaylistByMode) {
            // Animate scroll with an offset to keep the active line in the center
            subtitleListState.animateScrollToItem(index, scrollOffset = -300) 
        }
    }

    // Auto-hide controls logic
    LaunchedEffect(showControls, lastInteractionTime, playerViewModel.isPlaying) {
        if (showControls && playerViewModel.isPlaying) {
            delay(1500)
            showControls = false
        }
    }

    LaunchedEffect(gestureTimer) {
        if (gestureTimer > 0) {
            delay(1500)
            gestureInfo = null
            gestureTimer = 0
        }
    }
    
    LaunchedEffect(seekTimer) {
        if (seekTimer > 0) {
            delay(800)
            seekAction = null
            seekTimer = 0
        }
    }

    val content = @Composable {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    // Consolidate gestures to avoid crash and conflicts
                    detectTapGestures(
                        onTap = { 
                            showControls = !showControls
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        onDoubleTap = { offset ->
                            val width = size.width
                            if (offset.x < width / 2) {
                                playerViewModel.seekBack()
                                seekAction = "back"
                            } else {
                                playerViewModel.seekForward()
                                seekAction = "forward"
                            }
                            seekTimer = System.currentTimeMillis()
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val width = size.width
                            if (change.position.x < width / 2) {
                                activity?.window?.let { window ->
                                    val params = window.attributes
                                    val currentBrightness = if (params.screenBrightness < 0) 0.5f else params.screenBrightness
                                    val newBrightness = (currentBrightness - dragAmount.y / 500f).coerceIn(0.01f, 1.0f)
                                    params.screenBrightness = newBrightness
                                    window.attributes = params
                                    gestureInfo = "Brightness"
                                    gestureProgress = newBrightness
                                    gestureTimer = System.currentTimeMillis()
                                }
                            } else {
                                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val delta = if (dragAmount.y > 0) -1 else 1
                                val newVolume = (currentVolume + delta).coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                gestureInfo = "Volume"
                                gestureProgress = newVolume.toFloat() / maxVolume
                                gestureTimer = System.currentTimeMillis()
                            }
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    )
                }
        ) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        this.player = player
                        useController = false
                        // Disable internal subtitle view to keep video clean
                        subtitleView?.visibility = android.view.View.GONE
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // HUD: Brightness / Volume Progress Bar
            gestureInfo?.let { info ->
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                        .width(200.dp)
                        .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.medium)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (info == "Volume") Icons.Default.VolumeUp else Icons.Default.BrightnessMedium,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(info, color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = gestureProgress,
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color.White,
                        trackColor = Color.Gray.copy(alpha = 0.5f)
                    )
                }
            }

            // Double Tap Seek Animation Overlay
            seekAction?.let { action ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                    contentAlignment = if (action == "back") Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Column(
                        modifier = Modifier
                            .padding(40.dp)
                            .size(100.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (action == "back") Icons.Default.FastRewind else Icons.Default.FastForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(if (action == "back") "-5s" else "+5s", color = Color.White)
                    }
                }
            }

            // Custom Controls
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    // Top Bar (Back button and title)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        val currentMedia = playerViewModel.currentPlaylist.getOrNull(playerViewModel.currentMediaIndex)
                        Text(
                            text = currentMedia?.name ?: "Playing",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    }

                    // Center Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        IconButton(onClick = { 
                            playerViewModel.playPrevious()
                            lastInteractionTime = System.currentTimeMillis()
                        }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                        
                        IconButton(
                            onClick = { 
                                playerViewModel.togglePlayPause()
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                if (playerViewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(72.dp)
                            )
                        }

                        IconButton(onClick = { 
                            playerViewModel.playNext()
                            lastInteractionTime = System.currentTimeMillis()
                        }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                    }

                    // Bottom Seekbar, Time, and Fullscreen Toggle
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (isLandscape) 16.dp else 8.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Slider(
                            value = playerViewModel.currentPosition.toFloat(),
                            onValueChange = { 
                                playerViewModel.seekTo(it.toLong())
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            valueRange = 0f..playerViewModel.duration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Red,
                                activeTrackColor = Color.Red,
                                inactiveTrackColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Combined Time Display (Bottom-Left)
                            Text(
                                text = "${formatTime(playerViewModel.currentPosition)} / ${formatTime(playerViewModel.duration)}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                            
                            // Fullscreen Toggle Button (Bottom-Right)
                            IconButton(onClick = {
                                if (isLandscape) {
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                                lastInteractionTime = System.currentTimeMillis()
                            }) {
                                Icon(
                                    if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isLandscape) {
        // Landscape: Fullscreen Video Only
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    } else {
        // Portrait: Fixed Split-Screen (Top 40% Video, Bottom 60% Content)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Top Pane: Video Area (40%)
            Box(modifier = Modifier.fillMaxWidth().weight(0.4f)) {
                content()
            }
            
            // Bottom Pane: Content Area (60%)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(if (showPlaylistByMode) MaterialTheme.colorScheme.surface else Color(0xFF121212))
            ) {
                // Header with Toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showPlaylistByMode) "Playlist" else "Lyrics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (showPlaylistByMode) MaterialTheme.colorScheme.onSurface else Color.White
                    )
                    
                    TextButton(
                        onClick = { showPlaylistByMode = !showPlaylistByMode },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (showPlaylistByMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(if (showPlaylistByMode) "Switch to Lyrics" else "Switch to Playlist")
                    }
                }
                
                Divider(color = if (showPlaylistByMode) DividerDefaults.color else Color.White.copy(alpha = 0.1f))
                
                if (showPlaylistByMode) {
                    // Playlist View
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(playerViewModel.currentPlaylist.size) { index ->
                            val media = playerViewModel.currentPlaylist[index]
                            ListItem(
                                headlineContent = { Text(media.name) },
                                supportingContent = { Text(if (media.isVideo) "Video" else "Audio") },
                                leadingContent = {
                                    Icon(
                                        if (index == playerViewModel.currentMediaIndex) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (index == playerViewModel.currentMediaIndex) Color.Red else Color.Gray
                                    )
                                },
                                modifier = Modifier.clickable {
                                    playerViewModel.playAtIndex(index)
                                }
                            )
                        }
                    }
                } else {
                    // Lyrics View (Karaoke Style Scrolling)
                    if (playerViewModel.subtitleLines.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No lyrics available", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            state = subtitleListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 150.dp) // Adjusted for 60% pane
                        ) {
                            items(playerViewModel.subtitleLines.size) { index ->
                                val line = playerViewModel.subtitleLines[index]
                                val isActive = index == playerViewModel.currentSubtitleIndex
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp, horizontal = 24.dp)
                                        .clickable { playerViewModel.seekTo(line.start) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = line.text,
                                        style = if (isActive) {
                                            MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 22.sp
                                            )
                                        } else {
                                            MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 16.sp
                                            )
                                        },
                                        color = if (isActive) Color.White else Color.Gray.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Restore portrait if we were in landscape when leaving
            if (isLandscape) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
