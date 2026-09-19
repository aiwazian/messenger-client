/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.dateOfBirth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.result.ResultEffect
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionRadioItem
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.enums.PrivacyField
import com.aiwazian.messenger.ui.screens.settings.privacy.exceptions.PrivacyExceptionSelection
import com.aiwazian.messenger.ui.screens.settings.privacy.exceptions.PrivacyExceptionsSection

@Composable
fun SettingsDateOfBirthScreen(
    level: PrivacyLevel
) {
    val navBackStack = LocalNavBackStack.current

    val settingsDateOfBirthViewModel = hiltViewModel<SettingsDateOfBirthViewModel>()

    val currentValue by settingsDateOfBirthViewModel.currentLevel.collectAsState()
    val currentExceptions by settingsDateOfBirthViewModel.currentExceptions.collectAsState()
    val showSaveButton by settingsDateOfBirthViewModel.showSaveButton.collectAsState()

    LaunchedEffect(Unit) {
        settingsDateOfBirthViewModel.effect.collect { effect ->
            when (effect) {
                is SettingsDateOfBirthEffect.Back -> {
                    navBackStack.removeLastOrNull()
                }
            }
        }
    }

    ResultEffect<PrivacyExceptionSelection> { selection ->
        settingsDateOfBirthViewModel.applyExceptionSelection(selection)
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

    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.date_of_birth))
                },
                actions = actions
            )
        }) {
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

        PrivacyExceptionsSection(
            field = PrivacyField.DATE_OF_BIRTH,
            exceptions = currentExceptions,
            onNavigate = { field, kind, selectedUserIds ->
                navBackStack.add(
                    AppRoute.SelectPrivacyExceptionUsers(
                        field = field,
                        kind = kind,
                        selectedUserIds = selectedUserIds
                    )
                )
            })
    }
}
