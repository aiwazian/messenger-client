/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.util.Log
import com.aiwazian.messenger.database.entity.AccountEntity
import com.aiwazian.messenger.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Итог завершения текущей сессии: либо на устройстве остался ещё один аккаунт и
 * приложение продолжает работу под ним, либо аккаунтов больше нет и нужен экран
 * авторизации.
 */
sealed interface SessionEndResolution {
    data class SwitchedToAccount(val userId: Long) : SessionEndResolution
    data object NoAccountsLeft : SessionEndResolution
}

object SessionManager {

    private const val TAG = "SessionManager"

    private val _token = MutableStateFlow("")
    val token = _token.asStateFlow()

    private var _isAuthorized = false
    private var currentUserId: Long? = null
    private var sessionEndCallback: ((SessionEndResolution) -> Unit)? = null
    private var authRepository: AuthRepository? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Сессия может отвалиться сразу в нескольких местах: параллельные запросы
     * получают 401 одновременно с событием сокета. Разбор должен произойти один
     * раз, иначе будет удалено несколько аккаунтов подряд.
     */
    private val resolutionMutex = Mutex()
    private var isResolvingSessionEnd = false

    /**
     * Один и тот же мёртвый токен приходит из разных источников: интерцептор,
     * сокет, повторные попытки переподключения. Без этой отметки каждый повтор
     * удалял бы ещё один аккаунт.
     */
    @Volatile
    private var lastInvalidatedToken: String? = null

    /**
     * Явный выход сам решает, куда идти дальше, поэтому 401 от запроса logout не
     * должен запускать второй разбор.
     */
    @Volatile
    private var suppressUnauthorizedHandling = false

    var isInit = false
        private set

    fun init(repository: AuthRepository) {
        authRepository = repository
        isInit = true
    }

    fun getToken(): String {
        return _token.value
    }

    fun getCurrentUserId(): Long? {
        return currentUserId
    }

    fun setAuthorized(value: Boolean) {
        _isAuthorized = value
    }

    suspend fun loadSession() {
        val repository = authRepository
        var currentToken = repository?.getCurrentToken() ?: ""
        var loadedUserId = repository?.getAllAccounts()?.find { it.isCurrent }?.userId
        
        if (currentToken.isEmpty()) {
            val fallback = repository?.getFirstAccountWithToken()
            if (fallback != null) {
                repository.setCurrent(fallback.userId)
                currentToken = fallback.token
                loadedUserId = fallback.userId
            }
        }

        _token.update { currentToken }
        currentUserId = loadedUserId?.takeIf { currentToken.isNotEmpty() }
        _isAuthorized = currentToken.isNotEmpty()
        isInit = true
    }

    suspend fun saveSession(
        userId: Long,
        token: String,
        createdAt: Long
    ) {
        _token.update { token }
        currentUserId = userId.takeIf { token.isNotEmpty() }
        _isAuthorized = token.isNotEmpty()
        lastInvalidatedToken = null

        val repository = authRepository
            ?: throw IllegalStateException("AuthRepository not initialized. Call init() first.")
        
        repository.saveAccount(
            AccountEntity(
                userId = userId,
                isCurrent = true,
                token = token,
                createdAt = createdAt
            )
        )
    }
    
    suspend fun logout() {
        _token.update { "" }
        currentUserId = null
        _isAuthorized = false
        authRepository?.clearCurrentToken()
    }
    
    suspend fun hasAnySession(): Boolean {
        val accounts = authRepository?.getAllAccounts() ?: emptyList()
        return accounts.any { it.token.isNotEmpty() }
    }
    
    suspend fun switchAccount(userId: Long) {
        authRepository?.switchAccount(userId)
        loadSession()
    }

    /**
     * Завершает текущую сессию и переключается на следующий аккаунт устройства.
     *
     * Текущий аккаунт удаляется всегда: его токен либо уже недействителен (сессию
     * отключили), либо станет таким после выхода. Дальше берётся любой другой
     * аккаунт с непустым токеном — так выход из одного аккаунта не выкидывает
     * пользователя из остальных.
     *
     * @param revokeOnServer нужно ли гасить сессию на сервере. При отключённой
     * сессии её там уже нет, поэтому запрос не имеет смысла.
     */
    suspend fun endCurrentSessionAndResolve(revokeOnServer: Boolean): SessionEndResolution {
        val repository = authRepository

        if (repository == null) {
            Log.e(TAG, "AuthRepository не инициализирован")
            return SessionEndResolution.NoAccountsLeft
        }

        val accounts = repository.getAllAccounts()
        val currentUserId = accounts.find { it.isCurrent }?.userId

        val nextAccount = accounts.firstOrNull {
            it.userId != currentUserId && it.token.isNotEmpty()
        }

        suppressUnauthorizedHandling = true

        try {
            if (revokeOnServer) {
                repository.logout().onFailure { error ->
                    Log.e(TAG, "Ошибка при выходе на сервере: ${error.message}")
                }
            } else {
                repository.clearCurrentToken()
            }

            if (nextAccount == null) {
                _token.update { "" }
                this.currentUserId = null
                _isAuthorized = false
                return SessionEndResolution.NoAccountsLeft
            }

            switchAccount(nextAccount.userId)

            /*
             * Токен мог не подтянуться, если строка аккаунта успела испортиться.
             * Тогда честнее показать авторизацию, чем оставить приложение без
             * рабочей сессии.
             */
            if (getToken().isEmpty()) {
                Log.e(TAG, "Не удалось переключиться на аккаунт ${nextAccount.userId}")
                this.currentUserId = null
                _isAuthorized = false
                return SessionEndResolution.NoAccountsLeft
            }

            return SessionEndResolution.SwitchedToAccount(nextAccount.userId)
        } finally {
            suppressUnauthorizedHandling = false
        }
    }

    /**
     * Сообщает, что текущая сессия больше недействительна: пришёл 401 или сокет
     * получил событие об отключении сессии.
     *
     * Вызывается из потоков без корутин (интерцептор OkHttp, колбэк сокета),
     * поэтому работа с базой уходит в фон, а результат отдаётся в колбэк.
     */
    fun notifyUnauthorized() {
        if (suppressUnauthorizedHandling) {
            return
        }

        val invalidToken = getToken()

        if (invalidToken.isNotEmpty() && invalidToken == lastInvalidatedToken) {
            return
        }

        lastInvalidatedToken = invalidToken
        _isAuthorized = false

        scope.launch {
            val alreadyRunning = resolutionMutex.withLock {
                val running = isResolvingSessionEnd
                if (!running) {
                    isResolvingSessionEnd = true
                }
                running
            }

            if (alreadyRunning) {
                return@launch
            }

            try {
                val resolution = endCurrentSessionAndResolve(revokeOnServer = false)
                Log.d(TAG, "Сессия завершена, результат: $resolution")
                sessionEndCallback?.invoke(resolution)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при разборе завершённой сессии", e)
                sessionEndCallback?.invoke(SessionEndResolution.NoAccountsLeft)
            } finally {
                resolutionMutex.withLock {
                    isResolvingSessionEnd = false
                }
            }
        }
    }

    fun setSessionEndCallback(callback: (SessionEndResolution) -> Unit) {
        sessionEndCallback = callback
    }

    fun getSessionEndCallback() = sessionEndCallback
}
