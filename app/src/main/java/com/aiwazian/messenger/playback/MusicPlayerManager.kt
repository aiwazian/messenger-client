/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
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
class MusicPlayerManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val _state = MutableStateFlow(MusicPlayerState())
    val state = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionJob: Job? = null

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

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
                        ctrl.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                    _state.update { it.copy(durationMs = duration.toInt()) }
                }

                Player.STATE_ENDED -> {
                    positionJob?.cancel()
                    ctrl.clearMediaItems()
                    _state.update { MusicPlayerState() }
                }

                else -> {}
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Player error", error)
            stop()
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
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

    fun play(track: MusicTrack, startPositionMs: Int = 0) {
        val ctrl = controller
        if (ctrl == null) {
            connect()
            controllerFuture?.addListener(
                { play(track, startPositionMs) }, MoreExecutors.directExecutor()
            )
            return
        }
        playInternal(ctrl, track, startPositionMs)
    }

    private fun playInternal(ctrl: MediaController, track: MusicTrack, startPositionMs: Int) {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setIsBrowsable(false)
            .setIsPlayable(true)

        track.artist?.let {
            metadataBuilder.setArtist(it)
            metadataBuilder.setSubtitle(it)
        }
        track.artworkUri?.let { metadataBuilder.setArtworkUri(it) }
        track.artworkData?.let {
            metadataBuilder.setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }

        val mediaItem = MediaItem.Builder()
            .setUri(track.uri)
            .setMediaId(track.fileId)
            .setMediaMetadata(metadataBuilder.build())
            .build()

        ctrl.setMediaItem(mediaItem, startPositionMs.toLong())
        ctrl.prepare()
        ctrl.play()
        _state.update {
            MusicPlayerState(
                fileId = track.fileId,
                title = track.title,
                artist = track.artist,
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
        _state.update { MusicPlayerState() }
    }

    fun release() {
        positionJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        _state.update { MusicPlayerState() }
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
        private const val TAG = "MusicPlayerManager"
    }
}
