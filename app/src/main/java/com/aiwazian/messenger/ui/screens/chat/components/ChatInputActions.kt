/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.net.Uri
import com.aiwazian.messenger.domain.MessageReplyPreview
interface ChatInputActions {
    fun onKeyboardHeightChanged(height: Float)

    fun changeText(text: String)

    fun onSendMessageClicked()

    fun onSendWithoutSoundClicked()

    fun onJoinClicked()

    fun sendFiles(uris: List<Uri>)

    fun sendMediaUris(uris: List<Uri>, caption: String, replyTo: MessageReplyPreview?)

    fun sendSticker(stickerId: Long)

    fun startRecording()

    fun onMicrophonePermissionDenied()

    fun lockRecording()

    fun cancelRecording()

    fun stopRecordingAndSend()

    fun cancelReply()

    fun onReplyPanelClicked()

    fun cancelEditing()

    fun showBlockDialog()
}
