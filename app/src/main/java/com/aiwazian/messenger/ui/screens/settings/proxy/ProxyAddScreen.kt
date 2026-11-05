/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.proxy

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.ProxyServer
import com.aiwazian.messenger.enums.ProxyType
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionRadio
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction

@Composable
fun ProxyAddScreen(
    proxyId: Long?,
    viewModel: ProxyAddViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val resultBus = LocalResultEventBus.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(proxyId) {
        viewModel.load(proxyId)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ProxyAddUiEffect.Created -> {
                    resultBus.sendResult<ProxyServer>(result = effect.proxy)
                    navBackStack.removeLastOrNull()
                }
            }
        }
    }

    val actions = if (uiState.canSave) {
        listOf(
            TopBarAction(
                icon = Icons.Rounded.Check,
                onClick = viewModel::save
            )
        )
    } else {
        emptyList()
    }

    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.isEditing) R.string.edit else R.string.add_proxy
                        )
                    )
                },
                actions = actions
            )
        }
    ) {
        SectionContainer {
            SectionRadio(
                headlineText = stringResource(R.string.socks5),
                selected = uiState.type == ProxyType.SOCKS5,
                onClick = {
                    viewModel.selectType(ProxyType.SOCKS5)
                }
            )
            SectionRadio(
                headlineText = stringResource(R.string.https),
                selected = uiState.type == ProxyType.HTTPS,
                onClick = {
                    viewModel.selectType(ProxyType.HTTPS)
                }
            )
        }

        SectionContainer {
                FramelessTextBox(
                    placeholder = stringResource(R.string.proxy_server),
                    value = uiState.host,
                    onValueChange = viewModel::onHostChange,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false
                    )
                )
                FramelessTextBox(
                    placeholder = stringResource(R.string.proxy_port),
                    value = uiState.port,
                    onValueChange = viewModel::onPortChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                FramelessTextBox(
                    placeholder = stringResource(R.string.username),
                    value = uiState.username,
                    onValueChange = viewModel::onUsernameChange,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false
                    )
                )
                FramelessTextBox(
                    placeholder = stringResource(R.string.password),
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false
                    )
                )
        }
    }
}
