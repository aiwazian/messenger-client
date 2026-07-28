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
    val invites: PrivacyLevel,
    val profilePhoto: PrivacyLevel,
    /**
     * Кто может перейти в мой профиль по заголовку «Переслано от».
     *
     * NOBODY — сервер отдаёт таким пересылкам access = RESTRICTED,
     * и вместо перехода в чат показывается тултип.
     */
    val forwardedProfile: PrivacyLevel = PrivacyLevel.EVERYBODY,
    val deleteAfterDays: Int
)
