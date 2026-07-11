/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.invites.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyDateWithYear
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.section.SectionToggleItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun CreateGroupInviteLinkScreen(
    groupId: Long, viewModel: CreateGroupInviteLinkViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(groupId) {
        viewModel.init(groupId)
    }
    val context = LocalContext.current
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateGroupInviteLinkEffect.NavigateBack -> navBackStack.removeLastOrNull()
                
                is CreateGroupInviteLinkEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(
                            message = effect.message.asString(context),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }
    
    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.expirationDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val selectedDate =
                        Instant.ofEpochMilli(utcTimeMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    
                    val today = LocalDate.now()
                    
                    return !selectedDate.isBefore(today)
                }
            })
        
        DatePickerDialog(onDismissRequest = viewModel::hideDatePicker, confirmButton = {
            TextButton(onClick = {
                viewModel.onExpirationDateChange(datePickerState.selectedDateMillis)
            }) {
                Text(stringResource(R.string.ok))
            }
        }, dismissButton = {
            TextButton(onClick = viewModel::hideDatePicker) {
                Text(stringResource(R.string.cancel))
            }
        }) {
            DatePicker(state = datePickerState)
        }
    }
    
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.new_link)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                ),
                actions = listOf(
                    TopBarAction(
                        icon = Icons.Rounded.Check, onClick = viewModel::createLink
                    )
                )
            )
        },
        snackbarHost = {
            CustomSnackbar(snackbarHostState)
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SectionContainer(
                header = { SectionHeader(title = "Ограничения") },
                footer = { SectionDescription(text = "Вы можете ограничить срок действия ссылки или количество её использований.") }) {
                
                if (!uiState.requireApproval) {
                    FramelessTextBox(
                        value = uiState.maxUses,
                        onValueChange = viewModel::onMaxUsesChange,
                        placeholder = "Количество использований",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                
                SectionItem(
                    headlineText = "Срок действия",
                    trailingText = uiState.expirationDate?.toInstance()?.toPrettyDateWithYear()
                        ?: "Бессрочно",
                    onClick = viewModel::showDatePicker
                )
                
                AnimatedContent(targetState = uiState.expirationDate) { date ->
                    if (date != null) {
                        SectionItem(
                            headlineText = "Сбросить срок действия",
                            contentColor = MaterialTheme.colorScheme.error,
                            onClick = { viewModel.onExpirationDateChange(null) })
                    }
                }
                
                SectionToggleItem(
                    text = "Заявки на вступление",
                    isChecked = uiState.requireApproval,
                    onCheckedChange = { viewModel.onRequireApprovalChange(!uiState.requireApproval) }
                )
            }
        }
    }
}
