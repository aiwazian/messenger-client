/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.components.CountdownTextButton
import com.aiwazian.messenger.ui.screens.chat.ChatUiState
import com.aiwazian.messenger.ui.screens.chat.ChatViewModel

@Composable
fun ChatDialogs(
    uiState: ChatUiState,
    chatViewModel: ChatViewModel
) {
    if (uiState.showDeleteChatDialog) {
        val isPrivateChat =
            ChatType.fromId(uiState.chatId) == ChatType.PRIVATE && uiState.chatId != uiState.myId
        DeleteChatDialog(
            onDismissRequest = chatViewModel::hideDeleteChatDialog,
            onConfirm = chatViewModel::onDeleteChatConfirmed,
            vibrate = chatViewModel::vibrate,
            deleteForRecipient = uiState.deleteForRecipient,
            onDeleteForRecipientChanged = chatViewModel::setDeleteForRecipient,
            isPrivateChat = isPrivateChat
        )
    }
    
    if (uiState.showDeleteMessageDialog) {
        val isPrivateChat =
            ChatType.fromId(uiState.chatId) == ChatType.PRIVATE && uiState.chatId != uiState.myId
        DeleteMessageDialog(
            onDismissRequest = chatViewModel::hideDeleteMessageDialog,
            onConfirm = chatViewModel::onDeleteMessageConfirmed,
            deleteForRecipient = uiState.deleteForRecipient,
            onDeleteForRecipientChanged = chatViewModel::setDeleteForRecipient,
            isPrivateChat = isPrivateChat
        )
    }
    
    if (uiState.showLeaveDialog) {
        val chatType = ChatType.fromId(uiState.chatId)
        LeaveDialog(
            onDismiss = chatViewModel::hideLeaveDialog,
            onConfirm = chatViewModel::onLeaveClicked,
            chatName = uiState.chatName.asString(),
            chatType = chatType
        )
    }
}

@Composable
private fun DeleteChatDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    vibrate: () -> Unit,
    deleteForRecipient: Boolean,
    onDeleteForRecipientChanged: (Boolean) -> Unit,
    isPrivateChat: Boolean
) {
    AppDialog(
        title = stringResource(R.string.delete_chat),
        onDismissRequest = onDismissRequest,
        content = {
            Column {
                Text(text = stringResource(R.string.delete_chat_confirm), lineHeight = 16.sp)
                if (isPrivateChat) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onDeleteForRecipientChanged(!deleteForRecipient) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(
                            checked = deleteForRecipient,
                            onCheckedChange = null,
                            interactionSource = remember { MutableInteractionSource() })
                        Text(
                            text = stringResource(R.string.delete_for_recipient),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
            CountdownTextButton(
                text = stringResource(R.string.delete),
                seconds = 5,
                onClickAfterFinish = onConfirm,
                onClickWhileRunning = vibrate,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            )
        })
}

@Composable
private fun DeleteMessageDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    deleteForRecipient: Boolean,
    onDeleteForRecipientChanged: (Boolean) -> Unit,
    isPrivateChat: Boolean
) {
    AppDialog(
        title = stringResource(R.string.delete_message),
        onDismissRequest = onDismissRequest,
        content = {
            Column {
                Text(
                    text = stringResource(R.string.delete_message_description), lineHeight = 16.sp
                )
                if (isPrivateChat) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onDeleteForRecipientChanged(!deleteForRecipient) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(
                            checked = deleteForRecipient,
                            onCheckedChange = null,
                            interactionSource = remember { MutableInteractionSource() })
                        Text(
                            text = stringResource(R.string.delete_for_recipient),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
            }
        })
}

@Composable
private fun LeaveDialog(
    onDismiss: () -> Unit, onConfirm: () -> Unit, chatName: String, chatType: ChatType
) {
    val title = when (chatType) {
        ChatType.CHANNEL -> stringResource(R.string.leave_channel)
        ChatType.GROUP -> stringResource(R.string.leave_group)
        else -> stringResource(R.string.leave)
    }
    
    val message = buildAnnotatedString {
        append(stringResource(R.string.leave_channel_confirm_message))
        withStyle(style = SpanStyle(fontWeight = FontWeight.W500)) { append(" $chatName") }
        append("?")
    }
    
    AppDialog(title = title, onDismissRequest = onDismiss, content = {
        Text(text = message)
    }, buttons = {
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.cancel))
        }
        TextButton(
            onClick = onConfirm,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text(title)
        }
    })
}
