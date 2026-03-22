/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.PrivacyLevel

data class PrivacySettings(
    val bio: Int = PrivacyLevel.Everybody.ordinal,
    val dateOfBirth: Int = PrivacyLevel.Everybody.ordinal,
    val lastSeen: Int = PrivacyLevel.Everybody.ordinal,
    val messages: Int = PrivacyLevel.Everybody.ordinal,
    val invites: Int = PrivacyLevel.Everybody.ordinal
)
