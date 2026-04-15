/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.dateOfBirth

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

@Composable
fun SettingsDateOfBirthScreen(
    level: PrivacyLevel
) {
    val navBackStack = LocalNavBackStack.current
    
    val settingsDateOfBirthViewModel = hiltViewModel<SettingsDateOfBirthViewModel>()
    
    val currentValue by settingsDateOfBirthViewModel.currentLevel.collectAsState()
    val showSaveButton by settingsDateOfBirthViewModel.showSaveButton.collectAsState()
    
    val scrollState = rememberScrollState()
    
    LaunchedEffect(Unit) {
        settingsDateOfBirthViewModel.effect.collect { effect ->
            when (effect) {
                is SettingsDateOfBirthEffect.Back -> {
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
                    settingsDateOfBirthViewModel.onSaveClick()
                })
        )
    } else {
        emptyList()
    }
    
    LaunchedEffect(level) {
        settingsDateOfBirthViewModel.init(level)
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.date_of_birth))
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
                SectionHeader("Кто видит дату моего рождения?")
            }) {
                SectionRadioItem(
                    text = stringResource(R.string.everybody),
                    selected = currentValue == PrivacyLevel.EVERYBODY,
                    onClick = {
                        settingsDateOfBirthViewModel.selectValue(PrivacyLevel.EVERYBODY)
                    })
                SectionRadioItem(
                    text = stringResource(R.string.nobody),
                    selected = currentValue == PrivacyLevel.NOBODY,
                    onClick = {
                        settingsDateOfBirthViewModel.selectValue(PrivacyLevel.NOBODY)
                    })
            }
        }
    }
}



