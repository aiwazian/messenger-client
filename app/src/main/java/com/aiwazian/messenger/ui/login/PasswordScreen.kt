package com.aiwazian.messenger.ui.login

import android.app.Activity
import android.content.Context
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.services.VibrateService
import com.aiwazian.messenger.ui.element.CustomDialog
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.viewModels.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun PasswordScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    
    val vibrateService = VibrateService(context)
    
    var isLoad by remember { mutableStateOf(true) }
    
    val scope = rememberCoroutineScope()
    
    var showFailureLogin by remember { mutableStateOf(false) }
    var showFailureRegister by remember { mutableStateOf(false) }
    val passwordFieldError by authViewModel.passwordFieldError.collectAsState()
    val isUserFound by authViewModel.isUserFound.collectAsState()
    
    val onClick = {
        val isValidPassword = authViewModel.checkValidPassword()
        
        scope.launch {
            if (!isValidPassword) {
                vibrateService.vibrate(VibrationPattern.Error)
                return@launch
            }
            
            if (isUserFound) {
                isLoad = false
                
                val isLogin = authViewModel.onLoginClicked()
                
                isLoad = true
                
                if (!isLogin) {
                    showFailureLogin = true
                    return@launch
                }
                
                startMainActivity(context)
            } else {
                isLoad = false
                
                val isRegister = authViewModel.onRegisterClicked()
                
                isLoad = true
                
                if (!isRegister) {
                    showFailureRegister = true
                    isLoad = true
                    return@launch
                }
                
                val isLogin = authViewModel.onLoginClicked()
                
                if (!isLogin) {
                    showFailureRegister = true
                    return@launch
                }
                
                startMainActivity(context)
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            authViewModel.onPasswordChanged("")
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onClick()
                },
                modifier = Modifier.imePadding(),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                if (isLoad) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
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
        
        if (showFailureLogin) {
            CustomDialog(
                title = stringResource(R.string.app_name),
                onDismissRequest = { showFailureLogin = false },
                content = {
                    Text(
                        text = "Не удалось войти в аккаунт. Попробуйте ещё раз.",
                        lineHeight = 18.sp
                    )
                },
                buttons = {
                    TextButton(onClick = {
                        showFailureLogin = false
                    }) {
                        Text("Ок")
                    }
                })
        }
        
        if (showFailureRegister) {
            CustomDialog(
                title = stringResource(R.string.app_name),
                onDismissRequest = { showFailureRegister = false },
                content = {
                    Text(
                        text = "Не удалось создать пользователя. Попробуйте ещё раз.",
                        lineHeight = 18.sp
                    )
                },
                buttons = {
                    TextButton(onClick = {
                        showFailureRegister = false
                    }) {
                        Text("Ок")
                    }
                })
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
                    imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        })
}

private fun startMainActivity(context: Context) {
    val intent = Intent(
        context,
        MainActivity::class.java
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    
    context.startActivity(intent)
    (context as Activity).finish()
}