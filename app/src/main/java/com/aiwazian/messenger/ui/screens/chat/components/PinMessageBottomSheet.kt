/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionRadioItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinMessageBottomSheet(
    chatType: ChatType,
    forEveryone: Boolean,
    isPinned: Boolean,
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
            
            SectionContainer(contentPadding = PaddingValues.Zero) {
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
            
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.pin_message))
            }
            
            if (isPinned) {
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
}

private fun pinScopeLabel(chatType: ChatType): Int = when (chatType) {
    ChatType.CHANNEL -> R.string.pin_scope_for_all_subscribers
    ChatType.GROUP -> R.string.pin_scope_for_all_members
    else -> R.string.pin_scope_for_both
}
