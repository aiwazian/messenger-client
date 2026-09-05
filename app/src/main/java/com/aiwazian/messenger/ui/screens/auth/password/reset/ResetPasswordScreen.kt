/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.password.reset

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.topBar.PageTopBar

@Composable
fun ResetPasswordScreen(
    login: String,
    code: String,
    viewModel: ResetPasswordViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.setLogin(login)
        viewModel.setCode(code)
    }
    
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ResetPasswordUiEffect.NavigateToMainActivity -> {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                    (context as Activity).finish()
                }
                
                is ResetPasswordUiEffect.ShowSnackbar -> {
                    // Handle snackbar if needed
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            PageTopBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::resetPassword,
                shape = CircleShape,
                modifier = Modifier.imePadding(),
            ) {
                AnimatedContent(targetState = uiState.isLoading) { loading ->
                    if (loading) {
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
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                shape = MaterialTheme.shapes.medium,
                value = uiState.newPassword,
                onValueChange = viewModel::onInputNewPassword,
                label = {
                    Text(
                        uiState.errorText?.asString() ?: stringResource(R.string.enter_new_password)
                    )
                },
                supportingText = {
                    if (uiState.errorText == null) {
                        Text(stringResource(R.string.new_password_hint))
                    }
                },
                isError = uiState.errorText != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                )
            )
        }
    }
}
