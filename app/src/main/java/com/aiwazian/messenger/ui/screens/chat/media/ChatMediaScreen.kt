/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.media

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.ChatMediaCounts
import com.aiwazian.messenger.domain.ChatMediaItem
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.ui.app.AppPrimaryScrollableTabRow
import com.aiwazian.messenger.ui.app.AppTab
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.chat.components.FullScreenViewer
import com.aiwazian.messenger.ui.screens.chat.components.ViewerMediaItem
import com.aiwazian.messenger.ui.screens.chat.media.components.ChatFileCard
import com.aiwazian.messenger.ui.screens.chat.media.components.ChatMediaCell
import com.aiwazian.messenger.ui.screens.chat.media.components.ChatVoiceCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val MEDIA_COLUMNS = 3

private const val PREFETCH_DISTANCE = 12

private enum class ChatMediaTab(@param:StringRes val titleRes: Int) {
    MEDIA(R.string.chat_media_tab),
    FILES(R.string.chat_files_tab),
    VOICES(R.string.chat_voices_tab)
}

@Composable
fun ChatMediaScreen(
    chatId: Long,
    chatName: String? = null,
    viewModel: ChatMediaViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    val tabs = remember(
        uiState.media.isEmpty(),
        uiState.files.isEmpty(),
        uiState.voices.isEmpty()
    ) {
        buildList {
            if (uiState.media.isNotEmpty()) {
                add(ChatMediaTab.MEDIA)
            }
            
            if (uiState.files.isNotEmpty()) {
                add(ChatMediaTab.FILES)
            }
            
            if (uiState.voices.isNotEmpty()) {
                add(ChatMediaTab.VOICES)
            }
        }
    }
    
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    
    LaunchedEffect(chatId) { viewModel.init(chatId) }
    
    val resolvedChatName = chatName?.takeIf { it.isNotBlank() }
    val counts = uiState.counts
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = {
                    Column {
                        Text(text = resolvedChatName ?: stringResource(R.string.chat_media))
                        
                        AnimatedContent(
                            targetState = pagerState.currentPage,
                            transitionSpec = {
                                val direction = if (targetState > initialState) 1 else -1
                                
                                slideInHorizontally { width -> direction * width } togetherWith
                                    slideOutHorizontally { width -> -direction * width }
                            }) { page ->
                            Text(
                                text = tabCountsText(tabs.getOrNull(page), counts) ?: "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = { navBackStack.removeLastOrNull() }))
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isLoading =
                uiState.isMediaLoading || uiState.isFilesLoading || uiState.isVoicesLoading
            
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
            
            AppPrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, tab ->
                    AppTab(
                        selected = pagerState.currentPage == index,
                        text = stringResource(tab.titleRes),
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } })
                }
            }
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (tabs.getOrNull(page)) {
                    ChatMediaTab.MEDIA -> MediaTab(
                        items = uiState.media,
                        onItemClick = viewModel::onMediaClick,
                        onVisibleItems = viewModel::onMediaVisible,
                        onLoadMore = viewModel::loadMoreMedia
                    )
                    
                    ChatMediaTab.FILES -> FilesTab(
                        items = uiState.files,
                        onItemClick = viewModel::onFileClick,
                        onLoadMore = viewModel::loadMoreFiles
                    )
                    
                    ChatMediaTab.VOICES -> VoicesTab(
                        items = uiState.voices,
                        chatName = resolvedChatName,
                        myId = uiState.myId,
                        playingFileId = uiState.playingFileId,
                        isPlaying = uiState.isVoicePlaying,
                        onItemClick = viewModel::onVoiceClick,
                        onVisibleItems = viewModel::onVoicesVisible,
                        onDurationResolved = viewModel::onVoiceDurationResolved,
                        onLoadMore = viewModel::loadMoreVoices
                    )
                    
                    null -> Unit
                }
            }
        }
    }
    
    if (uiState.showFullScreenViewer) {
        val viewerMedia = remember(uiState.media) {
            uiState.media.mapNotNull { item ->
                item.localUri?.let { uri ->
                    ViewerMediaItem(uri = uri, isVideo = item.type == AttachmentType.VIDEO)
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
            onDismiss = viewModel::onViewerDismiss
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
        
        ChatMediaTab.VOICES -> pluralStringResource(
            R.plurals.chat_media_voices_count,
            counts.voices,
            counts.voices
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
    onItemClick: (ChatMediaItem) -> Unit,
    onVisibleItems: (List<ChatMediaItem>) -> Unit,
    onLoadMore: () -> Unit
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
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(items = items, key = { it.id }) { item ->
            ChatMediaCell(item = item, onClick = { onItemClick(item) })
        }
    }
}

@Composable
private fun FilesTab(
    items: List<ChatMediaItem>,
    onItemClick: (ChatMediaItem) -> Unit,
    onLoadMore: () -> Unit
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
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items = items, key = { it.id }) { item ->
            ChatFileCard(file = item, onClick = { onItemClick(item) })
        }
    }
}

@Composable
private fun VoicesTab(
    items: List<ChatMediaItem>,
    chatName: String?,
    myId: Long,
    playingFileId: String?,
    isPlaying: Boolean,
    onItemClick: (ChatMediaItem) -> Unit,
    onVisibleItems: (List<ChatMediaItem>) -> Unit,
    onDurationResolved: (ChatMediaItem, Int) -> Unit,
    onLoadMore: () -> Unit
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
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items = items, key = { it.id }) { item ->
            ChatVoiceCard(
                voice = item,
                author = if (item.senderId == myId) youLabel else chatName,
                isPlaying = isPlaying && playingFileId == item.fileId,
                onClick = { onItemClick(item) },
                onDurationResolved = { durationMs -> onDurationResolved(item, durationMs) })
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
