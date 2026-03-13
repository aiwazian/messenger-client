package com.aiwazian.messenger.ui.group

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.ChatInfo
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.services.VibrateService
import com.aiwazian.messenger.ui.ChatScreen
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.viewModels.GroupViewModel
import com.aiwazian.messenger.viewModels.MainViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel
import kotlinx.coroutines.launch

@Composable
fun CreateGroupScreen() {
    Content()
}

@Composable
private fun Content() {
    val context = LocalContext.current
    
    val mainViewModel = hiltViewModel<MainViewModel>()
    val navViewModel = viewModel<NavigationViewModel>()
    val groupViewModel = hiltViewModel<GroupViewModel>()
    
    LaunchedEffect(Unit) {
        groupViewModel.cleanData()
    }

    DisposableEffect(Unit) {
        onDispose {
            groupViewModel.cleanData()
        }
    }
    
    val groupInfo by groupViewModel.groupInfo.collectAsState()
    
    val vibrateService = VibrateService(context)
    
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.imePadding(),
                onClick = {
                    scope.launch {
                        val isValid = groupViewModel.checkValid()
                        
                        if (!isValid) {
                            vibrateService.vibrate(VibrationPattern.Error)
                            return@launch
                        }
                        
                        isLoading = true
                        
                        val createdId = groupViewModel.createGroup()
                        
                        if (createdId == null) {
                            vibrateService.vibrate(VibrationPattern.Error)
                            isLoading = false
                            return@launch
                        }
                        
                        val chatInfo = ChatInfo(
                            id = createdId,
                            chatName = groupInfo.name
                        )
                        
                        mainViewModel.showNewChat(chatInfo)
                        
                        navViewModel.goToMain()
                        navViewModel.addScreenInStack {
                            ChatScreen(createdId)
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
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
        }
    ) {
        Column(
            Modifier
                .padding(it)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            SectionContainer {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    value = groupInfo.name,
                    onValueChange = groupViewModel::changeGroupName,
                    placeholder = { Text("Название группы") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    PageTopBar(
        title = { Text(text = stringResource(R.string.create_group)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        )
    )
}