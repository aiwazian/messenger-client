/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

/** Администратор канала. */
data class ChannelAdmin(
    val userId: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    /** Пригласительные ссылки: создание и удаление. */
    val canManageInviteLinks: Boolean,
    /** Изменение профиля канала: название, описание и фотографии. */
    val canEditProfile: Boolean
)

/** Администратор группы. Тег есть только в группах. */
data class GroupAdmin(
    val userId: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val canManageInviteLinks: Boolean,
    val canEditProfile: Boolean,
    /** Тег участника: подпись рядом с именем отправителя. */
    val tag: String?
)

/**
 * Права текущего пользователя в канале или группе.
 *
 * У владельца всегда все права.
 */
data class ChatAdminPermissions(
    val isOwner: Boolean = false,
    val isAdmin: Boolean = false,
    val canManageInviteLinks: Boolean = false,
    val canEditProfile: Boolean = false,
    val tag: String? = null
)
