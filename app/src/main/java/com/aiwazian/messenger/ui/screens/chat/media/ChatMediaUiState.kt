/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.media

import com.aiwazian.messenger.domain.ChatMediaItem

/**
 * Состояние галереи чата.
 *
 * Вкладки держат свои списки и свои флаги загрузки: они листаются
 * независимо, и пустая вторая не должна показывать «ничего нет», пока ещё
 * идёт её собственный запрос.
 */
data class ChatMediaUiState(
    val media: List<ChatMediaItem> = emptyList(),
    val files: List<ChatMediaItem> = emptyList(),
    val isMediaLoading: Boolean = true,
    val isFilesLoading: Boolean = true,
    val hasError: Boolean = false,
    /**
     * Номер страницы в просмотрщике.
     *
     * Считается только по скачанным: показать во весь экран можно лишь то,
     * что уже лежит на устройстве, и нумерация сетки здесь не годится.
     */
    val initialMediaIndex: Int = 0,
    val showFullScreenViewer: Boolean = false,
    val isVideoLooping: Boolean = false,
    val videoPlaybackSpeed: Float = 1.0f
)
