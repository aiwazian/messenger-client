package com.aiwazian.messenger.ui.channel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.UserInfo
import com.aiwazian.messenger.ui.ProfileScreen
import com.aiwazian.messenger.ui.element.MinimizeChatCard
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.viewModels.ChannelViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel

@Composable
fun ChannelSubscribersScreen(id: Long) {
    Content(id)
}

@Composable
private fun Content(id: Long) {
    val navViewModel = viewModel<NavigationViewModel>()
    val channelViewModel = hiltViewModel<ChannelViewModel>()
    
    var subscribers by remember { mutableStateOf<List<UserInfo>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        subscribers = channelViewModel.getSubscribers(id)
    }
    
    Scaffold(topBar = { TopBar() }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                LazyColumn {
                    items(
                        items = subscribers,
                        key = { it.id }) { user ->
                        MinimizeChatCard(
                            chatName = "${user.firstName} ${user.lastName}",
                            onClick = {
                                navViewModel.addScreenInStack {
                                    ProfileScreen(user.id)
                                }
                            })
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    PageTopBar(
        title = { Text(stringResource(R.string.subscribers)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        )
    )
}