/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.media

import com.aiwazian.messenger.domain.ChatMediaCounts
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
    val voices: List<ChatMediaItem> = emptyList(),
    val isMediaLoading: Boolean = true,
    val isFilesLoading: Boolean = true,
    val isVoicesLoading: Boolean = true,
    val hasError: Boolean = false,
    /**
     * Сколько вложений в чате всего — для подписи в шапке.
     *
     * Не считается по спискам выше: там лежит только загруженное окно, а
     * подпись говорит о всём чате. Пусто, пока счётчики не пришли.
     */
    val counts: ChatMediaCounts? = null,
    /** Свой идентификатор: список голосовых отличает свои записи от чужих. */
    val myId: Long = 0,
    /** Голосовое, которое сейчас в проигрывателе, даже если на паузе. */
    val playingFileId: String? = null,
    val isVoicePlaying: Boolean = false,
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
