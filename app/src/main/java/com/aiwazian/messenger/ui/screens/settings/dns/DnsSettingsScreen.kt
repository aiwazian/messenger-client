/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.dns

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.DnsProviderType
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionRadio
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun DnsSettingsScreen(
    viewModel: DnsSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(text = stringResource(R.string.dns))
                }
            )
        }
    ) {
        SectionContainer {
            SectionRadio(
                headlineText = stringResource(R.string.dns_system),
                selected = uiState.type == DnsProviderType.SYSTEM,
                onClick = {
                    viewModel.selectType(DnsProviderType.SYSTEM)
                }
            )
            SectionRadio(
                headlineText = stringResource(R.string.dns_cloudflare),
                selected = uiState.type == DnsProviderType.CLOUDFLARE,
                onClick = {
                    viewModel.selectType(DnsProviderType.CLOUDFLARE)
                }
            )
            SectionRadio(
                headlineText = stringResource(R.string.dns_google),
                selected = uiState.type == DnsProviderType.GOOGLE,
                onClick = {
                    viewModel.selectType(DnsProviderType.GOOGLE)
                }
            )
            SectionRadio(
                headlineText = stringResource(R.string.dns_adguard),
                selected = uiState.type == DnsProviderType.ADGUARD,
                onClick = {
                    viewModel.selectType(DnsProviderType.ADGUARD)
                }
            )
            SectionRadio(
                headlineText = stringResource(R.string.dns_custom),
                selected = uiState.type == DnsProviderType.CUSTOM,
                onClick = {
                    viewModel.selectType(DnsProviderType.CUSTOM)
                }
            )
        }
        
        AnimatedVisibility(visible = uiState.type == DnsProviderType.CUSTOM) {
            SectionContainer {
                FramelessTextBox(
                    placeholder = stringResource(R.string.dns_primary),
                    value = uiState.primaryDns,
                    onValueChange = viewModel::onPrimaryChange,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false
                    )
                )
                FramelessTextBox(
                    placeholder = stringResource(R.string.dns_secondary),
                    value = uiState.secondaryDns,
                    onValueChange = viewModel::onSecondaryChange,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false
                    )
                )
            }
        }
    }
}
