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

/**
 * Держит соединение с сервером и подтягивает состояние после каждого подключения.
 *
 * Раньше и то и другое жило в MainViewModel, то есть зависело от того, что открылся
 * именно список чатов. NavDisplay компонует только верхний экран бэкстека, поэтому
 * запуск сразу в чат — ярлыком с рабочего стола или тапом по уведомлению — оставлял
 * приложение без сокета: сообщения, профиль чата и папки читались из Room и больше
 * никогда не обновлялись.
 *
 * Синхронизация висит на состоянии соединения, а не на старте: после обрыва и
 * переподключения состояние подтягивается заново, и экраны в этом не участвуют.
 *
 * Список онлайна обновляет RealtimeEventSyncService — там это уже сделано для всего
 * приложения, и дублировать вызов здесь незачем.
 */
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
    
    /**
     * Вызывается из MainActivity, а не из Application: для подключения нужен токен, а он
     * появляется только после SessionManager.loadSession(). Повторные вызовы безопасны —
     * активити пересоздаётся при смене темы или повороте, а подписка нужна одна.
     */
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
