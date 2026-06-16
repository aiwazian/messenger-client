/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.type

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionRadioItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun GroupTypeSettingsScreen(
    groupId: Long, viewModel: GroupTypeSettingsViewModel = hiltViewModel()
) {
    LaunchedEffect(groupId) {
        viewModel.init(groupId)
    }
    
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is GroupTypeSettingsEffect.NavigateBack -> navBackStack.removeLastOrNull()
                
                is GroupTypeSettingsEffect.ShowSnackbar -> {
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
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.group_type)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                ),
                actions = if (uiState.canSave) {
                    listOf(
                        TopBarAction(
                            icon = Icons.Rounded.Check,
                            onClick = viewModel::save
                        )
                    )
                } else emptyList()
            )
        }, snackbarHost = {
            CustomSnackbar(snackbarHostState)
        }, modifier = Modifier.imePadding()
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                SectionRadioItem(
                    text = stringResource(R.string.private_group),
                    selected = uiState.groupType == GroupType.PRIVATE,
                    description = "В частные группы можно вступить только по ссылке-приглашению.",
                    onClick = { viewModel.changeGroupType(GroupType.PRIVATE) })
                SectionRadioItem(
                    text = stringResource(R.string.public_group),
                    selected = uiState.groupType == GroupType.PUBLIC,
                    description = "Публичные группы можно найти через поиск.",
                    onClick = { viewModel.changeGroupType(GroupType.PUBLIC) })
            }
            
            AnimatedContent(
                targetState = uiState.groupType,
                transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() },
                label = "group_type_animation"
            ) { type ->
                if (type == GroupType.PUBLIC) {
                    Column {
                        SectionContainer(header = { SectionHeader(title = stringResource(R.string.public_link)) }) {
                            FramelessTextBox(
                                placeholder = stringResource(R.string.username),
                                value = uiState.username,
                                onValueChange = viewModel::onChangeUsername
                            )
                        }
                        
                        var text by remember { mutableStateOf("") }
                        uiState.statusText?.asString()?.let { text = it }
                        AnimatedVisibility(
                            visible = !uiState.statusText?.asString().isNullOrBlank(),
                            enter = slideInVertically { -it } + fadeIn(),
                            exit = slideOutVertically { -it } + fadeOut(),
                            modifier = Modifier.padding(start = 24.dp)
                        ) {
                            Text(
                                text = text,
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                color = if (uiState.isError) MaterialTheme.colorScheme.error else Color.Unspecified
                            )
                        }
                    }
                }
            }
        }
    }
}
