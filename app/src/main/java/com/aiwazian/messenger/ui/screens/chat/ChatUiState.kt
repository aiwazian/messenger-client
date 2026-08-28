/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.net.Uri
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.ChatAdminPermissions
import com.aiwazian.messenger.domain.InviteLinkInfo
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.domain.MessageReadInfo
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.domain.MessageSearchHit
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.ui.screens.chat.paging.ScrollTarget
import com.aiwazian.messenger.utils.UiText

data class ChatUiState(
    val chatId: Long = -1,
    val chatName: UiText = UiText.DynamicString(""),
    val subTitle: UiText = UiText.DynamicString(""),
    val avatarUri: Uri? = null,
    val topBarActions: List<TopBarAction> = emptyList(),
    val chatItems: List<ChatItem> = emptyList(),
    val messageText: String = "",
    val isConnected: Boolean = true,
    val isJoined: Boolean = true,
    val isOwner: Boolean = false,
    /** Права текущего пользователя в канале или группе. */
    val myPermissions: ChatAdminPermissions = ChatAdminPermissions(),
    val isMuted: Boolean = false,
    val showDeleteChatDialog: Boolean = false,
    val showDeleteMessageDialog: Boolean = false,
    val deleteForRecipient: Boolean = false,
    val showLeaveDialog: Boolean = false,
    val selectedMessages: Set<Message> = emptySet(),
    val userNamesCache: Map<Long, String> = emptyMap(),
    /**
     * Теги участников группы: id → тег.
     *
     * Рисуются рядом с именем отправителя, в каналах всегда пусты.
     */
    val memberTagsCache: Map<Long, String> = emptyMap(),
    val myId: Long = -1L,
    /** Нужно для заголовка ответа на своё же сообщение в личном чате. */
    val myName: String = "",
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val inviteLinkInfo: InviteLinkInfo? = null,
    val inviteLinkCode: String? = null,
    val showInviteBottomSheet: Boolean = false,
    val isProcessingInvite: Boolean = false,
    val inviteLinkError: String? = null,
    val showBannedDialog: Boolean = false,
    val mediaItems: List<MessageAttachment> = emptyList(),
    val initialMediaIndex: Int = 0,
    val showFullScreenViewer: Boolean = false,
    val isVideoLooping: Boolean = false,
    val videoPlaybackSpeed: Float = 1.0f,
    val canDownloadMedia: Boolean = true,
    /**
     * Запрет копирования контента канала или группы.
     *
     * Готовые правила доступны через [copyPolicy].
     */
    val noCopy: Boolean = false,
    val isRecording: Boolean = false,
    val isRecordingLocked: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val recordingAmplitude: Float = 0f,
    val showMicrophonePermissionSheet: Boolean = false,
    val currentPlayingVoiceFileId: String? = null,
    val isVoicePlaying: Boolean = false,
    val voicePositionMs: Int = 0,
    val voiceDurationMs: Int = 0,
    val isFirstLoadDone: Boolean = false,
    val groupReadInfo: Map<Long, List<MessageReadInfo>> = emptyMap(),
    val editingMessageId: Long? = null,
    val editingOriginalText: String? = null,
    val isBlocked: Boolean = false,
    val isBlockedByThem: Boolean = false,
    val showBlockDialog: Boolean = false,

    // region Ответ на сообщение
    /** Активный ответ: панель над полем ввода и replyToId в следующей отправке. */
    val replyToMessage: MessageReplyPreview? = null,
    // endregion

    // region Пересылка сообщения
    val forwardingMessage: Message? = null,
    val isForwardSheetVisible: Boolean = false,
    /** Чаты, в которые пользователь может писать. */
    val forwardCandidates: List<Chat> = emptyList(),
    val selectedForwardChatIds: Set<Long> = emptySet(),
    val isForwarding: Boolean = false,
    // endregion

    // region Окно истории и переходы к сообщениям
    /** Идёт догрузка более старых сообщений (вверх). */
    val isLoadingOlder: Boolean = false,
    /** Идёт догрузка более новых сообщений (вниз, после прыжка). */
    val isLoadingNewer: Boolean = false,
    /** Есть ли сообщения новее текущего окна. */
    val hasMoreNewerMessages: Boolean = false,
    /** Идёт прыжок в другое место истории: обычную пагинацию надо притормозить. */
    val isRelocating: Boolean = false,
    /** Окно прижато к концу чата. */
    val isAtLiveEdge: Boolean = true,
    /** Невыполненный запрос прокрутки. */
    val scrollTarget: ScrollTarget? = null,
    val highlightedMessageId: Long? = null,
    /** Есть куда вернуться после перехода по ответу. */
    val canJumpBack: Boolean = false,
    /** Сколько сообщений было не прочитано на момент открытия чата. */
    val unreadCount: Int = 0,
    /** Граница непрочитанного: на неё открывается чат. */
    val firstUnreadMessageId: Long? = null,
    // endregion

    // region Поиск сообщений в чате
    val isMessageSearchActive: Boolean = false,
    val messageSearchQuery: String = "",
    /** Загруженные совпадения, от новых к старым. */
    val messageSearchResults: List<MessageSearchHit> = emptyList(),
    val isSearchingMessages: Boolean = false,
    val hasMoreSearchResults: Boolean = false,
    /** Всего совпадений в чате: приходит с сервера вместе с первой страницей. */
    val messageSearchTotal: Int = 0,
    /** false, если сервер не досчитал совпадения до конца истории. */
    val isMessageSearchTotalExact: Boolean = true,
    /**
     * Позиция текущего совпадения в [messageSearchResults].
     *
     * -1 — пользователь ещё ни к какому результату не переходил.
     */
    val messageSearchIndex: Int = -1,
    /** Результаты показываются списком во весь экран вместо чата. */
    val isMessageSearchListMode: Boolean = false,
    /** Отправители найденных сообщений: id → имя и аватарка. */
    val messageSearchSenders: Map<Long, MessageSearchSender> = emptyMap()
    // endregion
) {

    /** Правила копирования, пересылки и сохранения медиа для текущего чата. */
    val copyPolicy: ChatCopyPolicy
        get() = ChatCopyPolicy(noCopy)

    /**
     * Есть ли куда идти по стрелке «вверх».
     *
     * Вверх по чату — это к более старому совпадению, а если загруженная
     * страница закончилась, то ещё и повод догрузить следующую.
     */
    val canGoToOlderSearchResult: Boolean
        get() = messageSearchIndex + 1 < messageSearchResults.size || hasMoreSearchResults

    /** Есть ли куда идти по стрелке «вниз», то есть к более новому совпадению. */
    val canGoToNewerSearchResult: Boolean
        get() = messageSearchIndex > 0
}

/**
 * Отправитель найденного сообщения.
 *
 * В карточке результата поиска нужны имя и аватарка, а в самом совпадении с
 * сервера лежит только id отправителя.
 */
data class MessageSearchSender(
    val name: String,
    val avatarUri: Uri? = null
)
