/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.admins

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.group.GroupAdminsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Выбор участника группы, которого назначат администратором.
 *
 * Сервер отдаёт список без владельца: у него и так все права.
 */
@HiltViewModel
class AddGroupAdminViewModel @Inject constructor(
    private val groupAdminsRepository: GroupAdminsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddGroupAdminUiState())
    val uiState = _uiState.asStateFlow()
    
    fun init(groupId: Long) {
        viewModelScope.launch {
            groupAdminsRepository.getAdminCandidates(groupId).onSuccess { candidates ->
                _uiState.update { it.copy(members = candidates) }
            }.onFailure { error ->
                Log.e(TAG, "Error loading group admin candidates", error)
            }
        }
    }
    
    private companion object {
        const val TAG = "AddGroupAdminViewModel"
    }
}
