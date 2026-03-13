package com.aiwazian.messenger.ui.login

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.aiwazian.messenger.R
import com.aiwazian.messenger.services.DialogController
import com.aiwazian.messenger.services.VibrateService
import com.aiwazian.messenger.ui.element.CustomDialog
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.viewModels.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    
    val vibrateService = VibrateService(context)
    
    var isLoaded by remember { mutableStateOf(true) }
    
    val dialogController = DialogController()
    
    var isFoundUser by remember { mutableStateOf<Boolean?>(null) }
    
    val loginFieldError by authViewModel.loginFieldError.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        val isValidLogin = authViewModel.checkValidLogin()
                        
                        if (!isValidLogin) {
                            vibrateService.vibrate(VibrationPattern.Error)
                            return@launch
                        }
                        
                        isLoaded = false
                        
                        isFoundUser = authViewModel.findUserByLogin()
                        
                        dialogController.show()
                        
                        isLoaded = true
                    }
                },
                modifier = Modifier.imePadding(),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                if (isLoaded) {
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
            }
        }
        
        if (dialogController.isVisible) {
            when (isFoundUser) {
                null -> {
                    CustomDialog(
                        title = stringResource(R.string.app_name),
                        onDismissRequest = dialogController::hide,
                        content = {
                            Text(
                                text = "Не удалось проверить, попробуйте ещё раз.",
                                lineHeight = 18.sp
                            )
                        },
                        buttons = {
                            TextButton(onClick = dialogController::hide) {
                                Text("ОК")
                            }
                        }
                    )
                }
                
                true -> {
                    CustomDialog(
                        title = stringResource(R.string.app_name),
                        onDismissRequest = dialogController::hide,
                        content = {
                            Text(
                                text = "Пользователь найден. Продолжить?",
                                lineHeight = 18.sp
                            )
                        },
                        buttons = {
                            TextButton(onClick = dialogController::hide) {
                                Text("Нет")
                            }
                            TextButton(onClick = {
                                dialogController.hide()
                                navController.navigate(Screen.PASSWORD)
                            }) {
                                Text("Да")
                            }
                        }
                    )
                }
                
                else -> {
                    CustomDialog(
                        title = stringResource(R.string.app_name),
                        onDismissRequest = dialogController::hide,
                        content = {
                            Text(
                                text = "Пользователь не найден. Создать?",
                                lineHeight = 18.sp
                            )
                        },
                        buttons = {
                            TextButton(onClick = dialogController::hide) {
                                Text("Нет")
                            }
                            TextButton(onClick = {
                                dialogController.hide()
                                navController.navigate(Screen.PASSWORD)
                            }) {
                                Text("Да")
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
        isError = isError
    )
}
