/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.forwardedProfile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.repository.PrivacyRepository
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

/**
 * Кто может перейти в мой профиль по заголовку «Переслано от».
 *
 * Поведение повторяет остальные экраны конфиденциальности: выбор показывает кнопку
 * сохранения, сохранение шлёт PATCH и закрывает экран.
 */
@HiltViewModel
class SettingsForwardedProfileViewModel @Inject constructor(
    private val vibrationManager: VibrationManager,
    private val privacyRepository: PrivacyRepository
) : ViewModel() {
    
    private val _initialLevel = MutableStateFlow(PrivacyLevel.EVERYBODY)
    
    private val _currentLevel = MutableStateFlow(PrivacyLevel.EVERYBODY)
    val currentLevel = _currentLevel.asStateFlow()
    
    private val _showSaveButton = MutableStateFlow(false)
    val showSaveButton = _showSaveButton.asStateFlow()
    
    private val _effect = MutableSharedFlow<SettingsForwardedProfileEffect>()
    val effect = _effect.asSharedFlow()
    
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
                privacyRepository.updateForwardedProfilePrivacy(_currentLevel.value).onSuccess {
                    _effect.emit(SettingsForwardedProfileEffect.Back)
                }.onFailure {
                    vibrate(VibrationPattern.Error)
                }
            } catch (e: Exception) {
                Log.e(
                    "SettingsForwardedProfileViewModel",
                    "Ошибка при отправке настроек конфиденциальности для пересылки сообщений",
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
