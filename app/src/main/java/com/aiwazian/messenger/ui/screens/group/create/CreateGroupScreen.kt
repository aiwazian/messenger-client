/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CreateGroupScreen(viewModel: CreateGroupViewModel = hiltViewModel()) {
    val navHost = LocalNavHost.current
    
    val groupInfo by viewModel.groupInfo.collectAsState()
    val createState by viewModel.createState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.createEffect.collectLatest { effect ->
            when (effect) {
                is CreateGroupEffect.NavigateToChat -> {
                    val chat = Chat(
                        id = effect.chat.id,
                        chatName = effect.chat.chatName
                    )
                    
                    navHost.clear()
                    navHost.add(AppRoute.Main)
                    navHost.add(AppRoute.Chat(chat.id))
                }
            }
        }
    }
    
    val isLoading = createState is CreateGroupState.Loading
    
    Scaffold(
        topBar = {
            TopBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.imePadding(),
                onClick = {
                    viewModel.createGroup()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                AnimatedContent(targetState = isLoading) { isLoading ->
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
        }
    ) {
        Column(
            Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            SectionContainer {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    value = groupInfo.name,
                    onValueChange = viewModel::changeGroupName,
                    placeholder = { Text(stringResource(R.string.group_name)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navHost = LocalNavHost.current
    
    PageTopBar(
        title = { Text(text = stringResource(R.string.create_group)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navHost::removeLastOrNull
        )
    )
}

