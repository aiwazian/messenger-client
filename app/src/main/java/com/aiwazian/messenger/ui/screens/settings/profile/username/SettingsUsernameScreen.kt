/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile.username

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun SettingsUsernameScreen(
    username: String?,
    viewModel: SettingsUsernameViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.initUsername(username)
    }
    
    val navBackStack = LocalNavBackStack.current
    
    val context = LocalContext.current
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is UsernameUiEffect.NavigateBack -> navBackStack.removeLastOrNull()
                
                is UsernameUiEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(effect.message.asString(context))
                    }
                }
            }
        }
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.username)) },
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
        },
        snackbarHost = {
            AppSnackbar(snackbarHostState)
        }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                FramelessTextBox(
                    placeholder = stringResource(R.string.username),
                    value = uiState.username,
                    onValueChange = viewModel::onChangeUsername,
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
