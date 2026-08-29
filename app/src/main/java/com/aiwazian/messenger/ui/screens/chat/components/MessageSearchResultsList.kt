/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.MessageSearchHit
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.extensions.toChatListTime
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.ui.components.ChatAvatar
import com.aiwazian.messenger.ui.screens.chat.ChatUiState
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun MessageSearchResultsList(
    uiState: ChatUiState,
    contentPadding: PaddingValues,
    onResultClick: (MessageSearchHit) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val isChannel = ChatType.fromId(uiState.chatId) == ChatType.CHANNEL
    
    LaunchedEffect(listState, uiState.hasMoreSearchResults, uiState.isSearchingMessages) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (!uiState.hasMoreSearchResults || uiState.isSearchingMessages) return@collect
                val itemCount = listState.layoutInfo.totalItemsCount
                if (itemCount > 0 && lastVisibleIndex >= itemCount - PREFETCH_THRESHOLD) {
                    onLoadMore()
                }
            }
    }
    
    if (uiState.messageSearchResults.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            if (!uiState.isSearchingMessages) {
                Text(
                    text = stringResource(
                        if (uiState.messageSearchQuery.isBlank()) R.string.chat_search_hint
                        else R.string.chat_search_no_results
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CircularWavyProgressIndicator()
            }
        }
        return
    }
    
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        overscrollEffect = rememberOverscrollEffect()
    ) {
        items(
            items = uiState.messageSearchResults,
            key = { hit -> "search_result_${hit.id}" }) { hit ->
            val sender = uiState.messageSearchSenders[hit.senderId]
            
            MessageSearchResultCard(
                hit = hit,
                senderName = if (isChannel) uiState.chatName.asString()
                else sender?.name.orEmpty(),
                avatarUri = if (isChannel) uiState.avatarUri else sender?.avatarUri,
                onClick = { onResultClick(hit) })
        }
        
        if (uiState.isSearchingMessages) {
            item(key = "search_results_loader") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun MessageSearchResultCard(
    hit: MessageSearchHit,
    senderName: String,
    avatarUri: Uri?,
    onClick: () -> Unit
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
        content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = senderName,
                    maxLines = 1,
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = hit.sendTime.toInstance().toChatListTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        supportingContent = {
            Text(
                text = hit.text.orEmpty(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            ChatAvatar(
                id = hit.senderId, chatName = senderName, avatarUri = avatarUri, size = 50.dp
            )
        })
}

private const val PREFETCH_THRESHOLD = 10
