/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.invites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionRadioItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction

@Composable
fun SettingsInvitesScreen(
    level: PrivacyLevel,
    settingsInvitesViewModel: SettingsInvitesViewModel = hiltViewModel()
) {
    val navHost = LocalNavHost.current
    
    val currentValue by settingsInvitesViewModel.currentLevel.collectAsState()
    val showSaveButton by settingsInvitesViewModel.showSaveButton.collectAsState()
    
    val scrollState = rememberScrollState()
    
    LaunchedEffect(Unit) {
        settingsInvitesViewModel.effect.collect { effect ->
            when (effect) {
                is SettingsInvitesEffect.Back -> {
                    navHost.removeLastOrNull()
                }
            }
        }
    }
    
    val actions = if (showSaveButton) {
        listOf(
            TopBarAction(
                icon = Icons.Rounded.Check,
                onClick = {
                    settingsInvitesViewModel.onSaveClick()
                })
        )
    } else {
        emptyList()
    }
    
    LaunchedEffect(level) {
        settingsInvitesViewModel.init(level)
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.invites))
                },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navHost::removeLastOrNull
                ),
                actions = actions
            )
        }) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(scrollState)
        ) {
            SectionContainer(header = {
                SectionHeader("Кто может добавлять меня в группы?")
            }) {
                SectionRadioItem(
                    text = stringResource(R.string.everybody),
                    selected = currentValue == PrivacyLevel.Everybody,
                    onClick = {
                        settingsInvitesViewModel.selectValue(PrivacyLevel.Everybody)
                    })
                SectionRadioItem(
                    text = stringResource(R.string.nobody),
                    selected = currentValue == PrivacyLevel.Nobody,
                    onClick = {
                        settingsInvitesViewModel.selectValue(PrivacyLevel.Nobody)
                    })
            }
        }
    }
}
