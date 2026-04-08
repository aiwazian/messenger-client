/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

import com.aiwazian.messenger.R
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.PrivacySettings
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

    private val _sideEffect = MutableSharedFlow<SettingsPrivacySideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    init {
        loadValues()
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

    fun onDeleteAccountClick() {
        viewModelScope.launch {
            val createdAt = authRepository.getCurrentAccountCreatedAt()
            val currentTime = System.currentTimeMillis()
            val twentyFourHoursInMs = 24 * 60 * 60 * 1000L

            if (currentTime - createdAt < twentyFourHoursInMs) {
                vibrationManager.vibrate(VibrationPattern.Error)
                _sideEffect.emit(SettingsPrivacySideEffect.ShowSnackbar(R.string.delete_account_after_twenty_four_hours))
            } else {
                _sideEffect.emit(SettingsPrivacySideEffect.ShowDeleteBottomSheet)
            }
        }
    }

    fun onDeleteConfirmClick() {
        viewModelScope.launch {
            _sideEffect.emit(SettingsPrivacySideEffect.ShowDeleteDialog)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val result = authRepository.deleteMe()
            if (result.isSuccess) {
                _sideEffect.emit(SettingsPrivacySideEffect.NavigateToLogin)
            } else {
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
}
