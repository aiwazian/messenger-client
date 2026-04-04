/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

enum class ChannelType {
    PUBLIC,
    PRIVATE;
    
    companion object {
        fun fromOrdinal(value: Int): ChannelType {
            return entries.first { it.ordinal == value }
        }
    }
}
