/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.password

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.screens.auth.components.PasswordField
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun PasswordScreen(login: String, viewModel: PasswordViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.setLogin(login)
    }
    
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is PasswordUiEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(effect.message.asString(context))
                    }
                }
                
                is PasswordUiEffect.NavigateToMainActivity -> {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                    (context as Activity).finish()
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
                onClick = viewModel::signIn,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary,
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
            CustomSnackbar(snackbarHostState)
        }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(50.dp)
        ) {
            Spacer(modifier = Modifier.height(it.calculateTopPadding() + 50.dp))
            Text(
                text = stringResource(R.string.authorization),
                fontSize = 28.sp
            )
            Column(Modifier.width(300.dp)) {
                PasswordField(
                    modifier = Modifier.focusRequester(focusRequester),
                    value = uiState.password,
                    onValueChange = viewModel::changePassword,
                    label = stringResource(R.string.password),
                    isError = uiState.errorText != null,
                    supportingText = uiState.errorText?.asString(),
                    onSendClick = viewModel::signIn
                )
            }
            Spacer(modifier = Modifier.height(it.calculateBottomPadding() + 50.dp))
        }
    }
}
