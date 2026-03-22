/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.InputField
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

@Composable
fun SettingsProfileScreen() {
    Content()
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    PageTopBar(
        title = { Text(stringResource(R.string.profile)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = onBack::invoke
        )
    )
}

@SuppressLint("NonObservableLocale")
@Composable
private fun Content() {
    val navHost = LocalNavHost.current
    
    val settingsProfileViewModel = hiltViewModel<SettingsProfileViewModel>()
    
    val isVisibleDatePicker = settingsProfileViewModel.dataOfBirthDialog
    
    val user by settingsProfileViewModel.user.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    val scrollState = rememberScrollState()
    
    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                settingsProfileViewModel.save()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopBar(
                onBack = {
                    scope.launch {
                        settingsProfileViewModel.save()
                        navHost.removeLastOrNull()
                    }
                })
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
                    SectionHeader("Ваше имя")
                }) {
                    InputField(
                        placeholder = stringResource(R.string.first_name),
                        value = user.firstName,
                        onValueChange = settingsProfileViewModel::onChangeFirstName
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 1.dp,
                    )
                    
                    InputField(
                        placeholder = stringResource(R.string.last_name),
                        value = user.lastName.orEmpty(),
                        onValueChange = settingsProfileViewModel::onChangeLastName
                    )
                }
                
                
                SectionContainer(
                    header = {
                        SectionHeader(title = stringResource(R.string.bio))
                    },
                    footer = {
                        SectionDescription("В настройках можно выбрать, кому они будут видны.")
                    }) {
                    InputField(
                        placeholder = "Напишите что-нибудь о себе",
                        value = user.bio.orEmpty(),
                        onValueChange = settingsProfileViewModel::onChangeBio
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
                        text = if (user.username != null) {
                            "@${user.username}"
                        } else {
                            "Задать имя пользователя"
                        },
                        onClick = {
                            scope.launch {
                                navHost.add(AppRoute.SettingsUsername)
                                settingsProfileViewModel.save()
                            }
                        }
                    )
                }
                
                
                val locale = LocalLocale.current.platformLocale
                
                SectionContainer(header = {
                    SectionHeader(title = stringResource(R.string.date_of_birth))
                }) {
                    SectionItem(
                        text = "Дата Вашего рождения",
                        primaryText = if (user.dateOfBirth != null) {
                            SimpleDateFormat(
                                "d MMM yyyy",
                                locale
                            ).format(user.dateOfBirth)
                        } else {
                            "Указать"
                        },
                        onClick = isVisibleDatePicker::show
                    )
                    
                    AnimatedContent(targetState = user.dateOfBirth) { dateOfBirth ->
                        if (dateOfBirth != null) {
                            SectionItem(
                                text = stringResource(R.string.remove_date_of_birth),
                                onClick = {
                                    settingsProfileViewModel.onChangeDateOfBirth(null)
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                }
                
                val datePickerState = rememberDatePickerState(user.dateOfBirth)
                
                if (isVisibleDatePicker.isVisible) {
                    DatePickerDialog(
                        onDismissRequest = isVisibleDatePicker::hide,
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val selected = datePickerState.selectedDateMillis
                                    if (selected != null) {
                                        settingsProfileViewModel.onChangeDateOfBirth(selected)
                                    }
                                    isVisibleDatePicker.hide()
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
                                onClick = isVisibleDatePicker::hide,
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
