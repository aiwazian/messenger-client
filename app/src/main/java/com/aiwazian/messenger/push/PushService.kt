/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.push

import android.app.NotificationManager
import android.util.Log
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.rustore.sdk.pushclient.RuStorePushClient
import ru.rustore.sdk.pushclient.messaging.model.RemoteMessage
import ru.rustore.sdk.pushclient.messaging.service.RuStoreMessagingService
import javax.inject.Inject

@AndroidEntryPoint
class PushService : RuStoreMessagingService() {
    
    @Inject
    lateinit var accountDao: AccountDao
    
    @Inject
    lateinit var sessionRepository: SessionRepository
    
    lateinit var notificationManager: NotificationManager
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        notificationManager = getSystemService(NotificationManager::class.java)
        
        scope.launch {
            accountDao.getCurrentAccount()?.let { account ->
                RuStorePushClient.getToken().addOnSuccessListener { token ->
                    if (account.fcmToken.isNullOrBlank()) {
                        scope.launch {
                            sessionRepository.updateFcmToken(token).onSuccess {
                                accountDao.update(account.copy(fcmToken = token))
                            }.onFailure {
                                Log.e("PushService", "Error saving token", it)
                            }
                        }
                    }
                    Log.d("PushService", token)
                }.addOnFailureListener { th ->
                    Log.e("PushService", "Error getting token", th)
                }
            }
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("PushService", "New token $token")
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("PushService", message.notification.toString())
        
        val data = message.data
        val title = data["title"] ?: return
        val body = data["body"] ?: return
        val chatId = data["chatId"]?.toLong() ?: return
        
        NotificationHelper.showMessageNotification(this, chatId, title, body)
    }
}
