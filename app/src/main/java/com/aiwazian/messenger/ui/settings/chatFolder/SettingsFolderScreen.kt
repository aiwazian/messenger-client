package com.aiwazian.messenger.ui.settings.chatFolder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.TopBarAction
import com.aiwazian.messenger.services.UserManager
import com.aiwazian.messenger.ui.element.CustomDialog
import com.aiwazian.messenger.ui.element.InputField
import com.aiwazian.messenger.ui.element.MinimizeChatCard
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionDescription
import com.aiwazian.messenger.ui.element.SectionHeader
import com.aiwazian.messenger.ui.element.SectionItem
import com.aiwazian.messenger.viewModels.FolderViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsFolderScreen(folderId: Int = 0) {
    Content(folderId)
}

@Composable
private fun Content(folderId: Int) {
    val navViewModel = viewModel<NavigationViewModel>()
    val folderViewModel = hiltViewModel<FolderViewModel>()
    
    val openFolder by folderViewModel.openFolder.collectAsState()
    val canSave by folderViewModel.canSave.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    val action = if (canSave) {
        listOf(
            TopBarAction(
                icon = Icons.Outlined.Check,
                onClick = {
                    scope.launch {
                        folderViewModel.save()
                        navViewModel.removeLastScreenInStack()
                    }
                })
        )
    } else {
        emptyList()
    }
    
    LaunchedEffect(Unit) {
        folderViewModel.open(folderId)
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(text = openFolder.name.ifBlank { stringResource(R.string.new_folder) })
                },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = navViewModel::removeLastScreenInStack
                ),
                actions = action
            )
        },
        modifier = Modifier.imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            SectionHeader(title = stringResource(R.string.folder_name))
            
            SectionContainer {
                InputField(
                    placeholder = stringResource(R.string.folder_name),
                    value = openFolder.name,
                    onValueChange = folderViewModel::changeFolderName
                )
            }
            
            SectionHeader(title = stringResource(R.string.included_chats))
            
            SectionContainer {
                SectionItem(
                    text = stringResource(R.string.add_chats),
                    icon = Icons.Outlined.AddCircle,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    onClick = {
                        navViewModel.addScreenInStack {
                            SettingsChatInFolderScreen { selectedChats ->
                                folderViewModel.updateFolderChats(selectedChats)
                            }
                        }
                    })
                
                val user by UserManager.user.collectAsState()
                
                LazyColumn {
                    items(
                        openFolder.chats,
                        { it.id }) { chat ->
                        val chatName = if (chat.id == user.id) {
                            stringResource(R.string.saved_messages)
                        } else {
                            chat.chatName
                        }
                        
                        MinimizeChatCard(chatName)
                    }
                }
            }
            
            SectionDescription(text = "Выберите чаты, которые нужно показывать в этой папке.")
            
            val removeFolderDialog = folderViewModel.removeFolderDialog
            
            if (folderId != 0) {
                SectionContainer {
                    SectionItem(
                        text = stringResource(R.string.remove_folder),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        onClick = removeFolderDialog::show
                    )
                }
            }
            
            if (removeFolderDialog.isVisible) {
                CustomDialog(
                    title = stringResource(R.string.remove_folder),
                    onDismissRequest = removeFolderDialog::hide,
                    content = {
                        Text("Это не затронет чаты, находящиеся внутри.")
                    },
                    buttons = {
                        TextButton(onClick = removeFolderDialog::hide) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    removeFolderDialog.hide()
                                    folderViewModel.remove(folderId)
                                    navViewModel.removeLastScreenInStack()
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    })
            }
        }
    }
}
