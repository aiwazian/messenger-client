/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.MarkChatRead
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSearchBarState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.aiwazian.messenger.BuildConfig
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.enums.ThemeOption
import com.aiwazian.messenger.ui.animations.expressiveScaleIn
import com.aiwazian.messenger.ui.animations.expressiveScaleOut
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.components.AnimatedDotsText
import com.aiwazian.messenger.ui.components.ChatAvatar
import com.aiwazian.messenger.ui.components.ChatCard
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.screens.lock.LockScreen
import com.aiwazian.messenger.ui.screens.main.search.ChatResultsList
import com.aiwazian.messenger.ui.screens.main.search.EmptySearchResultsPlaceholder
import com.aiwazian.messenger.ui.screens.main.search.LoadingPlaceholder
import com.aiwazian.messenger.ui.screens.main.search.SearchViewModel
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdTheme
import com.yandex.mobile.ads.compose.Banner
import com.yandex.mobile.ads.compose.BannerEvents
import com.yandex.mobile.ads.compose.BannerSize
import com.yandex.mobile.ads.compose.rememberBannerAdState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (!isGranted) {
            viewModel.showNotificationSheet()
        }
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
            DrawerContent(drawerState = drawerState, user = uiState.me, theme = uiState.theme)
        },
    ) {
        Content(drawerState, viewModel)
    }
    
    AnimatedVisibility(
        visible = uiState.isLocked,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        LockScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    drawerState: DrawerState, viewModel: MainViewModel
) {
    val navBackStack = LocalNavBackStack.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val hasSelection = uiState.selectedChatIds.isNotEmpty()
    val socketState by viewModel.socketState.collectAsState()
    val accountSwitcherViewModel: AccountSwitcherViewModel = hiltViewModel()
    val accountSwitcherState by accountSwitcherViewModel.uiState.collectAsState()
    
    BackHandler(hasSelection) {
        viewModel.clearSelection()
    }
    
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        AnimatedContent(
            targetState = hasSelection, transitionSpec = {
                fadeIn() togetherWith fadeOut()
            }) { hasSelection ->
            if (!hasSelection) {
                DefaultTopBar(
                    drawerState = drawerState,
                    passcodeEnabled = uiState.hasPasscode,
                    onLockClick = {
                        scope.launch {
                            viewModel.lockApp()
                        }
                    },
                    socketState = socketState,
                    onProfileClick = viewModel::showAccountDialog
                )
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
        AnimatedVisibility(
            visible = !hasSelection, enter = expressiveScaleIn, exit = expressiveScaleOut
        ) {
            FloatingActionButton(
                shape = CircleShape, onClick = {
                    navBackStack.add(AppRoute.NewMessage)
                }, containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Rounded.Create, contentDescription = null)
            }
        }
    }) { innerPadding ->
        val openChat: (Chat, String) -> Unit = { chat, chatName ->
            navBackStack.add(
                AppRoute.Chat(
                    chatId = chat.id,
                    chatName = chatName,
                    avatarUri = chat.avatarUri?.toString()
                )
            )
        }
        
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(
                    start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                    end = innerPadding.calculateEndPadding(LayoutDirection.Ltr)
                )
            ) {
                if (uiState.chats.isEmpty()) {
                    EmptyChatPlaceholder(text = "Чтобы начать общение нажмите на поле поиска сверху экрана и найдите пользователя по его @username")
                } else if (uiState.folderPages.size <= 1) {
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
                    val pagerState = rememberPagerState(pageCount = { uiState.folderPages.size })
                    
                    Spacer(Modifier.height(innerPadding.calculateTopPadding()))
                    
                    ChatFolderTabs(
                        pages = uiState.folderPages,
                        selectedIndex = pagerState.currentPage,
                        onTabClick = { index ->
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        })
                    
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        userScrollEnabled = !hasSelection
                    ) { page ->
                        ChatList(
                            chats = uiState.folderPages[page].chats,
                            myId = uiState.me.id,
                            selectedChatIds = uiState.selectedChatIds,
                            onlineUserIds = uiState.onlineUserIds,
                            hasSelection = hasSelection,
                            topPadding = 0.dp,
                            bottomPadding = innerPadding.calculateBottomPadding(),
                            onOpenChat = openChat,
                            onToggleSelection = viewModel::toggleChatSelection
                        )
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
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
                    .align(Alignment.TopCenter)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        innerPadding.calculateBottomPadding() + WindowInsets().getBottom(
                            LocalDensity.current
                        ).dp
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .align(Alignment.BottomCenter)
            )
        }
        
        if (uiState.showNotificationBottomSheet) {
            val context = LocalContext.current
            val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
            AppBottomSheet(
                onDismissRequest = viewModel::hideNotificationSheet,
                sheetState = sheetState
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Rounded.NotificationsNone,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(14.dp)
                                .size(28.dp)
                        )
                    }
                    Text("Включите уведомления, чтобы не пропускать важные сообщения")
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
    
    if (uiState.showAccountDialog) {
        AccountSwitcherDialog(
            currentUser = accountSwitcherState.currentUser,
            otherAccounts = accountSwitcherState.otherAccounts,
            onAccountClick = accountSwitcherViewModel::switchAccount,
            onAddAccount = {
                viewModel.hideAccountDialog()
                navBackStack.add(AppRoute.Login)
            },
            onDismissRequest = viewModel::hideAccountDialog
        )
    }
}

@Composable
private fun ChatFolderTabs(
    pages: List<ChatFolderPage>, selectedIndex: Int, onTabClick: (Int) -> Unit
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = Modifier
            .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 8.dp)
            .clip(CircleShape),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        edgePadding = 0.dp,
        indicator = {
            Column(
                Modifier
                    .tabIndicatorOffset(selectedIndex)
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {}
        },
        divider = {},
        minTabWidth = 100.dp
    ) {
        pages.forEachIndexed { index, page ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                targetValue = if (isPressed) 0.96f else 1f,
                label = "button_${index}_scale_animation"
            )
            TextButton(
                onClick = {
                    onTabClick(index)
                },
                modifier = Modifier
                    .zIndex(1f)
                    .graphicsLayer(scaleX = scale, scaleY = scale),
                shape = CircleShape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (index == selectedIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ),
                contentPadding = PaddingValues(0.dp),
                interactionSource = interactionSource
            ) {
                Text(
                    text = page.name.asString(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatList(
    chats: List<Chat>,
    myId: Long,
    selectedChatIds: Set<Long>,
    onlineUserIds: Set<Long>,
    hasSelection: Boolean,
    topPadding: Dp,
    bottomPadding: Dp,
    onOpenChat: (Chat, String) -> Unit,
    onToggleSelection: (Long) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Spacer(Modifier.height(topPadding))
        }
        items(chats) { chat ->
            val chatName = chat.chatName.asString()
            val isSelected = chat.id in selectedChatIds
            val isOnline = ChatType.fromId(chat.id) == ChatType.PRIVATE &&
                    chat.id != myId &&
                    chat.id in onlineUserIds
            ChatCard(
                modifier = Modifier.animateItem(),
                chat = chat,
                myId = myId,
                isSelected = isSelected,
                isOnline = isOnline,
                unreadMessageCount = chat.unreadCount,
                onClickChat = {
                    if (hasSelection) {
                        onToggleSelection(chat.id)
                    } else {
                        onOpenChat(chat, chatName)
                    }
                },
                onLongClickChat = {
                    onToggleSelection(chat.id)
                })
        }
        item {
            Spacer(Modifier.height(bottomPadding))
        }
    }
}

@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onPinClick: () -> Unit,
    onUnpinClick: () -> Unit,
    hasUnpinnedChats: Boolean,
    onMarkReadClick: () -> Unit,
    onMarkUnreadClick: () -> Unit,
    hasUnreadChats: Boolean
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Rounded.Close, null)
                }
                AnimatedContent(
                    modifier = Modifier.padding(start = 10.dp, end = 18.dp),
                    targetState = selectedCount,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInVertically { it } + fadeIn() + scaleIn() togetherWith slideOutVertically { -it } + fadeOut() + scaleOut()
                        } else {
                            slideInVertically { -it } + fadeIn() + scaleIn() togetherWith slideOutVertically { it } + fadeOut() + scaleOut()
                        }
                    }) { count ->
                    Text(text = "$count")
                }
            }
        }, navigationIcon = {}, actions = {
            var expand by remember { mutableStateOf(false) }
            
            IconButton(
                onClick = { expand = true },
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Icon(Icons.Rounded.MoreVert, null)
            }
            
            AppDropdownMenu(expanded = expand, onDismissRequest = { expand = false }) {
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.PushPin,
                            null,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(45f)
                        )
                    },
                    text = { Text(stringResource(if (hasUnpinnedChats) R.string.pin else R.string.unpin)) },
                    onClick = {
                        if (hasUnpinnedChats) {
                            onPinClick()
                        } else {
                            onUnpinClick()
                        }
                    })
                
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            if (hasUnreadChats) Icons.Outlined.MarkChatRead
                            else Icons.Outlined.MarkChatUnread,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            stringResource(
                                if (hasUnreadChats) R.string.mark_as_read
                                else R.string.mark_as_unread
                            )
                        )
                    },
                    onClick = {
                        expand = false
                        if (hasUnreadChats) {
                            onMarkReadClick()
                        } else {
                            onMarkUnreadClick()
                        }
                    })
            }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun EmptyChatPlaceholder(
    text: String, animation: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (animation != null) {
            val composition by rememberLottieComposition(
                spec = LottieCompositionSpec.Asset(animation)
            )
            
            LottieAnimation(
                composition = composition,
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 10.dp),
                iterations = LottieConstants.IterateForever,
                isPlaying = true
            )
        }
        
        Text(
            text = text, textAlign = TextAlign.Center, lineHeight = 16.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultTopBar(
    drawerState: DrawerState,
    passcodeEnabled: Boolean,
    onLockClick: () -> Unit,
    socketState: ConnectionState,
    onProfileClick: () -> Unit,
    searchViewModel: SearchViewModel = hiltViewModel(),
    accountSwitcherViewModel: AccountSwitcherViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    val searchUiState by searchViewModel.uiState.collectAsState()
    val accountSwitcherState by accountSwitcherViewModel.uiState.collectAsState()
    val textFieldState = rememberTextFieldState(searchUiState.query)
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(textFieldState.text) {
        searchViewModel.onQueryChange(textFieldState.text.toString())
    }
    
    LaunchedEffect(Unit) {
        accountSwitcherViewModel.sideEffect.collectLatest { sideEffect ->
            when (sideEffect) {
                is AccountSwitcherSideEffect.AccountSwitched -> {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }
            }
        }
    }
    
    val inputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = {},
            placeholder = {
                AnimatedContent(
                    targetState = socketState,
                    contentKey = { it },
                    transitionSpec = {
                        slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                    },
                    label = "connection_state_animation"
                ) { state ->
                    when (state) {
                        ConnectionState.CONNECTED -> Text(stringResource(R.string.search))
                        ConnectionState.DISCONNECTED -> AnimatedDotsText(
                            stringResource(R.string.waiting_for_network)
                        )
                        
                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> AnimatedDotsText(
                            stringResource(R.string.connecting)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                AnimatedContent(searchBarState.currentValue) {
                    if (it == SearchBarValue.Collapsed) {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                Icons.Rounded.Menu, null
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            textFieldState.edit {
                                replace(0, length, "")
                            }
                            scope.launch {
                                searchBarState.animateToCollapsed()
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack, null
                            )
                        }
                    }
                }
            },
            trailingIcon = {
                AnimatedContent(searchBarState.currentValue) {
                    if (it == SearchBarValue.Collapsed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (passcodeEnabled) {
                                IconButton(onClick = onLockClick) {
                                    Icon(
                                        imageVector = Icons.Rounded.LockOpen,
                                        contentDescription = "Lock"
                                    )
                                }
                            }
                            accountSwitcherState.currentUser?.let { currentUser ->
                                IconButton(onClick = onProfileClick) {
                                    ChatAvatar(
                                        id = currentUser.id,
                                        chatName = currentUser.firstName,
                                        avatarUri = currentUser.avatars.firstOrNull()?.uri,
                                        size = 30.dp,
                                        sharedTransition = false
                                    )
                                }
                            }
                        }
                    } else if (textFieldState.text.isNotEmpty()) {
                        IconButton(onClick = {
                            textFieldState.edit {
                                replace(0, length, "")
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close search"
                            )
                        }
                    }
                }
            })
    }
    
    AppBarWithSearch(
        state = searchBarState,
        inputField = inputField,
        colors = SearchBarDefaults.appBarWithSearchColors(appBarContainerColor = Color.Transparent)
    )
    
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        colors = SearchBarDefaults.colors(dividerColor = Color.Transparent)
    ) {
        if (searchUiState.isChatLoading && searchUiState.chatResults.isEmpty()) {
            LoadingPlaceholder()
        } else if (searchUiState.chatResults.isEmpty() && searchUiState.query.isNotBlank()) {
            EmptySearchResultsPlaceholder()
        } else {
            ChatResultsList(
                results = searchUiState.chatResults,
                query = searchUiState.query,
                isLoading = searchUiState.isChatLoading,
                onLoadMore = searchViewModel::loadMore,
                onChatClick = { chatId, chatName ->
                    scope.launch {
                        navBackStack.add(AppRoute.Chat(chatId, chatName))
                    }
                })
        }
    }
}

@Composable
private fun AccountSwitcherDialog(
    currentUser: User?,
    otherAccounts: List<User>,
    onAccountClick: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AppDialog(
        onDismissRequest = onDismissRequest,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                currentUser?.let { user ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ChatAvatar(
                            id = user.id,
                            chatName = user.firstName,
                            avatarUri = user.avatars.firstOrNull()?.uri,
                            size = 64.dp,
                            sharedTransition = false
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                otherAccounts.forEach { account ->
                    AccountRow(
                        user = account,
                        onClick = { onAccountClick(account.id) }
                    )
                }
                AddAccountRow(onClick = onAddAccount)
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        })
}

@Composable
private fun AccountRow(
    user: User, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChatAvatar(
            id = user.id,
            chatName = user.firstName,
            avatarUri = user.avatars.firstOrNull()?.uri,
            size = 40.dp,
            sharedTransition = false
        )
        Text(
            text = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AddAccountRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = stringResource(R.string.add_account),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DrawerContent(
    drawerState: DrawerState, user: User, theme: ThemeOption
) {
    val navBackStack = LocalNavBackStack.current
    val scope = rememberCoroutineScope()
    val screenHeight = LocalWindowInfo.current.containerDpSize.height
    
    val verticalPadding = if (screenHeight < 400.dp) {
        20.dp
    } else {
        80.dp
    }
    
    val maxAdHeight = if (screenHeight < 400.dp) {
        100.dp
    } else {
        300.dp
    }
    
    ModalDrawerSheet(
        drawerState = drawerState,
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        windowInsets = WindowInsets()
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Text(
                text = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = verticalPadding),
                fontSize = 24.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            DrawerItem(
                label = stringResource(R.string.profile), icon = Icons.Outlined.AccountCircle
            ) {
                scope.launch {
                    drawerState.close()
                }
                navBackStack.add(
                    AppRoute.Profile(
                        profileId = user.id,
                        profileName = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                        avatarUri = user.avatars.firstOrNull()?.uri.toString()
                    )
                )
            }
            
            val savedMessagesText = stringResource(R.string.saved_messages)
            
            DrawerItem(
                label = savedMessagesText, icon = Icons.Rounded.BookmarkBorder
            ) {
                scope.launch {
                    drawerState.close()
                }
                navBackStack.add(
                    AppRoute.Chat(
                        chatId = user.id,
                        chatName = savedMessagesText,
                        avatarUri = user.avatars.firstOrNull()?.uri?.toString()
                    )
                )
            }
            
            DrawerItem(
                label = stringResource(R.string.settings), icon = Icons.Outlined.Settings
            ) {
                scope.launch {
                    drawerState.close()
                }
                navBackStack.add(AppRoute.Settings)
            }
        }
        
        Box(Modifier.weight(1f))
        
        val scope = rememberCoroutineScope()
        var loadTrigger by remember { mutableLongStateOf(0L) }
        val adTheme =
            if (theme == ThemeOption.DARK || theme == ThemeOption.SYSTEM && isSystemInDarkTheme()) {
                AdTheme.DARK
            } else {
                AdTheme.LIGHT
            }
        val adRequest =
            AdRequest.Builder(BuildConfig.AD_BANNER_ID).setPreferredTheme(adTheme).build()
        
        val bannerState = rememberBannerAdState(
            adSize = BannerSize.Inline(width = 300.dp, maxHeight = maxAdHeight),
            events = BannerEvents(onAdFailedToLoad = { error ->
                Log.e("YandexAds", error.description)
                scope.launch {
                    delay(4.seconds)
                    loadTrigger++
                }
            }, onImpression = { data ->
                Log.d("YandexAds", "Показ: ${data?.rawData}")
                scope.launch {
                    delay(60.seconds)
                    loadTrigger++
                }
            })
        )
        
        LaunchedEffect(loadTrigger) {
            bannerState.loadAd(adRequest)
        }
        
        Banner(
            state = bannerState, modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun DrawerItem(
    label: String, icon: ImageVector, onClick: () -> Unit
) {
    NavigationDrawerItem(
        shape = RectangleShape, label = {
            Text(text = label)
        }, icon = {
            Icon(
                imageVector = icon, contentDescription = null
            )
        }, selected = false, onClick = onClick
    )
}
