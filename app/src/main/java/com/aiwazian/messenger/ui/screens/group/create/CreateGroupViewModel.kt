/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.Group
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.utils.VibrationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {

    private val _group = MutableStateFlow(Group())
    val groupInfo = _group.asStateFlow()

    private val _createState = MutableStateFlow<CreateGroupState>(CreateGroupState.Idle)
    val createState: StateFlow<CreateGroupState> = _createState.asStateFlow()

    private val _createEffect = MutableSharedFlow<CreateGroupEffect>()
    val createEffect: SharedFlow<CreateGroupEffect> = _createEffect.asSharedFlow()

    fun changeGroupName(newName: String) {
        _group.update { it.copy(name = newName) }
    }
    
    fun changeGroupDescription(new: String) {
        _group.update { it.copy(bio = new) }
    }
    
    fun checkValid(): Boolean {
        return _group.value.name.isNotBlank()
    }
    
    fun createGroup() {
        viewModelScope.launch {
            if (!checkValid()) {
                _createState.value = CreateGroupState.Error("Введите название группы")
                vibrationManager.vibrate(VibrationPattern.Error)
                return@launch
            }
            
            _createState.value = CreateGroupState.Loading
            
            try {
                val result = groupRepository.create(_group.value)
                
                result.fold(
                    onSuccess = { createdId ->
                        val groupName = _group.value.name
                        
                        _createState.value = CreateGroupState.Success(createdId, groupName)
                        
                        val chat = Chat(
                            id = createdId,
                            chatName = groupName
                        )
                        
                        _createEffect.emit(CreateGroupEffect.NavigateToChat(chat))
                    },
                    onFailure = { exception ->
                        _createState.value = CreateGroupState.Error(
                            exception.message ?: "Ошибка при создании группы"
                        )
                        
                        vibrationManager.vibrate(VibrationPattern.Error)
                    }
                )
            } catch (e: Exception) {
                _createState.value = CreateGroupState.Error(
                    e.message ?: "Неизвестная ошибка"
                )
                
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
}
