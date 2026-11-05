/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.proxy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.ProxyPing
import com.aiwazian.messenger.domain.ProxyPingStatus
import com.aiwazian.messenger.domain.ProxyRoutingConfig
import com.aiwazian.messenger.domain.ProxyServer
import com.aiwazian.messenger.network.proxy.ProxyChecker
import com.aiwazian.messenger.repository.ProxyRepository
import com.aiwazian.messenger.utils.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProxySettingsViewModel @Inject constructor(
    private val proxyRepository: ProxyRepository,
    private val proxyChecker: ProxyChecker,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProxySettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            proxyRepository.observeProxies().collect { proxies ->
                _uiState.update { it.copy(proxies = proxies) }
                proxies.forEach { proxy ->
                    checkProxy(proxy.id)
                }
            }
        }
        viewModelScope.launch {
            dataStoreManager.observeProxyRouting().collect { routing ->
                _uiState.update {
                    it.copy(
                        useProxy = routing.use,
                        activeProxyId = routing.activeProxyId.takeIf { id -> id > 0 }
                    )
                }
            }
        }
    }

    private fun checkProxy(proxyId: Long) {
        val proxy = _uiState.value.proxies.firstOrNull { it.id == proxyId } ?: return
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(pings = state.pings + (proxyId to ProxyPing(status = ProxyPingStatus.CHECKING)))
            }
            val ping = proxyChecker.check(proxy)
            _uiState.update { state ->
                state.copy(pings = state.pings + (proxyId to ping))
            }
        }
    }

    fun onProxyCreated(proxy: ProxyServer) {
        _uiState.update { state ->
            val proxies = state.proxies.filterNot { it.id == proxy.id } + proxy
            state.copy(proxies = proxies.sortedBy { it.id })
        }
    }

    fun setProxyUse(use: Boolean) {
        viewModelScope.launch {
            updateRouting { it.copy(use = use) }
        }
    }

    fun selectProxy(proxyId: Long) {
        viewModelScope.launch {
            updateRouting { it.copy(activeProxyId = proxyId) }
            checkProxy(proxyId)
        }
    }

    fun showDeleteDialog(proxy: ProxyServer) {
        _uiState.update { it.copy(deleteTarget = proxy) }
    }

    fun hideDeleteDialog() {
        _uiState.update { it.copy(deleteTarget = null) }
    }

    fun deleteProxy(proxy: ProxyServer) {
        viewModelScope.launch {
            proxyRepository.deleteProxy(proxy)
            if (dataStoreManager.observeProxyRouting().first().activeProxyId == proxy.id) {
                updateRouting { it.copy(activeProxyId = -1) }
            }
            _uiState.update { it.copy(deleteTarget = null) }
        }
    }

    private suspend fun updateRouting(transform: (ProxyRoutingConfig) -> ProxyRoutingConfig) {
        dataStoreManager.saveProxyRouting(
            transform(dataStoreManager.observeProxyRouting().first())
        )
    }
}
