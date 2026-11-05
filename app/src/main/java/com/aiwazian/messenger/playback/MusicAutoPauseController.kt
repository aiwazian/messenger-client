/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.playback

import android.content.Context
import com.aiwazian.messenger.utils.DataStoreManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicAutoPauseController @Inject constructor(
    private val musicPlayerManager: MusicPlayerManager,
    private val dataStoreManager: DataStoreManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val activeReasons = mutableSetOf<Reason>()
    private var isPausedByController = false

    fun onRecordingStarted() = pause(Reason.RECORDING, dataStoreManager.getPauseMusicOnRecord())

    fun onRecordingStopped() = release(Reason.RECORDING)

    fun onMediaPlaybackStarted() = pause(Reason.MEDIA, dataStoreManager.getPauseMusicOnMedia())

    fun onMediaPlaybackStopped() = release(Reason.MEDIA)

    private fun pause(reason: Reason, enabled: Flow<Boolean>) {
        scope.launch {
            if (!enabled.first()) return@launch
            if (!activeReasons.add(reason)) return@launch

            if (musicPlayerManager.state.value.isPlaying) {
                isPausedByController = true
                musicPlayerManager.pause()
            }
        }
    }

    private fun release(reason: Reason) {
        scope.launch {
            activeReasons.remove(reason)
            if (activeReasons.isNotEmpty()) return@launch
            if (!isPausedByController) return@launch

            isPausedByController = false
            musicPlayerManager.resume()
        }
    }

    private enum class Reason {
        RECORDING,
        MEDIA
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MusicAutoPauseEntryPoint {
    fun musicAutoPauseController(): MusicAutoPauseController
}

fun Context.musicAutoPauseController(): MusicAutoPauseController =
    EntryPointAccessors.fromApplication(
        applicationContext,
        MusicAutoPauseEntryPoint::class.java
    ).musicAutoPauseController()
