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
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.components.InputField
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.utils.VibrationPattern
import kotlinx.coroutines.launch

@Composable
fun SettingsUsernameScreen() {
    Content()
}

@Composable
private fun Content() {
    val navHost = LocalNavHost.current
    
    val viewModel = hiltViewModel<SettingsUsernameViewModel>()
    
    val username by viewModel.username.collectAsState()
    
    val errorText = viewModel.errorText
    
    LaunchedEffect(Unit) {
        viewModel.init()
    }
    
    Scaffold(topBar = {
        TopBar(
            navHost::removeLastOrNull,
            viewModel
        )
    }) {
        Column(modifier = Modifier.padding(it)) {
            SectionContainer {
                InputField(
                    placeholder = stringResource(R.string.username),
                    value = username,
                    onValueChange = viewModel::onChangeUsername
                )
            }
            
            AnimatedContent(targetState = errorText) { text ->
                if (text != null) {
                    Text(
                        text = text,
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
    viewModel: SettingsUsernameViewModel
) {
    val navHost = LocalNavHost.current
    
    val canSave by viewModel.canSave.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    val actions = if (canSave) {
        listOf(
            TopBarAction(
                icon = Icons.Rounded.Check,
                onClick = {
                    scope.launch {
                        val isSaved = viewModel.trySave()
                        
                        if (isSaved) {
                            navHost.removeLastOrNull()
                        } else {
                            viewModel.vibrate(VibrationPattern.Error)
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



