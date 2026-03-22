/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.security

import android.util.Log
import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.domain.Session
import com.aiwazian.messenger.repository.SessionRepository
import com.aiwazian.messenger.utils.DialogController
import com.aiwazian.messenger.utils.DeviceInfoProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val deviceInfoProvider: DeviceInfoProvider,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    
    private val _currentSession = MutableStateFlow(Session())
    val currentSession = _currentSession.asStateFlow()
    
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions = _sessions.asStateFlow()
    
    val terminateSessionDialog = DialogController()
    
    val sessionInfoDialog = DialogController()
    
    private var _confirmDialogAction: (suspend () -> Unit)? = null
    
    private val _openedSession = MutableStateFlow(Session())
    
    val openedSession = _openedSession.asStateFlow()
    
    init {
        val deviceName = deviceInfoProvider.getDeviceName()
        
        _currentSession.update { it.copy(deviceModel = deviceName) }
    }
    
    suspend fun terminateSession(sessionId: Int) {
        try {
            val result = sessionRepository.deleteSession(sessionId)
            
            if (result) {
                val sessionList = _sessions.value.filter { it.id != sessionId }
                
                _sessions.update { sessionList }
            }
        } catch (e: Exception) {
            Log.e(
                "DeviceSettings",
                "Ошибка при отключении сессии",
                e
            )
        }
    }
    
    suspend fun getSessions() {
        try {
            val result = sessionRepository.getAllSessions()
            
            if (result.isNotEmpty()) {
                _sessions.update { result }
            }
        } catch (e: Exception) {
            Log.e(
                "DeviceSettings",
                "Ошибка при получении сессий",
                e
            )
        }
    }
    
    suspend fun terminateAllOtherSessions() {
        try {
            val result = sessionRepository.deleteAllSessions()
            
            if (result) {
                _sessions.update { emptyList() }
            }
        } catch (e: Exception) {
            Log.e(
                "DeviceSettings",
                "${e.message}"
            )
        }
    }
    
    fun openSession(sessionId: Int) {
        if (sessionId == 0) {
            _openedSession.update {
                Session(
                    0,
                    0,
                    deviceInfoProvider.getDeviceName(),
                    deviceInfoProvider.getOsVersion()
                )
            }
        } else {
            _openedSession.update { _sessions.value.first { it.id == sessionId } }
        }
    }
    
    fun setConfirmDialogAction(action: suspend () -> Unit) {
        _confirmDialogAction = action
    }
    
    fun getConfirmDialogAction() = _confirmDialogAction
}


