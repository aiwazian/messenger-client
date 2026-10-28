/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.media

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.AudioTrackMetadata
import com.aiwazian.messenger.domain.ChatContactItem
import com.aiwazian.messenger.domain.ChatMediaCounts
import com.aiwazian.messenger.domain.ChatMediaItem
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.ui.animations.expressiveScaleIn
import com.aiwazian.messenger.ui.animations.expressiveScaleOut
import com.aiwazian.messenger.ui.app.AppPrimaryScrollableTabRow
import com.aiwazian.messenger.ui.app.AppTab
import com.aiwazian.messenger.ui.components.BottomBarScrim
import com.aiwazian.messenger.ui.components.ShareBottomSheet
import com.aiwazian.messenger.ui.components.ShareForwardOptions
import com.aiwazian.messenger.ui.components.ShareItem
import com.aiwazian.messenger.ui.components.TopBarScrim
import com.aiwazian.messenger.ui.components.chatMediaKey
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.chat.components.ContactViewBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.FullScreenViewer
import com.aiwazian.messenger.ui.screens.chat.components.ViewerMediaItem
import com.aiwazian.messenger.ui.screens.chat.media.components.ChatContactCard
import com.aiwazian.messenger.ui.screens.chat.media.components.ChatFileCard
import com.aiwazian.messenger.ui.screens.chat.media.components.ChatMediaCell
import com.aiwazian.messenger.ui.screens.chat.media.components.ChatMusicCard
import com.aiwazian.messenger.ui.screens.chat.media.components.ChatVoiceCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val MEDIA_COLUMNS = 3

private const val PREFETCH_DISTANCE = 12

private enum class ChatMediaTab(@param:StringRes val titleRes: Int) {
    MEDIA(R.string.chat_media_tab),
    FILES(R.string.chat_files_tab),
    MUSIC(R.string.chat_media_music_tab),
    VOICES(R.string.chat_voices_tab),
    CONTACTS(R.string.chat_contacts_tab)
}

@Composable
fun ChatMediaScreen(
    chatId: Long,
    chatName: String? = null,
    viewModel: ChatMediaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val navBackStack = LocalNavBackStack.current
    val resultBus = LocalResultEventBus.current
    
    val tabs = remember(
        uiState.media.isEmpty(),
        uiState.files.isEmpty(),
        uiState.music.isEmpty(),
        uiState.voices.isEmpty(),
        uiState.contacts.isEmpty()
    ) {
        buildList {
            if (uiState.media.isNotEmpty()) {
                add(ChatMediaTab.MEDIA)
            }
            
            if (uiState.files.isNotEmpty()) {
                add(ChatMediaTab.FILES)
            }
            
            if (uiState.music.isNotEmpty()) {
                add(ChatMediaTab.MUSIC)
            }
            
            if (uiState.voices.isNotEmpty()) {
                add(ChatMediaTab.VOICES)
            }
            
            if (uiState.contacts.isNotEmpty()) {
                add(ChatMediaTab.CONTACTS)
            }
        }
    }
    
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    
    LaunchedEffect(chatId) { viewModel.init(chatId) }
    
    val resolvedChatName = chatName?.takeIf { it.isNotBlank() }
    val counts = uiState.counts
    
    val selectionCount = uiState.selectedAttachmentIds.size + uiState.selectedContactMessageIds.size
    val hasSelection = selectionCount > 0
    val selectedMessageId: Long? = when {
        uiState.selectedContactMessageIds.size == 1 -> uiState.selectedContactMessageIds.first()
        
        uiState.selectedAttachmentIds.size == 1 -> (uiState.media + uiState.files + uiState.music + uiState.voices)
            .firstOrNull { it.id == uiState.selectedAttachmentIds.first() }?.messageId
        
        else -> null
    }
    
    BackHandler(enabled = hasSelection) {
        viewModel.clearSelection()
    }
    
    Scaffold(
        topBar = {
            Column {
                PageTopBar(
                    title = {
                        Column {
                            Text(text = resolvedChatName ?: stringResource(R.string.chat_media))
                            
                            AnimatedContent(
                                targetState = pagerState.currentPage,
                                transitionSpec = {
                                    val direction = if (targetState > initialState) 1 else -1
                                    
                                    slideInHorizontally { width -> direction * width } + fadeIn() togetherWith
                                            slideOutHorizontally { width -> -direction * width } + fadeOut()
                                }) { page ->
                                tabCountsText(tabs.getOrNull(page), counts)?.let { text ->
                                    Text(
                                        text = text,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                
                AnimatedContent(
                    targetState = hasSelection,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "chat_media_selection_bar"
                ) { selecting ->
                    if (selecting) {
                        ChatMediaSelectionBar(
                            count = selectionCount,
                            canForward = uiState.canForward,
                            isEyeEnabled = selectedMessageId != null,
                            onClearSelection = viewModel::clearSelection,
                            onShowInChat = {
                                selectedMessageId?.let { messageId ->
                                    resultBus.sendResult(ShowInChatResult(messageId))
                                    navBackStack.removeLastOrNull()
                                }
                            },
                            onForwardClick = viewModel::startForwardSelection,
                            modifier = Modifier.padding(
                                TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues()
                                    .plus(PaddingValues(horizontal = 4.dp))
                                    .plus(PaddingValues(bottom = 4.dp))
                            )
                        )
                    } else if (tabs.isNotEmpty()) {
                        AppPrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
                            tabs.forEachIndexed { index, tab ->
                                AppTab(
                                    selected = pagerState.currentPage == index,
                                    text = stringResource(tab.titleRes),
                                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } })
                            }
                        }
                    }
                }
            }
        }) { innerPadding ->
        Box {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val isLoading =
                    uiState.isMediaLoading || uiState.isFilesLoading || uiState.isMusicLoading ||
                            uiState.isVoicesLoading || uiState.isContactsLoading
                
                if (isLoading && tabs.isEmpty()) {
                    LoadingState()
                    
                    return@Column
                }
                
                if (tabs.isEmpty()) {
                    EmptyState(
                        text = stringResource(
                            if (uiState.hasError) {
                                R.string.chat_media_load_error
                            } else {
                                R.string.chat_attachments_empty
                            }
                        )
                    )
                    
                    return@Column
                }
                
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (tabs.getOrNull(page)) {
                        ChatMediaTab.MEDIA -> MediaTab(
                            items = uiState.media,
                            selectedIds = uiState.selectedAttachmentIds,
                            onItemClick = viewModel::onMediaClick,
                            onToggleSelection = viewModel::toggleAttachmentSelection,
                            onVisibleItems = viewModel::onMediaVisible,
                            onLoadMore = viewModel::loadMoreMedia,
                            contentPadding = PaddingValues(
                                start = innerPadding.calculateStartPadding(LocalLayoutDirection.current) + 2.dp,
                                top = innerPadding.calculateTopPadding() + 2.dp,
                                end = innerPadding.calculateEndPadding(LocalLayoutDirection.current) + 2.dp,
                                bottom = innerPadding.calculateBottomPadding() + 2.dp
                            )
                        )
                        
                        ChatMediaTab.FILES -> FilesTab(
                            items = uiState.files,
                            selectedIds = uiState.selectedAttachmentIds,
                            onItemClick = viewModel::onFileClick,
                            onToggleSelection = viewModel::toggleAttachmentSelection,
                            onLoadMore = viewModel::loadMoreFiles,
                            contentPadding = innerPadding
                        )
                        
                        ChatMediaTab.MUSIC -> MusicTab(
                            items = uiState.music,
                            selectedIds = uiState.selectedAttachmentIds,
                            musicMetadata = uiState.musicMetadata,
                            currentFileId = uiState.musicPlayingFileId,
                            isPlaying = uiState.isMusicPlaying,
                            positionMs = uiState.musicPositionMs,
                            durationMs = uiState.musicDurationMs,
                            onItemClick = viewModel::onMusicClick,
                            onToggleSelection = viewModel::toggleAttachmentSelection,
                            onSeek = viewModel::onMusicSeek,
                            onMetadataResolved = viewModel::onMusicMetadataResolved,
                            onLoadMore = viewModel::loadMoreMusic,
                            contentPadding = innerPadding
                        )
                        
                        ChatMediaTab.VOICES -> VoicesTab(
                            items = uiState.voices,
                            selectedIds = uiState.selectedAttachmentIds,
                            chatName = resolvedChatName,
                            myId = uiState.myId,
                            playingFileId = uiState.playingFileId,
                            isPlaying = uiState.isVoicePlaying,
                            onItemClick = viewModel::onVoiceClick,
                            onToggleSelection = viewModel::toggleAttachmentSelection,
                            onVisibleItems = viewModel::onVoicesVisible,
                            onDurationResolved = viewModel::onVoiceDurationResolved,
                            onLoadMore = viewModel::loadMoreVoices,
                            contentPadding = innerPadding
                        )
                        
                        ChatMediaTab.CONTACTS -> ContactsTab(
                            items = uiState.contacts,
                            selectedMessageIds = uiState.selectedContactMessageIds,
                            onCopyValue = viewModel::onCopyContactValue,
                            onToggleSelection = viewModel::toggleContactSelection,
                            onLoadMore = viewModel::loadMoreContacts,
                            contentPadding = innerPadding
                        )
                        
                        null -> Unit
                    }
                }
            }
            
            TopBarScrim(height = innerPadding.calculateTopPadding())
            
            BottomBarScrim(height = innerPadding.calculateBottomPadding())
        }
    }
    
    if (uiState.showFullScreenViewer) {
        val viewerMedia = remember(uiState.media) {
            uiState.media.mapNotNull { item ->
                item.localUri?.let { uri ->
                    ViewerMediaItem(
                        uri = uri,
                        isVideo = item.type == AttachmentType.VIDEO,
                        messageId = item.messageId,
                        originKey = chatMediaKey(item.messageId, uri)
                    )
                }
            }
        }
        
        FullScreenViewer(
            media = viewerMedia,
            initialPage = uiState.initialMediaIndex,
            isVideoLooping = uiState.isVideoLooping,
            videoPlaybackSpeed = uiState.videoPlaybackSpeed,
            canDownloadMedia = true,
            onVideoLoopingChange = viewModel::onVideoLoopingChange,
            onVideoPlaybackSpeedChange = viewModel::onVideoPlaybackSpeedChange,
            onSaveToGallery = viewModel::onSaveToGallery,
            onShowInChat = { messageId ->
                resultBus.sendResult<ShowInChatResult>(ShowInChatResult(messageId))
                navBackStack.removeLastOrNull()
            },
            onDismiss = viewModel::onViewerDismiss
        )
    }
    
    if (uiState.isForwardSheetVisible) {
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
            text = uiState.forwardText,
            onTextChange = viewModel::changeForwardText,
            enabled = !uiState.isForwarding,
            forwardOptions = ShareForwardOptions(
                hasAttachments = uiState.selectedAttachmentIds.isNotEmpty(),
                hideAuthor = uiState.forwardHideAuthor,
                hideCaption = uiState.forwardHideCaption,
                onHideAuthorClick = viewModel::toggleForwardHideAuthor,
                onHideCaptionClick = viewModel::toggleForwardHideCaption
            )
        )
    }
}

@Composable
private fun tabCountsText(tab: ChatMediaTab?, counts: ChatMediaCounts?): String? {
    if (tab == null || counts == null) {
        return null
    }
    
    return when (tab) {
        ChatMediaTab.MEDIA -> mediaCountsText(counts)
        
        ChatMediaTab.FILES -> pluralStringResource(
            R.plurals.chat_media_files_count,
            counts.files,
            counts.files
        )
        
        ChatMediaTab.MUSIC -> pluralStringResource(
            R.plurals.chat_media_music_count,
            counts.music,
            counts.music
        )
        
        ChatMediaTab.VOICES -> pluralStringResource(
            R.plurals.chat_media_voices_count,
            counts.voices,
            counts.voices
        )
        
        ChatMediaTab.CONTACTS -> pluralStringResource(
            R.plurals.chat_media_contacts_count,
            counts.contacts,
            counts.contacts
        )
    }
}

@Composable
private fun mediaCountsText(counts: ChatMediaCounts): String? {
    val photos = if (counts.photos > 0) {
        pluralStringResource(R.plurals.chat_media_photos_count, counts.photos, counts.photos)
    } else {
        null
    }
    
    val videos = if (counts.videos > 0) {
        pluralStringResource(R.plurals.chat_media_videos_count, counts.videos, counts.videos)
    } else {
        null
    }
    
    return listOfNotNull(photos, videos)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ")
}

@Composable
private fun MediaTab(
    items: List<ChatMediaItem>,
    selectedIds: Set<Int>,
    onItemClick: (ChatMediaItem) -> Unit,
    onToggleSelection: (ChatMediaItem) -> Unit,
    onVisibleItems: (List<ChatMediaItem>) -> Unit,
    onLoadMore: () -> Unit,
    contentPadding: PaddingValues = PaddingValues.Zero
) {
    val gridState = rememberLazyGridState()
    
    LaunchedEffect(gridState, items) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.map { it.index } }
            .distinctUntilChanged()
            .collect { visible ->
                if (visible.isEmpty()) {
                    return@collect
                }
                
                onVisibleItems(visible.mapNotNull(items::getOrNull))
                
                if (visible.max() >= items.lastIndex - PREFETCH_DISTANCE) {
                    onLoadMore()
                }
            }
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(MEDIA_COLUMNS),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(items = items, key = { it.id }) { item ->
            ChatMediaCell(
                item = item,
                isSelected = item.id in selectedIds,
                onClick = {
                    if (selectedIds.isNotEmpty()) onToggleSelection(item) else onItemClick(item)
                },
                onLongClick = { onToggleSelection(item) })
        }
    }
}

@Composable
private fun FilesTab(
    items: List<ChatMediaItem>,
    selectedIds: Set<Int>,
    onItemClick: (ChatMediaItem) -> Unit,
    onToggleSelection: (ChatMediaItem) -> Unit,
    onLoadMore: () -> Unit,
    contentPadding: PaddingValues = PaddingValues.Zero
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(listState, items) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible != null && lastVisible >= items.lastIndex - PREFETCH_DISTANCE) {
                    onLoadMore()
                }
            }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        items(items = items, key = { it.id }) { item ->
            ChatFileCard(
                file = item,
                isSelected = item.id in selectedIds,
                onClick = {
                    if (selectedIds.isNotEmpty()) onToggleSelection(item) else onItemClick(item)
                },
                onLongClick = { onToggleSelection(item) })
        }
    }
}

@Composable
private fun MusicTab(
    items: List<ChatMediaItem>,
    selectedIds: Set<Int>,
    musicMetadata: Map<String, AudioTrackMetadata>,
    currentFileId: String?,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    onItemClick: (ChatMediaItem) -> Unit,
    onToggleSelection: (ChatMediaItem) -> Unit,
    onSeek: (ChatMediaItem, Int) -> Unit,
    onMetadataResolved: (ChatMediaItem, AudioTrackMetadata?) -> Unit,
    onLoadMore: () -> Unit,
    contentPadding: PaddingValues = PaddingValues.Zero
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(listState, items) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible != null && lastVisible >= items.lastIndex - PREFETCH_DISTANCE) {
                    onLoadMore()
                }
            }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        items(items = items, key = { it.id }) { item ->
            ChatMusicCard(
                music = item,
                metadata = musicMetadata[item.fileId],
                isCurrentTrack = currentFileId == item.fileId,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                isSelected = item.id in selectedIds,
                onClick = {
                    if (selectedIds.isNotEmpty()) onToggleSelection(item) else onItemClick(item)
                },
                onLongClick = { onToggleSelection(item) },
                onSeek = { onSeek(item, it) },
                onMetadataResolved = { onMetadataResolved(item, it) }
            )
        }
    }
}

@Composable
private fun VoicesTab(
    items: List<ChatMediaItem>,
    selectedIds: Set<Int>,
    chatName: String?,
    myId: Long,
    playingFileId: String?,
    isPlaying: Boolean,
    onItemClick: (ChatMediaItem) -> Unit,
    onToggleSelection: (ChatMediaItem) -> Unit,
    onVisibleItems: (List<ChatMediaItem>) -> Unit,
    onDurationResolved: (ChatMediaItem, Int) -> Unit,
    onLoadMore: () -> Unit,
    contentPadding: PaddingValues = PaddingValues.Zero
) {
    val listState = rememberLazyListState()
    val youLabel = stringResource(R.string.you)
    
    LaunchedEffect(listState, items) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index } }
            .distinctUntilChanged()
            .collect { visible ->
                if (visible.isEmpty()) {
                    return@collect
                }
                
                onVisibleItems(visible.mapNotNull(items::getOrNull))
                
                if (visible.max() >= items.lastIndex - PREFETCH_DISTANCE) {
                    onLoadMore()
                }
            }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        items(items = items, key = { it.id }) { item ->
            ChatVoiceCard(
                voice = item,
                author = if (item.senderId == myId) youLabel else chatName,
                isPlaying = isPlaying && playingFileId == item.fileId,
                isSelected = item.id in selectedIds,
                onClick = {
                    if (selectedIds.isNotEmpty()) onToggleSelection(item) else onItemClick(item)
                },
                onLongClick = { onToggleSelection(item) },
                onDurationResolved = { durationMs -> onDurationResolved(item, durationMs) })
        }
    }
}

@Composable
private fun ContactsTab(
    items: List<ChatContactItem>,
    selectedMessageIds: Set<Long>,
    onCopyValue: (String) -> Unit,
    onToggleSelection: (ChatContactItem) -> Unit,
    onLoadMore: () -> Unit,
    contentPadding: PaddingValues = PaddingValues.Zero
) {
    val listState = rememberLazyListState()
    var viewingContact by remember { mutableStateOf<ChatContactItem?>(null) }
    
    LaunchedEffect(listState, items) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible != null && lastVisible >= items.lastIndex - PREFETCH_DISTANCE) {
                    onLoadMore()
                }
            }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        items(items = items, key = { it.messageId }) { contact ->
            ChatContactCard(
                contact = contact,
                isSelected = contact.messageId in selectedMessageIds,
                onClick = {
                    if (selectedMessageIds.isNotEmpty()) {
                        onToggleSelection(contact)
                    } else {
                        viewingContact = contact
                    }
                },
                onLongClick = { onToggleSelection(contact) })
        }
    }
    
    viewingContact?.let { item ->
        ContactViewBottomSheet(
            contact = item.contact,
            onCopyValue = onCopyValue,
            onDismiss = { viewingContact = null })
    }
}

@Composable
private fun ChatMediaSelectionBar(
    count: Int,
    canForward: Boolean,
    isEyeEnabled: Boolean,
    onClearSelection: () -> Unit,
    onShowInChat: () -> Unit,
    onForwardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedContent(
                modifier = Modifier.padding(start = 10.dp, end = 18.dp),
                targetState = count,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInVertically { it } + fadeIn() + scaleIn() togetherWith slideOutVertically { -it } + fadeOut() + scaleOut()
                    } else {
                        slideInVertically { -it } + fadeIn() + scaleIn() togetherWith slideOutVertically { it } + fadeOut() + scaleOut()
                    }
                },
                label = "chat_media_selection_count"
            ) { value ->
                Text(text = "$value")
            }
        }
        
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    expressiveScaleIn togetherWith expressiveScaleOut
                },
                contentAlignment = Alignment.Center
            ) { count ->
                if (count == 1) {
                    IconButton(onClick = onShowInChat, enabled = isEyeEnabled) {
                        Icon(
                            imageVector = Icons.Outlined.RemoveRedEye,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (canForward) {
                IconButton(onClick = onForwardClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Forward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator()
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
