/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.usecase.CreateGroupUseCase
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
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
    private val createGroupUseCase: CreateGroupUseCase,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CreateGroupState())
    val uiState: StateFlow<CreateGroupState> = _uiState.asStateFlow()
    
    private val _createEffect = MutableSharedFlow<CreateGroupEffect>()
    val createEffect: SharedFlow<CreateGroupEffect> = _createEffect.asSharedFlow()
    
    fun changeGroupName(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }
    
    fun changeGroupDescription(new: String) {
        _uiState.update { it.copy(bio = new) }
    }
    
    fun createGroup() {
        viewModelScope.launch {
            if (!checkValid()) {
                _createEffect.emit(CreateGroupEffect.ShowSnackbar(UiText.DynamicString("Введите название группы")))
                vibrationManager.vibrate(VibrationPattern.Error)
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                createGroupUseCase(_uiState.value.name, _uiState.value.bio)
                    .onSuccess { createdId ->
                        _createEffect.emit(CreateGroupEffect.NavigateToChat(createdId))
                    }
                    .onFailure {
                        _createEffect.emit(CreateGroupEffect.ShowSnackbar(UiText.DynamicString("Ошибка при создании группы")))
                        vibrationManager.vibrate(VibrationPattern.Error)
                    }
            } catch (_: Exception) {
                _createEffect.emit(CreateGroupEffect.ShowSnackbar(UiText.DynamicString("Неизвестная ошибка")))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    private fun checkValid(): Boolean {
        return _uiState.value.name.isNotBlank()
    }
}
