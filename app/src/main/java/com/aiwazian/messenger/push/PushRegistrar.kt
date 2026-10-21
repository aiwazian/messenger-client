/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.push

import android.util.Log
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.repository.SessionRepository
import com.aiwazian.messenger.utils.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushRegistrar @Inject constructor(
    private val accountDao: AccountDao,
    private val sessionRepository: SessionRepository
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val mutex = Mutex()
    
    @Volatile
    private var lastKnownInstallationId: String? = null
    
    /** Токен прежнего активного аккаунта. Читается только внутри коллектора. */
    private var lastActiveToken: String? = null
    
    init {
        SessionManager.token
            .filter { it.isNotEmpty() }
            .onEach { onActiveAccountChanged(it) }
            .launchIn(scope)
    }

    fun ensureRegistered() {
        FirebaseMessaging.getInstance().register().addOnFailureListener { e ->
            Log.e(TAG, "Register failed", e)
        }
    }
    
    fun onRegistered(installationId: String) {
        scope.launch {
            sync(installationId)
        }
    }

    private suspend fun onActiveAccountChanged(token: String) {
        val previousToken = lastActiveToken
        lastActiveToken = token
        
        if (previousToken != null && previousToken != token) {
            accountDao.clearInstallationIds()
        }
        
        val installationId = lastKnownInstallationId
        
        if (installationId == null) {
            ensureRegistered()
            return
        }
        
        sync(installationId)
    }
    
    private suspend fun sync(installationId: String) {
        mutex.withLock {
            lastKnownInstallationId = installationId
            
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
