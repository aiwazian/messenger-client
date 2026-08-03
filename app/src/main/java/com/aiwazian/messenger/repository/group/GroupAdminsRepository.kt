/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository.group

import android.util.Log
import com.aiwazian.messenger.domain.ChatAdminPermissions
import com.aiwazian.messenger.domain.GroupAdmin
import com.aiwazian.messenger.network.api.GroupApi
import com.aiwazian.messenger.network.dto.UpsertGroupAdminRequestDto
import javax.inject.Inject

/**
 * Администраторы группы и теги участников.
 *
 * Список администраторов сервер отдаёт только владельцу, теги — любому участнику.
 */
class GroupAdminsRepository @Inject constructor(
    private val groupApi: GroupApi
) {
    
    suspend fun getAdmins(groupId: Long): Result<List<GroupAdmin>> {
        return try {
            val response = groupApi.getAdmins(groupId)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                Result.success(dtos.map { dto ->
                    GroupAdmin(
                        userId = dto.userId.toLongOrNull() ?: 0L,
                        firstName = dto.firstName.orEmpty(),
                        lastName = dto.lastName,
                        username = dto.username,
                        canManageInviteLinks = dto.canManageInviteLinks,
                        canEditProfile = dto.canEditProfile,
                        tag = dto.tag
                    )
                })
            } else {
                Result.failure(Exception("Failed to get group admins"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting admins for group $groupId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Теги участников группы.
     *
     * Отдаётся готовой картой id → тег: экран чата кэширует её рядом с именами.
     */
    suspend fun getMemberTags(groupId: Long): Result<Map<Long, String>> {
        return try {
            val response = groupApi.getMemberTags(groupId)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                Result.success(
                    dtos.mapNotNull { dto ->
                        val userId = dto.userId.toLongOrNull() ?: return@mapNotNull null
                        userId to dto.tag
                    }.toMap()
                )
            } else {
                Result.failure(Exception("Failed to get member tags"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting member tags for group $groupId", e)
            Result.failure(e)
        }
    }
    
    suspend fun getMyPermissions(groupId: Long): Result<ChatAdminPermissions> {
        return try {
            val response = groupApi.getMyGroupPermissions(groupId)
            val dto = response.body()
            if (response.isSuccessful && dto != null) {
                Result.success(
                    ChatAdminPermissions(
                        isOwner = dto.isOwner,
                        isAdmin = dto.isAdmin,
                        canManageInviteLinks = dto.canManageInviteLinks,
                        canEditProfile = dto.canEditProfile,
                        tag = dto.tag
                    )
                )
            } else {
                Result.failure(Exception("Failed to get my group permissions"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting my permissions for group $groupId", e)
            Result.failure(e)
        }
    }
    
    /** Назначает администратора или перезаписывает его права и тег. */
    suspend fun upsertAdmin(
        groupId: Long,
        userId: Long,
        canManageInviteLinks: Boolean,
        canEditProfile: Boolean,
        tag: String?
    ): Result<Unit> {
        return try {
            val response = groupApi.upsertAdmin(
                groupId = groupId,
                userId = userId,
                request = UpsertGroupAdminRequestDto(
                    canManageInviteLinks = canManageInviteLinks,
                    canEditProfile = canEditProfile,
                    tag = tag?.trim()?.ifBlank { null }
                )
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Upsert group admin failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error upserting admin in group $groupId", e)
            Result.failure(e)
        }
    }
    
    suspend fun removeAdmin(groupId: Long, userId: Long): Result<Unit> {
        return try {
            val response = groupApi.removeAdmin(groupId, userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Remove group admin failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing admin in group $groupId", e)
            Result.failure(e)
        }
    }
    
    private companion object {
        const val TAG = "GroupAdminsRepository"
    }
}
