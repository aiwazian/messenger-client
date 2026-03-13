package com.aiwazian.messenger.ui.settings.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.TopBarAction
import com.aiwazian.messenger.ui.element.InputField
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionDescription
import com.aiwazian.messenger.ui.element.SectionHeader
import com.aiwazian.messenger.ui.element.SectionItem
import com.aiwazian.messenger.viewModels.NavigationViewModel
import com.aiwazian.messenger.viewModels.SettingsProfileViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SettingsProfileScreen() {
    Content()
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val navViewModel = viewModel<NavigationViewModel>()
    
    val actions = arrayOf(
        TopBarAction(
            icon = Icons.Outlined.MoreVert,
            //            dropdownActions = arrayOf(
            //                DropdownMenuAction(
            //                    icon = Icons.Outlined.ColorLens,
            //                    text = stringResource(R.string.change_profile_color),
            //                    onClick = { navViewModel.addScreenInStack { SettingsProfileColorScreen() } })
            //            )
        )
    )
    
    PageTopBar(
        title = { Text(stringResource(R.string.profile)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = onBack::invoke
        ),
        //        actions = actions
    )
}

@Composable
private fun Content() {
    val navViewModel = viewModel<NavigationViewModel>()
    
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
                        navViewModel.removeLastScreenInStack()
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
                
                SectionHeader("Ваше имя")
                
                SectionContainer {
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
                        value = user.lastName,
                        onValueChange = settingsProfileViewModel::onChangeLastName
                    )
                }
                
                SectionHeader(title = stringResource(R.string.bio))
                
                SectionContainer {
                    InputField(
                        placeholder = "Напишите что-нибудь о себе",
                        value = user.bio,
                        onValueChange = settingsProfileViewModel::onChangeBio
                    )
                }
                
                SectionDescription("В настройках можно выбрать, кому они будут видны.")
                
                SectionHeader(title = stringResource(R.string.username))
                
                SectionContainer {
                    SectionItem(
                        text = if (user.username != null) {
                            "@${user.username}"
                        } else {
                            "Задать имя пользователя"
                        },
                        onClick = {
                            scope.launch {
                                navViewModel.addScreenInStack {
                                    SettingsUsernameScreen()
                                }
                                settingsProfileViewModel.save()
                            }
                        }
                    )
                }
                
                SectionDescription("Другие пользователи смогут найти Вас по такому имени и связаться.")
                
                SectionHeader(title = stringResource(R.string.date_of_birth))
                
                SectionContainer {
                    SectionItem(
                        text = "Дата Вашего рождения",
                        primaryText = if (user.dateOfBirth != null) {
                            SimpleDateFormat(
                                "d MMM yyyy",
                                Locale.getDefault()
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
                
                if (isVisibleDatePicker.isVisible) {
                    val datePickerState = rememberDatePickerState(user.dateOfBirth)
                    
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
