/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import android.app.Activity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexAlignItems
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexBoxScope
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults.rememberDropdownMenuPopupPositionProvider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.extensions.sharedBounds
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyDateWithYear
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.ChatCard
import com.aiwazian.messenger.ui.components.ShareBottomSheet
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.screens.chat.components.InviteLinkBottomSheet
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ProfileScreen(
    profileId: Long,
    profileName: String? = null,
    avatarUri: String? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.init(profileId, profileName, avatarUri?.toUri())
    }
    
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsState()
    var showLeaveDialog by remember { mutableStateOf(false) }
    var leaveDialogData by remember { mutableStateOf<Pair<String, ChatType>?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
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
                
                is ProfileUiEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(
                            message = effect.message.asString(context),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                
                is ProfileUiEffect.OpenUrl -> {
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .setTranslateLocale(Locale.getDefault())
                        .build()
                        .launchUrl(context, effect.url.toUri())
                }
                
                is ProfileUiEffect.NavigateToChat -> {
                    val previous = navBackStack.getOrNull(navBackStack.size - 2)
                    if (previous is AppRoute.Chat && previous.chatId == effect.chatId) {
                        navBackStack.removeLastOrNull()
                    } else {
                        navBackStack.add(AppRoute.Chat(effect.chatId, null))
                    }
                }
            }
        }
    }
    
    val scrollState = rememberScrollState()
    val view = LocalView.current
    val window = (view.context as Activity).window
    val insetsController = WindowCompat.getInsetsController(window, view)
    
    val hasAvatar = uiState.avatars.any { it != null }
    
    DisposableEffect(hasAvatar) {
        val previousState = insetsController.isAppearanceLightStatusBars
        if (hasAvatar) {
            insetsController.isAppearanceLightStatusBars = false
        }
        onDispose {
            insetsController.isAppearanceLightStatusBars = previousState
        }
    }
    
    Scaffold(snackbarHost = {
        AppSnackbar(snackbarHostState)
    }, topBar = {
        TopBar(
            chatId = uiState.id,
            title = uiState.title.asString(),
            subTitle = uiState.subTitle.asString(),
            actions = uiState.actions,
            contentColor = if (hasAvatar) Color.White else Color.Unspecified
        )
    }) { innerPadding ->
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    if (uiState.avatars.isNotEmpty()) {
                        ProfileImageCarousel(
                            modifier = Modifier.padding(bottom = 10.dp),
                            avatars = uiState.avatars,
                            profileId = uiState.id
                        )
                    } else {
                        Spacer(Modifier.padding(top = innerPadding.calculateTopPadding()))
                    }
                }
                
                Box(
                    modifier = Modifier.padding(
                        start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                        end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                        bottom = innerPadding.calculateBottomPadding()
                    )
                ) {
                    when (val profile = uiState.profile) {
                        is Profile.User -> UserProfile(
                            myId = uiState.myId,
                            userId = uiState.id,
                            user = profile,
                            channelInfo = uiState.profileChannelInfo,
                            onChatClick = viewModel::onChatButtonClicked,
                            onEditClick = { navBackStack.add(AppRoute.SettingsProfile) },
                            onLinkClicked = viewModel::onLinkClicked,
                            onUsernameClicked = viewModel::onUsernameClicked,
                            onCopyClick = viewModel::copyToClipboard,
                            onShareClick = viewModel::onShareUsername
                        )
                        
                        is Profile.Channel -> ChannelProfile(
                            myId = uiState.myId,
                            channel = profile,
                            onJoinClick = viewModel::onJoinClicked,
                            onLeaveClick = viewModel::showLeaveDialog,
                            onLinkClicked = viewModel::onLinkClicked,
                            onUsernameClicked = viewModel::onUsernameClicked,
                            onCopyClick = viewModel::copyToClipboard,
                            onShareClick = viewModel::onShareUsername
                        )
                        
                        is Profile.Group -> GroupProfile(
                            myId = uiState.myId,
                            group = profile,
                            onChatClick = viewModel::onChatButtonClicked,
                            onJoinClick = viewModel::onJoinClicked,
                            onLeaveClick = viewModel::showLeaveDialog,
                            onLinkClicked = viewModel::onLinkClicked,
                            onUsernameClicked = viewModel::onUsernameClicked,
                            onCopyClick = viewModel::copyToClipboard,
                            onShareClick = viewModel::onShareUsername
                        )
                        
                        else -> {}
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(innerPadding.calculateTopPadding())
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(innerPadding.calculateBottomPadding())
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            )
                        )
                    )
            )
        }
    }
    
    if (showLeaveDialog && leaveDialogData != null) {
        LeaveProfileDialog(
            onDismiss = {
                showLeaveDialog = false
                viewModel.hideLeaveDialog()
            },
            onConfirm = viewModel::onLeaveConfirmed,
            profileName = leaveDialogData!!.first,
            chatType = leaveDialogData!!.second
        )
    }
    
    if (uiState.showShareBottomSheet) {
        ShareBottomSheet(
            items = uiState.shareTargets,
            onItemClick = viewModel::toggleShareTarget,
            onSendClick = viewModel::sendShare,
            onDismiss = viewModel::dismissShareBottomSheet
        )
    }
    
    if (uiState.showInviteBottomSheet && uiState.inviteLinkInfo != null) {
        val info = uiState.inviteLinkInfo!!
        InviteLinkBottomSheet(
            chatId = info.chatId,
            name = info.name.orEmpty(),
            description = info.description,
            count = info.membersCount ?: 0,
            requireApproval = info.requireApproval,
            isLoading = uiState.isProcessingInvite,
            onDismiss = viewModel::dismissInviteBottomSheet,
            onJoin = viewModel::onSubscribeViaInviteLink
        )
    }
    
    if (uiState.showBannedDialog) {
        AppDialog(
            title = stringResource(R.string.no_access),
            onDismissRequest = viewModel::dismissBannedDialog,
            buttons = {
                TextButton(onClick = viewModel::dismissBannedDialog) {
                    Text(stringResource(R.string.ok))
                }
            },
            content = {
                Text("Вас заблокировал администратор этого чата")
            })
    }
    
    if (uiState.showBlockDialog) {
        AppDialog(
            title = if (uiState.isBlockedStateForDialog) stringResource(R.string.unblock) else stringResource(
                R.string.block
            ),
            onDismissRequest = viewModel::dismissBlockDialog,
            buttons = {
                TextButton(onClick = viewModel::dismissBlockDialog) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = viewModel::toggleBlockUser) {
                    Text(stringResource(R.string.yes))
                }
            },
            content = {
                Text(if (uiState.isBlockedStateForDialog) "Вы уверены что хотите разблокировать этого пользователя?" else "Вы уверены что хотите заблокировать этого пользователя?")
            }
        )
    }
}

/**
 * Строка профиля с выпадающим меню.
 *
 * [AppDropdownMenu] не принимает modifier и строится относительно родителя,
 * поэтому строка завёрнута в свой Box: иначе меню прижималось бы к верху всего
 * блока, а не к нажатой строке. Провайдер позиции дополнительно прижимает меню
 * к правому краю строки.
 *
 * Ссылки и упоминания внутри текста обрабатываются своими обработчиками и
 * до меню не доходят, так что тап по ссылке по-прежнему открывает ссылку.
 */
@Composable
private fun SectionItemWithMenu(
    headlineText: String,
    supportingText: String,
    onLinkClicked: ((String) -> Unit)? = null,
    onUsernameClicked: ((String) -> Unit)? = null,
    menuContent: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        SectionItem(
            headlineText = headlineText,
            supportingText = supportingText,
            onLinkClicked = onLinkClicked,
            onUsernameClicked = onUsernameClicked,
            onClick = { expanded = true },
            onLongClick = { expanded = true }
        )
        
        AppDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            popupPositionProvider = rememberDropdownMenuPopupPositionProvider(MenuAnchorPosition.End)
        ) {
            menuContent { expanded = false }
        }
    }
}

@Composable
private fun CopyMenuItem(onClick: () -> Unit) {
    AppDropdownMenuItem(
        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
        text = { Text(stringResource(R.string.copy)) },
        onClick = onClick
    )
}

@Composable
private fun ShareMenuItem(onClick: () -> Unit) {
    AppDropdownMenuItem(
        leadingIcon = { Icon(Icons.Rounded.Share, null) },
        text = { Text(stringResource(R.string.share)) },
        onClick = onClick
    )
}

@Composable
private fun GroupProfile(
    myId: Long,
    group: Profile.Group,
    onChatClick: () -> Unit,
    onJoinClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onLinkClicked: (String) -> Unit,
    onUsernameClicked: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onShareClick: (String) -> Unit
) {
    Column {
        ProfileActions(
            actions = if (!group.isMember) {
                listOf(
                    ProfileActionData(
                        onClick = onJoinClick,
                        icon = Icons.Outlined.PersonAdd,
                        text = UiText.StringResource(R.string.join)
                    )
                )
            } else {
                buildList {
                    add(
                        ProfileActionData(
                            onClick = onChatClick,
                            icon = Icons.Rounded.ChatBubbleOutline,
                            text = UiText.StringResource(R.string.chat)
                        )
                    )
                    if (group.ownerId != myId) {
                        add(
                            ProfileActionData(
                                onClick = onLeaveClick,
                                icon = Icons.AutoMirrored.Rounded.Logout,
                                text = UiText.StringResource(R.string.leave),
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        )
        
        SectionContainer {
            val bio = group.bio
            
            if (!bio.isNullOrBlank()) {
                SectionItemWithMenu(
                    headlineText = bio,
                    supportingText = stringResource(R.string.description),
                    onLinkClicked = onLinkClicked,
                    onUsernameClicked = onUsernameClicked
                ) { dismiss ->
                    CopyMenuItem {
                        onCopyClick(bio)
                        dismiss()
                    }
                }
            }
            
            val username = group.username
            
            if (!username.isNullOrBlank()) {
                val usernameText = "@$username"
                
                SectionItemWithMenu(
                    headlineText = usernameText,
                    supportingText = stringResource(R.string.public_link)
                ) { dismiss ->
                    CopyMenuItem {
                        onCopyClick(usernameText)
                        dismiss()
                    }
                    ShareMenuItem {
                        onShareClick(usernameText)
                        dismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelProfile(
    myId: Long,
    channel: Profile.Channel,
    onJoinClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onLinkClicked: (String) -> Unit,
    onUsernameClicked: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onShareClick: (String) -> Unit
) {
    Column {
        ProfileActions(
            actions = if (!channel.isSubscribed) {
                listOf(
                    ProfileActionData(
                        onClick = onJoinClick,
                        icon = Icons.Outlined.PersonAdd,
                        text = UiText.StringResource(R.string.subscribe)
                    )
                )
            } else {
                buildList {
                    if (channel.ownerId != myId) {
                        add(
                            ProfileActionData(
                                onClick = onLeaveClick,
                                icon = Icons.AutoMirrored.Rounded.Logout,
                                text = UiText.StringResource(R.string.leave),
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        )
        
        SectionContainer {
            val bio = channel.bio
            
            if (!bio.isNullOrBlank()) {
                SectionItemWithMenu(
                    headlineText = bio,
                    supportingText = stringResource(R.string.description),
                    onLinkClicked = onLinkClicked,
                    onUsernameClicked = onUsernameClicked
                ) { dismiss ->
                    CopyMenuItem {
                        onCopyClick(bio)
                        dismiss()
                    }
                }
            }
            
            val username = channel.username
            
            if (!username.isNullOrBlank()) {
                val usernameText = "@$username"
                
                SectionItemWithMenu(
                    headlineText = usernameText,
                    supportingText = stringResource(R.string.public_link)
                ) { dismiss ->
                    CopyMenuItem {
                        onCopyClick(usernameText)
                        dismiss()
                    }
                    ShareMenuItem {
                        onShareClick(usernameText)
                        dismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun UserProfile(
    myId: Long,
    userId: Long,
    user: Profile.User,
    channelInfo: ProfileChannelInfo? = null,
    onChatClick: () -> Unit,
    onEditClick: () -> Unit,
    onLinkClicked: (String) -> Unit,
    onUsernameClicked: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onShareClick: (String) -> Unit
) {
    val navBackStack = LocalNavBackStack.current
    
    Column {
        ProfileActions(
            actions = if (myId == userId) {
                listOf(
                    ProfileActionData(
                        onClick = onEditClick,
                        icon = Icons.Rounded.Edit,
                        text = UiText.StringResource(R.string.edit)
                    )
                )
            } else {
                listOf(
                    ProfileActionData(
                        onClick = onChatClick,
                        icon = Icons.Rounded.ChatBubbleOutline,
                        text = UiText.StringResource(R.string.chat)
                    )
                )
            }
        )
        
        if (user.profileChannelId != null && channelInfo != null) {
            val channelChat = Chat(
                id = channelInfo.id,
                chatName = UiText.DynamicString(channelInfo.name),
                isPinned = false,
                avatarUri = channelInfo.avatarUri,
                lastMessage = channelInfo.lastMessage
            )
            SectionContainer {
                ChatCard(
                    chat = channelChat, onClickChat = {
                        navBackStack.add(
                            AppRoute.Chat(
                                chatId = channelInfo.id,
                                chatName = channelInfo.name,
                                avatarUri = channelInfo.avatarUri?.toString()
                            )
                        )
                    })
            }
        }
        
        SectionContainer {
            val bio = user.bio
            
            if (!bio.isNullOrBlank()) {
                SectionItemWithMenu(
                    headlineText = bio,
                    supportingText = stringResource(R.string.bio),
                    onLinkClicked = onLinkClicked,
                    onUsernameClicked = onUsernameClicked
                ) { dismiss ->
                    CopyMenuItem {
                        onCopyClick(bio)
                        dismiss()
                    }
                }
            }
            
            val username = user.username
            
            if (!username.isNullOrBlank()) {
                val usernameText = "@$username"
                
                SectionItemWithMenu(
                    headlineText = usernameText,
                    supportingText = stringResource(R.string.username)
                ) { dismiss ->
                    CopyMenuItem {
                        onCopyClick(usernameText)
                        dismiss()
                    }
                    ShareMenuItem {
                        onShareClick(usernameText)
                        dismiss()
                    }
                }
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

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
private fun ProfileActions(
    actions: List<ProfileActionData>,
    modifier: Modifier = Modifier
) {
    FlexBox(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
        config = {
            wrap(FlexWrap.NoWrap)
            direction(FlexDirection.Row)
            alignItems(FlexAlignItems.Stretch)
            gap(10.dp)
        }) {
        actions.forEach { action ->
            ProfileAction(
                onClick = action.onClick,
                icon = action.icon,
                text = action.text.asString(),
                contentColor = action.contentColor
            )
        }
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
private fun FlexBoxScope.ProfileAction(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.Unspecified
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.flex {
            grow(1f)
            basis(0.dp)
        },
        shapes = ButtonDefaults.shapes(pressedShape = MaterialTheme.shapes.large),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
            Text(text, fontSize = 12.sp, lineHeight = 12.sp)
        }
    }
}

@Composable
private fun TopBar(
    chatId: Long,
    title: String,
    subTitle: String,
    actions: List<TopBarAction>,
    contentColor: Color = Color.Unspecified
) {
    val navBackStack = LocalNavBackStack.current
    
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    maxLines = 1,
                    fontSize = 18.sp,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .sharedBounds(
                            key = "chat-name-$chatId", zIndexInOverlay = 1f
                        )
                        .fillMaxWidth()
                )
                Text(
                    text = subTitle.lowercase(),
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    modifier = Modifier
                        .sharedBounds(
                            key = "chat-sub-title-$chatId", zIndexInOverlay = 1f
                        )
                        .fillMaxWidth()
                )
            }
        }, navigationIcon = {
            IconButton(onClick = navBackStack::removeLastOrNull) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
            }
        }, actions = {
            actions.forEach { action ->
                var expand by remember { mutableStateOf(false) }
                IconButton(
                    onClick = {
                        if (action.onClick != null) {
                            action.onClick()
                        } else {
                            expand = true
                        }
                    }) {
                    Icon(action.icon, null)
                }
                AppDropdownMenu(expanded = expand, onDismissRequest = { expand = false }) {
                    action.dropdownActions.forEach { action ->
                        AppDropdownMenuItem(leadingIcon = {
                            Icon(action.icon, null)
                        }, text = {
                            Text(action.text.asString())
                        }, onClick = {
                            action.onClick?.invoke()
                            expand = false
                        })
                    }
                }
            }
        }, colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor,
            titleContentColor = contentColor,
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
    
    AppDialog(title = title, onDismissRequest = onDismiss, content = {
        Text(text = message)
    }, buttons = {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        TextButton(
            onClick = onConfirm,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text(title) }
    })
}
