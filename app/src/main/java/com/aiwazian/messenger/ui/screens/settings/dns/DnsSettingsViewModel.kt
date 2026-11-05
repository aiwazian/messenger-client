/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.dns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.DnsConfig
import com.aiwazian.messenger.enums.DnsProviderType
import com.aiwazian.messenger.utils.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DnsSettingsViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DnsSettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val config = dataStoreManager.observeDnsConfig().first()
            _uiState.update {
                it.copy(
                    type = config.type,
                    primaryDns = config.primaryDns,
                    secondaryDns = config.secondaryDns
                )
            }
        }
    }

    fun selectType(type: DnsProviderType) {
        _uiState.update { it.copy(type = type) }
        persist()
    }

    fun onPrimaryChange(primaryDns: String) {
        _uiState.update { it.copy(primaryDns = primaryDns) }
        persist()
    }

    fun onSecondaryChange(secondaryDns: String) {
        _uiState.update { it.copy(secondaryDns = secondaryDns) }
        persist()
    }

    private fun persist() {
        val state = _uiState.value
        viewModelScope.launch {
            dataStoreManager.saveDnsConfig(
                DnsConfig(
                    type = state.type,
                    primaryDns = state.primaryDns,
                    secondaryDns = state.secondaryDns
                )
            )
        }
    }
}
