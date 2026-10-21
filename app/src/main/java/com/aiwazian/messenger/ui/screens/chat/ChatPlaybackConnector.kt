/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.content.Context
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.extensions.isAudioFile
import com.aiwazian.messenger.extensions.isMusicFile
import com.aiwazian.messenger.playback.MusicPlayerManager
import com.aiwazian.messenger.playback.MusicTrack
import com.aiwazian.messenger.playback.VoicePlayerManager
import com.aiwazian.messenger.playback.VoiceQueueItem
import com.aiwazian.messenger.utils.AudioMetadataCache
import com.aiwazian.messenger.utils.EqualizerManager
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatPlaybackConnector(
    private val context: Context,
    private val scope: CoroutineScope,
    private val stateFlow: StateFlow<ChatUiState>,
    private val updateState: ((ChatUiState) -> ChatUiState) -> Unit,
    private val voicePlayerManager: VoicePlayerManager,
    private val musicPlayerManager: MusicPlayerManager,
    private val equalizerManager: EqualizerManager
) {
    private val pendingVoiceStartPositions = mutableMapOf<String, Int>()
    private val pendingMusicStartPositions = mutableMapOf<String, Int>()
    private val pendingMetadataRequests = mutableSetOf<String>()

    fun observe() {
        observeVoicePlayer()
        observeVoicePlayerInfo()
        observeMusicPlayer()
        observeMusicQueueUpdates()
        observeEqualizer()
        observeAudioMetadata()
        observeQueueUpdates()
    }

    fun handlePlayVoice(file: MessageAttachment) {
        if (file.type != AttachmentType.VOICE || file.localUri == null) return
        musicPlayerManager.stop()
        if (stateFlow.value.currentPlayingVoiceFileId == file.fileId) {
            voicePlayerManager.togglePlayPause()
        } else {
            val startPos = pendingVoiceStartPositions.remove(file.fileId) ?: 0
            playVoice(file.fileId, startPos)
        }
    }

    fun handlePlayMusic(file: MessageAttachment) {
        if (file.localUri == null) return

        if (stateFlow.value.currentMusicFileId == file.fileId) {
            musicPlayerManager.togglePlayPause()
            return
        }

        voicePlayerManager.stop()
        val queue = buildMusicQueue(stateFlow.value)
        val startPosition = pendingMusicStartPositions.remove(file.fileId) ?: 0
        musicPlayerManager.play(queue, file.fileId, startPosition)
    }

    fun musicStartPosition(fileId: String): Int {
        return pendingMusicStartPositions.remove(fileId) ?: 0
    }

    fun updateMusicQueue() {
        musicPlayerManager.updateQueue(buildMusicQueue(stateFlow.value))
    }

    fun onVoiceSeek(file: MessageAttachment, positionMs: Int) {
        if (stateFlow.value.currentPlayingVoiceFileId != file.fileId) {
            pendingVoiceStartPositions[file.fileId] = positionMs
        } else {
            voicePlayerManager.seekTo(positionMs)
        }
    }

    fun onMusicSeek(file: MessageAttachment, positionMs: Int) {
        if (stateFlow.value.currentMusicFileId == file.fileId) {
            musicPlayerManager.seekTo(positionMs)
        } else {
            pendingMusicStartPositions[file.fileId] = positionMs
        }
    }

    fun voiceQueue(): List<VoiceQueueItem> {
        val state = stateFlow.value
        return buildVoiceQueue(state.chatItems, state.chatName)
    }

    private fun observeVoicePlayer() {
        voicePlayerManager.connect()
        scope.launch {
            voicePlayerManager.state.collect { state ->
                updateState {
                    it.copy(
                        currentPlayingVoiceFileId = state.currentFileId,
                        isVoicePlaying = state.isPlaying,
                        voicePositionMs = state.positionMs,
                        voiceDurationMs = state.durationMs
                    )
                }
            }
        }
    }

    private fun observeVoicePlayerInfo() {
        scope.launch {
            stateFlow.collect { state ->
                val fileId = state.currentPlayingVoiceFileId
                val item = fileId?.let { id ->
                    state.chatItems.filterIsInstance<ChatItem.MessageItem>()
                        .firstOrNull { item -> item.message.attachments.any { it.fileId == id } }
                }
                val messageId = item?.message?.id
                val senderName = item?.let { resolveVoiceSenderName(it, state) }
                val sendTime = item?.message?.sendTime

                if (state.currentVoiceMessageId != messageId ||
                    state.currentVoiceSenderName != senderName ||
                    state.currentVoiceSendTime != sendTime
                ) {
                    updateState {
                        it.copy(
                            currentVoiceMessageId = messageId,
                            currentVoiceSenderName = senderName,
                            currentVoiceSendTime = sendTime
                        )
                    }
                }
            }
        }
    }

    private fun resolveVoiceSenderName(item: ChatItem.MessageItem, state: ChatUiState): String {
        if (item.isMine) return context.getString(R.string.you)
        return item.senderName ?: state.chatName.asString(context)
    }

    private fun observeMusicPlayer() {
        musicPlayerManager.connect()
        scope.launch {
            musicPlayerManager.state.collect { state ->
                updateState {
                    it.copy(
                        currentMusicFileId = state.fileId,
                        currentMusicTitle = state.title,
                        currentMusicArtist = state.artist,
                        isMusicPlaying = state.isPlaying,
                        musicPositionMs = state.positionMs,
                        musicDurationMs = state.durationMs
                    )
                }
            }
        }
        scope.launch {
            musicPlayerManager.repeatMode.collect { mode ->
                updateState { it.copy(musicRepeatMode = mode) }
            }
        }
    }

    private fun observeMusicQueueUpdates() {
        scope.launch {
            stateFlow.collect { state ->
                if (state.currentMusicFileId == null) return@collect
                musicPlayerManager.updateQueue(buildMusicQueue(state))
            }
        }
    }

    private fun observeEqualizer() {
        scope.launch {
            equalizerManager.info.collect { info ->
                updateState { it.copy(equalizerInfo = info) }
            }
        }
        scope.launch {
            equalizerManager.bandLevels.collect { levels ->
                updateState { it.copy(equalizerBandLevels = levels) }
            }
        }
    }

    private fun observeAudioMetadata() {
        scope.launch {
            stateFlow.collect { state ->
                state.chatItems.forEach { item ->
                    if (item !is ChatItem.MessageItem) return@forEach
                    item.message.attachments.forEach { attachment ->
                        val fileId = attachment.fileId
                        if (!attachment.extension.isAudioFile()) return@forEach
                        val localUri = attachment.localUri ?: return@forEach
                        if (state.audioMetadata.containsKey(fileId)) return@forEach
                        if (!pendingMetadataRequests.add(fileId)) return@forEach

                        scope.launch {
                            val metadata = AudioMetadataCache.get(
                                fileId = fileId,
                                filePath = localUri.path ?: localUri.toString(),
                                fallbackTitle = attachment.name
                            )
                            pendingMetadataRequests.remove(fileId)
                            if (metadata != null) {
                                updateState {
                                    it.copy(audioMetadata = it.audioMetadata + (fileId to metadata))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeQueueUpdates() {
        scope.launch {
            stateFlow.collect { state ->
                val playingId = state.currentPlayingVoiceFileId ?: return@collect
                val queue = buildVoiceQueue(state.chatItems, state.chatName)
                if (queue.any { it.fileId == playingId }) {
                    voicePlayerManager.updateQueue(queue)
                }
            }
        }
    }

    private fun playVoice(fileId: String, startPositionMs: Int = 0) {
        val queue = buildVoiceQueue(stateFlow.value.chatItems, stateFlow.value.chatName)
        if (queue.none { it.fileId == fileId }) return
        voicePlayerManager.play(queue, fileId, startPositionMs)
    }

    private fun buildVoiceQueue(items: List<ChatItem>, chatName: UiText): List<VoiceQueueItem> {
        val title = chatName.asString(context).ifBlank { context.getString(R.string.voice_message) }
        return items.asReversed().filterIsInstance<ChatItem.MessageItem>()
            .flatMap { it.message.attachments }
            .filter { it.type == AttachmentType.VOICE }
            .map { attachment ->
                val isReady =
                    attachment.localUri != null && (attachment.status == DownloadStatus.COMPLETED || attachment.status == DownloadStatus.UPLOADED)
                VoiceQueueItem(
                    uri = if (isReady) attachment.localUri else null,
                    fileId = attachment.fileId,
                    title = title,
                    subtitle = context.getString(R.string.voice_message),
                    artworkUri = stateFlow.value.avatarUri
                )
            }
    }

    private fun buildMusicQueue(state: ChatUiState): List<MusicTrack> {
        return state.chatItems.asReversed()
            .filterIsInstance<ChatItem.MessageItem>()
            .flatMap { it.message.attachments }
            .filter { it.extension.isMusicFile() && it.localUri != null }
            .map { attachment ->
                val metadata = state.audioMetadata[attachment.fileId]
                MusicTrack(
                    fileId = attachment.fileId,
                    uri = attachment.localUri!!,
                    title = metadata?.title ?: attachment.name,
                    artist = metadata?.artist,
                    artworkUri = state.avatarUri,
                    artworkData = metadata?.cover
                )
            }
    }
}
