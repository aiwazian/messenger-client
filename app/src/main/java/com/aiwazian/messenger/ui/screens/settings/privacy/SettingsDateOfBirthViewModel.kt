/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

import android.util.Log
import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.repository.PrivacyRepository
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.utils.VibrationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SettingsDateOfBirthViewModel @Inject constructor(
    private val vibrationManager: VibrationManager,
    private val privacyRepository: PrivacyRepository
) : ViewModel() {

    private val _initialLevel = MutableStateFlow(PrivacyLevel.Everybody)

    private val _currentLevel = MutableStateFlow(PrivacyLevel.Everybody)
    val currentLevel = _currentLevel.asStateFlow()

    private val _showSaveButton = MutableStateFlow(false)
    val showSaveButton = _showSaveButton.asStateFlow()

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

    suspend fun trySave(): Boolean {
        try {
            val success = privacyRepository.updateDateOfBirthPrivacy(_currentLevel.value.ordinal)

            return success
        } catch (e: Exception) {
            Log.e(
                "SettingsDateOfBirthViewModel",
                "Ошибка при отправке настроек конфиденциальности для раздела даты рождения",
                e
            )

            return false
        }
    }

    private fun showSaveButton() {
        _showSaveButton.update { true }
    }

    private fun hideSaveButton() {
        _showSaveButton.update { false }
    }
}


