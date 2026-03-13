package com.aiwazian.messenger.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.data.SessionInfo
import com.aiwazian.messenger.services.DeviceHelper
import com.aiwazian.messenger.services.DialogController
import com.aiwazian.messenger.services.SessionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val deviceHelper: DeviceHelper,
    private val sessionService: SessionService
) : ViewModel() {
    
    private val _currentSessionInfo = MutableStateFlow(SessionInfo())
    val currentSession = _currentSessionInfo.asStateFlow()
    
    private val _sessions = MutableStateFlow<Set<SessionInfo>>(emptySet())
    val sessions = _sessions.asStateFlow()
    
    val terminateSessionDialog = DialogController()
    
    val sessionInfoDialog = DialogController()
    
    private var _confirmDialogAction: (suspend () -> Unit)? = null
    
    private val _openedSessionInfo = MutableStateFlow(SessionInfo())
    
    val openedSession = _openedSessionInfo.asStateFlow()
    
    init {
        val deviceName = deviceHelper.getDeviceName()
        
        _currentSessionInfo.update { session ->
            session.deviceName = deviceName
            session
        }
    }
    
    suspend fun terminateSession(sessionId: Int) {
        try {
            val isTerminated = sessionService.terminateSession(sessionId)
            
            if (isTerminated) {
                val sessionList = _sessions.value.filter { it.id == sessionId }.toSet()
                
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
            val sessions = sessionService.getSessions()
            
            if (sessions?.isNotEmpty() == true) {
                _sessions.update { sessions.toSet() }
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
            val isTerminated = sessionService.terminateAllSessions()
            
            if (isTerminated) {
                _sessions.update { emptySet() }
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
            val deviceName = deviceHelper.getDeviceName()
            
            _openedSessionInfo.update {
                SessionInfo(
                    0,
                    deviceName,
                    ""
                )
            }
        } else {
            _openedSessionInfo.update { _sessions.value.first { it.id == sessionId } }
        }
    }
    
    fun setConfirmDialogAction(action: suspend () -> Unit) {
        _confirmDialogAction = action
    }
    
    fun getConfirmDialogAction() = _confirmDialogAction
}