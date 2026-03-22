/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.login

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.CustomDialog

@Composable
fun PasswordScreen(
    authViewModel: AuthViewModel = hiltViewModel<AuthViewModel>(LocalContext.current as ComponentActivity)
) {
    val context = LocalContext.current
    
    val passwordFieldError by authViewModel.passwordFieldError.collectAsState()
    val isLoadingPassword by authViewModel.isLoadingPassword.collectAsState()
    
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordDialogType by remember { mutableStateOf<String?>(null) }
    var passwordDialogError by remember { mutableStateOf<String?>(null) }

    val uiEffect by authViewModel.uiEffect.collectAsState(null)

    LaunchedEffect(uiEffect) {
        when (uiEffect) {
            is AuthUiEffect.ShowPasswordDialog -> {
                val effect = uiEffect as AuthUiEffect.ShowPasswordDialog
                passwordDialogType = effect.type
                passwordDialogError = effect.errorMessage
                showPasswordDialog = true
            }
            is AuthUiEffect.HidePasswordDialog -> {
                showPasswordDialog = false
            }
            is AuthUiEffect.NavigateToMain -> {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
                (context as Activity).finish()
            }
            else -> {}
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = authViewModel::onPasswordNextClicked,
                modifier = Modifier.imePadding(),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                if (!isLoadingPassword) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }) {
        val password by authViewModel.password.collectAsState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Пароль аккаунта",
                modifier = Modifier.padding(vertical = 40.dp),
                fontSize = 28.sp
            )
            Column(Modifier.width(300.dp)) {
                PasswordField(
                    value = password,
                    onValueChange = authViewModel::onPasswordChanged,
                    label = passwordFieldError ?: "Пароль",
                    isError = passwordFieldError != null
                )
            }
        }
        
        if (showPasswordDialog) {
            when (passwordDialogType) {
                "login" -> {
                    CustomDialog(
                        title = stringResource(R.string.app_name),
                        onDismissRequest = authViewModel::hidePasswordDialog,
                        content = {
                            Text(
                                text = passwordDialogError ?: "Не удалось войти в аккаунт. Попробуйте ещё раз.",
                                lineHeight = 18.sp
                            )
                        },
                        buttons = {
                            TextButton(onClick = authViewModel::hidePasswordDialog) {
                                Text("Ок")
                            }
                        })
                }

                "register" -> {
                    CustomDialog(
                        title = stringResource(R.string.app_name),
                        onDismissRequest = authViewModel::hidePasswordDialog,
                        content = {
                            Text(
                                text = passwordDialogError ?: "Не удалось создать пользователя. Попробуйте ещё раз.",
                                lineHeight = 18.sp
                            )
                        },
                        buttons = {
                            TextButton(onClick = authViewModel::hidePasswordDialog) {
                                Text("Ок")
                            }
                        })
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
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
        isError = isError,
        visualTransformation = if (!passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            IconButton(onClick = {
                passwordVisible = !passwordVisible
            }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        })
}



