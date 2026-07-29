/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.type

import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.utils.UiText

data class ChannelTypeSettingsUiState(
    val channelId: Long = -1,
    val originalName: String = "",
    val username: String = "",
    val channelType: ChannelType = ChannelType.PRIVATE,
    val isError: Boolean = false,
    val canSave: Boolean = false,
    val statusText: UiText? = null,
    /** Запрет копирования контента канала. */
    val noCopy: Boolean = false,
    /** Доступен ли переключатель запрета копирования (только для владельца). */
    val canChangeNoCopy: Boolean = false
)
