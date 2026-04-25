/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

enum class ChatType {
    PRIVATE,
    GROUP,
    CHANNEL,
    UNKNOWN;
    
    companion object {
        fun fromId(id: Long): ChatType {
            if (id == -1L) return UNKNOWN
            
            val firstDigit = id.toString().firstOrNull()?.digitToInt()
            
            return when (firstDigit) {
                1 -> PRIVATE
                2 -> CHANNEL
                3 -> GROUP
                else -> UNKNOWN
            }
        }
    }
}
