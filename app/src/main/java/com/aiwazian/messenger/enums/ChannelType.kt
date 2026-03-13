package com.aiwazian.messenger.enums

enum class ChannelType {
    PUBLIC,
    PRIVATE;
    
    companion object {
        fun fromInt(value: Int): ChannelType {
            return entries.first { it.ordinal == value }
        }
    }
}