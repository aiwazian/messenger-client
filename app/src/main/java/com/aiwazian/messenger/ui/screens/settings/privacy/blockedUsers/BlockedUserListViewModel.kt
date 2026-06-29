/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.blockedUsers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlockedUserListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BlockedUserListUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<BlockedUserListSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    fun init() {
        fetchBlockedUsers()
    }
    
    private fun fetchBlockedUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userRepository.getBlockedUsers()
                .onSuccess { users ->
                    _uiState.update { it.copy(blockedUsers = users, isLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                    _sideEffect.emit(BlockedUserListSideEffect.ShowSnackbar("Не удалось загрузить список"))
                    vibrationManager.vibrate(VibrationPattern.Error)
                }
        }
    }
    
    fun showUnblockDialog(user: User) {
        _uiState.update { it.copy(selectedUserToUnblock = user, showUnblockDialog = true) }
    }
    
    fun hideUnblockDialog() {
        _uiState.update { it.copy(selectedUserToUnblock = null, showUnblockDialog = false) }
    }
    
    fun unblockUser() {
        val user = _uiState.value.selectedUserToUnblock ?: return
        viewModelScope.launch {
            hideUnblockDialog()
            userRepository.unblockUser(user.id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(blockedUsers = state.blockedUsers.filter { it.id != user.id })
                    }
                    _sideEffect.emit(BlockedUserListSideEffect.ShowSnackbar("Пользователь разблокирован"))
                }
                .onFailure {
                    _sideEffect.emit(BlockedUserListSideEffect.ShowSnackbar("Не удалось разблокировать пользователя"))
                    vibrationManager.vibrate(VibrationPattern.Error)
                }
        }
    }
}
