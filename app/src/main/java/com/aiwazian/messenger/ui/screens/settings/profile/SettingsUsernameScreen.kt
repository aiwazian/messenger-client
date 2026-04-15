/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import kotlinx.coroutines.launch

@Composable
fun SettingsUsernameScreen(
    username: String?,
    viewModel: SettingsUsernameViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initUsername(username)
    }
    
    Scaffold(topBar = {
        TopBar(
            onBack = navBackStack::removeLastOrNull,
            viewModel = viewModel,
            uiState = uiState
        )
    }) {
        Column(modifier = Modifier.padding(it)) {
            SectionContainer {
                FramelessTextBox(
                    placeholder = stringResource(R.string.username),
                    value = uiState.username,
                    onValueChange = viewModel::onChangeUsername
                )
            }
            
            AnimatedContent(targetState = uiState.messageText) { text ->
                if (text != null) {
                    Text(
                        text = text,
                        color = uiState.messageColor,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 8.dp
                        ),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit,
    viewModel: SettingsUsernameViewModel,
    uiState: UsernameScreenUiState
) {
    val navBackStack = LocalNavBackStack.current
    
    val scope = rememberCoroutineScope()
    
    val actions = if (uiState.isAvailable) {
        listOf(
            TopBarAction(
                icon = Icons.Rounded.Check,
                onClick = {
                    scope.launch {
                        if (viewModel.save()) {
                            navBackStack.removeLastOrNull()
                        }
                    }
                })
        )
    } else {
        emptyList()
    }
    
    PageTopBar(
        title = { Text(stringResource(R.string.username)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = onBack::invoke
        ),
        actions = actions
    )
}
