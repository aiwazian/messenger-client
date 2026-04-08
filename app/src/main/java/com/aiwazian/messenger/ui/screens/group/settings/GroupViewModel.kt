/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val vibrationManager: VibrationManager
) : ViewModel() {

    private var _groupId: Long = -1L

    private val _group = MutableStateFlow(Group())
    val groupInfo = _group.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateGroupState>(UpdateGroupState.Idle)
    val updateState: StateFlow<UpdateGroupState> = _updateState.asStateFlow()

    private val _updateEffect = MutableSharedFlow<UpdateGroupEffect>()
    val updateEffect: SharedFlow<UpdateGroupEffect> = _updateEffect.asSharedFlow()

    private var isInitialized = false

    fun init(groupId: Long) {
        if (isInitialized) return
        isInitialized = true
        _groupId = groupId
        
        _updateState.value = UpdateGroupState.Idle
        
        viewModelScope.launch {
            load(groupId)
        }
    }

    fun vibrate(pattern: LongArray) {
        vibrationManager.vibrate(pattern)
    }
    
    fun changeGroupName(newName: String) {
        _group.update { it.copy(name = newName) }
    }
    
    fun changeGroupBio(newBio: String) {
        _group.update { it.copy(bio = newBio) }
    }
    
    suspend fun load(groupId: Long) {
        if (_group.value.id == groupId && _group.value.name.isNotBlank()) {
            return
        }
        
        groupRepository.getById(groupId).collectLatest { group ->
            _group.update { group }
        }
    }
    
    fun checkValid(): Boolean {
        return _group.value.name.isNotBlank()
    }
    
    fun saveGroup() {
        viewModelScope.launch {
            if (!checkValid()) {
                _updateState.value = UpdateGroupState.Error("Введите название группы")
                vibrationManager.vibrate(VibrationPattern.Error)
                return@launch
            }
            
            _updateState.value = UpdateGroupState.Loading
            
            try {
                val result = groupRepository.update(_group.value)
                
                result.fold(
                    onSuccess = {
                        _updateState.value = UpdateGroupState.Success(_group.value.id)
                        _updateEffect.emit(UpdateGroupEffect.NavigateBack)
                    },
                    onFailure = { exception ->
                        _updateState.value = UpdateGroupState.Error(
                            exception.message ?: "Ошибка при обновлении группы"
                        )
                        vibrationManager.vibrate(VibrationPattern.Error)
                    }
                )
            } catch (e: Exception) {
                _updateState.value = UpdateGroupState.Error(
                    e.message ?: "Неизвестная ошибка"
                )
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }

    fun deleteGroup() {
        viewModelScope.launch {
            _updateState.value = UpdateGroupState.Loading
            
            try {
                val result = groupRepository.delete(_group.value.id)
                
                result.fold(
                    onSuccess = {
                        _updateEffect.emit(UpdateGroupEffect.NavigateToMain)
                    },
                    onFailure = { exception ->
                        _updateState.value = UpdateGroupState.Error(
                            exception.message ?: "Ошибка при удалении группы"
                        )
                        vibrationManager.vibrate(VibrationPattern.Error)
                    }
                )
            } catch (e: Exception) {
                _updateState.value = UpdateGroupState.Error(
                    e.message ?: "Неизвестная ошибка"
                )
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
}
