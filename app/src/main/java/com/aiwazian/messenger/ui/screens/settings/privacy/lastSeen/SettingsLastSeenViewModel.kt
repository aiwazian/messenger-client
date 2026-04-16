/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.lastSeen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.repository.PrivacyRepository
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsLastSeenViewModel @Inject constructor(
    private val vibrationManager: VibrationManager,
    private val privacyRepository: PrivacyRepository
) : ViewModel() {

    private val _initialLevel = MutableStateFlow(PrivacyLevel.EVERYBODY)

    private val _currentLevel = MutableStateFlow(PrivacyLevel.EVERYBODY)
    val currentLevel = _currentLevel.asStateFlow()

    private val _showSaveButton = MutableStateFlow(false)
    val showSaveButton = _showSaveButton.asStateFlow()

    private val _effect = Channel<SettingsLastSeenEffect>()
    val effect = _effect.receiveAsFlow()

    fun vibrate(pattern: LongArray) {
        vibrationManager.vibrate(pattern)
    }

    fun init(initialValue: PrivacyLevel) {
        _initialLevel.update { initialValue }
        _currentLevel.update { initialValue }
        hideSaveButton()
    }

    fun selectValue(value: PrivacyLevel) {
        _currentLevel.update { value }

        if (_currentLevel.value == _initialLevel.value) {
            hideSaveButton()
        } else {
            showSaveButton()
        }
    }

    fun onSaveClick() {
        viewModelScope.launch {
            try {
                val success = privacyRepository.updateLastSeenPrivacy(_currentLevel.value)

                if (success) {
                    _effect.send(SettingsLastSeenEffect.Back)
                } else {
                    vibrate(VibrationPattern.Error)
                }
            } catch (e: Exception) {
                Log.e(
                    "SettingsLastSeenViewModel",
                    "Ошибка при отправке настроек конфиденциальности для времени захода",
                    e
                )
                vibrate(VibrationPattern.Error)
            }
        }
    }

    private fun showSaveButton() {
        _showSaveButton.update { true }
    }

    private fun hideSaveButton() {
        _showSaveButton.update { false }
    }
}
