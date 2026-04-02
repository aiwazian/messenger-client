/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyDateWithYear
import com.aiwazian.messenger.ui.components.CustomDialog
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(
    profileId: Long,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val navHost = LocalNavHost.current
    
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
                    navHost.removeLastOrNull()
                }
                
                is ProfileUiEffect.NavigateToMain -> {
                    navHost.clear()
                    navHost.add(AppRoute.Main)
                }
                
                is ProfileUiEffect.NavigateToUserSettings -> {
                    navHost.add(AppRoute.SettingsProfile)
                }
                
                is ProfileUiEffect.NavigateToGroupSettings -> {
                    navHost.add(AppRoute.GroupSettings(effect.chatId))
                }
                
                is ProfileUiEffect.NavigateToChannelSettings -> {
                    navHost.add(AppRoute.ChannelSettings(effect.chatId))
                }
                
                is ProfileUiEffect.ShowLeaveDialog -> {
                    leaveDialogData =
                        Pair(
                            effect.profileName,
                            effect.chatType
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
    
    when {
        uiState.isLoading -> {
            Scaffold(topBar = {
                PageTopBar(
                    navigationIcon = NavigationIcon(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        onClick = navHost::removeLastOrNull
                    )
                )
            }) {
                Column(
                    Modifier
                        .padding(it)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularWavyProgressIndicator()
                }
            }
        }
        
        uiState.error != null -> {
            Scaffold(topBar = {
                PageTopBar(
                    navigationIcon = NavigationIcon(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        onClick = navHost::removeLastOrNull
                    )
                )
            }) {
                Column(
                    Modifier
                        .padding(it)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = uiState.error ?: "Error")
                }
            }
        }
        
        uiState.profile is Profile.User -> {
            UserProfile(
                user = uiState.profile as Profile.User,
                actions = uiState.actions
            )
        }
        
        uiState.profile is Profile.Channel -> {
            ChannelProfile(
                channel = uiState.profile as Profile.Channel,
                actions = uiState.actions
            )
        }
        
        uiState.profile is Profile.Group -> {
            GroupProfile(
                group = uiState.profile as Profile.Group,
                actions = uiState.actions
            )
        }
        
        else -> {
            Scaffold(topBar = {
                PageTopBar(
                    navigationIcon = NavigationIcon(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        onClick = navHost::removeLastOrNull
                    )
                )
            }) {
                Column(
                    Modifier
                        .padding(it)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularWavyProgressIndicator()
                }
            }
        }
    }
    
    if (showLeaveDialog && leaveDialogData != null) {
        LeaveProfileDialog(
            onDismiss = {
                showLeaveDialog = false
                profileViewModel.hideLeaveDialog()
            },
            onConfirm = {
                profileViewModel.onLeaveConfirmed()
            },
            profileName = leaveDialogData!!.first,
            chatType = leaveDialogData!!.second
        )
    }
}

@Composable
private fun GroupProfile(
    group: Profile.Group,
    actions: List<TopBarAction>
) {
    Scaffold(topBar = {
        TopBar(
            title = group.name,
            actions = actions
        )
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                if (!group.bio.isNullOrBlank()) {
                    SectionItem(
                        text = group.bio,
                        description = stringResource(R.string.description)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelProfile(
    channel: Profile.Channel,
    actions: List<TopBarAction>
) {
    Scaffold(topBar = {
        TopBar(
            title = channel.name,
            actions = actions
        )
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                if (!channel.bio.isNullOrBlank()) {
                    SectionItem(
                        text = channel.bio,
                        description = stringResource(R.string.description)
                    )
                }
                
                if (!channel.publicLink.isNullOrBlank()) {
                    SectionItem(
                        text = channel.publicLink,
                        description = stringResource(R.string.public_link)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserProfile(
    user: Profile.User,
    actions: List<TopBarAction>
) {
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopBar(
                title = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                actions = actions
            )
        },
    ) {
        Column(
            Modifier
                .padding(it)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            SectionContainer {
                val userBio = user.bio
                
                if (!userBio.isNullOrBlank()) {
                    SectionItem(
                        text = userBio,
                        description = stringResource(R.string.bio)
                    )
                }
                
                val username = user.username
                
                if (!username.isNullOrBlank()) {
                    SectionItem(
                        text = ("@$username"),
                        description = stringResource(R.string.username)
                    )
                }
                
                val dateOfBirth = user.dateOfBirth
                
                if (dateOfBirth != null) {
                    SectionItem(
                        text = dateOfBirth.toInstance().toPrettyDateWithYear(),
                        description = stringResource(R.string.date_of_birth)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    actions: List<TopBarAction>
) {
    val navHost = LocalNavHost.current
    
    PageTopBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = navHost::removeLastOrNull
        ),
        actions = actions
    )
}

@Composable
private fun LeaveProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    profileName: String,
    chatType: ChatType
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
    
    CustomDialog(
        title = title,
        onDismissRequest = onDismiss,
        content = {
            Text(text = message)
        },
        buttons = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(title) }
        }
    )
}
