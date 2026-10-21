/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.utils.LastSeenHelper
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.compactCount
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun ChatViewModel.loadChatInfo() {
    val chatId = _uiState.value.chatId
    viewModelScope.launch {
        val me = userRepository.getMe().first()
        val myId = me.id
        _uiState.update {
            it.copy(
                myId = myId,
                myName = "${me.firstName} ${me.lastName.orEmpty()}".trim()
            )
        }
        when (ChatType.fromId(chatId)) {
            ChatType.CHANNEL -> loadChannelInfo(chatId, myId)
            ChatType.GROUP -> loadGroupInfo(chatId, myId)
            ChatType.PRIVATE -> loadUserInfo(chatId, myId)
            else -> {}
        }
    }
}

private suspend fun ChatViewModel.loadChannelInfo(chatId: Long, myId: Long) {
    viewModelScope.launch {
        channelRepository.fetchById(chatId)
    }
    loadMyChannelPermissions(chatId)
    channelRepository.getById(chatId).collectLatest { channel ->
        _uiState.update {
            it.copy(
                chatName = UiText.DynamicString(channel.name),
                subTitle = compactCountSubtitle(
                    count = channel.subscribers,
                    pluralRes = R.plurals.subscribers_count,
                    compactRes = R.string.subscribers_compact
                ),
                isJoined = channel.isSubscribed,
                isOwner = channel.ownerId == myId,
                avatarUri = channel.avatars.firstOrNull()?.uri,
                noCopy = channel.noCopy,
                commentsEnabled = channel.commentsEnabled,
                topBarActions = createTopBarActions(
                    channel.ownerId == myId,
                    channel.isSubscribed,
                    ChatType.CHANNEL
                )
            )
        }
        if (lastMessages.isNotEmpty()) updateChatItems(lastMessages)
    }
}

private suspend fun ChatViewModel.loadGroupInfo(chatId: Long, myId: Long) {
    viewModelScope.launch {
        groupRepository.fetchById(chatId)
    }
    loadMyGroupPermissions(chatId)
    loadMemberTags(chatId)
    groupRepository.getById(chatId).collectLatest { group ->
        _uiState.update {
            it.copy(
                chatName = UiText.DynamicString(group.name),
                subTitle = compactCountSubtitle(
                    count = group.members,
                    pluralRes = R.plurals.members_count,
                    compactRes = R.string.members_compact
                ),
                isJoined = group.isMember,
                isOwner = group.ownerId == myId,
                avatarUri = group.avatars.firstOrNull()?.uri,
                noCopy = group.noCopy,
                topBarActions = createTopBarActions(
                    group.ownerId == myId,
                    group.isMember,
                    ChatType.GROUP
                )
            )
        }
        if (lastMessages.isNotEmpty()) updateChatItems(lastMessages)
    }
}

private fun ChatViewModel.loadMemberTags(chatId: Long) {
    viewModelScope.launch {
        groupAdminsRepository.getMemberTags(chatId).onSuccess { tags ->
            if (tags.isEmpty() && _uiState.value.memberTagsCache.isEmpty()) return@onSuccess
            _uiState.update { it.copy(memberTagsCache = tags) }
            if (lastMessages.isNotEmpty()) updateChatItems(lastMessages)
        }
    }
}

private fun ChatViewModel.loadMyGroupPermissions(chatId: Long) {
    viewModelScope.launch {
        groupAdminsRepository.getMyPermissions(chatId).onSuccess { permissions ->
            _uiState.update { it.copy(myPermissions = permissions) }
        }
    }
}

private fun ChatViewModel.loadMyChannelPermissions(chatId: Long) {
    viewModelScope.launch {
        channelAdminsRepository.getMyPermissions(chatId).onSuccess { permissions ->
            _uiState.update { it.copy(myPermissions = permissions) }
        }
    }
}

private suspend fun ChatViewModel.loadUserInfo(chatId: Long, myId: Long) {
    viewModelScope.launch {
        userRepository.fetchById(chatId)
    }

    if (chatId == myId) {
        userRepository.getMe().collectLatest { user ->
            _uiState.update {
                it.copy(
                    chatName = UiText.StringResource(R.string.saved_messages),
                    subTitle = UiText.DynamicString(""),
                    avatarUri = user.avatars.firstOrNull()?.uri,
                    topBarActions = createTopBarActions(
                        isOwner = true,
                        isJoined = true,
                        type = ChatType.PRIVATE
                    )
                )
            }
        }
    } else {
        combine(
            userRepository.getById(chatId),
            onlineUsersTracker.onlineUsers
        ) { user, onlineUsers ->
            user to onlineUsers.contains(user.id)
        }.collectLatest { (user, isOnline) ->
            val subTitle = LastSeenHelper.getSubtitle(isOnline, user.lastSeen)
            _uiState.update {
                it.copy(
                    chatName = UiText.DynamicString("${user.firstName} ${user.lastName.orEmpty()}".trim()),
                    subTitle = subTitle,
                    avatarUri = user.avatars.firstOrNull()?.uri,
                    topBarActions = createTopBarActions(
                        isOwner = false,
                        isJoined = true,
                        type = ChatType.PRIVATE
                    ),
                    isBlocked = user.isBlocked,
                    isBlockedByThem = user.isBlockedByThem,
                    peerNoCopy = !user.canForwardAndCopy
                )
            }
            if (lastMessages.isNotEmpty()) updateChatItems(lastMessages)
        }
    }
}

private fun ChatViewModel.createTopBarActions(
    isOwner: Boolean,
    isJoined: Boolean,
    type: ChatType
): List<TopBarAction> {
    val actions = mutableListOf<DropdownMenuAction>()

    if (type == ChatType.PRIVATE) {
        actions.add(
            DropdownMenuAction(
                Icons.Rounded.Wallpaper,
                UiText.StringResource(R.string.change_wallpaper),
                this::showWallpaperSheet
            )
        )
        actions.add(
            DropdownMenuAction(
                Icons.Rounded.DeleteOutline,
                UiText.StringResource(R.string.delete_chat),
                this::showDeleteChatDialog,
                isDestructive = true
            )
        )
    } else if (isJoined && !isOwner) {
        val leaveRes =
            if (type == ChatType.CHANNEL) R.string.leave_channel else R.string.leave_group
        actions.add(
            DropdownMenuAction(
                Icons.AutoMirrored.Rounded.Logout,
                UiText.StringResource(leaveRes),
                this::showLeaveDialog
            )
        )
    }

    return listOf(TopBarAction(icon = Icons.Rounded.MoreVert, dropdownActions = actions))
}

internal fun ChatViewModel.compactCountSubtitle(count: Int, pluralRes: Int, compactRes: Int): UiText {
    return if (count >= 1000) {
        UiText.DynamicString(context.getString(compactRes, compactCount(count)))
    } else {
        UiText.PluralResource(pluralRes, count, count)
    }
}
