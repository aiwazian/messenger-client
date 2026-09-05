/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.screens.auth.components.InputTextField
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is LoginUiEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(effect.message.asString(context))
                    }
                }
            }
        }
    }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::checkLogin,
                shape = CircleShape
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        snackbarHost = {
            AppSnackbar(snackbarHostState)
        }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .padding(
                        top = it.calculateTopPadding(),
                        bottom = it.calculateBottomPadding() + 10.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.user_login),
                    fontSize = 28.sp
                )
                Text(
                    text = stringResource(R.string.login_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                Spacer(Modifier.height(20.dp))
                InputTextField(
                    modifier = Modifier.focusRequester(focusRequester),
                    value = uiState.login,
                    onValueChange = viewModel::changeLogin,
                    label = stringResource(R.string.login),
                    isError = uiState.errorText != null,
                    supportingText = uiState.errorText?.asString(),
                    onSendClick = viewModel::checkLogin
                )
            }
        }
        
        if (uiState.showFoundDialog) {
            AppDialog(
                title = stringResource(R.string.app_name),
                onDismissRequest = viewModel::hideFoundDialog,
                content = {
                    Text(
                        text = "Аккаунт с таким логином уже существует. Войти?",
                        lineHeight = 18.sp
                    )
                },
                buttons = {
                    TextButton(onClick = viewModel::hideFoundDialog) {
                        Text(stringResource(R.string.no))
                    }
                    TextButton(onClick = {
                        viewModel.hideFoundDialog()
                        navBackStack.add(AppRoute.Password(uiState.login, uiState.canReset))
                    }) {
                        Text(stringResource(R.string.yes))
                    }
                }
            )
        }
        
        if (uiState.showNotFoundDialog) {
            AppDialog(
                title = stringResource(R.string.app_name),
                onDismissRequest = viewModel::hideNotFoundDialog,
                content = {
                    Text(
                        text = "Аккаунт не найден. Зарегистрироваться?",
                        lineHeight = 18.sp
                    )
                },
                buttons = {
                    TextButton(onClick = viewModel::hideNotFoundDialog) {
                        Text(stringResource(R.string.no))
                    }
                    TextButton(onClick = {
                        viewModel.hideNotFoundDialog()
                        navBackStack.add(AppRoute.Register(uiState.login))
                    }) {
                        Text(stringResource(R.string.yes))
                    }
                }
            )
        }
    }
}
