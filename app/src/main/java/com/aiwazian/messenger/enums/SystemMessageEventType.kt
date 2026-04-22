/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

enum class SystemMessageEventType {
    CHANNEL_CREATED,
    GROUP_CREATED,
    HISTORY_CLEARED;
    
    companion object {
        fun fromOrdinal(value: Int): SystemMessageEventType {
            return entries.first { it.ordinal == value }
        }
    }
}
