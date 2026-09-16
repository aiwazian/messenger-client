/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkChatRead
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.animations.expressiveScaleIn
import com.aiwazian.messenger.ui.animations.expressiveScaleOut
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.app.AppPrimaryScrollableTabRow
import com.aiwazian.messenger.ui.components.BottomBarScrim
import com.aiwazian.messenger.ui.components.ChatCard
import com.aiwazian.messenger.ui.components.TopBarScrim
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.screens.lock.LockScreen
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (!isGranted) viewModel.showNotificationSheet()
    }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !uiState.askedPermission) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    ModalNavigationDrawer(
        gesturesEnabled = uiState.selectedChatIds.isEmpty(),
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                drawerState = drawerState,
                user = uiState.me,
                theme = uiState.theme,
                showAccountSheet = uiState.showAccountBottomSheet,
                onShowAccountSheet = viewModel::showAccountSheet,
                onHideAccountSheet = viewModel::hideAccountSheet
            )
        },
    ) { Content(drawerState, viewModel) }
    
    AnimatedVisibility(visible = uiState.isLocked, enter = fadeIn(), exit = fadeOut()) {
        LockScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(drawerState: DrawerState, viewModel: MainViewModel) {
    val navBackStack = LocalNavBackStack.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val hasSelection = uiState.selectedChatIds.isNotEmpty()
    val socketState by viewModel.socketState.collectAsState()
    
    BackHandler(hasSelection) { viewModel.clearSelection() }
    val pagerState = rememberPagerState(pageCount = { uiState.folderPages.size })
    BackHandler(enabled = !hasSelection && !drawerState.isOpen && pagerState.currentPage != 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }
    
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        AnimatedContent(targetState = hasSelection, transitionSpec = { fadeIn() togetherWith fadeOut() }) { selected ->
            if (!selected) {
                Column {
                    DefaultTopBar(
                        drawerState = drawerState,
                        passcodeEnabled = uiState.hasPasscode,
                        onLockClick = { scope.launch { viewModel.lockApp() } },
                        socketState = socketState
                    )
                    if (uiState.folderPages.size > 1) {
                        ChatFolderTabs(
                            pages = uiState.folderPages,
                            selectedIndex = pagerState.currentPage,
                            onTabClick = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                            onEditFolder = { folderId -> navBackStack.add(AppRoute.ChatFolderEditor(folderId)) },
                            onEditFolders = { navBackStack.add(AppRoute.ChatFolders) },
                            onDeleteFolder = viewModel::requestFolderDeletion,
                            onMarkFolderRead = viewModel::markFolderChatsRead
                        )
                    }
                }
            } else {
                SelectionTopBar(
                    selectedCount = uiState.selectedChatIds.size,
                    onClearSelection = viewModel::clearSelection,
                    onPinClick = viewModel::pinSelectedChats,
                    onUnpinClick = viewModel::unpinSelectedChats,
                    hasUnpinnedChats = viewModel.hasUnpinnedSelectedChats(),
                    onMarkReadClick = viewModel::markSelectedChatsRead,
                    onMarkUnreadClick = viewModel::markSelectedChatsUnread,
                    hasUnreadChats = viewModel.hasUnreadSelectedChats()
                )
            }
        }
    }, floatingActionButton = {
        AnimatedVisibility(visible = !hasSelection, enter = expressiveScaleIn, exit = expressiveScaleOut) {
            FloatingActionButton(shape = CircleShape, onClick = { navBackStack.add(AppRoute.NewMessage) }) {
                Icon(imageVector = Icons.Rounded.Create, contentDescription = null)
            }
        }
    }) { innerPadding ->
        val openChat: (Chat, String) -> Unit = { chat, chatName ->
            navBackStack.add(AppRoute.Chat(chatId = chat.id, chatName = chatName, avatarUri = chat.avatarUri?.toString()))
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(
                start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                end = innerPadding.calculateEndPadding(LayoutDirection.Ltr)
            )) {
                if (uiState.chats.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.empty_chats_hint),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                } else if (uiState.folderPages.size <= 1) {
                    LaunchedEffect(Unit) { viewModel.setActiveFolder(ALL_CHATS_FOLDER_ID) }
                    ChatList(
                        chats = uiState.chats,
                        myId = uiState.me.id,
                        selectedChatIds = uiState.selectedChatIds,
                        onlineUserIds = uiState.onlineUserIds,
                        hasSelection = hasSelection,
                        topPadding = innerPadding.calculateTopPadding(),
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        onOpenChat = openChat,
                        onToggleSelection = viewModel::toggleChatSelection
                    )
                } else {
                    LaunchedEffect(pagerState.currentPage, uiState.folderPages) {
                        uiState.folderPages.getOrNull(pagerState.currentPage)?.let { viewModel.setActiveFolder(it.id) }
                    }
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), userScrollEnabled = !hasSelection) { page ->
                        ChatList(
                            chats = uiState.folderPages[page].chats,
                            myId = uiState.me.id,
                            selectedChatIds = uiState.selectedChatIds,
                            onlineUserIds = uiState.onlineUserIds,
                            hasSelection = hasSelection,
                            topPadding = innerPadding.calculateTopPadding(),
                            bottomPadding = innerPadding.calculateBottomPadding(),
                            onOpenChat = openChat,
                            onToggleSelection = viewModel::toggleChatSelection
                        )
                    }
                }
            }
            TopBarScrim(height = innerPadding.calculateTopPadding())
            BottomBarScrim(height = innerPadding.calculateBottomPadding())
        }
        if (uiState.showNotificationBottomSheet) {
            val context = LocalContext.current
            val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
            AppBottomSheet(onDismissRequest = viewModel::hideNotificationSheet, sheetState = sheetState) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)) {
                        Icon(
                            Icons.Rounded.NotificationsNone,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(14.dp)
                                .size(28.dp)
                        )
                    }
                    Text(stringResource(R.string.notification_exception_receive))
                    TextButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }, shape = MaterialTheme.shapes.medium) {
                        Text(stringResource(R.string.open_settings))
                    }
                }
            }
        }
    }
    uiState.folderPendingDeletion?.let {
        AppDialog(
            title = stringResource(R.string.remove_folder),
            onDismissRequest = viewModel::cancelFolderDeletion,
            content = { Text(stringResource(R.string.delete_folder_confirm_message)) },
            buttons = {
                TextButton(onClick = viewModel::cancelFolderDeletion) { Text(stringResource(R.string.cancel)) }
                TextButton(
                    onClick = viewModel::confirmFolderDeletion,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(text = stringResource(R.string.delete)) }
            })
    }
}

@Composable
private fun ChatFolderTabs(
    pages: List<ChatFolderPage>, selectedIndex: Int, onTabClick: (Int) -> Unit,
    onEditFolder: (Int) -> Unit, onEditFolders: () -> Unit,
    onDeleteFolder: (Int) -> Unit, onMarkFolderRead: (Int) -> Unit,
) {
    AppPrimaryScrollableTabRow(selectedTabIndex = selectedIndex) {
        pages.forEachIndexed { index, page ->
            val isAllChats = page.id == ALL_CHATS_FOLDER_ID
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                targetValue = if (isPressed) 0.96f else 1f,
                label = "button_${index}_scale_animation"
            )
            var expanded by remember { mutableStateOf(false) }
            val backgroundColor by animateColorAsState(
                targetValue = if (expanded && selectedIndex != index) MaterialTheme.colorScheme.surfaceContainerHighest
                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0f)
            )
            val accentColor by animateColorAsState(
                targetValue = if (index == selectedIndex) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(modifier = Modifier
                .padding(vertical = 4.dp)
                .zIndex(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .widthIn(min = TabRowDefaults.ScrollableTabRowMinTabWidth)
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .combinedClickable(
                            onClick = { onTabClick(index) },
                            onLongClick = { expanded = !expanded },
                            interactionSource = interactionSource,
                            indication = null
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = page.name.asString(), color = accentColor, fontSize = 14.sp,
                            lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        AnimatedVisibility(visible = page.unreadChatCount > 0, enter = expressiveScaleIn, exit = expressiveScaleOut) {
                            val containerColor by animateColorAsState(
                                targetValue = if (selectedIndex == index) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                else Color(0xFFC6C6C6)
                            )
                            Badge(containerColor = containerColor) {
                                Text(
                                    text = page.unreadChatCount.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }
                AppDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    AppDropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        text = stringResource(if (isAllChats) R.string.edit_folders else R.string.edit_folder),
                        onClick = {
                            expanded = false
                            if (isAllChats) onEditFolders() else onEditFolder(page.id)
                        })
                    if (page.unreadChatCount > 0) {
                        AppDropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.MarkChatRead, null) },
                            text = stringResource(R.string.mark_all_as_read),
                            onClick = {
                                expanded = false
                                onMarkFolderRead(page.id)
                            })
                    }
                    if (!isAllChats) {
                        AppDropdownMenuItem(
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) },
                            text = stringResource(R.string.delete),
                            onClick = {
                                expanded = false
                                onDeleteFolder(page.id)
                            },
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatList(
    chats: List<Chat>, myId: Long, selectedChatIds: Set<Long>, onlineUserIds: Set<Long>,
    hasSelection: Boolean, topPadding: Dp, bottomPadding: Dp,
    onOpenChat: (Chat, String) -> Unit, onToggleSelection: (Long) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Spacer(Modifier.height(topPadding)) }
        items(chats) { chat ->
            val chatName = chat.chatName.asString()
            val isSelected = chat.id in selectedChatIds
            val isOnline = ChatType.fromId(chat.id) == ChatType.PRIVATE && chat.id != myId && chat.id in onlineUserIds
            ChatCard(
                modifier = Modifier.animateItem(), chat = chat, myId = myId,
                isSelected = isSelected, isOnline = isOnline,
                unreadMessageCount = chat.unreadCount,
                onClickChat = {
                    if (hasSelection) onToggleSelection(chat.id) else onOpenChat(chat, chatName)
                },
                onLongClickChat = { onToggleSelection(chat.id) })
        }
        item { Spacer(Modifier.height(bottomPadding)) }
    }
}
