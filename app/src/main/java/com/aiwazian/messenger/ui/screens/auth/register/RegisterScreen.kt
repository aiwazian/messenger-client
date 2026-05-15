/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.auth.register

import android.app.Activity
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.CustomSnackbar
import com.aiwazian.messenger.ui.screens.auth.components.InputTextField
import com.aiwazian.messenger.ui.screens.auth.components.PasswordField
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(login: String, viewModel: RegisterViewModel = hiltViewModel()) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.setLogin(login)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is RegisterUiEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(effect.message.asString(context))
                    }
                }
                
                is RegisterUiEffect.NavigateToMainActivity -> {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                    (context as Activity).finish()
                }
            }
        }
    }
    
    val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setTranslateLocale(LocalLocale.current.platformLocale)
        .build()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::signUp,
                modifier = Modifier.imePadding(),
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
                text = stringResource(R.string.registration),
                fontSize = 28.sp
            )
            Column(
                Modifier
                    .width(300.dp)
                    .imePadding()
            ) {
                InputTextField(
                    value = uiState.firstName,
                    onValueChange = viewModel::changeFirstName,
                    label = stringResource(R.string.first_name),
                    isError = uiState.firstNameFieldError != null,
                    supportingText = uiState.firstNameFieldError?.asString()
                )
                
                InputTextField(
                    value = uiState.lastName,
                    onValueChange = viewModel::changeLastName,
                    label = "${stringResource(R.string.last_name)} (${stringResource(R.string.optional)})"
                )
                
                PasswordField(
                    value = uiState.password,
                    onValueChange = viewModel::changePassword,
                    label = stringResource(R.string.password),
                    isError = uiState.passwordFieldError != null,
                    supportingText = uiState.passwordFieldError?.asString(),
                    onSendClick = viewModel::signUp
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.checkedPrivacyTerms,
                        onCheckedChange = viewModel::changePrivacyCheck,
                        colors = CheckboxDefaults.colors(
                            uncheckedBorderColor = if (uiState.isPrivacyError) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    )
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.i_agree))
                            append(" ")
                            withLink(
                                LinkAnnotation.Clickable(
                                    tag = "privacy",
                                    styles = TextLinkStyles(
                                        style = SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.Underline
                                        ),
                                        pressedStyle = SpanStyle(
                                            background = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.4f
                                            )
                                        )
                                    ),
                                    linkInteractionListener = {
                                        customTabsIntent.launchUrl(
                                            context, "https://aiwazian.ru/privacy".toUri()
                                        )
                                    }
                                )
                            ) {
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append(stringResource(R.string.privacy_policy))
                                }
                            }
                            append(" & ")
                            withLink(
                                LinkAnnotation.Clickable(
                                    tag = "terms",
                                    styles = TextLinkStyles(
                                        style = SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.Underline
                                        ),
                                        pressedStyle = SpanStyle(
                                            background = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.4f
                                            )
                                        )
                                    ),
                                    linkInteractionListener = {
                                        customTabsIntent.launchUrl(
                                            context, "https://aiwazian.ru/tos".toUri()
                                        )
                                    }
                                )
                            ) {
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append(stringResource(R.string.terms_of_use))
                                }
                            }
                        },
                        fontSize = 14.sp,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(it.calculateBottomPadding() + 50.dp))
            }
        }
    }
}
