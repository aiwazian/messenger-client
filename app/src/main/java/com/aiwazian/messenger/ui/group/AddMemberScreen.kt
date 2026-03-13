package com.aiwazian.messenger.ui.group

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.data.ChatInfo
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.TopBarAction
import com.aiwazian.messenger.services.ChatService
import com.aiwazian.messenger.services.SearchService
import com.aiwazian.messenger.ui.element.InputField
import com.aiwazian.messenger.ui.element.MinimizeChatCard
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.viewModels.NavigationViewModel

@Composable
fun AddMemberScreen(
    membersId: LongArray,
    callback: (LongArray) -> Unit
) {
    val navViewModel = viewModel<NavigationViewModel>()
    
    var allChats by remember { mutableStateOf<List<ChatInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedUsers by remember { mutableStateOf<List<ChatInfo>>(emptyList()) }
    
    var networkSearchResults by remember { mutableStateOf<List<ChatInfo>>(emptyList()) }
    
    val localFilteredChats = allChats.filter { chatInfo ->
        chatInfo.chatName.contains(
            other = searchQuery.trim(),
            ignoreCase = true
        )
    }
    
    val filteredChats = remember(
        localFilteredChats,
        networkSearchResults,
        searchQuery
    ) {
        if (searchQuery.isBlank()) {
            localFilteredChats
        } else {
            val combined = networkSearchResults.toMutableList()
            
            localFilteredChats.forEach { localChat ->
                if (combined.none { it.id == localChat.id }) {
                    combined.add(localChat)
                }
            }
            combined.toList()
        }
    }
    
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            networkSearchResults = emptyList()
            return@LaunchedEffect
        }
        
        try {
            val result = SearchService().searchUserByUsername(searchQuery)
            
            val ds = result?.map {
                ChatInfo(
                    id = it.chatId,
                    chatName = it.name
                )
            } ?: emptyList()
            
            networkSearchResults = ds
            
        } catch (e: Exception) {
            Log.e(
                "AddMemberScreen",
                "Ошибка при поиске пользователей",
                e
            )
        }
    }
    
    LaunchedEffect(Unit) {
        try {
            val chats = ChatService().getAllChatsWithOtherUser()
            allChats = chats.orEmpty()
            
            selectedUsers = allChats.filter { it.id in membersId }
        } catch (e: Exception) {
            Log.e(
                "AddMemberScreen",
                "Не удалось получить всех пользователей",
                e
            )
        }
    }
    
    Scaffold(topBar = {
        PageTopBar(
            navigationIcon = NavigationIcon(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                onClick = navViewModel::removeLastScreenInStack
            ),
            actions = listOf(
                TopBarAction(
                    icon = Icons.Outlined.Check,
                    onClick = {
                        callback(selectedUsers.map { it.id }.toLongArray())
                        
                        navViewModel.removeLastScreenInStack()
                    }
                )
            )
        )
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                InputField(
                    placeholder = "Поиск людей...",
                    value = searchQuery,
                    onValueChange = { searchQuery = it })
            }
            
            SectionContainer {
                LazyColumn {
                    items(
                        filteredChats,
                        { it.id }) { chat ->
                        val isPermanentMember = remember { chat.id in membersId }
                        
                        val selected = isPermanentMember || selectedUsers.any { it.id == chat.id }
                        
                        MinimizeChatCard(
                            chatName = chat.chatName,
                            selected = selected,
                            onClick = {
                                if (!isPermanentMember) {
                                    selectedUsers = if (selected) {
                                        selectedUsers - chat
                                    } else {
                                        selectedUsers + chat
                                    }
                                }
                            })
                    }
                }
            }
        }
    }
}