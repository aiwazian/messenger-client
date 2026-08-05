/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Администратор канала и его права. */
@Serializable
data class ChannelAdminResponseDto(
    @SerialName("userId") val userId: String,
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("canManageInviteLinks") val canManageInviteLinks: Boolean = false,
    @SerialName("canEditProfile") val canEditProfile: Boolean = false,
    @SerialName("canManageAdmins") val canManageAdmins: Boolean = false,
    @SerialName("grantedAt") val grantedAt: String? = null
)

/** Администратор группы, его права и тег участника. */
@Serializable
data class GroupAdminResponseDto(
    @SerialName("userId") val userId: String,
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("canManageInviteLinks") val canManageInviteLinks: Boolean = false,
    @SerialName("canEditProfile") val canEditProfile: Boolean = false,
    @SerialName("canManageAdmins") val canManageAdmins: Boolean = false,
    @SerialName("tag") val tag: String? = null,
    @SerialName("grantedAt") val grantedAt: String? = null
)

/** Назначение администратора канала или изменение его прав. */
@Serializable
data class UpsertChannelAdminRequestDto(
    @SerialName("canManageInviteLinks") val canManageInviteLinks: Boolean = false,
    @SerialName("canEditProfile") val canEditProfile: Boolean = false,
    @SerialName("canManageAdmins") val canManageAdmins: Boolean = false
)

/**
 * Назначение администратора группы или изменение его прав.
 *
 * Пустой tag очищает тег участника.
 */
@Serializable
data class UpsertGroupAdminRequestDto(
    @SerialName("canManageInviteLinks") val canManageInviteLinks: Boolean = false,
    @SerialName("canEditProfile") val canEditProfile: Boolean = false,
    @SerialName("canManageAdmins") val canManageAdmins: Boolean = false,
    @SerialName("tag") val tag: String? = null
)

/** Права текущего пользователя в канале или группе. */
@Serializable
data class ChatAdminPermissionsResponseDto(
    @SerialName("isOwner") val isOwner: Boolean = false,
    @SerialName("isAdmin") val isAdmin: Boolean = false,
    @SerialName("canManageInviteLinks") val canManageInviteLinks: Boolean = false,
    @SerialName("canEditProfile") val canEditProfile: Boolean = false,
    @SerialName("canManageAdmins") val canManageAdmins: Boolean = false,
    @SerialName("tag") val tag: String? = null
)

/** Тег участника группы: показывается рядом с именем отправителя. */
@Serializable
data class GroupMemberTagResponseDto(
    @SerialName("userId") val userId: String,
    @SerialName("tag") val tag: String
)
