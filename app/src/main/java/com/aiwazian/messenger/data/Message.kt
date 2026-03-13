package com.aiwazian.messenger.data

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Message(
    @Keep var id: Int = 0,
    @Keep val senderId: Long = 0,
    @Keep val chatId: Long = 0,
    @Keep val text: String? = null,
    @Keep val sendTime: Long = 0,
    @Keep var isRead: Boolean = false,
    @Keep val attachments: Array<Attachment> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as Message
        
        if (id != other.id) return false
        if (senderId != other.senderId) return false
        if (chatId != other.chatId) return false
        if (sendTime != other.sendTime) return false
        if (isRead != other.isRead) return false
        if (text != other.text) return false
        if (!attachments.contentEquals(other.attachments)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = id
        result = 31 * result + senderId.hashCode()
        result = 31 * result + chatId.hashCode()
        result = 31 * result + sendTime.hashCode()
        result = 31 * result + isRead.hashCode()
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + attachments.contentHashCode()
        return result
    }
}
