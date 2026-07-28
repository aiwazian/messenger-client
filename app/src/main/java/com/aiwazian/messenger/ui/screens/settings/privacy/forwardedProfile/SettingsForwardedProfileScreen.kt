/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.forwardedProfile

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
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionRadioItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction

/**
 * Кто может перейти в мой профиль по заголовку «Переслано от».
 */
@Composable
fun SettingsForwardedProfileScreen(
    level: PrivacyLevel,
    settingsForwardedProfileViewModel: SettingsForwardedProfileViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    
    val currentValue by settingsForwardedProfileViewModel.currentLevel.collectAsState()
    val showSaveButton by settingsForwardedProfileViewModel.showSaveButton.collectAsState()
    
    val scrollState = rememberScrollState()
    
    LaunchedEffect(Unit) {
        settingsForwardedProfileViewModel.effect.collect { effect ->
            when (effect) {
                is SettingsForwardedProfileEffect.Back -> {
                    navBackStack.removeLastOrNull()
                }
            }
        }
    }
    
    val actions = if (showSaveButton) {
        listOf(
            TopBarAction(
                icon = Icons.Rounded.Check,
                onClick = {
                    settingsForwardedProfileViewModel.onSaveClick()
                })
        )
    } else {
        emptyList()
    }
    
    LaunchedEffect(level) {
        settingsForwardedProfileViewModel.init(level)
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.message_forwarding))
                },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
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
                SectionHeader(stringResource(R.string.who_can_open_my_profile_from_forwarded_messages))
            }) {
                SectionRadioItem(
                    text = stringResource(R.string.everybody),
                    selected = currentValue == PrivacyLevel.EVERYBODY,
                    onClick = {
                        settingsForwardedProfileViewModel.selectValue(PrivacyLevel.EVERYBODY)
                    })
                SectionRadioItem(
                    text = stringResource(R.string.nobody),
                    selected = currentValue == PrivacyLevel.NOBODY,
                    onClick = {
                        settingsForwardedProfileViewModel.selectValue(PrivacyLevel.NOBODY)
                    })
            }
        }
    }
}
