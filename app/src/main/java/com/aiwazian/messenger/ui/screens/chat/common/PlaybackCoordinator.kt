/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.common

import android.content.Context
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.AudioTrackMetadata
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
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

interface PlaybackHost {
    val attachments: List<MessageAttachment>
    val audioMetadata: Map<String, AudioTrackMetadata>
    val voiceTitle: String
    val artworkUri: Uri?
    val currentPlayingVoiceFileId: String?
    val currentMusicFileId: String?

    fun onVoicePlayerState(
        currentFileId: String?, isPlaying: Boolean, positionMs: Int, durationMs: Int
    )

    fun onMusicPlayerState(
        fileId: String?, title: String, artist: String?, isPlaying: Boolean,
        positionMs: Int, durationMs: Int
    )

    fun onAudioMetadata(fileId: String, metadata: AudioTrackMetadata)
}

class PlaybackCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val voicePlayerManager: VoicePlayerManager,
    private val musicPlayerManager: MusicPlayerManager,
    private val hostProvider: () -> PlaybackHost
) {
    private val pendingVoiceStartPositions = mutableMapOf<String, Int>()
    private val pendingMusicStartPositions = mutableMapOf<String, Int>()
    private val pendingMetadataRequests = mutableSetOf<String>()

    fun connect() {
        voicePlayerManager.connect()
        musicPlayerManager.connect()

        scope.launch {
            voicePlayerManager.state.collect { state ->
                hostProvider().onVoicePlayerState(
                    currentFileId = state.currentFileId,
                    isPlaying = state.isPlaying,
                    positionMs = state.positionMs,
                    durationMs = state.durationMs
                )
            }
        }

        scope.launch {
            musicPlayerManager.state.collect { state ->
                hostProvider().onMusicPlayerState(
                    fileId = state.fileId,
                    title = state.title,
                    artist = state.artist,
                    isPlaying = state.isPlaying,
                    positionMs = state.positionMs,
                    durationMs = state.durationMs
                )
            }
        }
    }

    fun handlePlayVoice(file: MessageAttachment) {
        if (file.type != AttachmentType.VOICE || file.localUri == null) return
        musicPlayerManager.stop()
        if (hostProvider().currentPlayingVoiceFileId == file.fileId) {
            voicePlayerManager.togglePlayPause()
        } else {
            val startPosition = pendingVoiceStartPositions.remove(file.fileId) ?: 0
            playVoice(file.fileId, startPosition)
        }
    }

    fun handlePlayMusic(file: MessageAttachment) {
        if (file.localUri == null) return

        if (hostProvider().currentMusicFileId == file.fileId) {
            musicPlayerManager.togglePlayPause()
            return
        }

        voicePlayerManager.stop()
        val startPosition = pendingMusicStartPositions.remove(file.fileId) ?: 0
        musicPlayerManager.play(buildMusicQueue(), file.fileId, startPosition)
    }

    fun onVoiceSeek(file: MessageAttachment, positionMs: Int) {
        if (hostProvider().currentPlayingVoiceFileId != file.fileId) {
            pendingVoiceStartPositions[file.fileId] = positionMs
        } else {
            voicePlayerManager.seekTo(positionMs)
        }
    }

    fun onMusicSeek(file: MessageAttachment, positionMs: Int) {
        if (hostProvider().currentMusicFileId == file.fileId) {
            musicPlayerManager.seekTo(positionMs)
        } else {
            pendingMusicStartPositions[file.fileId] = positionMs
        }
    }

    fun musicStartPosition(fileId: String): Int {
        return pendingMusicStartPositions.remove(fileId) ?: 0
    }

    fun toggleVoicePlayPause() {
        voicePlayerManager.togglePlayPause()
    }

    fun toggleMusicPlayPause() {
        musicPlayerManager.togglePlayPause()
    }

    fun buildVoiceQueue(): List<VoiceQueueItem> {
        val host = hostProvider()
        return host.attachments
            .filter { it.type == AttachmentType.VOICE }
            .map { attachment ->
                val isReady = attachment.localUri != null &&
                        (attachment.status == DownloadStatus.COMPLETED ||
                                attachment.status == DownloadStatus.UPLOADED)
                VoiceQueueItem(
                    uri = if (isReady) attachment.localUri else null,
                    fileId = attachment.fileId,
                    title = host.voiceTitle,
                    subtitle = context.getString(R.string.voice_message),
                    artworkUri = host.artworkUri
                )
            }
    }

    fun buildMusicQueue(): List<MusicTrack> {
        val host = hostProvider()
        return host.attachments
            .filter { it.extension.isMusicFile() && it.localUri != null }
            .map { attachment ->
                val metadata = host.audioMetadata[attachment.fileId]
                MusicTrack(
                    fileId = attachment.fileId,
                    uri = attachment.localUri!!,
                    title = metadata?.title ?: attachment.name,
                    artist = metadata?.artist,
                    artworkUri = host.artworkUri,
                    artworkData = metadata?.cover
                )
            }
    }

    fun observeAudioMetadata(attachmentsFlow: Flow<List<MessageAttachment>>) {
        scope.launch {
            attachmentsFlow.collect { attachments ->
                val metadata = hostProvider().audioMetadata
                attachments.forEach { attachment ->
                    val fileId = attachment.fileId
                    if (!attachment.extension.isAudioFile()) return@forEach
                    val localUri = attachment.localUri ?: return@forEach
                    if (metadata.containsKey(fileId)) return@forEach
                    if (!pendingMetadataRequests.add(fileId)) return@forEach

                    scope.launch {
                        val audioMetadata = AudioMetadataCache.get(
                            fileId = fileId,
                            filePath = localUri.path ?: localUri.toString(),
                            fallbackTitle = attachment.name
                        )
                        pendingMetadataRequests.remove(fileId)
                        if (audioMetadata != null) {
                            hostProvider().onAudioMetadata(fileId, audioMetadata)
                        }
                    }
                }
            }
        }
    }

    private fun playVoice(fileId: String, startPositionMs: Int = 0) {
        val queue = buildVoiceQueue()
        if (queue.none { it.fileId == fileId }) return
        voicePlayerManager.play(queue, fileId, startPositionMs)
    }
}
