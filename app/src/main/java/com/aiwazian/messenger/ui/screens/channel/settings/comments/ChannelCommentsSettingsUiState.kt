/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.comments

data class ChannelCommentsSettingsUiState(
    val channelId: Long = -1,
    val commentsEnabled: Boolean = false,
    val commentsRestrictedToSubscribers: Boolean = true,
    val isChangingCommentsEnabled: Boolean = false,
    val isChangingCommentsRestricted: Boolean = false,
    val isDeletingAllComments: Boolean = false
)
