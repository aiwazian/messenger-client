/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.type

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.section.SectionRadioItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.screens.channel.settings.type.LinkCheckStatus

@Composable
fun GroupTypeSettingsScreen(
    groupId: Long,
    viewModel: GroupTypeSettingsViewModel = hiltViewModel()
) {
    val navHost = LocalNavHost.current
    
    LaunchedEffect(groupId) {
        viewModel.init(groupId)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is GroupTypeSettingsEffect.NavigateBack -> navHost.removeLastOrNull()
                
                else -> {}
            }
        }
    }
    
    val actions = if (uiState.canSave) {
        listOf(TopBarAction(icon = Icons.Rounded.Check, onClick = { viewModel.save() }))
    } else emptyList()
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.group_type)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navHost::removeLastOrNull
                ),
                actions = actions
            )
        },
        modifier = Modifier.imePadding()
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer(header = { SectionHeader(title = stringResource(R.string.group_type)) }) {
                SectionRadioItem(
                    text = "Приватная группа",
                    selected = uiState.groupType == GroupType.PRIVATE,
                    description = "В частные группы можно вступить только по ссылке-приглашению.",
                    onClick = { viewModel.changeGroupType(GroupType.PRIVATE) }
                )
                SectionRadioItem(
                    text = "Публичная группа",
                    selected = uiState.groupType == GroupType.PUBLIC,
                    description = "Публичные группы можно найти через поиск.",
                    onClick = { viewModel.changeGroupType(GroupType.PUBLIC) }
                )
            }
            
            AnimatedContent(
                targetState = uiState.groupType,
                transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() },
                label = "group_type_animation"
            ) { type ->
                if (type == GroupType.PUBLIC) {
                    Column {
                        SectionContainer(header = { SectionHeader(title = "Публичная ссылка") }) {
                            FramelessTextBox(
                                placeholder = stringResource(R.string.username),
                                value = uiState.username,
                                onValueChange = viewModel::changePublicLink
                            )
                        }
                        
                        val (message, color) = when (val status = uiState.linkCheckStatus) {
                            LinkCheckStatus.Available -> "Имя доступно" to MaterialTheme.colorScheme.primary
                            LinkCheckStatus.Busy -> "Имя занято" to MaterialTheme.colorScheme.error
                            is LinkCheckStatus.Error -> status.message to MaterialTheme.colorScheme.error
                            else -> null to null
                        }
                        
                        if (message != null) {
                            Text(text = message, modifier = Modifier.padding(16.dp), fontSize = 12.sp, color = color!!)
                        }
                    }
                }
            }
            
            SectionContainer {
                SectionItem(
                    leadingIcon = Icons.Rounded.Link,
                    headlineText = stringResource(R.string.invite_links),
                    onClick = { navHost.add(AppRoute.GroupInviteLinks(groupId)) }
                )
            }
        }
    }
}
