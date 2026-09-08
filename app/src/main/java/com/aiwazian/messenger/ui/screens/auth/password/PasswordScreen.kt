/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.password

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.BottomAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.screens.auth.components.PasswordField
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun PasswordScreen(
    login: String,
    canReset: Boolean = false,
    viewModel: PasswordViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.setLogin(login)
        viewModel.setCanReset(canReset)
    }
    
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
                
                is PasswordUiEffect.NavigateToPasswordResetCode -> {
                    navBackStack.add(AppRoute.PasswordResetCode(effect.login))
                }
            }
        }
    }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.canReset) {
                        Text(text = buildAnnotatedString {
                            withLink(
                                link = LinkAnnotation.Clickable(
                                    tag = "forgot_password",
                                    styles = TextLinkStyles(
                                        style = SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.None
                                        ),
                                        pressedStyle = SpanStyle(
                                            background = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.4f
                                            )
                                        )
                                    ),
                                    linkInteractionListener = {
                                        viewModel.showForgotPasswordDialog()
                                    }
                                )
                            ) {
                                append(stringResource(R.string.forgot_password))
                            }
                        })
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    FloatingActionButton(
                        onClick = viewModel::signIn,
                        shape = CircleShape
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                            )
                        }
                    }
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
                    text = stringResource(R.string.authorization),
                    fontSize = 28.sp
                )
                Text(
                    text = stringResource(R.string.password_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                Spacer(Modifier.height(20.dp))
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
        }
        
        if (uiState.showForgotPasswordDialog) {
            AppDialog(
                title = stringResource(R.string.app_name),
                onDismissRequest = viewModel::hideForgotPasswordDialog,
                content = {
                    Text(
                        text = "Отправить код для сброса пароля на электронную почту привязанную к аккаунту?",
                        lineHeight = 18.sp
                    )
                },
                buttons = {
                    TextButton(onClick = viewModel::hideForgotPasswordDialog) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = viewModel::requestPasswordReset) {
                        Text(stringResource(R.string.send))
                    }
                }
            )
        }
    }
}
