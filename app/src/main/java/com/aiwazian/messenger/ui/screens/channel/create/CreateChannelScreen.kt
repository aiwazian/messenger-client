/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CreateChannelScreen(viewModel: CreateChannelViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.createEffect.collectLatest { effect ->
            when (effect) {
                is CreateChannelEffect.NavigateToChat -> {
                    navBackStack.clear()
                    navBackStack.add(AppRoute.Main)
                    navBackStack.add(AppRoute.Chat(effect.chatId))
                }
                
                is CreateChannelEffect.ShowSnackbar -> {
                
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.imePadding(),
                onClick = {
                    viewModel.createChannel()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                AnimatedContent(targetState = uiState.isLoading) { isLoading ->
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            null
                        )
                    }
                }
            }
        }) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            SectionContainer(footer = {
                SectionDescription("Можете указать дополнительное описание канала.")
            }) {
                FramelessTextBox(
                    value = uiState.name,
                    onValueChange = viewModel::changeName,
                    placeholder = stringResource(R.string.channel_name)
                )
                
                FramelessTextBox(
                    value = uiState.bio,
                    onValueChange = viewModel::changeBio,
                    placeholder = stringResource(R.string.description),
                )
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navBackStack = LocalNavBackStack.current
    
    PageTopBar(
        title = { Text(text = stringResource(R.string.create_channel)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navBackStack::removeLastOrNull
        )
    )
}
