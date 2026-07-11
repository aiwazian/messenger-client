/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.joinRequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.PendingJoinRequest
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.usecase.DownloadAvatarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingJoinRequestsState(
    val isLoading: Boolean = true,
    val requests: List<PendingJoinRequest> = emptyList()
)

@HiltViewModel
class PendingJoinRequestsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val downloadAvatarUseCase: DownloadAvatarUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PendingJoinRequestsState())
    val uiState = _uiState.asStateFlow()
    
    private val downloadingAvatars = mutableSetOf<String>()
    
    init {
        loadRequests()
    }
    
    private fun loadRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = userRepository.getPendingJoinRequests()
            if (result.isSuccess) {
                val requests = result.getOrNull().orEmpty()
                _uiState.update { it.copy(isLoading = false, requests = requests) }
                
                requests.forEach { request ->
                    val fileId = request.avatarFileId
                    if (fileId != null && downloadingAvatars.add(fileId)) {
                        viewModelScope.launch {
                            downloadAvatarUseCase(request.chatId, fileId)
                                .onFailure {
                                    downloadingAvatars.remove(fileId)
                                }
                        }
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun cancelRequest(chatId: Long) {
        viewModelScope.launch {
            val result = userRepository.cancelJoinRequest(chatId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(requests = state.requests.filter { it.chatId != chatId })
                }
            }
        }
    }
}
