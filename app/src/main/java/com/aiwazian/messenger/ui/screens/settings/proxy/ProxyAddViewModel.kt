/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.proxy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.ProxyServer
import com.aiwazian.messenger.enums.ProxyType
import com.aiwazian.messenger.repository.ProxyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProxyAddViewModel @Inject constructor(
    private val proxyRepository: ProxyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProxyAddUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProxyAddUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var editingProxyId: Long? = null

    fun load(proxyId: Long?) {
        if (proxyId == null || proxyId == editingProxyId) {
            return
        }
        editingProxyId = proxyId
        viewModelScope.launch {
            val proxy = proxyRepository.getProxy(proxyId) ?: return@launch
            _uiState.update {
                it.copy(
                    type = proxy.type,
                    host = proxy.host,
                    port = proxy.port.toString(),
                    username = proxy.username.orEmpty(),
                    password = proxy.password.orEmpty(),
                    isEditing = true
                )
            }
        }
    }

    fun selectType(type: ProxyType) {
        _uiState.update { it.copy(type = type) }
    }

    fun onHostChange(host: String) {
        _uiState.update { it.copy(host = host) }
    }

    fun onPortChange(port: String) {
        _uiState.update { it.copy(port = port.filter { symbol -> symbol.isDigit() }.take(5)) }
    }

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun save() {
        val state = _uiState.value
        val port = state.port.toIntOrNull() ?: return
        if (state.host.isBlank()) {
            return
        }

        viewModelScope.launch {
            val editingId = editingProxyId
            val proxy = if (editingId != null) {
                proxyRepository.updateProxy(
                    ProxyServer(
                        id = editingId,
                        type = state.type,
                        host = state.host.trim(),
                        port = port,
                        username = state.username.takeIf { it.isNotBlank() },
                        password = state.password.takeIf { it.isNotBlank() }
                    )
                )
            } else {
                proxyRepository.addProxy(
                    type = state.type,
                    host = state.host.trim(),
                    port = port,
                    username = state.username,
                    password = state.password
                )
            }
            _uiEffect.emit(ProxyAddUiEffect.Created(proxy))
        }
    }
}
