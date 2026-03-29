/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.domain.DownloadStatus
import com.aiwazian.messenger.domain.Search
import com.aiwazian.messenger.enums.ChatType

@Composable
fun SearchContent(
    state: SearchUiState,
    onLoadMore: () -> Unit,
    onChatClick: (Long) -> Unit,
    onFileClick: (Search) -> Unit
) {
    when (state.activeTab) {
        0 -> {
            if (state.isChatLoading && state.chatResults.isEmpty()) {
                LoadingPlaceholder()
            } else if (state.chatResults.isEmpty() && state.query.isNotBlank()) {
                EmptySearchResultsPlaceholder()
            } else {
                ChatResultsList(
                    results = state.chatResults,
                    isLoading = state.isChatLoading,
                    onLoadMore = onLoadMore,
                    onChatClick = onChatClick
                )
            }
        }
        
        1 -> {
            if (state.isFileLoading && state.fileResults.isEmpty()) {
                LoadingPlaceholder()
            } else if (state.fileResults.isEmpty() && state.query.isNotBlank()) {
                EmptySearchResultsPlaceholder()
            } else {
                FileResultsList(
                    results = state.fileResults,
                    state = state,
                    isLoading = state.isFileLoading,
                    onLoadMore = onLoadMore,
                    onFileClick = onFileClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator()
    }
}

@Composable
fun EmptySearchResultsPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Ничего не найдено",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChatResultsList(
    results: List<Search>,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    onChatClick: (Long) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(results) { index, chat ->
            if (index >= results.size - 5) {
                LaunchedEffect(Unit) {
                    onLoadMore()
                }
            }
            
            SearchChatItem(
                chat = chat,
                onClick = { onChatClick(chat.chatId) })
        }
        
        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun FileResultsList(
    results: List<Search>,
    state: SearchUiState,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    onFileClick: (Search) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(results) { index, file ->
            if (index >= results.size - 5) {
                LaunchedEffect(Unit) {
                    onLoadMore()
                }
            }
            
            SearchFileItem(
                file = file,
                state = state,
                onClick = { onFileClick(file) }
            )
        }

        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchChatItem(chat: Search, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = chat.name,
                fontSize = 16.sp,
                lineHeight = 16.sp
            )
            Text(
                text = when (ChatType.fromId(chat.chatId)) {
                    ChatType.PRIVATE -> "Пользователь"
                    ChatType.CHANNEL -> "Канал"
                    ChatType.GROUP -> "Группа"
                    else -> ""
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun SearchFileItem(
    file: Search,
    state: SearchUiState,
    onClick: () -> Unit
) {
    val download = state.downloads.findLast { it.fileId == file.fileId }
    val progress = download?.progress ?: 0
    val status = download?.status ?: DownloadStatus.IDLE
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = file.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.senderName ?: "Unknown",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.UPLOADING) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "$progress%",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (status != DownloadStatus.COMPLETED) {
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
