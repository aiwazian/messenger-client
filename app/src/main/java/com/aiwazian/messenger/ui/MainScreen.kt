package com.aiwazian.messenger.ui

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.ChatInfo
import com.aiwazian.messenger.data.DropdownMenuAction
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.data.TopBarAction
import com.aiwazian.messenger.services.UserManager
import com.aiwazian.messenger.ui.element.ChatCard
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SwipeableChatCard
import com.aiwazian.messenger.ui.settings.SettingsScreen
import com.aiwazian.messenger.utils.LottieAnimation
import com.aiwazian.messenger.utils.Shape
import com.aiwazian.messenger.utils.WebSocketManager
import com.aiwazian.messenger.viewModels.AdViewModel
import com.aiwazian.messenger.viewModels.MainViewModel
import com.aiwazian.messenger.viewModels.NavigationViewModel
import com.yandex.mobile.ads.nativeads.MediaView
import com.yandex.mobile.ads.nativeads.NativeAdView
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    MainScreenContent()
}

@Composable
private fun MainScreenContent() {
    val context = LocalContext.current
    
    var showPermissionRationale by remember { mutableStateOf(false) }
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            showPermissionRationale = true
        }
    }
    
    if (showPermissionRationale) {
        NotificationBottomModal(
            enable = {
                showPermissionRationale = false
                
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(
                        Settings.EXTRA_APP_PACKAGE,
                        context.packageName
                    )
                }
                
                context.startActivity(intent)
            },
            disable = {
                showPermissionRationale = false
            })
    }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(POST_NOTIFICATIONS)
            }
        }
    }
    
    val scope = rememberCoroutineScope()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    BackHandler(drawerState.currentValue == DrawerValue.Open) {
        scope.launch {
            drawerState.close()
        }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(drawerState)
        },
    ) {
        Content(drawerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationBottomModal(
    enable: () -> Unit,
    disable: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = disable,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Column {
                Text(
                    text = "Включите уведомления",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )
                
                Text(
                    text = "Разрешите приложению отправлять Вам уведомления, чтобы не пропустить сообщения от друзей и родных.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Button(
            onClick = enable,
            modifier = Modifier
                .padding(15.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Открыть настройки",
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun Content(drawerState: DrawerState) {
    val navViewModel = viewModel<NavigationViewModel>()
    
    val mainViewModel = hiltViewModel<MainViewModel>()
    
    val folders by mainViewModel.chatFolders.collectAsState()
    val archiveChats by mainViewModel.archivedChats.collectAsState()
    val selectedChats by mainViewModel.selectedChats.collectAsState()
    val allSelectedChatsArePinned by mainViewModel.allSelectedArePinned.collectAsState()
    
    val hasPasscode by mainViewModel.hasPasscode.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    val scope = rememberCoroutineScope()
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    val pagerState = rememberPagerState { folders.size }
    
    LaunchedEffect(selectedTabIndex) {
        pagerState.animateScrollToPage(selectedTabIndex)
    }
    
    LaunchedEffect(pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
    }
    
    BackHandler(selectedTabIndex != 0) {
        selectedTabIndex = 0
    }
    
    BackHandler(selectedChats.isNotEmpty()) {
        mainViewModel.unselectAllChats()
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DefaultTopBar(
                drawerState = drawerState,
                isLockApp = hasPasscode,
                onLockClick = {
                    scope.launch {
                        mainViewModel.lockApp()
                    }
                })
            
            val selectedChatCount = selectedChats.values.sumOf { it.size }
            
            AnimatedVisibility(
                visible = selectedChats.isNotEmpty(),
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                SelectedChatTopBar(
                    onBack = mainViewModel::unselectAllChats,
                    selectedCount = selectedChatCount,
                    dropdownActions = listOf(
                        DropdownMenuAction(
                            icon = Icons.Outlined.PushPin,
                            text = if (allSelectedChatsArePinned) {
                                stringResource(R.string.unpin)
                            } else {
                                stringResource(R.string.pin)
                            },
                            onClick = {
                                scope.launch {
                                    if (allSelectedChatsArePinned) {
                                        selectedChats.forEach { folder ->
                                            folder.value.forEach { chatId ->
                                                mainViewModel.unpinChat(
                                                    chatId = chatId,
                                                    folderId = folder.key
                                                )
                                            }
                                        }
                                    } else {
                                        selectedChats.forEach { folder ->
                                            folder.value.forEach { chatId ->
                                                mainViewModel.pinChat(
                                                    chatId = chatId,
                                                    folderId = folder.key
                                                )
                                            }
                                        }
                                    }
                                }
                                mainViewModel.unselectAllChats()
                            })
                    )
                )
            }
        },
        snackbarHost = {
            SwipeDismissSnackbarHost(snackbarHostState)
        },
        floatingActionButton = {
            FloatingButton(onClick = {
                navViewModel.addScreenInStack {
                    NewMessageScreen()
                }
            })
        }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {
            AnimatedVisibility(
                visible = folders.size > 1,
                enter = slideInVertically(tween(100)),
                exit = slideOutVertically(tween(100))
            ) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 8.dp,
                    divider = { }) {
                    folders.forEachIndexed { index, folder ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onClick = {
                                selectedTabIndex = index
                            },
                            modifier = Modifier.clip(shape = Shape.PrimaryTab),
                            text = {
                                Text(text = folder.name)
                            },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                    }
                }
            }
            
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = folders.size > 1 && selectedChats.isEmpty(),
                overscrollEffect = null,
                modifier = Modifier.fillMaxSize()
            ) { index ->
                val chatFolders = folders.toMutableList()
                
                val folder = chatFolders[index]
                
                Column(modifier = Modifier.fillMaxSize()) {
                    if (folder.id == 0) {
                        if (folder.chats.isEmpty() && archiveChats.isEmpty()) {
                            EmptyChatPlaceholder(text = "Чтобы начать общение нажмите на значок поиска в правом верхнем углу и найдите пользователя по его @username")
                            return@Column
                        }
                        
                        if (archiveChats.isNotEmpty()) {
                            ChatCard(
                                chatInfo = ChatInfo(chatName = stringResource(R.string.archive)),
                                onClickChat = {
                                    if (selectedChats.isEmpty()) {
                                        navViewModel.addScreenInStack {
                                            ArchiveScreen(mainViewModel)
                                        }
                                    }
                                })
                        }
                    }
                    
                    if (folder.id != 0 && folder.chats.isEmpty()) {
                        EmptyChatPlaceholder(
                            text = "Добавьте чаты в папку",
                            animation = LottieAnimation.FOLDER_CLOSED
                        )
                        return@Column
                    }
                    
                    val selectedChatsInFolder = selectedChats[folder.id] ?: emptyList()
                    
                    val snackBarActionLabel = stringResource(R.string.cancel).uppercase()
                    
                    val snackBarMessage = stringResource(R.string.chat_archived)
                    
                    ChatListSection(
                        chatList = folder.chats,
                        selectedChats = selectedChatsInFolder,
                        enableSwipeable = folder.id == 0,
                        onChatClick = { chat ->
                            if (selectedChats.isEmpty()) {
                                navViewModel.addScreenInStack {
                                    ChatScreen(chat.id)
                                }
                            } else {
                                mainViewModel.selectChat(
                                    chat.id,
                                    folder.id
                                )
                            }
                        },
                        onChatLongClick = { chat ->
                            mainViewModel.selectChat(
                                chat.id,
                                folder.id
                            )
                        },
                        onChatSwipe = { chat ->
                            mainViewModel.archiveChat(chat.id)
                            
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = snackBarMessage,
                                    actionLabel = snackBarActionLabel,
                                    duration = SnackbarDuration.Short
                                )
                                
                                if (result == SnackbarResult.ActionPerformed) {
                                    mainViewModel.unarchiveChat(chat.id)
                                }
                            }
                        })
                }
            }
        }
    }
}

@Composable
private fun ChatListSection(
    chatList: List<ChatInfo>,
    selectedChats: List<Long>,
    enableSwipeable: Boolean,
    onChatClick: (ChatInfo) -> Unit,
    onChatLongClick: (ChatInfo) -> Unit,
    onChatSwipe: (ChatInfo) -> Unit
) {
    val user by UserManager.user.collectAsState()
    
    LazyColumn {
        items(
            chatList,
            { it.lastMessage?.sendTime ?: it.id }) { chat ->
            var chatInfo = chat
            
            if (chatInfo.id == user.id) {
                chatInfo = chat.copy(chatName = stringResource(R.string.saved_messages))
            }
            
            if (enableSwipeable) {
                SwipeableChatCard(
                    chatInfo = chatInfo,
                    selected = chatInfo.id in selectedChats,
                    pinned = chatInfo.isPinned,
                    enableSwipeable = selectedChats.isEmpty(),
                    onClick = { onChatClick(chatInfo) },
                    onLongClick = { onChatLongClick(chatInfo) },
                    backgroundIcon = Icons.Outlined.Archive,
                    onDismiss = { onChatSwipe(chatInfo) })
            } else {
                ChatCard(
                    chatInfo = chatInfo,
                    selected = chatInfo.id in selectedChats,
                    pinned = chatInfo.isPinned,
                    onClickChat = { onChatClick(chatInfo) },
                    onLongClickChat = { onChatLongClick(chatInfo) },
                )
            }
        }
    }
}

@Composable
private fun EmptyChatPlaceholder(
    text: String,
    animation: String? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
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
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FloatingButton(onClick: () -> Unit) {
    FloatingActionButton(
        shape = CircleShape,
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            imageVector = Icons.Default.Create,
            contentDescription = null
        )
    }
}

@Composable
private fun SwipeDismissSnackbarHost(snackbarHostState: SnackbarHostState) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.fillMaxWidth()
    ) { data ->
        var dismissed by remember { mutableStateOf(false) }
        
        if (!dismissed) {
            val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
            
            SwipeToDismissBox(
                state = swipeToDismissBoxState,
                backgroundContent = { },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Archive,
                                contentDescription = null,
                            )
                            
                            Text(
                                text = data.visuals.message
                            )
                        }
                        
                        TextButton(
                            onClick = { data.performAction() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Reply,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            data.visuals.actionLabel?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            data.dismiss()
        }
    }
}

@Composable
private fun SelectedChatTopBar(
    onBack: () -> Unit,
    selectedCount: Int,
    dropdownActions: List<DropdownMenuAction>
) {
    val actions = listOf(
        TopBarAction(
            icon = Icons.Outlined.MoreVert,
            dropdownActions = dropdownActions
        )
    )
    
    PageTopBar(
        title = {
            AnimatedContent(
                targetState = selectedCount,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInVertically(tween(200)) { height -> height } + fadeIn(
                            tween(200)
                        ) togetherWith slideOutVertically(tween(200)) { height -> -height } + fadeOut(
                            tween(200)
                        )
                    } else {
                        slideInVertically(animationSpec = tween(200)) { height -> -height } + fadeIn(
                            tween(200)
                        ) togetherWith slideOutVertically(tween(200)) { height -> height } + fadeOut(
                            tween(200)
                        )
                    }
                }) { count ->
                Text(text = count.toString())
            }
        },
        navigationIcon = NavigationIcon(
            icon = Icons.Outlined.Close,
            onClick = onBack::invoke
        ),
        actions = actions
    )
}

@Composable
private fun DefaultTopBar(
    drawerState: DrawerState,
    isLockApp: Boolean,
    onLockClick: () -> Unit
) {
    val isConnected by WebSocketManager.isConnectedState.collectAsState()
    
    val navViewModel = viewModel<NavigationViewModel>()
    
    val scope = rememberCoroutineScope()
    
    val actions = if (isLockApp) {
        listOf(
            TopBarAction(
                icon = Icons.Outlined.LockOpen,
                onClick = onLockClick
            ),
            TopBarAction(
                icon = Icons.Outlined.Search,
                onClick = {
                    navViewModel.addScreenInStack {
                        SearchScreen()
                    }
                })
        )
    } else {
        listOf(
            TopBarAction(
                icon = Icons.Outlined.Search,
                onClick = {
                    navViewModel.addScreenInStack {
                        SearchScreen()
                    }
                })
        )
    }
    
    PageTopBar(
        title = {
            AnimatedContent(
                targetState = isConnected,
                transitionSpec = {
                    if (isConnected) {
                        slideInVertically(tween(200)) { height -> height } + fadeIn(
                            tween(200)
                        ) togetherWith slideOutVertically(tween(200)) { height -> -height } + fadeOut(
                            tween(200)
                        )
                    } else {
                        slideInVertically(tween(200)) { height -> -height } + fadeIn(
                            tween(200)
                        ) togetherWith slideOutVertically(tween(200)) { height -> height } + fadeOut(
                            tween(200)
                        )
                    }
                }) { connected ->
                Text(
                    text = if (connected) {
                        stringResource(R.string.app_name)
                    } else {
                        stringResource(R.string.connecting) + "..."
                    }
                )
            }
        },
        navigationIcon = NavigationIcon(
            icon = Icons.Default.Menu,
            onClick = {
                scope.launch {
                    drawerState.open()
                }
            }),
        actions = actions
    )
}

@Composable
private fun DrawerContent(drawerState: DrawerState) {
    val context = LocalContext.current
    
    val navViewModel = viewModel<NavigationViewModel>()
    
    val user by UserManager.user.collectAsState()
    val scope = rememberCoroutineScope()
    
    ModalDrawerSheet(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.padding(
                top = 80.dp,
                bottom = 40.dp
            )
        ) {
            Text(
                text = "${user.firstName} ${user.lastName}",
                modifier = Modifier.padding(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 40.dp
                ),
                fontSize = 24.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            
            DrawerItem(
                label = stringResource(R.string.profile),
                icon = Icons.Outlined.AccountCircle
            ) {
                scope.launch {
                    drawerState.close()
                }
                navViewModel.addScreenInStack {
                    ProfileScreen(user.id)
                }
            }
            
            DrawerItem(
                label = stringResource(R.string.saved_messages),
                icon = Icons.Outlined.BookmarkBorder
            ) {
                scope.launch {
                    drawerState.close()
                }
                navViewModel.addScreenInStack {
                    ChatScreen(user.id)
                }
            }
            
            DrawerItem(
                label = stringResource(R.string.settings),
                icon = Icons.Outlined.Settings
            ) {
                scope.launch {
                    drawerState.close()
                }
                navViewModel.addScreenInStack {
                    SettingsScreen()
                }
            }
        }
        
        Box(Modifier.weight(1f))
        
        val adViewModel = hiltViewModel<AdViewModel>()
        
        LaunchedEffect(Unit) {
            adViewModel.initialize(context)
        }
        
        val isAdLoaded by adViewModel.isAdLoaded.collectAsState()
        val nativeAd = adViewModel.nativeAd
        
        if (isAdLoaded) {
            val textColor = MaterialTheme.colorScheme.onSurface
            
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    LayoutInflater.from(context).inflate(
                        R.layout.fragment_ad_mob,
                        null,
                        false
                    ).apply {
                        val title = findViewById<TextView>(R.id.title)
                        val domain = findViewById<TextView>(R.id.domain)
                        val warning = findViewById<TextView>(R.id.warning)
                        val sponsored = findViewById<TextView>(R.id.sponsored)
                        val feedback = findViewById<ImageView>(R.id.feedback)
                        val media = findViewById<MediaView>(R.id.media)
                        val favicon = findViewById<ImageView>(R.id.favicon)
                        val price = findViewById<TextView>(R.id.price)
                        val appIcon = findViewById<ImageView>(R.id.app_icon)
                        
                        title.setTextColor(textColor.toArgb())
                        domain.setTextColor(textColor.toArgb())
                        warning.setTextColor(textColor.toArgb())
                        sponsored.setTextColor(textColor.toArgb())
                        price.setTextColor(textColor.toArgb())
                        
                        val nativeAdView = findViewById<NativeAdView>(R.id.native_ad_container)
                        
                        val viewBinder = NativeAdViewBinder.Builder(nativeAdView)
                            .setTitleView(title)
                            .setDomainView(domain)
                            .setWarningView(warning)
                            .setSponsoredView(sponsored)
                            .setFeedbackView(feedback)
                            .setMediaView(media)
                            .setIconView(appIcon)
                            .setPriceView(price)
                            .setFaviconView(favicon)
                            .build()
                        
                        nativeAd?.bindNativeAd(viewBinder)
                    }
                })
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        shape = RectangleShape,
        label = {
            Text(text = label)
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        selected = false,
        onClick = onClick
    )
}
