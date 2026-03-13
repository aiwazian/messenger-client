package com.aiwazian.messenger.ui.settings.chatFolder

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.ChatInfo
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.TopBarAction
import com.aiwazian.messenger.services.UserManager
import com.aiwazian.messenger.ui.element.InputField
import com.aiwazian.messenger.ui.element.MinimizeChatCard
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.viewModels.FolderViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel

@Composable
fun SettingsChatInFolderScreen(onConfirmSelect: (List<ChatInfo>) -> Unit) {
    Content(onConfirmSelect)
}

@Composable
private fun Content(onConfirmSelect: (List<ChatInfo>) -> Unit) {
    val navViewModel = viewModel<NavigationViewModel>()
    val folderViewModel = hiltViewModel<FolderViewModel>()
    
    val openFolder by folderViewModel.openFolder.collectAsState()
    
    var allChats by remember { mutableStateOf<List<ChatInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var currentSelectedChats by remember { mutableStateOf(openFolder.chats) }
    
    val filteredChats = allChats.filter { chatInfo ->
        chatInfo.chatName.contains(
            other = searchQuery.trim(),
            ignoreCase = true
        )
    }
    
    val action = listOf(
        TopBarAction(
            icon = Icons.Outlined.Check,
            onClick = {
                onConfirmSelect(currentSelectedChats)
                navViewModel.removeLastScreenInStack()
            })
    )
    
    LaunchedEffect(Unit) {
        try {
            val request = RetrofitInstance.api.getAllChats()
            if (request.isSuccessful) {
                allChats = request.body() ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(
                "SettingsChatInFolderScreen",
                "Ошибка при получении всех чатов",
                e
            )
        }
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = {
                    Column {
                        Text(text = "Чаты в папке")
                        Text(
                            text = "Количество чатов не ограничено",
                            fontSize = 12.sp,
                            lineHeight = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = navViewModel::removeLastScreenInStack
                ),
                actions = action
            )
        }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                InputField(
                    placeholder = stringResource(R.string.search),
                    value = searchQuery,
                    onValueChange = { newValue ->
                        searchQuery = newValue
                    })
            }
            
            SectionContainer {
                val user by UserManager.user.collectAsState()
                
                LazyColumn {
                    items(
                        filteredChats,
                        { it.id }) { chat ->
                        val chatName = if (chat.id == user.id) {
                            stringResource(R.string.saved_messages)
                        } else {
                            chat.chatName
                        }
                        
                        val selected = currentSelectedChats.any { it.id == chat.id }
                        
                        MinimizeChatCard(
                            chatName = chatName,
                            selected = selected,
                            onClick = {
                                currentSelectedChats = if (selected) {
                                    currentSelectedChats - chat
                                } else {
                                    currentSelectedChats + chat
                                }
                            })
                    }
                }
            }
        }
    }
}