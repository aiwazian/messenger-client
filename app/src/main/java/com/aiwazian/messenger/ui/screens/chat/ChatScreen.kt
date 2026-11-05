package com.aiwazian.messenger.ui.screens.chat

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.PowerManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.WallpaperTarget
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toMiniPlayerTime
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.BottomBarScrim
import com.aiwazian.messenger.ui.components.SecureScreenEffect
import com.aiwazian.messenger.ui.components.TopBarScrim
import com.aiwazian.messenger.ui.components.WallpaperImage
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.screens.chat.components.AudioMiniPlayer
import com.aiwazian.messenger.ui.screens.chat.components.ChatInputSection
import com.aiwazian.messenger.ui.screens.chat.components.ChatSearchNavigationButtons
import com.aiwazian.messenger.ui.screens.chat.components.ChatSearchSummaryBar
import com.aiwazian.messenger.ui.screens.chat.components.ChatTopBar
import com.aiwazian.messenger.ui.screens.chat.components.InviteLinkBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.MessageSearchResultsList
import com.aiwazian.messenger.ui.screens.chat.components.PinnedMessageBar
import com.aiwazian.messenger.ui.screens.chat.components.ScheduleSendBottomSheet
import com.aiwazian.messenger.utils.ActiveChatTracker
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

@OptIn(FlowPreview::class)
@Composable
fun ChatScreen(
    chatId: Long,
    chatName: String? = null,
    avatarUri: String? = null,
    scrollToMessageId: Long? = null,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val notificationsViewModel: ChatNotificationsViewModel = hiltViewModel()
    
    LaunchedEffect(Unit) {
        chatViewModel.init(chatId, chatName, avatarUri?.toUri())
        
        notificationsViewModel.bind(chatId)
        
        if (scrollToMessageId != null) {
            chatViewModel.jumpToMessageWhenReady(scrollToMessageId)
        }
    }
    
    DisposableEffect(chatId) {
        ActiveChatTracker.pushChat(chatId)
        onDispose {
            ActiveChatTracker.popChat(chatId)
        }
    }
    
    val navBackStack = LocalNavBackStack.current
    val uiState by chatViewModel.uiState.collectAsState()
    val readersViewModel: MessageReadersViewModel = hiltViewModel()
    val readerAvatars by readersViewModel.avatars.collectAsState()
    val stickersViewModel: ChatStickersViewModel = hiltViewModel()
    val stickersState by stickersViewModel.uiState.collectAsState()
    val emojiViewModel: ChatEmojiViewModel = hiltViewModel()
    val emojiState by emojiViewModel.uiState.collectAsState()
    val isChatMuted by notificationsViewModel.isMuted.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val chatWallpaper by remember(chatId) {
        chatViewModel.observeChatWallpaper(chatId)
    }.collectAsState(initial = null)
    
    val wallpaperTarget = when (ChatType.fromId(chatId)) {
        ChatType.CHANNEL -> WallpaperTarget.Channel(chatId)
        ChatType.GROUP -> WallpaperTarget.Group(chatId)
        else -> WallpaperTarget.Chat(chatId)
    }
    
    val pinnedMessageIds = remember(uiState.pinnedMessages) {
        uiState.pinnedMessages.map { pin -> pin.messageId }.toSet()
    }
    
    val density = LocalDensity.current
    val topBarHeightPx = remember { mutableIntStateOf(0) }
    val imeInsets = WindowInsets.ime
    
    LaunchedEffect(imeInsets, density) {
        val imeBottomPx = snapshotFlow { imeInsets.getBottom(density) }
            .debounce(KEYBOARD_MEASURE_DELAY_MS)
            .first { it > 0 }
        
        val imeBottomDp = with(density) { imeBottomPx.toDp() }
        
        chatViewModel.onKeyboardHeightChanged(imeBottomDp.value)
    }
    
    val copyPolicy = uiState.copyPolicy
    
    SecureScreenEffect(enabled = !copyPolicy.canTakeScreenshot)
    
    val firstVisibleItemIndex = remember { derivedStateOf { listState.firstVisibleItemIndex } }
    
    val isAtBottom by remember {
        derivedStateOf {
            val firstVisible = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            firstVisible == null || firstVisible.index <= BOTTOM_ITEM_INDEX
        }
    }
    
    val isScrollingUp = remember { mutableStateOf(false) }
    val scrollAccumulator = remember { mutableFloatStateOf(0f) }
    val scrollThresholdPx = with(LocalDensity.current) { 20.dp.toPx() }
    
    LaunchedEffect(listState) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset
        
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            val delta = if (index == lastIndex) {
                (offset - lastOffset).toFloat()
            } else if (index > lastIndex) {
                scrollThresholdPx + 1f
            } else {
                -(scrollThresholdPx + 1f)
            }
            
            val newAccumulator = scrollAccumulator.floatValue + delta
            scrollAccumulator.floatValue = newAccumulator
            
            when {
                newAccumulator > scrollThresholdPx -> {
                    isScrollingUp.value = true
                    scrollAccumulator.floatValue = 0f
                }
                
                newAccumulator < -scrollThresholdPx -> {
                    isScrollingUp.value = false
                    scrollAccumulator.floatValue = 0f
                }
            }
            
            lastIndex = index
            lastOffset = offset
        }
    }
    
    val lastVisibleItemIndex = remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }
    
    LaunchedEffect(
        lastVisibleItemIndex.value,
        uiState.hasMoreMessages,
        uiState.isRelocating,
        uiState.isLoadingOlder
    ) {
        if (uiState.isRelocating || uiState.isLoading || !uiState.isFirstLoadDone) return@LaunchedEffect
        if (uiState.scrollTarget != null) return@LaunchedEffect
        val total = listState.layoutInfo.totalItemsCount
        if (total > 0 &&
            lastVisibleItemIndex.value >= total - PREFETCH_THRESHOLD &&
            uiState.hasMoreMessages &&
            !uiState.isLoadingOlder
        ) {
            chatViewModel.loadOlderMessages()
        }
    }
    
    LaunchedEffect(
        firstVisibleItemIndex.value,
        uiState.hasMoreNewerMessages,
        uiState.isRelocating,
        uiState.isLoadingNewer
    ) {
        if (uiState.isRelocating || uiState.isLoading || !uiState.isFirstLoadDone) return@LaunchedEffect
        if (uiState.scrollTarget != null) return@LaunchedEffect
        if (firstVisibleItemIndex.value < PREFETCH_THRESHOLD && uiState.hasMoreNewerMessages && !uiState.isLoadingNewer) {
            chatViewModel.loadNewerMessages()
        }
    }
    
    LaunchedEffect(uiState.scrollTarget?.requestId, uiState.chatItems) {
        val target = uiState.scrollTarget ?: return@LaunchedEffect
        val targetId = target.messageId
        
        if (targetId == null) {
            if (target.animate) listState.animateScrollToItem(BOTTOM_ITEM_INDEX)
            else listState.scrollToItem(BOTTOM_ITEM_INDEX)
            chatViewModel.onScrollTargetHandled(target.requestId)
            return@LaunchedEffect
        }
        
        val itemIndex = uiState.chatItems.indexOfFirst {
            it is ChatItem.MessageItem && it.message.id == targetId
        }
        if (itemIndex < 0) return@LaunchedEffect
        
        val anchorIndex =
            if (itemIndex + 1 <= uiState.chatItems.lastIndex &&
                uiState.chatItems[itemIndex + 1] is ChatItem.UnreadSeparator
            ) {
                itemIndex + 1
            } else itemIndex
        
        val listIndex = anchorIndex + 1
        if (target.animate) {
            listState.animateScrollToItem(listIndex)
        } else {
            val viewportHeight = listState.layoutInfo.viewportSize.height
            val desiredOffset = -(viewportHeight * (1f - target.viewportFraction)).toInt()
            listState.animateScrollToItem(listIndex, desiredOffset)
            val itemHeight = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == listIndex }?.size ?: 0
            val offset = maxOf(
                desiredOffset,
                itemHeight - viewportHeight + topBarHeightPx.intValue
            )
            if (offset != desiredOffset) {
                listState.animateScrollToItem(listIndex, offset)
            }
        }
        chatViewModel.onScrollTargetHandled(target.requestId)
    }
    
    LaunchedEffect(isAtBottom) {
        chatViewModel.onViewportAtBottomChanged(isAtBottom)
    }
    
    val displayedPinIndex = remember { mutableIntStateOf(0) }
    
    LaunchedEffect(uiState.pinnedMessages) {
        displayedPinIndex.intValue = 0
    }
    
    val visibleMessageIds by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { info ->
                (uiState.chatItems.getOrNull(info.index - 1) as? ChatItem.MessageItem)
                    ?.takeIf { it.message.id > 0 }
                    ?.message?.id
            }.toSet()
        }
    }
    
    LaunchedEffect(visibleMessageIds, uiState.pinnedMessages) {
        val pins = uiState.pinnedMessages
        if (pins.isEmpty()) return@LaunchedEffect
        
        var index = displayedPinIndex.intValue.coerceIn(0, pins.lastIndex)
        var steps = 0
        
        while (steps <= pins.size && pins[index].messageId in visibleMessageIds) {
            index = (index + 1) % pins.size
            steps++
        }
        
        displayedPinIndex.intValue = index
    }
    
    LaunchedEffect(listState, uiState.myId) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            
            layoutInfo.visibleItemsInfo.filter { info ->
                if (info.size <= 0) return@filter false
                val visiblePart =
                    (minOf(info.offset + info.size, viewportEnd) - maxOf(
                        info.offset,
                        viewportStart
                    ))
                visiblePart * 2 >= info.size
            }.mapNotNull { info ->
                val chatItem = uiState.chatItems.getOrNull(info.index - 1)
                (chatItem as? ChatItem.MessageItem)?.message
            }.filter { message ->
                message.id > 0 && !message.isRead && message.senderId != uiState.myId
            }.maxOfOrNull { it.id }
        }.distinctUntilChanged()
            .collect { messageId ->
                if (messageId != null) chatViewModel.onMessagesSeen(messageId)
            }
    }
    
    var fileToCancelId by remember { mutableStateOf<Long?>(null) }
    var showCancelRecordingDialog by remember { mutableStateOf(false) }
    var showMusicPlayerSheet by remember { mutableStateOf(false) }
    var showWallpaperPicker by remember { mutableStateOf(false) }
    var viewingContact by remember { mutableStateOf<Message?>(null) }
    
    LaunchedEffect(uiState.currentMusicFileId) {
        if (uiState.currentMusicFileId == null) showMusicPlayerSheet = false
    }
    
    var tappedMedia by remember { mutableStateOf<MessageAttachment?>(null) }
    val scope = rememberCoroutineScope()
    
    val onBackClick: () -> Unit = {
        when {
            uiState.isRecording -> showCancelRecordingDialog = true
            uiState.isMessageSearchActive -> chatViewModel.stopMessageSearch()
            else -> navBackStack.removeLastOrNull()
        }
    }
    
    BackHandler(enabled = uiState.isRecording || uiState.isMessageSearchActive) {
        if (uiState.isRecording) showCancelRecordingDialog = true
        else chatViewModel.stopMessageSearch()
    }
    
    ChatScreenEffects(
        viewModel = chatViewModel,
        notificationsViewModel = notificationsViewModel,
        stickersNotice = stickersState.notice,
        onConsumeStickersNotice = stickersViewModel::consumeNotice,
        navBackStack = navBackStack,
        listState = listState,
        snackbarHostState = snackbarHostState,
        scope = scope,
        context = context
    )
    
    Scaffold(snackbarHost = {
        if (!uiState.showFullScreenViewer) {
            AppSnackbar(snackbarHostState)
        }
    }, topBar = {
        Column {
            ChatTopBar(
                title = uiState.chatName.asString(),
                avatarUri = uiState.avatarUri,
                subTitle = uiState.subTitle.asString(),
                topBarActions = uiState.topBarActions,
                isConnected = uiState.isConnected,
                chatId = uiState.chatId,
                myId = uiState.myId,
                isMuted = isChatMuted,
                isSearchActive = uiState.isMessageSearchActive,
                searchQuery = uiState.messageSearchQuery,
                onSearchQueryChange = chatViewModel::changeMessageSearchQuery,
                onClearSearchQuery = chatViewModel::clearMessageSearchQuery,
                onStartSearch = chatViewModel::startMessageSearch,
                onToggleNotifications = notificationsViewModel::toggle,
                onBackClick = onBackClick
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal)
                            .asPaddingValues()
                            .plus(PaddingValues(horizontal = 4.dp))
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                AnimatedContent(
                    targetState = uiState.pinnedMessages,
                    modifier = Modifier.fillMaxWidth(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    contentAlignment = Alignment.TopCenter
                ) { pins ->
                    if (pins.isNotEmpty() && !uiState.isMessageSearchActive) {
                        val displayed =
                            pins.getOrNull(displayedPinIndex.intValue) ?: pins.first()
                        
                        displayed.message?.let { message ->
                            PinnedMessageBar(
                                title = stringResource(R.string.pinned_message),
                                message = message,
                                onClick = { chatViewModel.jumpToMessage(displayed.messageId) },
                                onListClick = {
                                    navBackStack.add(AppRoute.PinnedMessages(uiState.chatId))
                                }
                            )
                        }
                    }
                }
                AnimatedContent(
                    targetState = uiState.currentMusicFileId,
                    modifier = Modifier.fillMaxWidth(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    contentAlignment = Alignment.TopCenter
                ) { currentMusicFileId ->
                    if (currentMusicFileId != null) {
                        AudioMiniPlayer(
                            title = uiState.currentMusicTitle,
                            artist = uiState.currentMusicArtist,
                            isPlaying = uiState.isMusicPlaying,
                            onTogglePlayPause = chatViewModel::toggleMusicPlayPause,
                            onClose = chatViewModel::stopMusic,
                            onOpen = { showMusicPlayerSheet = true }
                        )
                    }
                }
                AnimatedContent(
                    targetState = uiState.currentVoiceMessageId,
                    modifier = Modifier.fillMaxWidth(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    contentAlignment = Alignment.TopCenter
                ) { currentVoiceMessageId ->
                    if (currentVoiceMessageId != null) {
                        AudioMiniPlayer(
                            title = uiState.currentVoiceSenderName.orEmpty(),
                            artist = uiState.currentVoiceSendTime?.toInstance()
                                ?.toMiniPlayerTime(),
                            isPlaying = uiState.isVoicePlaying,
                            onTogglePlayPause = chatViewModel::toggleVoicePlayPause,
                            onClose = chatViewModel::stopVoice,
                            onOpen = { chatViewModel.jumpToMessage(currentVoiceMessageId) }
                        )
                    }
                }
            }
        }
    }, bottomBar = {
        val bottomBarModifier = Modifier.windowInsetsPadding(
            WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
        )
        
        AnimatedContent(targetState = uiState.isMessageSearchActive, transitionSpec = {
            fadeIn() togetherWith fadeOut()
        }) { isSearch ->
            if (isSearch) {
                ChatSearchSummaryBar(
                    uiState = uiState,
                    onToggleDisplayMode = chatViewModel::toggleMessageSearchDisplayMode,
                    modifier = bottomBarModifier
                )
            } else {
                ChatInputSection(
                    uiState = uiState,
                    actions = chatViewModel,
                    modifier = bottomBarModifier,
                    onContactSend = chatViewModel::sendContact
                )
            }
        }
    }, floatingActionButton = {
        if (uiState.isMessageSearchActive && !uiState.isMessageSearchListMode) {
            ChatSearchNavigationButtons(
                canGoOlder = uiState.canGoToOlderSearchResult,
                canGoNewer = uiState.canGoToNewerSearchResult,
                onOlderClick = chatViewModel::goToOlderSearchResult,
                onNewerClick = chatViewModel::goToNewerSearchResult
            )
        } else {
            AnimatedVisibility(
                visible = (!isAtBottom || !uiState.isAtLiveEdge) && !isScrollingUp.value && !uiState.isRecording,
                enter = scaleIn() + fadeIn() + slideInVertically { it },
                exit = scaleOut() + fadeOut() + slideOutVertically { it },
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                    targetValue = if (isPressed) 0.9f else 1f,
                    label = "scroll_bottom_button_scale_animation"
                )
                FloatingActionButton(
                    onClick = chatViewModel::jumpToLatest,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(44.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    interactionSource = interactionSource,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = null
                    )
                }
            }
        }
    }) { innerPadding ->
        SideEffect {
            topBarHeightPx.intValue =
                with(density) { innerPadding.calculateTopPadding().roundToPx() }
        }
        
        Box(modifier = Modifier.fillMaxSize()) {
            chatWallpaper?.let { wallpaper ->
                WallpaperImage(
                    model = wallpaper.url,
                    cacheKey = wallpaper.fileId,
                    darkenPercent = wallpaper.darken,
                    blurPercent = wallpaper.blur,
                    offsetX = wallpaper.offsetX,
                    offsetY = wallpaper.offsetY,
                    modifier = Modifier.matchParentSize()
                )
            }
            
            if (uiState.isMessageSearchActive && uiState.isMessageSearchListMode) {
                MessageSearchResultsList(
                    uiState = uiState,
                    contentPadding = innerPadding,
                    onResultClick = chatViewModel::onSearchResultClicked,
                    onLoadMore = chatViewModel::loadMoreSearchResults
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                            end = innerPadding.calculateEndPadding(LayoutDirection.Ltr)
                        )
                ) {
                    ChatMessagesList(
                        uiState = uiState,
                        chatViewModel = chatViewModel,
                        readersViewModel = readersViewModel,
                        readerAvatars = readerAvatars,
                        listState = listState,
                        innerPadding = innerPadding,
                        pinnedMessageIds = pinnedMessageIds,
                        copyPolicy = copyPolicy,
                        onFileCancel = { fileToCancelId = it },
                        onMediaOpen = { tappedMedia = it },
                        onContactClick = { viewingContact = it },
                        modifier = Modifier
                            .padding(
                                start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                                end = innerPadding.calculateEndPadding(LayoutDirection.Ltr)
                            )
                    )
                }
            }
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            }
            
            TopBarScrim(height = innerPadding.calculateTopPadding())
            
            BottomBarScrim(height = innerPadding.calculateBottomPadding())
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
                onDismiss = chatViewModel::dismissInviteBottomSheet,
                onJoin = chatViewModel::onSubscribeViaInviteLink
            )
        }

        if (uiState.showScheduleSendSheet) {
            ScheduleSendBottomSheet(
                onDismissRequest = chatViewModel::dismissScheduleSendSheet,
                onConfirm = chatViewModel::onSendScheduledClicked
            )
        }

        ChatScreenOverlays(
            uiState = uiState,
            chatViewModel = chatViewModel,
            stickersViewModel = stickersViewModel,
            emojiViewModel = emojiViewModel,
            stickersState = stickersState,
            emojiState = emojiState,
            chatWallpaper = chatWallpaper,
            wallpaperTarget = wallpaperTarget,
            navBackStack = navBackStack,
            viewingContact = viewingContact,
            fileToCancelId = fileToCancelId,
            showCancelRecordingDialog = showCancelRecordingDialog,
            showWallpaperPicker = showWallpaperPicker,
            showMusicPlayerSheet = showMusicPlayerSheet,
            pinnedMessageIds = pinnedMessageIds,
            onFileToCancelIdChange = { fileToCancelId = it },
            onDismissCancelRecordingDialog = { showCancelRecordingDialog = false },
            onShowWallpaperPicker = { showWallpaperPicker = true },
            onDismissMusicPlayerSheet = { showMusicPlayerSheet = false },
            onDismissContactView = { viewingContact = null }
        )
    }

    ChatMediaViewer(
        uiState = uiState,
        chatViewModel = chatViewModel,
        snackbarHostState = snackbarHostState,
        tappedMedia = tappedMedia
    )

    RaiseToListenHandler(
        isVoicePlaying = uiState.isVoicePlaying,
        raiseToListenEnabled = uiState.raiseToListen
    )
}

@Suppress("DEPRECATION")
@Composable
fun RaiseToListenHandler(
    isVoicePlaying: Boolean,
    raiseToListenEnabled: Boolean
) {
    val context = LocalContext.current
    var isNearEar by remember { mutableStateOf(false) }

    DisposableEffect(isVoicePlaying, raiseToListenEnabled) {
        if (!isVoicePlaying || !raiseToListenEnabled) {
            isNearEar = false
            return@DisposableEffect onDispose {}
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
            "Messenger:RaiseToListenWakeLock"
        )

        val originalMode = audioManager?.mode ?: AudioManager.MODE_NORMAL
        val originalSpeakerphone = audioManager?.isSpeakerphoneOn ?: true

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                    val distance = event.values[0]
                    val maxRange = proximitySensor?.maximumRange ?: 5f
                    val near = distance < maxRange && distance < 4.0f
                    if (near != isNearEar) {
                        isNearEar = near
                        if (near) {
                            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                            audioManager?.isSpeakerphoneOn = false
                            runCatching { wakeLock?.acquire(10 * 60 * 1000L) }
                        } else {
                            audioManager?.mode = AudioManager.MODE_NORMAL
                            audioManager?.isSpeakerphoneOn = true
                            runCatching { wakeLock?.release() }
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (proximitySensor != null) {
            sensorManager.registerListener(listener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
            runCatching { wakeLock?.release() }
            audioManager?.mode = originalMode
            audioManager?.isSpeakerphoneOn = originalSpeakerphone
            isNearEar = false
        }
    }

    if (isNearEar && isVoicePlaying && raiseToListenEnabled) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    }
}

private const val PREFETCH_THRESHOLD = 10

private const val BOTTOM_ITEM_INDEX = 0

private const val KEYBOARD_MEASURE_DELAY_MS = 300L
