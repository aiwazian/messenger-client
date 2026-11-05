/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.common

import com.aiwazian.messenger.utils.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MediaSettingsController(
    private val scope: CoroutineScope,
    private val dataStoreManager: DataStoreManager
) {
    fun observeVideoLooping(onChange: (Boolean) -> Unit) {
        scope.launch {
            dataStoreManager.getVideoLooping().collect { isLooping ->
                onChange(isLooping)
            }
        }
    }

    fun observeVideoPlaybackSpeed(onChange: (Float) -> Unit) {
        scope.launch {
            dataStoreManager.getVideoPlaybackSpeed().collect { speed ->
                onChange(speed)
            }
        }
    }

    fun saveVideoLooping(isLooping: Boolean) {
        scope.launch { dataStoreManager.saveVideoLooping(isLooping) }
    }

    fun saveVideoPlaybackSpeed(speed: Float) {
        scope.launch { dataStoreManager.saveVideoPlaybackSpeed(speed) }
    }

    fun persistKeyboardHeight(height: Float) {
        scope.launch {
            val savedHeight = dataStoreManager.getKeyboardHeight().first()

            if (savedHeight != height) {
                dataStoreManager.saveKeyboardHeight(height)
            }
        }
    }
}
