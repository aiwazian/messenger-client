/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
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
    
    private var onCompletionListener: ((finishedFileId: String) -> Unit)? = null
    
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
                    val finishedId = _state.value.currentFileId
                    positionJob?.cancel()
                    _state.update {
                        it.copy(
                            currentFileId = null,
                            isPlaying = false,
                            positionMs = 0
                        )
                    }
                    ctrl.clearMediaItems()
                    if (finishedId != null) {
                        onCompletionListener?.invoke(finishedId)
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
    
    fun setOnCompletionListener(listener: ((finishedFileId: String) -> Unit)?) {
        onCompletionListener = listener
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
    
    fun play(uri: Uri, fileId: String, title: String, startPositionMs: Int = 0) {
        val ctrl = controller
        if (ctrl == null) {
            connect()
            controllerFuture?.addListener(
                {
                    playInternal(uri, fileId, title, startPositionMs)
                }, MoreExecutors.directExecutor()
            )
            return
        }
        playInternal(uri, fileId, title, startPositionMs)
    }
    
    private fun playInternal(uri: Uri, fileId: String, title: String, startPositionMs: Int) {
        val ctrl = controller ?: return
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(fileId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
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
        _state.update { VoicePlayerState() }
    }
    
    fun release() {
        positionJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
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
    }
}
