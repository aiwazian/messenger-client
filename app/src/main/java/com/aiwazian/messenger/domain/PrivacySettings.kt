/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.enums.PrivacyLevel

data class PrivacySettings(
    val bio: PrivacyLevel = PrivacyLevel.Everybody,
    val dateOfBirth: PrivacyLevel = PrivacyLevel.Everybody,
    val lastSeen: PrivacyLevel = PrivacyLevel.Everybody,
    val messages: PrivacyLevel = PrivacyLevel.Everybody,
    val invites: PrivacyLevel = PrivacyLevel.Everybody
)
