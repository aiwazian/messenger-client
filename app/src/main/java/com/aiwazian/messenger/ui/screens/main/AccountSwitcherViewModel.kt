/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.push.NotificationHelper
import com.aiwazian.messenger.repository.AuthRepository
import com.aiwazian.messenger.repository.MessageReadInfoCache
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountSwitcherUiState(
    val currentUser: User? = null,
    val otherAccounts: List<User> = emptyList()
)

sealed interface AccountSwitcherSideEffect {
    data object AccountSwitched : AccountSwitcherSideEffect
}

@HiltViewModel
class AccountSwitcherViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val notificationHelper: NotificationHelper,
    private val messageReadInfoCache: MessageReadInfoCache
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AccountSwitcherUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<AccountSwitcherSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    init {
        viewModelScope.launch {
            userRepository.getMe().collectLatest { me ->
                _uiState.update { it.copy(currentUser = me) }
            }
        }
        
        loadOtherAccounts()
    }
    
    private fun loadOtherAccounts() {
        viewModelScope.launch {
            val currentUserId = userRepository.getMe().firstOrNull()?.id
            val accounts = authRepository.getAllAccounts()
            
            accounts
                .filter { it.userId != currentUserId && it.token.isNotEmpty() }
                .forEach { account ->
                    launch {
                        userRepository.fetchById(account.userId)
                        userRepository.getById(account.userId).collectLatest { user ->
                            _uiState.update { state ->
                                val updated = state.otherAccounts
                                    .filterNot { it.id == user.id } + user
                                state.copy(otherAccounts = updated.sortedBy { it.id })
                            }
                        }
                    }
                }
        }
    }
    
    fun switchAccount(userId: Long) {
        viewModelScope.launch {
            /*
             * Шторка и списки просмотров относятся к прежнему аккаунту. Уведомления живут
             * вне процесса, а кэш просмотров — в памяти Application, поэтому
             * перезапуск Activity их не сбросит: чистим вручную до смены сессии.
             */
            notificationHelper.clearAllNotifications()
            messageReadInfoCache.clear()
            
            SessionManager.switchAccount(userId)
            _sideEffect.emit(AccountSwitcherSideEffect.AccountSwitched)
        }
    }
}
