package com.aiwazian.messenger.ui.settings.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.TopBarAction
import com.aiwazian.messenger.services.VibrateService
import com.aiwazian.messenger.ui.element.InputField
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.viewModels.NavigationViewModel
import com.aiwazian.messenger.viewModels.SettingsUsernameViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsUsernameScreen() {
    Content()
}

@Composable
private fun Content() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    val viewModel = hiltViewModel<SettingsUsernameViewModel>()
    
    val username by viewModel.username.collectAsState()
    
    val errorText = viewModel.errorText
    
    LaunchedEffect(Unit) {
        viewModel.init()
    }
    
    Scaffold(topBar = {
        TopBar(
            navViewModel::removeLastScreenInStack,
            viewModel
        )
    }) {
        Column(modifier = Modifier.padding(it)) {
            SectionContainer {
                InputField(
                    placeholder = stringResource(R.string.username),
                    value = username,
                    onValueChange = viewModel::onChangeUsername
                )
            }
            
            AnimatedContent(targetState = errorText) { text ->
                if (text != null) {
                    Text(
                        text = text,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 8.dp
                        ),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit,
    viewModel: SettingsUsernameViewModel
) {
    val navViewModel = viewModel<NavigationViewModel>()
    
    val canSave by viewModel.canSave.collectAsState()
    
    val context = LocalContext.current
    
    val vibrationService = VibrateService(context)
    
    val scope = rememberCoroutineScope()
    
    val actions = if (canSave) {
        listOf(
            TopBarAction(
                icon = Icons.Outlined.Check,
                onClick = {
                    scope.launch {
                        val isSaved = viewModel.trySave()
                        
                        if (isSaved) {
                            navViewModel.removeLastScreenInStack()
                        } else {
                            vibrationService.vibrate(VibrationPattern.Error)
                        }
                    }
                })
        )
    } else {
        emptyList()
    }
    
    PageTopBar(
        title = { Text(stringResource(R.string.username)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = onBack::invoke
        ),
        actions = actions
    )
}