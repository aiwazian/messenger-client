/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.pdf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.FileRepository
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.FileHandler
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationPattern
import com.aiwazian.messenger.utils.VibrationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val chatRepository: ChatRepository,
    private val channelRepository: ChannelRepository,
    private val downloaderManager: DownloaderManager,
    private val fileHandler: FileHandler,
    private val vibrationManager: VibrationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfViewerUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PdfViewerUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var initializedFileId: String? = null

    fun init(chatId: Long, messageId: Long, fileId: String, fileName: String, postId: Long?) {
        if (initializedFileId == fileId) return
        initializedFileId = fileId

        _uiState.value = PdfViewerUiState(fileName = fileName)

        viewModelScope.launch {
            val entity = fileRepository.getById(fileId)

            if (entity?.path != null) {
                _uiState.update { it.copy(filePath = entity.path) }
                return@launch
            }

            val status = entity?.status
            if (status != DownloadStatus.DOWNLOADING && status != DownloadStatus.UPLOADING) {
                startDownload(chatId, messageId, fileId, fileName, postId)
            }

            observeDownload(fileId)
        }
    }

    private suspend fun startDownload(
        chatId: Long,
        messageId: Long,
        fileId: String,
        fileName: String,
        postId: Long?
    ) {
        val url = if (postId != null) {
            channelRepository.getCommentFileDownloadUrl(chatId, postId, messageId, fileId)
        } else {
            chatRepository.getDownloadUrl(chatId, messageId, fileId)
        }

        url.onSuccess { downloadUrl -> downloaderManager.download(downloadUrl, fileName, fileId) }
            .onFailure { _uiState.update { it.copy(isFailed = true) } }
    }

    private fun observeDownload(fileId: String) {
        viewModelScope.launch {
            fileRepository.observeById(fileId)
                .filterNotNull()
                .first { it.path != null }
                .let { entity -> _uiState.update { it.copy(filePath = entity.path) } }
        }

        viewModelScope.launch {
            downloaderManager.activeDownloads
                .map { downloads -> downloads.firstOrNull { it.fileId == fileId }?.status }
                .distinctUntilChanged()
                .first { it == DownloadStatus.FAILED || it == DownloadStatus.CANCELLED }

            _uiState.update { it.copy(isFailed = true) }
        }
    }

    fun saveToDownloads() {
        viewModelScope.launch {
            val path = _uiState.value.filePath ?: return@launch

            val saved = fileHandler.saveToDownloads(path, _uiState.value.fileName)

            if (saved) {
                _uiEffect.emit(
                    PdfViewerUiEffect.ShowSnackbar(UiText.StringResource(R.string.saved_to_downloads))
                )
            } else {
                _uiEffect.emit(
                    PdfViewerUiEffect.ShowSnackbar(
                        UiText.StringResource(R.string.failed_to_save_to_downloads)
                    )
                )
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }

    fun openExternally() {
        viewModelScope.launch {
            val path = _uiState.value.filePath ?: return@launch

            fileHandler.openFile(path)
        }
    }
}

data class PdfViewerUiState(
    val fileName: String = "",
    val filePath: String? = null,
    val isFailed: Boolean = false
)

sealed interface PdfViewerUiEffect {
    data class ShowSnackbar(val message: UiText) : PdfViewerUiEffect
}
