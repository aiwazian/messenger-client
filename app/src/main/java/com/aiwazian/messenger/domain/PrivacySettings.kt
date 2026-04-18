/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.PrivacyLevel

data class PrivacySettings(
    val bio: PrivacyLevel,
    val dateOfBirth: PrivacyLevel,
    val lastSeen: PrivacyLevel,
    val messages: PrivacyLevel,
    val invites: PrivacyLevel
)
