package com.aiwazian.messenger.enums

enum class ChatType {
    PRIVATE,
    GROUP,
    CHANNEL,
    UNKNOWN;
    
    companion object {
        fun fromOrdinal(ordinal: Int): ChatType {
            return entries.firstOrNull { it.ordinal == ordinal } ?: UNKNOWN
        }
        
        fun fromId(id: Long): ChatType {
            val idString = id.toString()
            val firstDigit = idString[0].digitToInt()
            
            return when (firstDigit) {
                1-> PRIVATE
                2-> CHANNEL
                3-> GROUP
                else -> UNKNOWN
            }
        }
    }
}