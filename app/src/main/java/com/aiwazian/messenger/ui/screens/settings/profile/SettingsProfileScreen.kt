/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyDateWithYear
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction

@Composable
fun SettingsProfileScreen(viewModel: SettingsProfileViewModel = hiltViewModel()) {
    val navHost = LocalNavHost.current
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is SettingsProfileSideEffect.NavigateBack -> navHost.removeLastOrNull()
                else -> {}
            }
        }
    }
    
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.profile)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navHost::removeLastOrNull
                ),
                actions = listOf(
                    TopBarAction(
                        icon = Icons.Rounded.Check,
                        onClick = viewModel::onSaveAndBack
                    )
                )
            )
        },
        modifier = Modifier.imePadding()
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                SectionContainer(header = {
                    SectionHeader(title = stringResource(R.string.your_name))
                }) {
                    FramelessTextBox(
                        placeholder = stringResource(R.string.first_name),
                        value = uiState.user.firstName,
                        onValueChange = viewModel::onChangeFirstName
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 1.dp,
                    )
                    
                    FramelessTextBox(
                        placeholder = stringResource(R.string.last_name),
                        value = uiState.user.lastName.orEmpty(),
                        onValueChange = viewModel::onChangeLastName
                    )
                }
                
                SectionContainer(
                    header = {
                        SectionHeader(title = stringResource(R.string.bio))
                    },
                    footer = {
                        SectionDescription(text = "В настройках можно выбрать, кому они будут видны.")
                    }) {
                    FramelessTextBox(
                        placeholder = stringResource(R.string.write_about_me),
                        value = uiState.user.bio.orEmpty(),
                        onValueChange = viewModel::onChangeBio
                    )
                }
                
                SectionContainer(
                    header = {
                        SectionHeader(title = stringResource(R.string.username))
                    },
                    footer = {
                        SectionDescription("Другие пользователи смогут найти Вас по такому имени и связаться.")
                    }) {
                    SectionItem(
                        headlineText = if (uiState.user.username != null) {
                            "@${uiState.user.username}"
                        } else {
                            "Задать имя пользователя"
                        },
                        onClick = {
                            navHost.add(AppRoute.SettingsUsername(uiState.user.username))
                        }
                    )
                }
                
                SectionContainer(header = {
                    SectionHeader(title = stringResource(R.string.date_of_birth))
                }) {
                    SectionItem(
                        headlineText = "Дата Вашего рождения",
                        trailingText = if (uiState.user.dateOfBirth != null) {
                            uiState.user.dateOfBirth!!.toInstance().toPrettyDateWithYear()
                        } else {
                            "Указать"
                        },
                        onClick = viewModel::showDatePicker
                    )
                    
                    AnimatedContent(targetState = uiState.user.dateOfBirth) { dateOfBirth ->
                        if (dateOfBirth != null) {
                            SectionItem(
                                headlineText = stringResource(R.string.remove_date_of_birth),
                                onClick = {
                                    viewModel.onChangeDateOfBirth(null)
                                },
                                contentColor = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                
                if (uiState.showDatePicker) {
                    val datePickerState = rememberDatePickerState(uiState.user.dateOfBirth)
                    DatePickerDialog(
                        onDismissRequest = viewModel::hideDatePicker,
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val selected = datePickerState.selectedDateMillis
                                    viewModel.onChangeDateOfBirth(selected)
                                },
                                modifier = Modifier.padding(end = 4.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(stringResource(R.string.ok))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = viewModel::hideDatePicker,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        }) {
                        DatePicker(
                            title = { },
                            state = datePickerState
                        )
                    }
                }
            }
        }
    }
}
