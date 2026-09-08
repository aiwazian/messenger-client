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
    
    fun canCopyText(isMine: Boolean): Boolean = isAllowedFor(isMine)
    
    fun canForward(isMine: Boolean): Boolean = isAllowedFor(isMine)
    
    fun hasNotice(isMine: Boolean): Boolean = noCopy || (peerNoCopy && !isMine)
    
    private fun isAllowedFor(isMine: Boolean): Boolean {
        if (noCopy) return false
        return isMine || !peerNoCopy
    }
    
    companion object {
        val Unrestricted = ChatCopyPolicy()
    }
}
