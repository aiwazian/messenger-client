/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel

sealed class CreateChannelState {
    data object Idle : CreateChannelState()
    data object Loading : CreateChannelState()
    data class Success(val channelId: Long, val channelName: String) : CreateChannelState()
    data class Error(val message: String) : CreateChannelState()
}