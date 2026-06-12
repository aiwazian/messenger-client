/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security.devices

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Session
import com.aiwazian.messenger.repository.AuthRepository
import com.aiwazian.messenger.repository.SessionRepository
import com.aiwazian.messenger.utils.UiText
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
class DevicesViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<DevicesSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    init {
        getSessions()
    }
    
    private fun getSessions() {
        viewModelScope.launch {
            try {
                sessionRepository.getAllSessions().onSuccess { sessions ->
                    _uiState.update { it.copy(sessions = sessions) }
                }
            } catch (e: Exception) {
                Log.e("DevicesViewModel", "Error getting sessions", e)
            }
        }
    }
    
    fun vibrate() {
        vibrationManager.vibrate(VibrationPattern.Error)
    }
    
    fun openSession(session: Session) {
        _uiState.update {
            it.copy(
                openedSession = session,
                showSessionInfoBottomSheet = true
            )
        }
    }
    
    fun closeSessionInfo() {
        _uiState.update { it.copy(showSessionInfoBottomSheet = false) }
    }
    
    fun showTerminateSessionDialog() {
        viewModelScope.launch {
            if (!isSessionAgeEnough()) {
                closeSessionInfo()
                return@launch
            }
            _uiState.update { it.copy(showTerminateSessionDialog = true) }
        }
    }
    
    fun hideTerminateSessionDialog() {
        _uiState.update { it.copy(showTerminateSessionDialog = false) }
    }
    
    fun showTerminateAllOtherSessionsDialog() {
        viewModelScope.launch {
            if (!isSessionAgeEnough()) return@launch
            _uiState.update { it.copy(showTerminateAllOtherSessionsDialog = true) }
        }
    }
    
    fun hideTerminateAllOtherSessionsDialog() {
        _uiState.update { it.copy(showTerminateAllOtherSessionsDialog = false) }
    }
    
    fun terminateSession() {
        val sessionId = _uiState.value.openedSession?.id ?: return
        viewModelScope.launch {
            hideTerminateSessionDialog()
            closeSessionInfo()
            try {
                val success = sessionRepository.deleteSession(sessionId)
                if (success) {
                    _uiState.update { state ->
                        state.copy(sessions = state.sessions.filter { it.id != sessionId })
                    }
                    _sideEffect.emit(DevicesSideEffect.ShowSnackbar(UiText.DynamicString("Сессия завершена")))
                } else {
                    handleError("Не удалось завершить сессию")
                }
            } catch (_: Exception) {
                handleError("Не удалось завершить сессию")
            }
        }
    }
    
    fun terminateAllOtherSessions() {
        viewModelScope.launch {
            hideTerminateAllOtherSessionsDialog()
            sessionRepository.deleteAllSessions().onSuccess {
                _uiState.update { state ->
                    state.copy(sessions = state.sessions.filter { it.isCurrent })
                }
                _sideEffect.emit(DevicesSideEffect.ShowSnackbar(UiText.DynamicString("Сессии завершены")))
            }.onFailure {
                handleError("Не удалось завершить сессии")
            }
        }
    }
    
    private suspend fun isSessionAgeEnough(): Boolean {
        val createdAt = authRepository.getCurrentAccountCreatedAt()
        val currentTime = System.currentTimeMillis()
        val twentyFourHoursInMs = 24 * 60 * 60 * 1000L
        
        if (currentTime - createdAt < twentyFourHoursInMs) {
            vibrationManager.vibrate(VibrationPattern.Error)
            _sideEffect.emit(
                DevicesSideEffect.ShowSnackbar(
                    UiText.StringResource(R.string.terminate_session_session_age)
                )
            )
            return false
        }
        return true
    }
    
    private suspend fun handleError(message: String) {
        vibrationManager.vibrate(VibrationPattern.Error)
        _sideEffect.emit(DevicesSideEffect.ShowSnackbar(UiText.DynamicString(message)))
    }
}
