/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.admins

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.channel.ChannelAdminsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Выбор подписчика канала, которого назначат администратором.
 *
 * Сервер отдаёт список без владельца: у него и так все права.
 */
@HiltViewModel
class AddChannelAdminViewModel @Inject constructor(
    private val channelAdminsRepository: ChannelAdminsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddChannelAdminUiState())
    val uiState = _uiState.asStateFlow()
    
    fun init(channelId: Long) {
        viewModelScope.launch {
            channelAdminsRepository.getAdminCandidates(channelId).onSuccess { candidates ->
                _uiState.update { it.copy(subscribers = candidates) }
            }.onFailure { error ->
                Log.e(TAG, "Error loading channel admin candidates", error)
            }
        }
    }
    
    private companion object {
        const val TAG = "AddChannelAdminViewModel"
    }
}
