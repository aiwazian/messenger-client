/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.net.Uri
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.screens.chat.components.DateSeparatorItem
import com.aiwazian.messenger.ui.screens.chat.components.MessageBubble
import com.aiwazian.messenger.ui.screens.chat.components.SystemMessageBubble
import com.aiwazian.messenger.ui.screens.chat.components.UnreadSeparatorItem

@Composable
internal fun BoxScope.ChatMessagesList(
    uiState: ChatUiState,
    chatViewModel: ChatViewModel,
    readersViewModel: MessageReadersViewModel,
    readerAvatars: Map<Long, Uri?>,
    listState: LazyListState,
    innerPadding: PaddingValues,
    pinnedMessageIds: Set<Long>,
    copyPolicy: ChatCopyPolicy,
    onFileCancel: (Long) -> Unit,
    onMediaOpen: (MessageAttachment) -> Unit,
    onContactClick: (Message) -> Unit,
    modifier: Modifier = Modifier
) {
    val navBackStack = LocalNavBackStack.current

    LazyColumn(
        modifier = modifier.align(Alignment.BottomCenter),
        state = listState,
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        overscrollEffect = rememberOverscrollEffect()
    ) {
        item(key = "chat_footer") {
            Column {
                if (uiState.isLoadingNewer) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularWavyProgressIndicator()
                    }
                }
                Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
            }
        }

        items(
            items = uiState.chatItems, key = { item ->
                when (item) {
                    is ChatItem.DateSeparator -> "date_${item.text}"
                    is ChatItem.UnreadSeparator -> "unread_separator"
                    is ChatItem.SystemMessage -> "sys_${item.sendTime}"
                    is ChatItem.MessageItem -> "msg_${item.message.id}"
                }
            }) { item ->
            when (item) {
                is ChatItem.DateSeparator -> DateSeparatorItem(
                    item.text, Modifier.animateItem()
                )

                is ChatItem.UnreadSeparator -> UnreadSeparatorItem(
                    Modifier.animateItem()
                )

                is ChatItem.SystemMessage -> SystemMessageBubble(
                    item.text.asString(), Modifier.animateItem()
                )

                is ChatItem.MessageItem -> MessageBubble(
                    modifier = Modifier.animateItem(),
                    item = item,
                    onFileAction = { file, action ->
                        if (action == FileAction.CANCEL) {
                            onFileCancel(item.message.id)
                        } else {
                            if (action == FileAction.OPEN) {
                                onMediaOpen(file)
                            }
                            chatViewModel.onFileAction(
                                item.message,
                                file,
                                action
                            )
                        }
                    },
                    currentPlayingVoiceFileId = uiState.currentPlayingVoiceFileId,
                    isVoicePlaying = uiState.isVoicePlaying,
                    voicePositionMs = uiState.voicePositionMs,
                    voiceDurationMs = uiState.voiceDurationMs,
                    onVoiceSeek = chatViewModel::onVoiceSeek,
                    audioMetadata = uiState.audioMetadata,
                    currentMusicFileId = uiState.currentMusicFileId,
                    isMusicPlaying = uiState.isMusicPlaying,
                    musicPositionMs = uiState.musicPositionMs,
                    musicDurationMs = uiState.musicDurationMs,
                    onMusicSeek = chatViewModel::onMusicSeek,
                    onLinkClicked = chatViewModel::onLinkClicked,
                    onUsernameClicked = chatViewModel::onUsernameClicked,
                    onEmailClicked = chatViewModel::onEmailClicked,
                    onPhoneClicked = chatViewModel::onPhoneClicked,
                    onSaveToDownloads = if (copyPolicy.canSaveMedia) {
                        {
                            chatViewModel.saveAttachmentsToDownloads(
                                item.message
                            )
                        }
                    } else null,
                    onReplyPreviewClick = {
                        chatViewModel.onReplyPreviewClicked(item.message)
                    },
                    onForwardedFromClick = {
                        chatViewModel.onForwardedFromClicked(item.message)
                    },
                    onContactClick = if (item.message.messageType == MessageType.CONTACT) {
                        { onContactClick(item.message) }
                    } else {
                        null
                    },
                    onSwipeThresholdReached = chatViewModel::vibrateTactile,
                    onSwipeToReply = {
                        chatViewModel.startReply(item.message)
                    },
                    readerAvatars = readerAvatars,
                    onReadersRequested = readersViewModel::onReadersRequested,
                    onSenderNameClick = if (item.chatType == ChatType.GROUP) {
                        {
                            navBackStack.add(
                                AppRoute.Profile(
                                    profileId = item.message.senderId,
                                    profileName = item.senderName
                                )
                            )
                        }
                    } else null,
                    onCommentsClick = if (
                        item.chatType == ChatType.CHANNEL &&
                        (uiState.commentsEnabled || item.message.commentsCount > 0)
                    ) {
                        {
                            navBackStack.add(
                                AppRoute.PostComments(
                                    chatId = uiState.chatId,
                                    postId = item.message.id
                                )
                            )
                        }
                    } else null,
                    onReaderClick = { reader ->
                        val readerName = listOf(
                            reader.firstName,
                            reader.lastName.orEmpty()
                        ).filter { it.isNotBlank() }.joinToString(" ")
                        navBackStack.add(
                            AppRoute.Profile(
                                profileId = reader.userId,
                                profileName = readerName.ifBlank { null },
                                avatarUri = readerAvatars[reader.userId]?.toString()
                            )
                        )
                    },
                    isPinned = item.message.id in pinnedMessageIds
                )
            }
        }

        item(key = "chat_header") {
            Column {
                if (uiState.isLoadingOlder) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularWavyProgressIndicator()
                    }
                }
                Spacer(Modifier.height(innerPadding.calculateTopPadding()))
            }
        }
    }
}
