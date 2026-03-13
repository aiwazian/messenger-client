package com.aiwazian.messenger.utils

object ChatState {
    
    private var openChatId: Long? = null

    fun openChat(chatId: Long) {
        openChatId = chatId
    }

    fun closeChat() {
        openChatId = null
    }

    fun isChatOpen(chatId: Long?): Boolean {
        return openChatId == chatId
    }
}
