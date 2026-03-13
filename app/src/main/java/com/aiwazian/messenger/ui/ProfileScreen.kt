package com.aiwazian.messenger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.ChannelInfo
import com.aiwazian.messenger.data.DropdownMenuAction
import com.aiwazian.messenger.data.GroupInfo
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.TopBarAction
import com.aiwazian.messenger.data.UserInfo
import com.aiwazian.messenger.interfaces.Profile
import com.aiwazian.messenger.services.ClipboardHelper
import com.aiwazian.messenger.services.DialogController
import com.aiwazian.messenger.services.UserManager
import com.aiwazian.messenger.ui.channel.ChannelSettingsScreen
import com.aiwazian.messenger.ui.element.CustomDialog
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionItem
import com.aiwazian.messenger.ui.group.GroupSettingsScreen
import com.aiwazian.messenger.ui.settings.profile.SettingsProfileScreen
import com.aiwazian.messenger.ui.settings.profile.SettingsUsernameScreen
import com.aiwazian.messenger.utils.ShortcutHelper
import com.aiwazian.messenger.viewModels.ChannelViewModel
import com.aiwazian.messenger.viewModels.GroupViewModel
import com.aiwazian.messenger.viewModels.MainViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel
import com.aiwazian.messenger.viewModels.ProfileViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(profileId: Long) {
    val navViewModel = viewModel<NavigationViewModel>()
    
    val profileViewModel = viewModel<ProfileViewModel>()
    
    var profileInfo by remember { mutableStateOf<Profile?>(null) }
    
    LaunchedEffect(Unit) {
        profileViewModel.open(profileId)
        profileInfo = profileViewModel.profile.first()
    }
    
    when (profileInfo) {
        is UserInfo -> {
            UserProfile(profileInfo as UserInfo)
        }
        
        is ChannelInfo -> {
            val channelViewModel = hiltViewModel<ChannelViewModel>()
            channelViewModel.open(profileInfo as ChannelInfo)
            ChannelProfile(profileInfo as ChannelInfo)
        }
        
        is GroupInfo -> {
            val groupViewModel = hiltViewModel<GroupViewModel>()
            groupViewModel.open(profileInfo as GroupInfo)
            GroupProfile(profileInfo as GroupInfo)
        }
        
        null -> {
            Scaffold(topBar = {
                PageTopBar(
                    navigationIcon = NavigationIcon(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        onClick = navViewModel::removeLastScreenInStack
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
                    LoadingIndicator()
                }
            }
        }
    }
}

@Composable
private fun GroupProfile(group: GroupInfo) {
    val context = LocalContext.current
    val navViewModel = viewModel<NavigationViewModel>()
    
    var myId by remember { mutableLongStateOf(0) }
    LaunchedEffect(Unit) {
        myId = UserManager.user.value.id
    }
    
    var actions by remember { mutableStateOf<List<TopBarAction>>(emptyList()) }
    
    LaunchedEffect(
        group.ownerId,
        myId
    ) {
        actions = if (group.ownerId == myId) {
            listOf(
                TopBarAction(
                    icon = Icons.Filled.Edit,
                    onClick = {
                        navViewModel.addScreenInStack { GroupSettingsScreen() }
                    }
                ),
                TopBarAction(
                    icon = Icons.Outlined.MoreVert,
                    dropdownActions = listOf(
                        DropdownMenuAction(
                            icon = Icons.Outlined.AddHome,
                            text = context.getString(R.string.add_to_home_screen),
                            onClick = {
                                ShortcutHelper.createChatShortcut(
                                    context,
                                    group.id,
                                    group.name
                                )
                            })
                    )
                )
            )
        } else {
            listOf(
                TopBarAction(
                    icon = Icons.Outlined.MoreVert,
                    dropdownActions = listOf(
                        DropdownMenuAction(
                            icon = Icons.Outlined.AddHome,
                            text = context.getString(R.string.add_to_home_screen),
                            onClick = {
                                ShortcutHelper.createChatShortcut(
                                    context,
                                    group.id,
                                    group.name
                                )
                            })
                    )
                )
            )
        }
    }
    
    Scaffold(topBar = {
        TopBar(
            title = group.name,
            actions = actions
        )
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                if (group.bio.isNotBlank()) {
                    SectionItem(text = group.bio)
                }
            }
        }
    }
}

@Composable
private fun ChannelProfile(channel: ChannelInfo) {
    val context = LocalContext.current
    val mainViewModel = hiltViewModel<MainViewModel>()
    val navViewModel = viewModel<NavigationViewModel>()
    val channelViewModel = hiltViewModel<ChannelViewModel>()
    val channelInfo by channelViewModel.channelInfo.collectAsState()
    
    val scope = rememberCoroutineScope()
    val leaveChannelDialog = DialogController()
    
    var myId by remember { mutableLongStateOf(0) }
    LaunchedEffect(Unit) {
        myId = UserManager.user.value.id
    }
    
    var actions by remember { mutableStateOf<List<TopBarAction>>(emptyList()) }
    
    LaunchedEffect(
        channelInfo,
        myId
    ) {
        val dropdown = mutableListOf<DropdownMenuAction>()
        
        dropdown += DropdownMenuAction(
            icon = Icons.Outlined.AddHome,
            text = context.getString(R.string.add_to_home_screen),
            onClick = {
                ShortcutHelper.createChatShortcut(
                    context,
                    channelInfo.id,
                    channelInfo.name
                )
            }
        )
        
        if (channelInfo.isSubscribed && channelInfo.ownerId != myId) {
            dropdown += DropdownMenuAction(
                icon = Icons.AutoMirrored.Outlined.Logout,
                text = context.getString(R.string.leave_channel),
                onClick = leaveChannelDialog::show
            )
        }
        
        actions = if (channel.ownerId == myId) {
            listOf(
                TopBarAction(
                    icon = Icons.Filled.Edit,
                    onClick = {
                        navViewModel.addScreenInStack { ChannelSettingsScreen() }
                    }
                ),
                TopBarAction(
                    icon = Icons.Outlined.MoreVert,
                    dropdownActions = dropdown
                )
            )
        } else {
            listOf(
                TopBarAction(
                    icon = Icons.Outlined.MoreVert,
                    dropdownActions = dropdown
                )
            )
        }
    }
    
    Scaffold(topBar = {
        TopBar(
            title = channelInfo.name,
            actions = actions
        )
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SectionContainer {
                if (channelInfo.bio.isNotEmpty()) {
                    SectionItem(
                        text = channelInfo.bio,
                        description = stringResource(R.string.description)
                    )
                }
                
                if (channelInfo.publicLink?.isNotEmpty() == true) {
                    SectionItem(
                        text = channelInfo.publicLink!!,
                        description = stringResource(R.string.invite_link)
                    )
                }
            }
        }
        
        if (leaveChannelDialog.isVisible) {
            CustomDialog(
                title = stringResource(R.string.leave_channel),
                onDismissRequest = leaveChannelDialog::hide,
                buttons = {
                    TextButton(onClick = leaveChannelDialog::hide) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                val isLeaved = channelViewModel.tryLeave(channel.id)
                                
                                if (isLeaved) {
                                    mainViewModel.deleteChat(channel.id)
                                    navViewModel.removeLastScreenInStack()
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(text = stringResource(R.string.leave_channel))
                    }
                }) {
                Text(text = buildAnnotatedString {
                    append(stringResource(R.string.leave_channel_confirm))
                    withStyle(SpanStyle(fontWeight = FontWeight.W500)) {
                        append(" ${channelInfo.name}")
                    }
                    append("?")
                })
            }
        }
    }
}

@Composable
private fun UserProfile(user: UserInfo) {
    val navViewModel = viewModel<NavigationViewModel>()
    val profileViewModel = viewModel<ProfileViewModel>()
    val context = LocalContext.current
    val clipboardHelper = ClipboardHelper(context = context)
    
    var myId by remember { mutableLongStateOf(0) }
    LaunchedEffect(Unit) {
        myId = UserManager.user.value.id
    }
    
    val scrollState = rememberScrollState()
    
    var actions by remember { mutableStateOf<List<TopBarAction>>(emptyList()) }
    
    LaunchedEffect(user.id) {
        actions = if (myId == user.id) {
            listOf(
                TopBarAction(
                    icon = Icons.Outlined.MoreVert,
                    dropdownActions = listOf(
                        DropdownMenuAction(
                            icon = Icons.Outlined.Edit,
                            text = context.getString(R.string.edit_info),
                            onClick = {
                                navViewModel.addScreenInStack { SettingsProfileScreen() }
                            }
                        ),
                        DropdownMenuAction(
                            icon = Icons.Outlined.AlternateEmail,
                            text = context.getString(R.string.change_username),
                            onClick = {
                                navViewModel.addScreenInStack { SettingsUsernameScreen() }
                            }
                        )
                    )
                )
            )
        } else {
            listOf(
                TopBarAction(
                    icon = Icons.Outlined.MoreVert,
                    dropdownActions = listOf(
                        DropdownMenuAction(
                            icon = Icons.Outlined.AddHome,
                            text = context.getString(R.string.add_to_home_screen),
                            onClick = {
                                val chatName = "${user.firstName} ${user.lastName}"
                                ShortcutHelper.createChatShortcut(
                                    context,
                                    user.id,
                                    chatName
                                )
                            }
                        )
                    )
                )
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopBar(
                title = "${user.firstName} ${user.lastName}",
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
                
                if (userBio.isNotEmpty()) {
                    SectionItem(
                        text = userBio,
                        description = stringResource(R.string.bio),
                        onLongClick = {
                            clipboardHelper.copy(userBio)
                        })
                }
                
                val username = user.username
                
                if (!username.isNullOrBlank()) {
                    SectionItem(
                        text = ("@$username"),
                        description = stringResource(R.string.username),
                        onLongClick = {
                            clipboardHelper.copy(username)
                        }, //                        primaryIcon = Icons.Outlined.QrCode,
                        primaryIconClick = {
                            navViewModel.addScreenInStack {
                                QRCodeScreen()
                            }
                        })
                }
                
                val dateOfBirth = user.dateOfBirth
                
                if (dateOfBirth != null) {
                    val date = SimpleDateFormat(
                        "d MMM yyyy",
                        Locale.getDefault()
                    ).format(dateOfBirth)
                    
                    SectionItem(
                        text = date,
                        description = stringResource(R.string.date_of_birth)
                    )
                }
            }
        }
        
        if (profileViewModel.blockUserDialog.isVisible) {
            CustomDialog(
                title = stringResource(R.string.block_user),
                onDismissRequest = profileViewModel.blockUserDialog::hide,
                content = {
                    Text(
                        text = "${stringResource(R.string.block_user_confirm)} ${user.firstName}?",
                        lineHeight = 18.sp
                    )
                },
                buttons = {
                    TextButton(
                        onClick = profileViewModel.blockUserDialog::hide
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = profileViewModel.blockUserDialog::hide,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.block_user))
                    }
                })
        }
        
        if (profileViewModel.startSecretChatDialog.isVisible) {
            CustomDialog(
                title = stringResource(R.string.secret_chat),
                onDismissRequest = profileViewModel.startSecretChatDialog::hide,
                content = {
                    Text(
                        text = stringResource(R.string.secret_chat_confirm),
                        lineHeight = 18.sp
                    )
                },
                buttons = {
                    TextButton(
                        onClick = profileViewModel.startSecretChatDialog::hide
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = profileViewModel.startSecretChatDialog::hide
                    ) {
                        Text(stringResource(R.string.start))
                    }
                })
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    actions: List<TopBarAction>
) {
    val navViewModel = viewModel<NavigationViewModel>()
    
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
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        ),
        actions = actions
    )
}