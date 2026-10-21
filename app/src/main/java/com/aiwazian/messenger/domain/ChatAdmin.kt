/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class ChannelAdmin(
    val userId: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val canManageInviteLinks: Boolean,
    val canEditProfile: Boolean,
    val canManageAdmins: Boolean,
    val canPinMessages: Boolean,
    val canDeleteComments: Boolean = false
)

data class GroupAdmin(
    val userId: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val canManageInviteLinks: Boolean,
    val canEditProfile: Boolean,
    val canManageAdmins: Boolean,
    val canPinMessages: Boolean,
    val tag: String?
)

data class ChatAdminPermissions(
    val isOwner: Boolean = false,
    val isAdmin: Boolean = false,
    val canManageInviteLinks: Boolean = false,
    val canEditProfile: Boolean = false,
    val canManageAdmins: Boolean = false,
    val canPinMessages: Boolean = false,
    val canDeleteComments: Boolean = false,
    val tag: String? = null
)
