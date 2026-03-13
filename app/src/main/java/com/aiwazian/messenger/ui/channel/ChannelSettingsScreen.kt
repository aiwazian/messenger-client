package com.aiwazian.messenger.ui.channel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.TopBarAction
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.services.VibrateService
import com.aiwazian.messenger.ui.element.CustomDialog
import com.aiwazian.messenger.ui.element.InputField
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionItem
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.viewModels.ChannelViewModel
import com.aiwazian.messenger.viewModels.MainViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel
import kotlinx.coroutines.launch

@Composable
fun ChannelSettingsScreen() {
    Content()
}

@Composable
private fun Content() {
    val context = LocalContext.current
    
    val navViewModel = viewModel<NavigationViewModel>()
    
    val channelViewModel = hiltViewModel<ChannelViewModel>()
    
    val channelInfo by channelViewModel.channelInfo.collectAsState()
    
    val scrollState = rememberScrollState()
    
    val scope = rememberCoroutineScope()
    
    val vibrateService = VibrateService(context)
    
    Scaffold(
        topBar = {
            TopBar(
                listOf(
                    TopBarAction(
                        icon = Icons.Outlined.Check,
                        onClick = {
                            scope.launch {
                                val savedId = channelViewModel.trySaveOrCreate()
                                
                                if (savedId == null) {
                                    vibrateService.vibrate(VibrationPattern.Error)
                                } else {
                                    navViewModel.removeLastScreenInStack()
                                }
                            }
                        })
                )
            )
        },
        modifier = Modifier.imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            SectionContainer {
                InputField(
                    placeholder = stringResource(R.string.channel_name),
                    value = channelInfo.name,
                    onValueChange = channelViewModel::changeName
                )
                InputField(
                    placeholder = stringResource(R.string.description),
                    value = channelInfo.bio,
                    onValueChange = channelViewModel::changeBio
                )
            }
            
            SectionContainer {
                SectionItem(
                    icon = Icons.Outlined.PeopleAlt,
                    text = stringResource(R.string.channel_type),
                    primaryText = if (channelInfo.channelType == ChannelType.PUBLIC.ordinal) {
                        stringResource(R.string.public_channel)
                    } else {
                        stringResource(R.string.private_channel)
                    },
                    onClick = {
                        navViewModel.addScreenInStack {
                            ChannelTypeSettingsScreen()
                        }
                    })
            }
            
            SectionContainer {
                SectionItem(
                    icon = Icons.Outlined.PeopleAlt,
                    text = stringResource(R.string.subscribers),
                    primaryText = channelInfo.subscribers.toString(),
                    onClick = {
                        navViewModel.addScreenInStack {
                            ChannelSubscribersScreen(channelInfo.id)
                        }
                    })
            }
            
            SectionContainer {
                SectionItem(
                    text = stringResource(R.string.delete_channel),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClick = channelViewModel.deleteDialog::show
                )
            }
            
            val mainViewModel = hiltViewModel<MainViewModel>()
            
            if (channelViewModel.deleteDialog.isVisible) {
                CustomDialog(
                    title = stringResource(R.string.delete_channel),
                    onDismissRequest = channelViewModel.deleteDialog::hide,
                    buttons = {
                        TextButton(onClick = channelViewModel.deleteDialog::hide) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val isDeleted = channelViewModel.tryDelete()
                                    
                                    if (isDeleted) {
                                        mainViewModel.deleteChat(channelInfo.id)
                                        channelViewModel.deleteDialog.hide()
                                        navViewModel.goToMain()
                                    } else {
                                        vibrateService.vibrate(VibrationPattern.Error)
                                    }
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.delete_channel))
                        }
                    },
                    content = {
                        Text("Вы точно хотите удалить канал?")
                    })
            }
        }
    }
}

@Composable
private fun TopBar(actions: List<TopBarAction>) {
    val navViewModel = viewModel<NavigationViewModel>()
    
    PageTopBar(
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        ),
        actions = actions
    )
}