/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.type

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
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.section.SectionRadioItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction

@Composable
fun ChannelTypeSettingsScreen(
    channelId: Long,
    viewModel: ChannelTypeSettingsViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    
    LaunchedEffect(channelId) {
        viewModel.init(channelId)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ChannelTypeSettingsEffect.NavigateBack -> navBackStack.removeLastOrNull()
                is ChannelTypeSettingsEffect.ShowSnackbar -> {
                
                }
            }
        }
    }
    
    val actions = if (uiState.canSave) {
        listOf(
            TopBarAction(
                icon = Icons.Rounded.Check,
                onClick = viewModel::save
            )
        )
    } else {
        emptyList()
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.channel_type)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                ),
                actions = actions
            )
        },
        modifier = Modifier.imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            SectionContainer(header = {
                SectionHeader(title = stringResource(R.string.channel_type))
            }) {
                SectionRadioItem(
                    text = stringResource(R.string.private_channel),
                    selected = uiState.channelType == ChannelType.PRIVATE,
                    description = "На частные каналы можно подписаться только по ссылке-приглашению.",
                    onClick = { viewModel.changeChannelType(ChannelType.PRIVATE) }
                )
                SectionRadioItem(
                    text = stringResource(R.string.public_channel),
                    selected = uiState.channelType == ChannelType.PUBLIC,
                    description = "Публичные каналы можно найти через поиск, подписаться на них может любой пользователь.",
                    onClick = { viewModel.changeChannelType(ChannelType.PUBLIC) }
                )
            }
            
            AnimatedContent(
                targetState = uiState.channelType,
                transitionSpec = {
                    scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                },
                label = "channel_type_animation"
            ) { type ->
                if (type == ChannelType.PUBLIC) {
                    Column {
                        SectionContainer(header = {
                            SectionHeader(title = stringResource(R.string.public_link))
                        }, footer = {
                            SectionDescription(text = "Если у канала будет постоянная публичная ссылка, другие пользователи смогут найти его и подписаться.")
                        }) {
                            FramelessTextBox(
                                placeholder = stringResource(R.string.username),
                                value = uiState.username.orEmpty(),
                                onValueChange = { viewModel.changePublicLink(it) }
                            )
                        }
                        
                        val (message, color) = when (val status = uiState.linkCheckStatus) {
                            LinkCheckStatus.Idle -> null to null
                            LinkCheckStatus.Checking -> "Проверка..." to MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.6f
                            )
                            
                            LinkCheckStatus.Available -> "Публичное имя доступно" to MaterialTheme.colorScheme.primary
                            LinkCheckStatus.Busy -> "Публичная ссылка занята" to MaterialTheme.colorScheme.error
                            is LinkCheckStatus.Error -> status.message to MaterialTheme.colorScheme.error
                        }
                        
                        if (message != null && color != null) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 8.dp
                                ),
                                fontSize = 12.sp,
                                color = color
                            )
                        }
                    }
                }
            }
            
            SectionContainer {
                SectionItem(
                    leadingIcon = Icons.Rounded.Link,
                    headlineText = stringResource(R.string.invite_links),
                    onClick = {
                        navBackStack.add(AppRoute.ChannelInviteLinks(channelId))
                    }
                )
            }
        }
    }
}
