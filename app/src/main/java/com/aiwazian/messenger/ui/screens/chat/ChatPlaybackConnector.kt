/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.content.Context
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.AudioTrackMetadata
import com.aiwazian.messenger.domain.MessageAttachment
import com.aiwazian.messenger.playback.MusicPlayerManager
import com.aiwazian.messenger.playback.VoicePlayerManager
import com.aiwazian.messenger.playback.VoiceQueueItem
import com.aiwazian.messenger.ui.screens.chat.common.PlaybackCoordinator
import com.aiwazian.messenger.ui.screens.chat.common.PlaybackHost
import com.aiwazian.messenger.utils.EqualizerManager
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ChatPlaybackConnector(
    private val context: Context,
    private val scope: CoroutineScope,
    private val stateFlow: StateFlow<ChatUiState>,
    private val updateState: ((ChatUiState) -> ChatUiState) -> Unit,
    private val voicePlayerManager: VoicePlayerManager,
    private val musicPlayerManager: MusicPlayerManager,
    private val equalizerManager: EqualizerManager
) {    private val coordinator = PlaybackCoordinator(
        context = context,
        scope = scope,
        voicePlayerManager = voicePlayerManager,
        musicPlayerManager = musicPlayerManager,
        hostProvider = { host() }
    )

    private fun host(): PlaybackHost {
        val state = stateFlow.value
        return object : PlaybackHost {
            override val attachments = state.chatItems.asReversed()
                .filterIsInstance<ChatItem.MessageItem>()
                .flatMap { it.message.attachments }

            override val audioMetadata = state.audioMetadata

            override val voiceTitle = state.chatName.asString(context)
                .ifBlank { context.getString(R.string.voice_message) }

            override val artworkUri = state.avatarUri

            override val currentPlayingVoiceFileId = state.currentPlayingVoiceFileId

            override val currentMusicFileId = state.currentMusicFileId

            override fun onVoicePlayerState(
                currentFileId: String?, isPlaying: Boolean, positionMs: Int, durationMs: Int
            ) {
                updateState {
                    it.copy(
                        currentPlayingVoiceFileId = currentFileId,
                        isVoicePlaying = isPlaying,
                        voicePositionMs = positionMs,
                        voiceDurationMs = durationMs
                    )
                }
            }

            override fun onMusicPlayerState(
                fileId: String?, title: String, artist: String?, isPlaying: Boolean,
                positionMs: Int, durationMs: Int
            ) {
                updateState {
                    it.copy(
                        currentMusicFileId = fileId,
                        currentMusicTitle = title,
                        currentMusicArtist = artist,
                        isMusicPlaying = isPlaying,
                        musicPositionMs = positionMs,
                        musicDurationMs = durationMs
                    )
                }
            }

            override fun onAudioMetadata(fileId: String, metadata: AudioTrackMetadata) {
                updateState {
                    it.copy(audioMetadata = it.audioMetadata + (fileId to metadata))
                }
            }
        }
    }

    fun observe() {
        coordinator.connect()
        observeVoicePlayerInfo()
        observeMusicQueueUpdates()
        observeEqualizer()
        observeQueueUpdates()
        coordinator.observeAudioMetadata(attachmentsFlow())
    }

    fun handlePlayVoice(file: MessageAttachment) = coordinator.handlePlayVoice(file)

    fun handlePlayMusic(file: MessageAttachment) = coordinator.handlePlayMusic(file)

    fun onVoiceSeek(file: MessageAttachment, positionMs: Int) =
        coordinator.onVoiceSeek(file, positionMs)

    fun onMusicSeek(file: MessageAttachment, positionMs: Int) =
        coordinator.onMusicSeek(file, positionMs)

    fun musicStartPosition(fileId: String): Int = coordinator.musicStartPosition(fileId)

    fun updateMusicQueue() {
        musicPlayerManager.updateQueue(coordinator.buildMusicQueue())
    }

    fun voiceQueue(): List<VoiceQueueItem> = coordinator.buildVoiceQueue()

    private fun attachmentsFlow(): Flow<List<MessageAttachment>> {
        return stateFlow.map { state ->
            state.chatItems.asReversed()
                .filterIsInstance<ChatItem.MessageItem>()
                .flatMap { it.message.attachments }
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

    private fun observeMusicQueueUpdates() {
        scope.launch {
            stateFlow.collect { state ->
                if (state.currentMusicFileId == null) return@collect
                musicPlayerManager.updateQueue(coordinator.buildMusicQueue())
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

    private fun observeQueueUpdates() {
        scope.launch {
            stateFlow.collect { state ->
                val playingId = state.currentPlayingVoiceFileId ?: return@collect
                val queue = coordinator.buildVoiceQueue()
                if (queue.any { it.fileId == playingId }) {
                    voicePlayerManager.updateQueue(queue)
                }
            }
        }
    }

}
