/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.proxy

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.ProxyPingStatus
import com.aiwazian.messenger.domain.ProxyServer
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionButton
import com.aiwazian.messenger.ui.components.section.SectionButtonDefaults
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionToggle
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun ProxySettingsScreen(
    viewModel: ProxySettingsViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ResultEffect<ProxyServer> { proxy ->
        viewModel.onProxyCreated(proxy)
    }

    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(text = stringResource(R.string.proxy_settings))
                }
            )
        }
    ) {
        SectionContainer {
            SectionToggle(
                headlineText = stringResource(R.string.use_proxy),
                isChecked = uiState.useProxy,
                onCheckedChange = {
                    if (uiState.proxies.isEmpty()) {
                        navBackStack.add(AppRoute.SettingsProxyAdd())
                    } else {
                        viewModel.setProxyUse(!uiState.useProxy)
                    }
                }
            )
        }
        SectionContainer(header = {
            SectionHeader(stringResource(R.string.connections))
        }) {
            uiState.proxies.forEach { proxy ->
                val ping = uiState.pings[proxy.id]
                val supportingText = ping?.let {
                    when (it.status) {
                        ProxyPingStatus.CHECKING -> stringResource(R.string.proxy_checking)
                        ProxyPingStatus.AVAILABLE -> stringResource(
                            R.string.proxy_available_ping,
                            it.pingMs ?: 0
                        )

                        ProxyPingStatus.UNAVAILABLE -> stringResource(R.string.proxy_unavailable)
                    }
                }
                SectionButton(
                    headlineText = "${proxy.host}:${proxy.port}",
                    supportingText = supportingText,
                    leadingIcon = if (proxy.id == uiState.activeProxyId) {
                        Icons.Rounded.Check
                    } else {
                        null
                    },
                    trailingContent = {
                        ProxyRowMenu(
                            onEdit = {
                                navBackStack.add(AppRoute.SettingsProxyAdd(proxyId = proxy.id))
                            },
                            onDelete = { viewModel.showDeleteDialog(proxy) }
                        )
                    },
                    colors = SectionButtonDefaults.colors(
                        supportColor = if (ping?.status == ProxyPingStatus.UNAVAILABLE) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color.Unspecified
                        }
                    ),
                    onClick = { viewModel.selectProxy(proxy.id) }
                )
            }
            SectionButton(
                headlineText = stringResource(R.string.add_proxy),
                onClick = {
                    navBackStack.add(AppRoute.SettingsProxyAdd())
                }
            )
        }
    }

    uiState.deleteTarget?.let { proxy ->
        ProxyDeleteConfirmationDialog(
            onConfirm = { viewModel.deleteProxy(proxy) },
            onDismiss = viewModel::hideDeleteDialog
        )
    }
}

@Composable
private fun ProxyRowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    Box {
        var expanded by remember { mutableStateOf(false) }

        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.actions)
            )
        }

        AppDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppDropdownMenuItem(
                text = stringResource(R.string.edit),
                onClick = {
                    expanded = false
                    onEdit()
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
                }
            )
            AppDropdownMenuItem(
                text = stringResource(R.string.delete),
                onClick = {
                    expanded = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.DeleteOutline, contentDescription = null)
                },
                contentColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ProxyDeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(
        title = stringResource(R.string.proxy_delete),
        onDismissRequest = onDismiss,
        content = {
            Text(
                text = stringResource(R.string.proxy_delete_confirm_message),
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    )
}
