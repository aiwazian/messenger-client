package com.aiwazian.messenger.ui.screens.chat

data class ChatCopyPolicy(
    val noCopy: Boolean = false,
    val peerNoCopy: Boolean = false
) {

    val isRestricted: Boolean
        get() = noCopy || peerNoCopy

    val canSaveMedia: Boolean
        get() = !isRestricted

    val canTakeScreenshot: Boolean
        get() = !isRestricted

    fun canCopyText(isMine: Boolean): Boolean = !isRestricted

    fun canForward(isMine: Boolean): Boolean = !isRestricted

    fun hasNotice(isMine: Boolean): Boolean = isRestricted

    companion object {
        val Unrestricted = ChatCopyPolicy()
    }
}
