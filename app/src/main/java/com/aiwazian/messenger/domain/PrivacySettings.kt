package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.PrivacyLevel

data class PrivacySettings(
    val bio: PrivacyLevel,
    val dateOfBirth: PrivacyLevel,
    val lastSeen: PrivacyLevel,
    val messages: PrivacyLevel,
    val invites: PrivacyLevel,
    val profilePhoto: PrivacyLevel,
    val forwardedProfile: PrivacyLevel = PrivacyLevel.EVERYBODY,
    val forwardAndCopy: PrivacyLevel = PrivacyLevel.EVERYBODY,
    val deleteAfterDays: Int
)
