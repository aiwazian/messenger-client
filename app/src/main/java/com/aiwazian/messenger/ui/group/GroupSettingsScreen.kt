package com.aiwazian.messenger.ui.group

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PeopleOutline
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.services.DialogController
import com.aiwazian.messenger.services.GroupService
import com.aiwazian.messenger.ui.element.CustomDialog
import com.aiwazian.messenger.ui.element.InputField
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionItem
import com.aiwazian.messenger.viewModels.GroupViewModel
import com.aiwazian.messenger.viewModels.MainViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel
import kotlinx.coroutines.launch

@Composable
fun GroupSettingsScreen() {
    Content()
}

@Composable
private fun Content() {
    val navViewModel = viewModel<NavigationViewModel>()
    val mainViewModel = hiltViewModel<MainViewModel>()
    val groupViewModel = hiltViewModel<GroupViewModel>()
    
    val groupInfo by groupViewModel.groupInfo.collectAsState()
    
    val deleteGroupDialog = DialogController()
    
    Scaffold(topBar = {
        PageTopBar(
            navigationIcon = NavigationIcon(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                onClick = navViewModel::removeLastScreenInStack
            )
        )
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                SectionItem(
                    icon = Icons.Outlined.PeopleOutline,
                    text = stringResource(R.string.members),
                    primaryText = groupInfo.members.toString(),
                    onClick = {
                        navViewModel.addScreenInStack {
                            GroupMembersScreen(groupInfo.id)
                        }
                    }
                )
            }
            
            SectionContainer {
                SectionItem(
                    text = stringResource(R.string.delete_group),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClick = deleteGroupDialog::show
                )
            }
        }
        
        val scope = rememberCoroutineScope()
        
        if (deleteGroupDialog.isVisible) {
            CustomDialog(
                title = stringResource(R.string.delete_group),
                onDismissRequest = deleteGroupDialog::hide,
                buttons = {
                    TextButton(onClick = deleteGroupDialog::hide) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val isDeleted = GroupService().delete(groupInfo.id)
                                    if (isDeleted) {
                                        mainViewModel.deleteChat(groupInfo.id)
                                        navViewModel.goToMain()
                                    }
                                } catch (e: Exception) {
                                    Log.e(
                                        "GroupSettingsScreen",
                                        "Ошибка при удалении группы",
                                        e
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete_group))
                    }
                }
            ) {
                Text("Вы точно хотите удалить группу для себя и всех участников?")
            }
        }
    }
}