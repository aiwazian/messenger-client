package com.aiwazian.messenger.enums

enum class PrivacyLevel(val id: Int) {
    Everybody(0),
    Nobody(1);
    
    companion object {
        fun fromId(id: Int): PrivacyLevel {
            return entries.first { it.id == id }
        }
    }
}