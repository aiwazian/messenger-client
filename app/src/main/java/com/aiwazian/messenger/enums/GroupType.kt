/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

enum class GroupType {
    PUBLIC,
    PRIVATE;
    
    companion object {
        fun fromOrdinal(value: Int): GroupType {
            return entries.first { it.ordinal == value }
        }
    }
}
