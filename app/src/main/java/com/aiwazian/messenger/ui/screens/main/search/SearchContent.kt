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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Search
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.components.ProfileCard

@Composable
fun highlightText(
    text: String,
    query: String,
    isUsername: Boolean = false
): AnnotatedString {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isBlank()) return AnnotatedString(text)
    
    val primaryColor = MaterialTheme.colorScheme.primary
    
    return buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = trimmedQuery.lowercase()
        val index = lowerText.indexOf(lowerQuery)
        
        if (index == -1) {
            append(text)
        } else {
            if (isUsername && index == 1 && text.startsWith("@")) {
                withStyle(SpanStyle(color = primaryColor)) {
                    append("@")
                }
            } else {
                append(text.substring(0, index))
            }
            
            withStyle(SpanStyle(color = primaryColor)) {
                append(text.substring(index, index + trimmedQuery.length))
            }
            
            append(text.substring(index + trimmedQuery.length))
        }
    }
}

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
    query: String,
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
            
            val isUsername = chat.username != null
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
                headlineText = highlightText(chat.name, query),
                supportingText = highlightText(supportText, query, isUsername),
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
