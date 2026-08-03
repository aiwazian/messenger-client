/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.admins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Выбор участника группы, которого назначат администратором. */
@HiltViewModel
class AddGroupAdminViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddGroupAdminUiState())
    val uiState = _uiState.asStateFlow()
    
    fun init(groupId: Long) {
        viewModelScope.launch {
            groupRepository.getMembers(groupId).collectLatest { members ->
                _uiState.update { it.copy(members = members) }
            }
        }
    }
}
