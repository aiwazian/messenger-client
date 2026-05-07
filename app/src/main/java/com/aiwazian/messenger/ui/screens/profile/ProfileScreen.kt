/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.extensions.sharedElement
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyDateWithYear
import com.aiwazian.messenger.extensions.toPrettyTime
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction

@Composable
fun ProfileScreen(
    profileId: Long, profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    
    val uiState by profileViewModel.uiState.collectAsState()
    var showLeaveDialog by remember { mutableStateOf(false) }
    var leaveDialogData by remember { mutableStateOf<Pair<String, ChatType>?>(null) }
    
    LaunchedEffect(profileId) {
        if (profileId != -1L) {
            profileViewModel.init(profileId)
        }
    }
    
    LaunchedEffect(Unit) {
        profileViewModel.uiEffect.collect { effect ->
            when (effect) {
                is ProfileUiEffect.NavigateBack -> {
                    navBackStack.removeLastOrNull()
                }
                
                is ProfileUiEffect.NavigateToMain -> {
                    navBackStack.clear()
                    navBackStack.add(AppRoute.Main)
                }
                
                is ProfileUiEffect.NavigateToUserSettings -> {
                    navBackStack.add(AppRoute.SettingsProfile)
                }
                
                is ProfileUiEffect.NavigateToGroupSettings -> {
                    navBackStack.add(AppRoute.GroupSettings(effect.chatId))
                }
                
                is ProfileUiEffect.NavigateToChannelSettings -> {
                    navBackStack.add(AppRoute.ChannelSettings(effect.chatId))
                }
                
                is ProfileUiEffect.ShowLeaveDialog -> {
                    leaveDialogData = Pair(
                        effect.profileName, effect.chatType
                    )
                    showLeaveDialog = true
                }
                
                is ProfileUiEffect.HideLeaveDialog -> {
                    showLeaveDialog = false
                    leaveDialogData = null
                }
            }
        }
    }
    
    val scrollState = rememberScrollState()
    
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            when (val profile = uiState.profile) {
                is Profile.User -> {
                    UserProfile(
                        user = profile,
                        actions = uiState.actions,
                        innerPadding.calculateTopPadding()
                    )
                }
                
                is Profile.Channel -> {
                    ChannelProfile(
                        channel = profile,
                        actions = uiState.actions,
                        innerPadding.calculateTopPadding()
                    )
                }
                
                is Profile.Group -> {
                    GroupProfile(
                        group = profile,
                        actions = uiState.actions,
                        innerPadding.calculateTopPadding()
                    )
                }
                
                else -> {}
            }
        }
    }
    
    if (showLeaveDialog && leaveDialogData != null) {
        LeaveProfileDialog(
            onDismiss = {
                showLeaveDialog = false
                profileViewModel.hideLeaveDialog()
            }, onConfirm = {
                profileViewModel.onLeaveConfirmed()
            }, profileName = leaveDialogData!!.first, chatType = leaveDialogData!!.second
        )
    }
}

@Composable
private fun GroupProfile(
    group: Profile.Group, actions: List<TopBarAction>, innerPadding: Dp
) {
    Box {
        ProfileImageCarousel(avatars = group.avatars)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TopAppBarDefaults.LargeAppBarCollapsedHeight + innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f), Color.Transparent
                        )
                    )
                )
        )
        
        TopBar(
            chatId = group.id,
            title = group.name,
            subTitle = pluralStringResource(R.plurals.members_count, group.members, group.members),
            actions = actions
        )
    }
    
    Column(modifier = Modifier.padding(innerPadding)) {
        SectionContainer {
            if (!group.bio.isNullOrBlank()) {
                SectionItem(
                    headlineText = group.bio, supportingText = stringResource(R.string.description)
                )
            }
            
            if (!group.username.isNullOrBlank()) {
                SectionItem(
                    headlineText = "@" + group.username,
                    supportingText = stringResource(R.string.public_link)
                )
            }
        }
    }
}

@Composable
private fun ChannelProfile(
    channel: Profile.Channel, actions: List<TopBarAction>, innerPadding: Dp
) {
    Box {
        ProfileImageCarousel(avatars = channel.avatars)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TopAppBarDefaults.LargeAppBarCollapsedHeight + innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f), Color.Transparent
                        )
                    )
                )
        )
        
        TopBar(
            chatId = channel.id, title = channel.name, subTitle = pluralStringResource(
                R.plurals.subscribers_count, channel.subscribers, channel.subscribers
            ), actions = actions
        )
    }
    
    Column {
        SectionContainer {
            if (!channel.bio.isNullOrBlank()) {
                SectionItem(
                    headlineText = channel.bio,
                    supportingText = stringResource(R.string.description)
                )
            }
            
            if (!channel.username.isNullOrBlank()) {
                SectionItem(
                    headlineText = "@" + channel.username,
                    supportingText = stringResource(R.string.public_link)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfile(
    user: Profile.User, actions: List<TopBarAction>, innerPadding: Dp
) {
    Box {
        ProfileImageCarousel(avatars = user.avatars)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TopAppBarDefaults.LargeAppBarCollapsedHeight + innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f), Color.Transparent
                        )
                    )
                )
        )
        
        TopBar(
            chatId = user.id,
            title = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
            subTitle = user.lastSeen?.toInstance()?.toPrettyTime() ?: "в сети недавно",
            actions = actions
        )
    }
    
    Column(modifier = Modifier.padding(top = 10.dp)) {
        SectionContainer {
            if (!user.bio.isNullOrBlank()) {
                SectionItem(
                    headlineText = user.bio, supportingText = stringResource(R.string.bio)
                )
            }
            
            if (!user.username.isNullOrBlank()) {
                SectionItem(
                    headlineText = "@" + user.username,
                    supportingText = stringResource(R.string.username)
                )
            }
            
            if (user.dateOfBirth != null) {
                SectionItem(
                    headlineText = user.dateOfBirth.toInstance().toPrettyDateWithYear(),
                    supportingText = stringResource(R.string.date_of_birth)
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    chatId: Long, title: String, subTitle: String, actions: List<TopBarAction>
) {
    val navBackStack = LocalNavBackStack.current
    
    PageTopBar(
        title = {
            Column {
                Text(
                    text = title,
                    maxLines = 1,
                    fontSize = 18.sp,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .sharedElement(key = "chat-name-$chatId")
                )
                Text(
                    text = subTitle,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .sharedElement(key = "chat-sub-title-$chatId")
                )
            }
        }, navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack, onClick = navBackStack::removeLastOrNull
        ), actions = actions, colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White,
            titleContentColor = Color.White,
        )
    )
}

@Composable
private fun LeaveProfileDialog(
    onDismiss: () -> Unit, onConfirm: () -> Unit, profileName: String, chatType: ChatType
) {
    val title = when (chatType) {
        ChatType.CHANNEL -> stringResource(R.string.leave_channel)
        ChatType.GROUP -> stringResource(R.string.leave_group)
        else -> stringResource(R.string.leave)
    }
    
    val message = when (chatType) {
        ChatType.CHANNEL -> buildAnnotatedString {
            append(stringResource(R.string.leave_channel_confirm_message))
            withStyle(style = SpanStyle(fontWeight = FontWeight.W500)) { append(" $profileName") }
            append("?")
        }
        
        ChatType.GROUP -> buildAnnotatedString {
            append(stringResource(R.string.leave_group_confirm))
            withStyle(style = SpanStyle(fontWeight = FontWeight.W500)) { append(" $profileName") }
            append("?")
        }
        
        else -> buildAnnotatedString {
            append(stringResource(R.string.leave_confirm))
            withStyle(style = SpanStyle(fontWeight = FontWeight.W500)) { append(" $profileName") }
            append("?")
        }
    }
    
    CustomDialog(title = title, onDismissRequest = onDismiss, content = {
        Text(text = message)
    }, buttons = {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        TextButton(
            onClick = onConfirm,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text(title) }
    })
}
