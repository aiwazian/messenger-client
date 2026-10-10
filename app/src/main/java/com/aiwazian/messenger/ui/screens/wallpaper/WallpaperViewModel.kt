/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.wallpaper

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Wallpaper
import com.aiwazian.messenger.domain.WallpaperTarget
import com.aiwazian.messenger.repository.WallpaperRepository
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.media.MediaTransform
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class WallpaperUiState(
    val target: WallpaperTarget? = null,
    val draftPath: String? = null,
    val darken: Float = 0f,
    val blur: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val isSaving: Boolean = false
)

sealed interface WallpaperUiEffect {
    data object NavigateBack : WallpaperUiEffect
    data class ShowSnackbar(val message: UiText) : WallpaperUiEffect
}

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WallpaperUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<WallpaperUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private val _currentWallpaper = MutableStateFlow<Wallpaper?>(null)
    val currentWallpaper: StateFlow<Wallpaper?> = _currentWallpaper.asStateFlow()

    private var observeJob: Job? = null

    fun init(target: WallpaperTarget, draftPath: String? = null) {
        _uiState.update {
            it.copy(
                target = target,
                draftPath = draftPath ?: it.draftPath
            )
        }

        observeJob?.cancel()

        observeJob = viewModelScope.launch {
            when (target) {
                WallpaperTarget.Global -> {
                    wallpaperRepository.refreshMyWallpaper()
                    wallpaperRepository.myWallpaper.collect { wallpaper ->
                        _currentWallpaper.value = wallpaper
                    }
                }

                is WallpaperTarget.Chat -> observeChatWallpaper(target.chatId)
                is WallpaperTarget.Channel -> observeChatWallpaper(target.channelId)
                is WallpaperTarget.Group -> observeChatWallpaper(target.groupId)
            }
        }
    }

    suspend fun prepareDraft(uri: Uri, transform: MediaTransform, overlay: Bitmap?): Uri? =
        wallpaperRepository.prepareDraft(uri, transform, overlay)

    fun setDarken(value: Float) {
        _uiState.update { it.copy(darken = value.coerceIn(0f, MAX_EFFECT_FRACTION)) }
    }

    fun setBlur(value: Float) {
        _uiState.update { it.copy(blur = value.coerceIn(0f, MAX_EFFECT_FRACTION)) }
    }

    fun setOffset(x: Float, y: Float) {
        _uiState.update { it.copy(offsetX = x, offsetY = y) }
    }

    fun apply() {
        when (val target = _uiState.value.target) {
            WallpaperTarget.Global -> save { draft, darken, blur, offsetX, offsetY ->
                wallpaperRepository.setGlobalWallpaper(draft, darken, blur, offsetX, offsetY)
            }

            is WallpaperTarget.Channel -> save { draft, darken, blur, offsetX, offsetY ->
                wallpaperRepository.setChannelWallpaper(
                    target.channelId, draft, darken, blur, offsetX, offsetY
                )
            }

            is WallpaperTarget.Group -> save { draft, darken, blur, offsetX, offsetY ->
                wallpaperRepository.setGroupWallpaper(
                    target.groupId, draft, darken, blur, offsetX, offsetY
                )
            }

            else -> Unit
        }
    }

    fun applyForMe() = applyChat(SCOPE_ME)

    fun applyForBoth() = applyChat(SCOPE_ALL)

    private fun applyChat(scope: String) {
        val target = _uiState.value.target as? WallpaperTarget.Chat ?: return

        save { draft, darken, blur, offsetX, offsetY ->
            wallpaperRepository.setChatWallpaper(
                chatId = target.chatId,
                scope = scope,
                draft = draft,
                darken = darken,
                blur = blur,
                offsetX = offsetX,
                offsetY = offsetY
            )
        }
    }

    fun resetWallpaper() {
        val target = _uiState.value.target ?: return

        viewModelScope.launch {
            val result = when (target) {
                WallpaperTarget.Global -> wallpaperRepository.resetGlobalWallpaper()
                is WallpaperTarget.Chat -> wallpaperRepository.resetChatWallpaper(target.chatId)
                is WallpaperTarget.Channel -> wallpaperRepository.resetChannelWallpaper(target.channelId)
                is WallpaperTarget.Group -> wallpaperRepository.resetGroupWallpaper(target.groupId)
            }

            result.onFailure {
                _uiEffect.emit(WallpaperUiEffect.ShowSnackbar(UiText.StringResource(R.string.wallpaper_apply_failed)))
            }
        }
    }

    fun resetChatWallpaper(chatId: Long) {
        viewModelScope.launch {
            wallpaperRepository.resetChatWallpaper(chatId).onFailure {
                _uiEffect.emit(WallpaperUiEffect.ShowSnackbar(UiText.StringResource(R.string.wallpaper_apply_failed)))
            }
        }
    }

    private fun observeChatWallpaper(chatId: Long) = viewModelScope.launch {
        wallpaperRepository.refreshChatWallpaper(chatId)
        wallpaperRepository.observeChatWallpaper(chatId).collect { wallpaper ->
            _currentWallpaper.value = wallpaper
        }
    }

    private fun save(block: suspend (Uri, Int, Int, Float, Float) -> kotlin.Result<Unit>) {
        if (_uiState.value.isSaving) return

        val state = _uiState.value
        val draft = state.draftPath?.let(Uri::parse) ?: return
        val darken = state.darken.toPercent()
        val blur = state.blur.toPercent()
        val offsetX = state.offsetX
        val offsetY = state.offsetY

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val result = block(draft, darken, blur, offsetX, offsetY)

            _uiState.update { it.copy(isSaving = false) }

            result.onSuccess {
                _uiEffect.emit(WallpaperUiEffect.NavigateBack)
            }.onFailure {
                _uiEffect.emit(WallpaperUiEffect.ShowSnackbar(UiText.StringResource(R.string.wallpaper_apply_failed)))
            }
        }
    }

    private fun Float.toPercent(): Int =
        (this * PERCENT_MULTIPLIER).roundToInt().coerceIn(0, MAX_EFFECT_PERCENT)

    private companion object {
        const val MAX_EFFECT_FRACTION = 0.9f
        const val MAX_EFFECT_PERCENT = 90
        const val PERCENT_MULTIPLIER = 100
        const val SCOPE_ME = "me"
        const val SCOPE_ALL = "all"
    }
}
