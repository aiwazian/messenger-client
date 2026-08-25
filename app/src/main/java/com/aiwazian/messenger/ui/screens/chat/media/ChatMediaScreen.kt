/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.media

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiwazian.messenger.R
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Три столбца: такая ячейка ещё читаема и на узком экране. */
private const val MEDIA_COLUMNS = 3

/** За сколько ячеек до конца просить следующую страницу. */
private const val PREFETCH_DISTANCE = 12

private enum class ChatMediaTab(@param:StringRes val titleRes: Int) {
    MEDIA(R.string.chat_media_tab),
    FILES(R.string.chat_files_tab)
}

/**
 * Галерея чата: фото с видео на одной вкладке, документы на другой.
 *
 * Вкладка появляется только под своё содержимое: в чате без документов
 * кнопки «Файлы» нет вовсе, а не есть с надписью «здесь ничего нет».
 * Пустой чат показывает одну надпись вместо двух пустых вкладок.
 *
 * Полный экран не свой, а тот же [FullScreenViewer], что и в переписке: зум,
 * листание, настройки видео и возврат в квадрат при свайпе вниз так
 * достаются целиком и без второго комплекта жестов.
 */
@Composable
fun ChatMediaScreen(
    chatId: Long,
    chatName: String? = null,
    viewModel: ChatMediaViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    /*
     * Пересчитывается не на каждой догруженной странице, а только когда
     * список из пустого стал непустым: от состава вкладок зависит число
     * страниц листалки.
     */
    val tabs = remember(uiState.media.isEmpty(), uiState.files.isEmpty()) {
        buildList {
            if (uiState.media.isNotEmpty()) {
                add(ChatMediaTab.MEDIA)
            }
            
            if (uiState.files.isNotEmpty()) {
                add(ChatMediaTab.FILES)
            }
        }
    }
    
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    
    LaunchedEffect(chatId) { viewModel.init(chatId) }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(text = chatName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.chat_media))
                },
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = { navBackStack.removeLastOrNull() })
            )
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            /*
             * Пока хотя бы один из двух запросов идёт, состав вкладок ещё
             * неизвестен: показанное сразу «нет вложений» сменилось бы
             * вкладками через мгновение.
             */
            if (uiState.isMediaLoading || uiState.isFilesLoading) {
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
                    
                    null -> Unit
                }
            }
        }
    }
    
    if (uiState.showFullScreenViewer) {
        /* Смотреть можно только скачанное, поэтому список страниц уже просеян. */
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
private fun MediaTab(
    items: List<ChatMediaItem>,
    onItemClick: (ChatMediaItem) -> Unit,
    onVisibleItems: (List<ChatMediaItem>) -> Unit,
    onLoadMore: () -> Unit
) {
    val gridState = rememberLazyGridState()
    
    /*
     * Загружается то, что реально видно, а не вся пришедшая страница: одним
     * броском можно пролететь пол-галереи, и качать промелькнувшее незачем.
     */
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
