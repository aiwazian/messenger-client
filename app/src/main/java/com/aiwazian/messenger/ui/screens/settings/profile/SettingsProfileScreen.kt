/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import android.graphics.Bitmap
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyDateWithYear
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun SettingsProfileScreen(viewModel: SettingsProfileViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is SettingsProfileSideEffect.NavigateBack -> navBackStack.removeLastOrNull()
                is SettingsProfileSideEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(effect.message.asString(context))
                    }
                }
            }
        }
    }
    
    val scrollState = rememberScrollState()
    
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.profile)) }, navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                ), actions = listOf(
                    TopBarAction(
                        icon = Icons.Rounded.Check, onClick = viewModel::onSaveAndBack
                    )
                )
            )
        },
        snackbarHost = {
            CustomSnackbar(snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            Box(modifier = Modifier.padding(start = 10.dp)) {
                SectionHeader(title = stringResource(R.string.profile_photos))
            }
            
            SettingsProfileImageCarousel(
                avatars = uiState.user.avatars,
                onAddPhoto = viewModel::setPendingAvatarUri,
                onDeletePhoto = viewModel::deleteAvatar
            )
            
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
            
            SectionContainer(footer = {
                SectionDescription(text = stringResource(R.string.write_about_me))
            }) {
                FramelessTextBox(
                    placeholder = stringResource(R.string.bio),
                    value = uiState.user.bio.orEmpty(),
                    onValueChange = viewModel::onChangeBio,
                    singleLine = false
                )
            }
            
            SectionContainer(header = {
                SectionHeader(title = stringResource(R.string.username))
            }) {
                SectionItem(
                    headlineText = if (uiState.user.username != null) {
                        "@${uiState.user.username}"
                    } else {
                        "Задать имя пользователя"
                    }, onClick = {
                        viewModel.save()
                        navBackStack.add(AppRoute.SettingsUsername(uiState.user.username))
                    })
            }
            
            SectionContainer {
                SectionItem(
                    headlineText = stringResource(R.string.date_of_birth),
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
            
            SectionContainer {
                SectionItem(
                    headlineText = stringResource(R.string.personal_channel),
                    supportingText = uiState.profileChannelName,
                    onClick = {
                        viewModel.save()
                        navBackStack.add(AppRoute.SettingsSelectChannel)
                    }
                )
            }
            
            if (uiState.showDatePicker) {
                val datePickerState = rememberDatePickerState(uiState.user.dateOfBirth)
                DatePickerDialog(onDismissRequest = viewModel::hideDatePicker, confirmButton = {
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
                }, dismissButton = {
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
                        title = { }, state = datePickerState
                    )
                }
            }
        }
    }
    
    if (uiState.pendingAvatarUri != null) {
        val context = LocalContext.current
        AvatarCropScreen(
            imageUri = uiState.pendingAvatarUri!!, onCropConfirmed = { bitmap ->
                val file = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                val contentUri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                
                viewModel.uploadAvatar(contentUri)
                viewModel.clearPendingAvatarUri()
            }, onDismiss = viewModel::clearPendingAvatarUri
        )
    }
}
