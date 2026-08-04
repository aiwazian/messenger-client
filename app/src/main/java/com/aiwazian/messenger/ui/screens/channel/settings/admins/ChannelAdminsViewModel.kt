/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.admins

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.repository.channel.ChannelAdminsRepository
import com.aiwazian.messenger.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Список администраторов канала: доступен владельцу и админам с правом на управление. */
@HiltViewModel
class ChannelAdminsViewModel @Inject constructor(
    private val channelAdminsRepository: ChannelAdminsRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private var _channelId = -1L
    
    private val _uiState = MutableStateFlow(ChannelAdminsUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _sideEffect = MutableSharedFlow<ChannelAdminsSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    fun init(channelId: Long) {
        _channelId = channelId
        loadCurrentUserId()
        loadAdmins()
    }
    
    /** Своего администратора нельзя ни уволить, ни отредактировать: это решает сервер. */
    private fun loadCurrentUserId() {
        viewModelScope.launch {
            val me = userRepository.getMe().firstOrNull()
            _uiState.update { it.copy(currentUserId = me?.id) }
        }
    }
    
    /** Перезагружается при каждом возврате на экран, чтобы показать нового администратора. */
    fun loadAdmins() {
        if (_channelId == -1L) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            channelAdminsRepository.getAdmins(_channelId).onSuccess { admins ->
                _uiState.update { it.copy(admins = admins, isLoading = false) }
            }.onFailure { error ->
                Log.e(TAG, "Error loading channel admins", error)
                _uiState.update { it.copy(isLoading = false) }
                _sideEffect.emit(
                    ChannelAdminsSideEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_load_administrators))
                )
            }
        }
    }
    
    fun showDemoteDialog(userId: Long) {
        if (userId == _uiState.value.currentUserId) return
        _uiState.update { it.copy(showDemoteDialog = true, selectedUserId = userId) }
    }
    
    fun hideDemoteDialog() {
        _uiState.update { it.copy(showDemoteDialog = false, selectedUserId = null) }
    }
    
    fun confirmDemote() {
        val userId = _uiState.value.selectedUserId ?: return
        
        viewModelScope.launch {
            channelAdminsRepository.removeAdmin(_channelId, userId).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        admins = state.admins.filter { it.userId != userId },
                        showDemoteDialog = false,
                        selectedUserId = null
                    )
                }
                _sideEffect.emit(
                    ChannelAdminsSideEffect.ShowSnackbar(UiText.StringResource(R.string.admin_removed))
                )
            }.onFailure { error ->
                Log.e(TAG, "Error removing channel admin", error)
                _uiState.update { it.copy(showDemoteDialog = false, selectedUserId = null) }
                _sideEffect.emit(
                    ChannelAdminsSideEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_save_changes))
                )
            }
        }
    }
    
    private companion object {
        const val TAG = "ChannelAdminsViewModel"
    }
}
