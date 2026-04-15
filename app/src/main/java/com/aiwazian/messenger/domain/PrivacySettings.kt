/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.PrivacyLevel

data class PrivacySettings(
    val bio: PrivacyLevel = PrivacyLevel.EVERYBODY,
    val dateOfBirth: PrivacyLevel = PrivacyLevel.EVERYBODY,
    val lastSeen: PrivacyLevel = PrivacyLevel.EVERYBODY,
    val messages: PrivacyLevel = PrivacyLevel.EVERYBODY,
    val invites: PrivacyLevel = PrivacyLevel.EVERYBODY
)
