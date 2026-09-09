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
    
    fun canCopyText(): Boolean = !isRestricted
    
    fun canForward(): Boolean = !isRestricted
    
    fun hasNotice(): Boolean = isRestricted

    companion object {
        val Unrestricted = ChatCopyPolicy()
    }
}
