/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.push

import android.util.Log
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.repository.SessionRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Держит на сервере актуальный Firebase Installation ID (FID) текущей сессии.
 *
 * Новый API FCM не отдаёт идентификатор в ответ на вызов: register() лишь запускает
 * регистрацию, а сам FID приходит в PushService.onRegistered. Поэтому «вытянуть» его по
 * требованию, как раньше делал FirebaseMessaging.getInstance().token, больше нельзя,
 * и отправка живёт в синглтоне, который переживает и сервис, и ViewModel.
 */
@Singleton
class PushRegistrar @Inject constructor(
    private val accountDao: AccountDao,
    private val sessionRepository: SessionRepository
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /** onRegistered и повторный register() могут сойтись: PATCH должен быть один. */
    private val mutex = Mutex()
    
    /**
     * Просит SDK зарегистрировать установку. onRegistered придёт даже если установка
     * уже зарегистрирована — это и нужно после входа в аккаунт: на старте приложения
     * колбек мог сработать, когда сессии ещё не было и отправлять FID было некуда.
     */
    fun ensureRegistered() {
        FirebaseMessaging.getInstance().register().addOnFailureListener { e ->
            Log.e(TAG, "Register failed", e)
        }
    }
    
    /** Вызывается из PushService.onRegistered. */
    fun onRegistered(installationId: String) {
        scope.launch {
            sync(installationId)
        }
    }
    
    /**
     * Отключает уведомления для этой установки: замена deleteToken().
     * Сам FID при этом остаётся жив, его удаляет только FirebaseInstallations.delete().
     */
    fun unregister() {
        FirebaseMessaging.getInstance().unregister().addOnFailureListener { e ->
            Log.e(TAG, "Unregister failed", e)
        }
    }
    
    private suspend fun sync(installationId: String) {
        mutex.withLock {
            val account = accountDao.getCurrentAccount()
            
            if (account == null) {
                Log.d(TAG, "No current account, installation id will be sent after sign in")
                return@withLock
            }
            
            if (account.installationId == installationId) {
                return@withLock
            }
            
            sessionRepository.updateInstallationId(installationId).onSuccess {
                accountDao.update(account.copy(installationId = installationId))
                Log.d(TAG, "Installation id updated")
            }.onFailure { e ->
                Log.e(TAG, "Error saving installation id", e)
            }
        }
    }
    
    private companion object {
        const val TAG = "PushRegistrar"
    }
}
