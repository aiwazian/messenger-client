/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.socket

import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.push.PushRegistrar
import com.aiwazian.messenger.repository.ChatFolderRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.NotificationSettingsRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerSyncService @Inject constructor(
    private val webSocketClient: WebSocketClient,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val chatFolderRepository: ChatFolderRepository,
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val pushRegistrar: PushRegistrar
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val isStarted = AtomicBoolean(false)
    
    fun start() {
        if (isStarted.compareAndSet(false, true)) {
            observeConnection()
            pushRegistrar.ensureRegistered()
        }
        
        webSocketClient.connect()
    }
    
    private fun observeConnection() {
        scope.launch {
            webSocketClient.connectionState.collectLatest { state ->
                if (state != ConnectionState.CONNECTED) return@collectLatest
                
                SessionManager.loadSession()
                userRepository.fetchMe()
                chatRepository.refreshChats()
                chatFolderRepository.refreshFolders()
                notificationSettingsRepository.refresh()
                notificationSettingsRepository.refreshChatExceptions()
            }
        }
    }
}
