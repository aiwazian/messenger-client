/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.push

import android.util.Log
import com.aiwazian.messenger.database.dao.AccountDao
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class PushService : FirebaseMessagingService() {
    
    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    @Inject
    lateinit var pushRegistrar: PushRegistrar
    
    @Inject
    lateinit var accountDao: AccountDao
    
    /**
     * Замена onNewToken: вместо registration token теперь приходит Firebase
     * Installation ID, и именно по нему адресуется уведомление.
     *
     * Колбек вызывается после register(), при смене FID (переустановка, очистка данных,
     * восстановление на другом устройстве) и при плановой синхронизации SDK.
     * Сохранением занимается PushRegistrar: сервис к ответу сервера может быть уже убит.
     */
    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        
        Log.d("PushService", "New installation id")
        pushRegistrar.onRegistered(installationId)
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val data = message.data
        val title = data["title"] ?: return
        val body = data["body"] ?: return
        val chatId = data["chatId"]?.toLongOrNull() ?: return
        
        if (!isForCurrentAccount(data["userId"]?.toLongOrNull())) {
            Log.d("PushService", "Notification for another account, skipped")
            return
        }
        
        /*
         * Время отправки берётся с сервера, а не из момента доставки: пуш мог
         * пролежать в очереди FCM час, и без этого старое сообщение выглядело бы
         * только что написанным. Старый сервер поля не пришлёт — тогда прежнее
         * поведение.
         */
        val sendTime = data["sendTime"]?.toLongOrNull() ?: System.currentTimeMillis()
        
        notificationHelper.showMessageNotification(
            chatId = chatId,
            title = title,
            body = body,
            sendTime = sendTime
        )
    }
    
    /**
     * Уведомление показывается только активному аккаунту. Сервер держит FID только
     * у активной сессии, но если переключение произошло без сети или пуш уже был
     * в полёте, чужое уведомление всё равно может доехать — здесь оно отбрасывается.
     *
     * onMessageReceived работает на фоновом потоке FCM, а решение нужно принять до
     * выхода из метода, поэтому один короткий запрос к базе ждём на месте.
     */
    private fun isForCurrentAccount(recipientId: Long?): Boolean {
        // Нет получателя — значит сервер старее этого клиента: показываем как раньше.
        recipientId ?: return true
        
        val currentUserId = runBlocking { accountDao.getCurrentAccount()?.userId }
        return currentUserId == recipientId
    }
}
