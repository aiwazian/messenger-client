/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.PrivacySettings
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.repository.AuthRepository
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
class SettingsPrivacyViewModel @Inject constructor(
    private val privacyRepository: PrivacyRepository,
    private val authRepository: AuthRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {

    private val _privacySettings = MutableStateFlow(PrivacySettings())
    val privacySettings = _privacySettings.asStateFlow()

    private val _deleteSuccess = MutableSharedFlow<Unit>()
    val deleteSuccess = _deleteSuccess.asSharedFlow()

    init {
        loadValues()
    }

    fun updateBioValue(privacyLevel: PrivacyLevel) {
        _privacySettings.update { it.copy(bio = privacyLevel) }
    }

    fun updateDateOfBirthValue(privacyLevel: PrivacyLevel) {
        _privacySettings.update { it.copy(dateOfBirth = privacyLevel) }
    }

    fun loadValues() {
        viewModelScope.launch {
            try {
                val myPrivacy = privacyRepository.getPrivacySettings()

                if (myPrivacy != null) {
                    _privacySettings.update { myPrivacy }
                }
            } catch (e: Exception) {
                Log.e(
                    "SettingsPrivacyViewModel",
                    "Ошибка при получении настроек конфиденциальности",
                    e
                )
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val result = authRepository.deleteMe()
            if (result.isSuccess) {
                _deleteSuccess.emit(Unit)
            } else {
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
}
