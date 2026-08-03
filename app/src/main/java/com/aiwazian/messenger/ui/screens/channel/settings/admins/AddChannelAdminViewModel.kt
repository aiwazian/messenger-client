/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.admins

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Выбор подписчика канала, которого назначат администратором. */
@HiltViewModel
class AddChannelAdminViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddChannelAdminUiState())
    val uiState = _uiState.asStateFlow()
    
    fun init(channelId: Long) {
        viewModelScope.launch {
            channelRepository.getSubscribers(channelId).onSuccess { subscribers ->
                _uiState.update { it.copy(subscribers = subscribers) }
            }.onFailure { error ->
                Log.e(TAG, "Error loading channel subscribers", error)
            }
        }
    }
    
    private companion object {
        const val TAG = "AddChannelAdminViewModel"
    }
}
