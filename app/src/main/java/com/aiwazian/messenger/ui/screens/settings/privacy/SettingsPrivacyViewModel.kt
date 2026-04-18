/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
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
    
    private val _uiState = MutableStateFlow(SettingsPrivacyUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<SettingsPrivacySideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    suspend fun init() {
        try {
            privacyRepository.getPrivacySettings()?.let { settings ->
                _uiState.update { it.copy(privacy = settings) }
            }
        } catch (e: Exception) {
            Log.e(
                "SettingsPrivacyViewModel",
                "Ошибка при получении настроек конфиденциальности",
                e
            )
        }
    }
    
    private fun showDeleteBottomSheet() {
        _uiState.update { it.copy(showDeleteBottomSheet = true) }
    }
    
    fun hideDeleteBottomSheet() {
        _uiState.update { it.copy(showDeleteBottomSheet = false) }
    }
    
    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }
    
    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }
    
    fun onDeleteAccountClick() {
        viewModelScope.launch {
            val createdAt = authRepository.getCurrentAccountCreatedAt()
            val currentTime = System.currentTimeMillis()
            val twentyFourHoursInMs = 24 * 60 * 60 * 1000L
            
            if (currentTime - createdAt < twentyFourHoursInMs) {
                _sideEffect.emit(SettingsPrivacySideEffect.ShowSnackbar(R.string.delete_account_after_twenty_four_hours))
                vibrationManager.vibrate(VibrationPattern.Error)
            } else {
                showDeleteBottomSheet()
            }
        }
    }
    
    fun deleteAccount() {
        viewModelScope.launch {
            authRepository.deleteMe().onSuccess {
                _sideEffect.emit(SettingsPrivacySideEffect.NavigateToLogin)
            }.onFailure {
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
}
