package com.aiwazian.messenger.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.DeviceContact
import com.aiwazian.messenger.repository.DeviceContactsRepository
import com.aiwazian.messenger.utils.ClipboardService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceContactsUiState(
    val isLoading: Boolean = false,
    val contacts: List<DeviceContact> = emptyList()
)

@HiltViewModel
class DeviceContactsViewModel @Inject constructor(
    private val deviceContactsRepository: DeviceContactsRepository,
    private val clipboardService: ClipboardService
) : ViewModel() {
    private val _uiState = MutableStateFlow(DeviceContactsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadContacts() {
        val state = _uiState.value

        if (state.isLoading || state.contacts.isNotEmpty()) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val contacts = deviceContactsRepository.getContacts()

            _uiState.update { it.copy(isLoading = false, contacts = contacts) }
        }
    }

    fun copyValue(value: String) {
        clipboardService.copy(value)
    }
}
