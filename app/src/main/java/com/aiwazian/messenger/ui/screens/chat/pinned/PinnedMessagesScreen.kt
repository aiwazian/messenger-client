/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.pinned

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.ui.app.AppPrimaryScrollableTabRow
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.app.AppTab
import com.aiwazian.messenger.ui.components.BottomBarScrim
import com.aiwazian.messenger.ui.components.ShareBottomSheet
import com.aiwazian.messenger.ui.components.ShareForwardOptions
import com.aiwazian.messenger.ui.components.ShareItem
import com.aiwazian.messenger.ui.components.TopBarScrim
import com.aiwazian.messenger.ui.components.chatMediaKey
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.chat.ChatItem
import com.aiwazian.messenger.ui.screens.chat.MessageReadersViewModel
import com.aiwazian.messenger.ui.screens.chat.components.DateSeparatorItem
import com.aiwazian.messenger.ui.screens.chat.components.DeleteMessageDialog
import com.aiwazian.messenger.ui.screens.chat.components.FullScreenViewer
import com.aiwazian.messenger.ui.screens.chat.components.MessageBubble
import com.aiwazian.messenger.ui.screens.chat.components.PinMessageBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.SystemMessageBubble
import com.aiwazian.messenger.ui.screens.chat.components.UnreadSeparatorItem
import com.aiwazian.messenger.ui.screens.chat.components.ViewerMediaItem
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PinnedMessagesScreen(
    chatId: Long,
    viewModel: PinnedMessagesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    val resultBus = LocalResultEventBus.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val readersViewModel: MessageReadersViewModel = hiltViewModel()
    val readerAvatars by readersViewModel.avatars.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var tappedMedia by remember { mutableStateOf<MessageAttachment?>(null) }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(chatId) {
        viewModel.init(chatId)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is PinnedMessagesUiEffect.NavigateBack -> navBackStack.removeLastOrNull()

                is PinnedMessagesUiEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(
                            message = effect.message.asString(context),
                            duration = SnackbarDuration.Short
                        )
                    }
                }

                is PinnedMessagesUiEffect.OpenUrl -> {
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .setTranslateLocale(Locale.getDefault())
                        .build()
                        .launchUrl(context, effect.url.toUri())
                }

                is PinnedMessagesUiEffect.OpenEmail -> {
                    val mailIntent = Intent(
                        Intent.ACTION_SENDTO,
                        "mailto:${Uri.encode(effect.email)}".toUri()
                    )
                    val chooser = Intent.createChooser(
                        mailIntent,
                        UiText.StringResource(R.string.open_email_with).asString(context)
                    )

                    val launched = runCatching { context.startActivity(chooser) }.isSuccess
                    if (!launched) {
                        snackbarJob?.cancel()
                        snackbarJob = scope.launch {
                            snackbarHostState.showSnackbar(
                                message = UiText.StringResource(R.string.no_email_app)
                                    .asString(context),
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }

                is PinnedMessagesUiEffect.NavigateToChat -> {
                    navBackStack.add(
                        AppRoute.Chat(
                            chatId = effect.chatId,
                            scrollToMessageId = effect.scrollToMessageId
                        )
                    )
                }

                is PinnedMessagesUiEffect.OpenInChat -> {
                    resultBus.sendResult<PinnedMessageResult>(
                        result = PinnedMessageResult(effect.message, effect.action)
                    )
                    navBackStack.removeLastOrNull()
                }
            }
        }
    }

    val tabs = remember(
        uiState.forEveryoneItems.isEmpty(),
        uiState.forMeItems.isEmpty()
    ) {
        buildList {
            if (uiState.forEveryoneItems.isNotEmpty()) add(PinnedTab.FOR_EVERYONE)
            if (uiState.forMeItems.isNotEmpty()) add(PinnedTab.FOR_ME)
        }
    }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val forEveryoneListState = rememberLazyListState()
    val forMeListState = rememberLazyListState()
    val forEveryoneScrollState = rememberChatListScrollState(forEveryoneListState)
    val forMeScrollState = rememberChatListScrollState(forMeListState)
    val selectedTabIndex = pagerState.currentPage.coerceIn(0, tabs.lastIndex.coerceAtLeast(0))
    val activeTab = tabs.getOrNull(selectedTabIndex)
    val activeListState = when (activeTab) {
        PinnedTab.FOR_EVERYONE -> forEveryoneListState
        PinnedTab.FOR_ME -> forMeListState
        null -> forEveryoneListState
    }
    val activeScrollState = when (activeTab) {
        PinnedTab.FOR_EVERYONE -> forEveryoneScrollState
        PinnedTab.FOR_ME -> forMeScrollState
        null -> forEveryoneScrollState
    }

    LaunchedEffect(tabs) {
        if (pagerState.currentPage > tabs.lastIndex) {
            pagerState.scrollToPage(tabs.lastIndex.coerceAtLeast(0))
        }
    }

    Scaffold(snackbarHost = {
        AppSnackbar(snackbarHostState)
    }, topBar = {
        Column {
            PageTopBar(
                title = {
                    Text(
                        text = pluralStringResource(
                            R.plurals.pinned_messages_count,
                            uiState.pinnedCount,
                            uiState.pinnedCount
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )

            if (tabs.size > 1) {
                AppPrimaryScrollableTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, tab ->
                        AppTab(
                            selected = selectedTabIndex == index,
                            text = stringResource(
                                if (tab == PinnedTab.FOR_EVERYONE) {
                                    R.string.pinned_tab_for_everyone
                                } else {
                                    R.string.pinned_tab_for_me
                                }
                            ),
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                        )
                    }
                }
            }
        }
    }, floatingActionButton = {
        PinnedScrollToBottomButton(
            visible = !activeScrollState.isAtBottom.value && !activeScrollState.isScrollingUp.value,
            onClick = {
                scope.launch { activeListState.animateScrollToItem(0) }
            }
        )
    }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (tabs.isEmpty()) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularWavyProgressIndicator()
                    }
                }
            } else {
                val pageContentPadding = PaddingValues(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (tabs.getOrNull(page)) {
                        PinnedTab.FOR_EVERYONE -> PinnedMessagesPage(
                            items = uiState.forEveryoneItems,
                            isLoading = uiState.isLoading,
                            uiState = uiState,
                            listState = forEveryoneListState,
                            readerAvatars = readerAvatars,
                            onReadersRequested = readersViewModel::onReadersRequested,
                            onTappedMediaChanged = { tappedMedia = it },
                            viewModel = viewModel,
                            contentPadding = pageContentPadding
                        )

                        PinnedTab.FOR_ME -> PinnedMessagesPage(
                            items = uiState.forMeItems,
                            isLoading = uiState.isLoading,
                            uiState = uiState,
                            listState = forMeListState,
                            readerAvatars = readerAvatars,
                            onReadersRequested = readersViewModel::onReadersRequested,
                            onTappedMediaChanged = { tappedMedia = it },
                            viewModel = viewModel,
                            contentPadding = pageContentPadding
                        )

                        null -> Unit
                    }
                }
            }

            TopBarScrim(height = innerPadding.calculateTopPadding())

            BottomBarScrim(height = innerPadding.calculateBottomPadding())
        }
    }

    if (uiState.pinSheetMessage != null) {
        PinMessageBottomSheet(
            chatType = uiState.chatType,
            forEveryone = uiState.pinForEveryone,
            isPinned = true,
            onSelectScope = viewModel::selectPinScope,
            onConfirm = viewModel::confirmPin,
            onUnpin = viewModel::confirmUnpin,
            onDismiss = viewModel::dismissPinSheet
        )
    }

    if (uiState.deleteMessage != null) {
        DeleteMessageDialog(
            onDismissRequest = viewModel::hideDeleteMessageDialog,
            onConfirm = viewModel::confirmDeleteMessage,
            deleteForRecipient = uiState.deleteForRecipient,
            onDeleteForRecipientChanged = viewModel::setDeleteForRecipient,
            isPrivateChat = uiState.chatType == ChatType.PRIVATE
        )
    }

    if (uiState.isForwardSheetVisible) {
        val forwardingMessage = uiState.forwardingMessage
        ShareBottomSheet(
            items = uiState.forwardCandidates.map { chat ->
                ShareItem(
                    id = chat.id,
                    name = chat.chatName,
                    isSelected = chat.id in uiState.selectedForwardChatIds,
                    avatarUri = chat.avatarUri,
                    isSavedMessages = chat.id == uiState.myId
                )
            },
            onItemClick = viewModel::toggleForwardTarget,
            onSendClick = viewModel::confirmForward,
            onDismiss = viewModel::dismissForwardSheet,
            forwardOptions = if (forwardingMessage != null) {
                ShareForwardOptions(
                    hasAttachments = forwardingMessage.attachments.isNotEmpty(),
                    hideAuthor = uiState.forwardHideAuthor,
                    hideCaption = uiState.forwardHideCaption,
                    onHideAuthorClick = viewModel::toggleForwardHideAuthor,
                    onHideCaptionClick = viewModel::toggleForwardHideCaption
                )
            } else {
                null
            }
        )
    }

    if (uiState.showFullScreenViewer) {
        val downloadedMedia = remember(uiState.forEveryoneItems, uiState.forMeItems) {
            (uiState.forEveryoneItems + uiState.forMeItems)
                .filterIsInstance<ChatItem.MessageItem>()
                .flatMap { it.message.attachments }
                .filter { attachment ->
                    attachment.localUri != null && (
                            attachment.type == AttachmentType.IMAGE ||
                                    attachment.type == AttachmentType.VIDEO ||
                                    attachment.type == AttachmentType.GIF
                            )
                }
                .distinctBy { it.messageId to it.fileId }
        }

        val tapped = tappedMedia
        val viewerAttachments = when {
            tapped == null -> downloadedMedia
            downloadedMedia.any {
                it.messageId == tapped.messageId && it.fileId == tapped.fileId
            } -> downloadedMedia

            tapped.localUri != null -> listOf(tapped)
            else -> downloadedMedia
        }

        val viewerEntries = viewerAttachments.mapNotNull { attachment ->
            val uri = attachment.localUri ?: return@mapNotNull null
            attachment to ViewerMediaItem(
                uri = uri,
                isVideo = attachment.type == AttachmentType.VIDEO,
                originKey = chatMediaKey(attachment.messageId, uri)
            )
        }
        val viewerMedia = viewerEntries.map { it.second }
        val viewerInitialPage = viewerEntries
            .indexOfFirst { (attachment, _) ->
                tapped != null &&
                        attachment.messageId == tapped.messageId &&
                        attachment.fileId == tapped.fileId
            }
            .coerceAtLeast(0)

        FullScreenViewer(
            media = viewerMedia,
            initialPage = viewerInitialPage,
            isVideoLooping = uiState.isVideoLooping,
            videoPlaybackSpeed = uiState.videoPlaybackSpeed,
            canDownloadMedia = uiState.copyPolicy.canSaveMedia,
            onVideoLoopingChange = viewModel::onVideoLoopingChange,
            onVideoPlaybackSpeedChange = viewModel::onVideoPlaybackSpeedChange,
            onSaveToGallery = viewModel::onSaveToGallery,
            onDismiss = viewModel::onViewerDismiss
        )
    }
}

@Composable
private fun PinnedMessagesPage(
    items: List<ChatItem>,
    isLoading: Boolean,
    uiState: PinnedMessagesUiState,
    listState: LazyListState,
    readerAvatars: Map<Long, Uri?>,
    onReadersRequested: (List<Long>) -> Unit,
    onTappedMediaChanged: (MessageAttachment?) -> Unit,
    viewModel: PinnedMessagesViewModel,
    contentPadding: PaddingValues
) {
    val navBackStack = LocalNavBackStack.current

    if (isLoading && items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularWavyProgressIndicator()
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        overscrollEffect = rememberOverscrollEffect(),
        contentPadding = contentPadding
    ) {
        itemsIndexed(items = items, key = { index, item ->
            when (item) {
                is ChatItem.DateSeparator -> "date_${item.text}_$index"
                is ChatItem.UnreadSeparator -> "unread_separator"
                is ChatItem.SystemMessage -> "sys_${item.sendTime}"
                is ChatItem.MessageItem -> "msg_${item.message.id}"
            }
        }) { _, item ->
            when (item) {
                is ChatItem.DateSeparator -> DateSeparatorItem(
                    item.text, Modifier.animateItem()
                )

                is ChatItem.UnreadSeparator -> UnreadSeparatorItem(
                    Modifier.animateItem()
                )

                is ChatItem.SystemMessage -> SystemMessageBubble(
                    item.text.asString(), Modifier.animateItem()
                )

                is ChatItem.MessageItem -> MessageBubble(
                    modifier = Modifier.animateItem(),
                    item = item,
                    onFileAction = { file, action ->
                        if (action == FileAction.OPEN) {
                            onTappedMediaChanged(file)
                        }
                        viewModel.onFileAction(item.message, file, action)
                    },
                    currentPlayingVoiceFileId = uiState.currentPlayingVoiceFileId,
                    isVoicePlaying = uiState.isVoicePlaying,
                    voicePositionMs = uiState.voicePositionMs,
                    voiceDurationMs = uiState.voiceDurationMs,
                    onVoiceSeek = viewModel::onVoiceSeek,
                    audioMetadata = uiState.audioMetadata,
                    currentMusicFileId = uiState.currentMusicFileId,
                    isMusicPlaying = uiState.isMusicPlaying,
                    musicPositionMs = uiState.musicPositionMs,
                    musicDurationMs = uiState.musicDurationMs,
                    onMusicSeek = viewModel::onMusicSeek,
                    onLinkClicked = viewModel::onLinkClicked,
                    onUsernameClicked = viewModel::onUsernameClicked,
                    onEmailClicked = viewModel::onEmailClicked,
                    onSaveToDownloads = if (uiState.copyPolicy.canSaveMedia) {
                        {
                            viewModel.saveAttachmentsToDownloads(item.message)
                        }
                    } else null,
                    onReplyPreviewClick = {
                        viewModel.onReplyPreviewClicked(item.message)
                    },
                    onForwardedFromClick = {
                        viewModel.onForwardedFromClicked(item.message)
                    },
                    readerAvatars = readerAvatars,
                    onReadersRequested = onReadersRequested,
                    onSenderNameClick = if (item.chatType == ChatType.GROUP) {
                        {
                            navBackStack.add(
                                AppRoute.Profile(
                                    profileId = item.message.senderId,
                                    profileName = item.senderName
                                )
                            )
                        }
                    } else null,
                    onReaderClick = { reader ->
                        val readerName = listOf(
                            reader.firstName,
                            reader.lastName.orEmpty()
                        ).filter { it.isNotBlank() }.joinToString(" ")
                        navBackStack.add(
                            AppRoute.Profile(
                                profileId = reader.userId,
                                profileName = readerName.ifBlank { null },
                                avatarUri = readerAvatars[reader.userId]?.toString()
                            )
                        )
                    },
                    isPinned = true
                )
            }
        }
    }
}

@Composable
private fun PinnedScrollToBottomButton(
    visible: Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn() + slideInVertically { it },
        exit = scaleOut() + fadeOut() + slideOutVertically { it }
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            targetValue = if (isPressed) 0.9f else 1f,
            label = "pinned_scroll_bottom_button_scale_animation"
        )
        FloatingActionButton(
            onClick = onClick,
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

private enum class PinnedTab {
    FOR_EVERYONE,
    FOR_ME
}

private class ChatListScrollState(
    val isAtBottom: State<Boolean>,
    val isScrollingUp: State<Boolean>
)

@Composable
private fun rememberChatListScrollState(listState: LazyListState): ChatListScrollState {
    val isAtBottom = remember {
        derivedStateOf {
            val firstVisible = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            firstVisible == null || firstVisible.index <= 0
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

    return ChatListScrollState(isAtBottom, isScrollingUp)
}
