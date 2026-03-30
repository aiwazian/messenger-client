/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.login

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = hiltViewModel(LocalActivity.current as ComponentActivity)
) {
    val navHost = LocalNavHost.current
    
    val loginFieldError by authViewModel.loginFieldError.collectAsState()
    val isLoadingLogin by authViewModel.isLoadingLogin.collectAsState()
    val checkError by authViewModel.checkError.collectAsState()
    
    var showLoginDialog by remember { mutableStateOf(false) }
    var loginDialogResult by remember { mutableStateOf<Boolean?>(null) }
    
    val uiEffect by authViewModel.uiEffect.collectAsState(null)
    
    LaunchedEffect(uiEffect) {
        when (uiEffect) {
            is AuthUiEffect.ShowLoginDialog -> {
                loginDialogResult = (uiEffect as AuthUiEffect.ShowLoginDialog).result
                showLoginDialog = true
            }
            
            is AuthUiEffect.HideLoginDialog -> {
                showLoginDialog = false
            }
            
            is AuthUiEffect.NavigateToPassword -> {
                navHost.add(AppRoute.Password)
            }
            
            else -> {}
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = authViewModel::onLoginNextClicked,
                modifier = Modifier.imePadding(),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                if (isLoadingLogin) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
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
        }) {
        val login by authViewModel.login.collectAsState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Логин пользователя",
                modifier = Modifier.padding(vertical = 40.dp),
                fontSize = 28.sp
            )
            Column(Modifier.width(300.dp)) {
                LoginField(
                    value = login,
                    onValueChange = authViewModel::onLoginChanged,
                    label = loginFieldError ?: "Логин",
                    isError = loginFieldError != null
                )
                checkError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        if (showLoginDialog) {
            when (loginDialogResult) {
                null -> {
                    CustomDialog(
                        title = stringResource(R.string.app_name),
                        onDismissRequest = authViewModel::hideLoginDialog,
                        content = {
                            Text(
                                text = checkError ?: "Не удалось проверить, попробуйте ещё раз.",
                                lineHeight = 18.sp
                            )
                        },
                        buttons = {
                            TextButton(onClick = authViewModel::hideLoginDialog) {
                                Text(stringResource(R.string.ok))
                            }
                        }
                    )
                }
                
                true -> {
                    CustomDialog(
                        title = stringResource(R.string.app_name),
                        onDismissRequest = authViewModel::hideLoginDialog,
                        content = {
                            Text(
                                text = "Пользователь найден. Продолжить?",
                                lineHeight = 18.sp
                            )
                        },
                        buttons = {
                            TextButton(onClick = authViewModel::hideLoginDialog) {
                                Text(stringResource(R.string.no))
                            }
                            TextButton(onClick = {
                                authViewModel.hideLoginDialog()
                                authViewModel.navigateToPassword()
                            }) {
                                Text(stringResource(R.string.yes))
                            }
                        }
                    )
                }
                
                false -> {
                    CustomDialog(
                        title = stringResource(R.string.app_name),
                        onDismissRequest = authViewModel::hideLoginDialog,
                        content = {
                            Text(
                                text = "Пользователь не найден. Создать?",
                                lineHeight = 18.sp
                            )
                        },
                        buttons = {
                            TextButton(onClick = authViewModel::hideLoginDialog) {
                                Text(stringResource(R.string.no))
                            }
                            TextButton(onClick = {
                                authViewModel.hideLoginDialog()
                                authViewModel.navigateToPassword()
                            }) {
                                Text(stringResource(R.string.yes))
                            }
                        }
                    )
                }
            }
        }
        
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary,
            errorLabelColor = MaterialTheme.colorScheme.error,
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorTextColor = MaterialTheme.colorScheme.error
        ),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError
    )
}
