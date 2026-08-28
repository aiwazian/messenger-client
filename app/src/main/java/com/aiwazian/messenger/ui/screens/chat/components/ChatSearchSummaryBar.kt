/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.screens.chat.ChatUiState

/**
 * Нижняя капсула во время поиска.
 *
 * Занимает место поля ввода сообщения: слева счётчик результатов, справа
 * переключатель «Списком» / «В чате».
 */
@Composable
fun ChatSearchSummaryBar(
    uiState: ChatUiState,
    onToggleDisplayMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(24.dp)
                )
                .heightIn(min = 48.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = searchSummaryText(uiState),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            
            TextButton(onClick = onToggleDisplayMode, shape = CircleShape) {
                Text(
                    /* Кнопка предлагает второй режим, а не называет текущий. */
                    text = stringResource(
                        if (uiState.isMessageSearchListMode) R.string.chat_search_show_in_chat
                        else R.string.chat_search_show_as_list
                    )
                )
            }
        }
    }
}

/**
 * Что показать слева: «10 результатов», «1 из 142» или подсказку.
 *
 * Позиция имеет смысл только в режиме «В чате»: в списке все совпадения видно
 * сразу. Плюс после числа означает, что сервер не досчитал совпадения до конца
 * истории и их может быть больше.
 */
@Composable
private fun searchSummaryText(uiState: ChatUiState): String {
    if (uiState.messageSearchQuery.isBlank()) {
        return stringResource(R.string.chat_search_hint)
    }
    
    if (uiState.isSearchingMessages && uiState.messageSearchResults.isEmpty()) {
        return stringResource(R.string.chat_search_in_progress)
    }
    
    val total = uiState.messageSearchTotal
    if (total <= 0) return stringResource(R.string.chat_search_no_results)
    
    val totalText = if (uiState.isMessageSearchTotalExact) total.toString() else "$total+"
    
    if (uiState.isMessageSearchListMode || uiState.messageSearchIndex < 0) {
        return pluralStringResource(R.plurals.chat_search_results_count, total, totalText)
    }
    
    return stringResource(
        R.string.chat_search_position, uiState.messageSearchIndex + 1, totalText
    )
}
