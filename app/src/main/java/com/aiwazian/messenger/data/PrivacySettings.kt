package com.aiwazian.messenger.data

import androidx.annotation.Keep
import com.aiwazian.messenger.enums.PrivacyLevel

@Keep
data class PrivacySettings(
    @Keep val bio: Int = PrivacyLevel.Everybody.ordinal,
    @Keep val dateOfBirth: Int = PrivacyLevel.Everybody.ordinal,
    @Keep val lastSeen: Int = PrivacyLevel.Everybody.ordinal,
    @Keep val messages: Int = PrivacyLevel.Everybody.ordinal,
    @Keep val invites: Int = PrivacyLevel.Everybody.ordinal
)