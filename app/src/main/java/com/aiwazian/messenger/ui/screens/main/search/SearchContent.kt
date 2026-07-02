/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Search
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.components.ProfileCard

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
    onChatClick: (Long, String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(results) { index, chat ->
            if (index >= results.size - 5) {
                LaunchedEffect(Unit) {
                    onLoadMore()
                }
            }
            
            val supportText = if (chat.username != null) {
                "@" + chat.username
            } else when (ChatType.fromId(chat.chatId)) {
                ChatType.PRIVATE -> stringResource(R.string.user)
                ChatType.CHANNEL -> stringResource(R.string.channel)
                ChatType.GROUP -> stringResource(R.string.group)
                else -> ""
            }
            
            ProfileCard(
                id = chat.chatId,
                headlineText = chat.name,
                supportingText = supportText,
                sharedTransition = false,
                onClick = { onChatClick(chat.chatId, chat.name) })
        }
        
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
