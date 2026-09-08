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

@HiltViewModel
class SettingsForwardedProfileViewModel @Inject constructor(
    private val vibrationManager: VibrationManager,
    private val privacyRepository: PrivacyRepository
) : ViewModel() {
    
    private val _initialLevel = MutableStateFlow(PrivacyLevel.EVERYBODY)
    
    private val _currentLevel = MutableStateFlow(PrivacyLevel.EVERYBODY)
    val currentLevel = _currentLevel.asStateFlow()
    
    private val _initialForwardAndCopyLevel = MutableStateFlow(PrivacyLevel.EVERYBODY)
    
    private val _currentForwardAndCopyLevel = MutableStateFlow(PrivacyLevel.EVERYBODY)
    val currentForwardAndCopyLevel = _currentForwardAndCopyLevel.asStateFlow()
    
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
        loadForwardAndCopyLevel()
    }
    
    fun selectValue(value: PrivacyLevel) {
        _currentLevel.update { value }
        updateSaveButtonVisibility()
    }
    
    fun selectForwardAndCopyValue(value: PrivacyLevel) {
        _currentForwardAndCopyLevel.update { value }
        updateSaveButtonVisibility()
    }
    
    fun onSaveClick() {
        viewModelScope.launch {
            try {
                privacyRepository.updateForwardingPrivacy(
                    forwardedProfile = _currentLevel.value,
                    forwardAndCopy = _currentForwardAndCopyLevel.value
                ).onSuccess {
                    _effect.emit(SettingsForwardedProfileEffect.Back)
                }.onFailure {
                    vibrate(VibrationPattern.Error)
                }
            } catch (e: Exception) {
                Log.e(
                    "SettingsForwardedProfileViewModel",
                    "Failed to update message forwarding privacy settings",
                    e
                )
                vibrate(VibrationPattern.Error)
            }
        }
    }
    
    private fun loadForwardAndCopyLevel() {
        viewModelScope.launch {
            privacyRepository.getPrivacySettings().onSuccess { settings ->
                if (_showSaveButton.value) return@onSuccess
                _initialForwardAndCopyLevel.update { settings.forwardAndCopy }
                _currentForwardAndCopyLevel.update { settings.forwardAndCopy }
            }
        }
    }
    
    private fun updateSaveButtonVisibility() {
        val hasChanges = _currentLevel.value != _initialLevel.value ||
                _currentForwardAndCopyLevel.value != _initialForwardAndCopyLevel.value
        _showSaveButton.update { hasChanges }
    }
    
    private fun hideSaveButton() {
        _showSaveButton.update { false }
    }
}
