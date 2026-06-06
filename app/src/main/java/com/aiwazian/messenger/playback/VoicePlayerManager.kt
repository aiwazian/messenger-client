/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.playback

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class VoicePlayerManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    
    private val _state = MutableStateFlow(VoicePlayerState())
    val state = _state.asStateFlow()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionJob: Job? = null
    
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    
    private var queue: List<VoiceQueueItem> = emptyList()
    private var currentIndex: Int = -1
    
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionTracking() else positionJob?.cancel()
        }
        
        override fun onPlaybackStateChanged(playbackState: Int) {
            val ctrl = controller ?: return
            when (playbackState) {
                Player.STATE_READY -> {
                    val duration =
                        ctrl.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET } ?: 0L
                    _state.update { it.copy(durationMs = duration.toInt()) }
                }
                
                Player.STATE_ENDED -> {
                    val finishedFileId = _state.value.currentFileId
                    positionJob?.cancel()
                    ctrl.clearMediaItems()
                    if (!playNextInQueue(finishedFileId)) {
                        _state.update {
                            it.copy(
                                currentFileId = null,
                                isPlaying = false,
                                positionMs = 0
                            )
                        }
                    }
                }
                
                else -> {}
            }
        }
        
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(TAG, "Player error", error)
            stop()
        }
    }
    
    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(
            context,
            ComponentName(context, VoicePlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    controller = future.get()
                    controller?.addListener(playerListener)
                }.onFailure { Log.e(TAG, "Failed to connect to media session", it) }
            }, MoreExecutors.directExecutor()
        )
    }
    
    fun play(
        queue: List<VoiceQueueItem>,
        fileId: String,
        startPositionMs: Int = 0
    ) {
        val index = queue.indexOfFirst { it.fileId == fileId }
        if (index < 0) return
        val item = queue[index]
        val uri = item.uri ?: return
        this.queue = queue
        this.currentIndex = index
        playMediaItem(uri, item.fileId, item.title, item.subtitle, item.artworkUri, startPositionMs)
    }
    
    fun updateQueue(newQueue: List<VoiceQueueItem>) {
        val currentFileId = _state.value.currentFileId ?: return
        val newIndex = newQueue.indexOfFirst { it.fileId == currentFileId }
        if (newIndex < 0) return
        queue = newQueue
        currentIndex = newIndex
    }
    
    private fun playNextInQueue(finishedFileId: String?): Boolean {
        if (finishedFileId == null) return false
        val baseIndex = queue.indexOfFirst { it.fileId == finishedFileId }
            .takeIf { it >= 0 } ?: currentIndex
        val nextIndex = baseIndex + 1
        val next = queue.getOrNull(nextIndex) ?: return false
        val nextUri = next.uri ?: return false
        currentIndex = nextIndex
        playMediaItem(nextUri, next.fileId, next.title, next.subtitle, next.artworkUri, 0)
        return true
    }
    
    private fun playMediaItem(
        uri: Uri,
        fileId: String,
        title: String,
        subtitle: String?,
        artworkUri: Uri?,
        startPositionMs: Int
    ) {
        val ctrl = controller
        if (ctrl == null) {
            connect()
            controllerFuture?.addListener(
                {
                    playMediaItemInternal(uri, fileId, title, subtitle, artworkUri, startPositionMs)
                }, MoreExecutors.directExecutor()
            )
            return
        }
        playMediaItemInternal(uri, fileId, title, subtitle, artworkUri, startPositionMs)
    }
    
    private fun playMediaItemInternal(
        uri: Uri,
        fileId: String,
        title: String,
        subtitle: String?,
        artworkUri: Uri?,
        startPositionMs: Int
    ) {
        val ctrl = controller ?: return
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(false)
            .setIsPlayable(true)
        if (subtitle != null) {
            metadataBuilder.setSubtitle(subtitle)
        }
        if (artworkUri != null) {
            metadataBuilder.setArtworkUri(artworkUri)
        }
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(fileId)
            .setMediaMetadata(metadataBuilder.build())
            .build()
        ctrl.setMediaItem(mediaItem, startPositionMs.toLong())
        ctrl.prepare()
        ctrl.play()
        _state.update {
            it.copy(
                currentFileId = fileId,
                isPlaying = true,
                positionMs = startPositionMs,
                durationMs = 0
            )
        }
        
        if (artworkUri != null) {
            loadArtwork(artworkUri, fileId)
        }
    }
    
    private fun loadArtwork(artworkUri: Uri, fileId: String) {
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { loadArtworkBytes(artworkUri) }.getOrNull()
            } ?: return@launch
            
            val ctrl = controller ?: return@launch
            if (_state.value.currentFileId != fileId) return@launch
            
            val current = ctrl.currentMediaItem ?: return@launch
            val updatedMetadata = current.mediaMetadata.buildUpon()
                .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                .build()
            val updatedItem = current.buildUpon().setMediaMetadata(updatedMetadata).build()
            runCatching {
                ctrl.replaceMediaItem(ctrl.currentMediaItemIndex, updatedItem)
            }.onFailure { Log.e(TAG, "Failed to replace media item with artwork", it) }
        }
    }
    
    private suspend fun loadArtworkBytes(uri: Uri): ByteArray? {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(ARTWORK_SIZE_PX)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request)
        if (result !is SuccessResult) return null
        val bitmap = result.drawable.toBitmap()
        val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        return ByteArrayOutputStream().use { stream ->
            safeBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
    }
    
    fun pause() {
        controller?.pause()
    }
    
    fun resume() {
        controller?.play()
    }
    
    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }
    
    fun seekTo(positionMs: Int) {
        controller?.seekTo(positionMs.toLong())
        _state.update { it.copy(positionMs = positionMs) }
    }
    
    fun stop() {
        positionJob?.cancel()
        controller?.let { ctrl ->
            runCatching {
                ctrl.stop()
                ctrl.clearMediaItems()
            }
        }
        queue = emptyList()
        currentIndex = -1
        _state.update { VoicePlayerState() }
    }
    
    fun release() {
        positionJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        queue = emptyList()
        currentIndex = -1
        _state.update { VoicePlayerState() }
    }
    
    private fun startPositionTracking() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                val ctrl = controller ?: break
                val pos = ctrl.currentPosition.toInt()
                _state.update { it.copy(positionMs = pos) }
                delay(50.milliseconds)
            }
        }
    }
    
    companion object {
        private const val TAG = "VoicePlayerManager"
        private const val ARTWORK_SIZE_PX = 512
    }
}
