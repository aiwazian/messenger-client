/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.InputField
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.utils.VibrationPattern
import kotlinx.coroutines.launch

@Composable
fun ChannelSubscribersScreen(
    channelId: Long,
    viewModel: ChannelSettingsViewModel = hiltViewModel()
) {
    val navHost = LocalNavHost.current
    var searchQuery by remember { mutableStateOf("") }
    var subscribers by remember { mutableStateOf(emptyList<User>()) }
    
    val scope = rememberCoroutineScope()
    
    fun loadSubscribers() {
        scope.launch {
            subscribers = viewModel.getSubscribers(searchQuery.ifBlank { null })
        }
    }
    
    LaunchedEffect(
        channelId,
        searchQuery
    ) {
        viewModel.init(channelId)
        loadSubscribers()
    }
    
    var userToKick by remember { mutableStateOf<User?>(null) }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.subscribers)) },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navHost::removeLastOrNull
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            InputField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.search)
            )
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(subscribers) { user ->
                    SubscriberItem(
                        user = user,
                        onKickClick = { userToKick = user }
                    )
                }
            }
        }
        
        userToKick?.let { user ->
            CustomDialog(
                title = "Выгнать участника",
                onDismissRequest = { userToKick = null },
                buttons = {
                    TextButton(onClick = { userToKick = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            viewModel.kickUser(user.id) { success ->
                                if (success) {
                                    userToKick = null
                                    loadSubscribers()
                                } else {
                                    viewModel.vibrate(VibrationPattern.Error)
                                }
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Выгнать")
                    }
                },
                content = {
                    Text("Вы уверены, что хотите выгнать ${user.firstName} из канала?")
                }
            )
        }
    }
}

@Composable
private fun SubscriberItem(
    user: User,
    onKickClick: () -> Unit
) {
    val navHost = LocalNavHost.current
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navHost.add(AppRoute.Chat(user.id)) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = "${user.firstName} ${user.lastName.orEmpty()}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        Box(modifier = Modifier.padding(end = 16.dp)) {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Выгнать") },
                    onClick = {
                        showMenu = false
                        onKickClick()
                    }
                )
            }
        }
    }
}
