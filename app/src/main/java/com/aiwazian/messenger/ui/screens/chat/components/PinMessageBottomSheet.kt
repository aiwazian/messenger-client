/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionRadioItem

/**
 * Выбор области закрепления и открепление.
 *
 * Первый вариант всегда «только для себя»; подпись второго зависит от типа
 * чата: «для обоих» в личном чате, «для всех подписчиков» в канале и
 * «для всех участников» в группе. Кнопка «Открепить» снимает закрепление
 * независимо от выбранного варианта.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinMessageBottomSheet(
    chatType: ChatType,
    forEveryone: Boolean,
    onSelectScope: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onUnpin: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.pin_message_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )

            SectionContainer {
                SectionRadioItem(
                    text = stringResource(R.string.pin_scope_for_me_only),
                    selected = !forEveryone,
                    onClick = { onSelectScope(false) }
                )

                SectionRadioItem(
                    text = stringResource(pinScopeLabel(chatType)),
                    selected = forEveryone,
                    onClick = { onSelectScope(true) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .combinedClickable(onClick = onConfirm)
                    .heightIn(min = 40.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pin_message),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            TextButton(
                onClick = onUnpin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(text = stringResource(R.string.unpin_message))
            }
        }
    }
}

private fun pinScopeLabel(chatType: ChatType): Int = when (chatType) {
    ChatType.CHANNEL -> R.string.pin_scope_for_all_subscribers
    ChatType.GROUP -> R.string.pin_scope_for_all_members
    else -> R.string.pin_scope_for_both
}
