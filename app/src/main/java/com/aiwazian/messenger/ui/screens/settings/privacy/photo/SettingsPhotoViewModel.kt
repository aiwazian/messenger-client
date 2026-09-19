/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.photo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.PrivacyExceptions
import com.aiwazian.messenger.enums.PrivacyExceptionKind
import com.aiwazian.messenger.enums.PrivacyField
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.repository.PrivacyRepository
import com.aiwazian.messenger.ui.screens.settings.privacy.exceptions.PrivacyExceptionSelection
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsPhotoViewModel @Inject constructor(
    private val vibrationManager: VibrationManager,
    private val privacyRepository: PrivacyRepository
) : ViewModel() {

    private val _initialLevel = MutableStateFlow(PrivacyLevel.EVERYBODY)

    private val _currentLevel = MutableStateFlow(PrivacyLevel.EVERYBODY)
    val currentLevel = _currentLevel.asStateFlow()

    private val _initialExceptions = MutableStateFlow<PrivacyExceptions?>(null)

    private val _currentExceptions = MutableStateFlow<PrivacyExceptions?>(null)
    val currentExceptions = _currentExceptions.asStateFlow()

    private val _showSaveButton = MutableStateFlow(false)
    val showSaveButton = _showSaveButton.asStateFlow()

    private val _effect = MutableSharedFlow<SettingsPhotoEffect>()
    val effect = _effect.asSharedFlow()

    fun vibrate(pattern: LongArray) {
        vibrationManager.vibrate(pattern)
    }

    fun init(initialValue: PrivacyLevel) {
        _initialLevel.update { initialValue }
        _currentLevel.update { initialValue }
        hideSaveButton()
        loadExceptions()
    }

    fun selectValue(value: PrivacyLevel) {
        _currentLevel.update { value }
        updateSaveButtonVisibility()
    }

    fun applyExceptionSelection(selection: PrivacyExceptionSelection) {
        if (selection.field != PrivacyField.PROFILE_PHOTO) {
            return
        }

        _currentExceptions.update { current ->
            val base = current ?: PrivacyExceptions()
            when (selection.kind) {
                PrivacyExceptionKind.ALWAYS_SHOW -> base.copy(alwaysShow = selection.userIds.toSet())
                PrivacyExceptionKind.ALWAYS_HIDE -> base.copy(alwaysHide = selection.userIds.toSet())
            }
        }
        updateSaveButtonVisibility()
    }

    fun onSaveClick() {
        viewModelScope.launch {
            try {
                privacyRepository.updateProfilePhotoPrivacy(
                    _currentLevel.value,
                    _currentExceptions.value
                ).onSuccess {
                    _effect.emit(SettingsPhotoEffect.Back)
                }.onFailure {
                    vibrate(VibrationPattern.Error)
                }
            } catch (e: Exception) {
                Log.e(
                    "SettingsPhotoViewModel",
                    "Ошибка при отправке настроек конфиденциальности для фото профиля",
                    e
                )
                vibrate(VibrationPattern.Error)
            }
        }
    }

    private fun loadExceptions() {
        viewModelScope.launch {
            privacyRepository.getPrivacySettings().onSuccess { settings ->
                if (_currentExceptions.value != null) return@onSuccess

                val exceptions = settings.exceptionsFor(PrivacyField.PROFILE_PHOTO)
                _initialExceptions.update { exceptions }
                _currentExceptions.update { exceptions }
            }
        }
    }

    private fun updateSaveButtonVisibility() {
        val hasChanges = _currentLevel.value != _initialLevel.value ||
                exceptionsChanged()
        _showSaveButton.update { hasChanges }
    }

    private fun exceptionsChanged(): Boolean {
        val current = _currentExceptions.value ?: return false
        return current != _initialExceptions.value
    }

    private fun hideSaveButton() {
        _showSaveButton.update { false }
    }
}
