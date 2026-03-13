package com.aiwazian.messenger.ui.settings.security

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.services.VibrateService
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.utils.Shape
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.viewModels.CloudPasswordViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsChangeCloudPasswordScreen() {
    val navViewModel = viewModel<NavigationViewModel>()
    val cloudPasswordViewModel = viewModel<CloudPasswordViewModel>()
    
    val newPassword by cloudPasswordViewModel.newPassword.collectAsState()
    val errorMessage by cloudPasswordViewModel.errorMessage.collectAsState()
    
    val vibrateService = VibrateService(LocalContext.current)
    
    val scope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(false) }
    
    DisposableEffect(Unit) {
        onDispose {
            cloudPasswordViewModel.cleanData()
        }
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = navViewModel::removeLastScreenInStack
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val isValid = cloudPasswordViewModel.checkValidPassword()
                    
                    isLoading = true
                    
                    scope.launch {
                        if (!isValid) {
                            vibrateService.vibrate(VibrationPattern.Error)
                            isLoading = false
                            return@launch
                        }
                        
                        val isChanged = cloudPasswordViewModel.changePassword()
                        if (isChanged) {
                            navViewModel.removeLastScreenInStack()
                        }
                    }
                },
                shape = CircleShape,
                modifier = Modifier.imePadding(),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                AnimatedContent(targetState = isLoading) { isLoading ->
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
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
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                value = newPassword,
                onValueChange = cloudPasswordViewModel::onInputNewPassword,
                shape = Shape.TextField,
                label = {
                    Text(errorMessage ?: stringResource(R.string.enter_password))
                },
                isError = errorMessage != null
            )
        }
    }
}