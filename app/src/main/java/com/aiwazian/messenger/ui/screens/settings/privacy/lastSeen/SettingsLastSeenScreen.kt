/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.lastSeen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionRadioItem
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction

@Composable
fun SettingsLastSeenScreen(
    level: PrivacyLevel
) {
    val navBackStack = LocalNavBackStack.current
    
    val settingsLastSeenViewModel = hiltViewModel<SettingsLastSeenViewModel>()
    
    val currentValue by settingsLastSeenViewModel.currentLevel.collectAsState()
    val showSaveButton by settingsLastSeenViewModel.showSaveButton.collectAsState()
    
    LaunchedEffect(Unit) {
        settingsLastSeenViewModel.effect.collect { effect ->
            when (effect) {
                is SettingsLastSeenEffect.Back -> {
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
                    settingsLastSeenViewModel.onSaveClick()
                })
        )
    } else {
        emptyList()
    }
    
    LaunchedEffect(level) {
        settingsLastSeenViewModel.init(level)
    }
    
    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.last_seen))
                },
                actions = actions
            )
        }) {
        SectionContainer(header = {
            SectionHeader(stringResource(R.string.who_can_see_my_last_seen))
        }) {
            SectionRadioItem(
                text = stringResource(R.string.everybody),
                selected = currentValue == PrivacyLevel.EVERYBODY,
                onClick = {
                    settingsLastSeenViewModel.selectValue(PrivacyLevel.EVERYBODY)
                })
            SectionRadioItem(
                text = stringResource(R.string.nobody),
                selected = currentValue == PrivacyLevel.NOBODY,
                onClick = {
                    settingsLastSeenViewModel.selectValue(PrivacyLevel.NOBODY)
                })
        }
    }
}
