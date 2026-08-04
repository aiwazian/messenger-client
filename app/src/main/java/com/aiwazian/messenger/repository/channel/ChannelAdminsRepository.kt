/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository.channel

import android.util.Log
import com.aiwazian.messenger.domain.ChannelAdmin
import com.aiwazian.messenger.domain.ChatAdminPermissions
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.network.api.ChannelApi
import com.aiwazian.messenger.network.dto.UpsertChannelAdminRequestDto
import javax.inject.Inject

/**
 * Администраторы канала.
 *
 * В каналах тегов нет: это только групповая возможность.
 */
class ChannelAdminsRepository @Inject constructor(
    private val channelApi: ChannelApi
) {
    
    suspend fun getAdmins(channelId: Long): Result<List<ChannelAdmin>> {
        return try {
            val response = channelApi.getAdmins(channelId)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                Result.success(dtos.map { dto ->
                    ChannelAdmin(
                        userId = dto.userId.toLongOrNull() ?: 0L,
                        firstName = dto.firstName.orEmpty(),
                        lastName = dto.lastName,
                        username = dto.username,
                        canManageInviteLinks = dto.canManageInviteLinks,
                        canEditProfile = dto.canEditProfile,
                        canManageAdmins = dto.canManageAdmins
                    )
                })
            } else {
                Result.failure(Exception("Failed to get channel admins"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting admins for channel $channelId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Кого можно назначить администратором.
     *
     * Владельца сервер в списке не отдаёт: у него и так все права.
     */
    suspend fun getAdminCandidates(channelId: Long): Result<List<User>> {
        return try {
            val response = channelApi.getAdminCandidates(channelId)
            if (response.isSuccessful) {
                Result.success(response.body().orEmpty().map { it.toDomain() })
            } else {
                Result.failure(Exception("Failed to get channel admin candidates"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting admin candidates for channel $channelId", e)
            Result.failure(e)
        }
    }
    
    suspend fun getMyPermissions(channelId: Long): Result<ChatAdminPermissions> {
        return try {
            val response = channelApi.getMyChannelPermissions(channelId)
            val dto = response.body()
            if (response.isSuccessful && dto != null) {
                Result.success(
                    ChatAdminPermissions(
                        isOwner = dto.isOwner,
                        isAdmin = dto.isAdmin,
                        canManageInviteLinks = dto.canManageInviteLinks,
                        canEditProfile = dto.canEditProfile,
                        canManageAdmins = dto.canManageAdmins
                    )
                )
            } else {
                Result.failure(Exception("Failed to get my channel permissions"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting my permissions for channel $channelId", e)
            Result.failure(e)
        }
    }
    
    suspend fun upsertAdmin(
        channelId: Long,
        userId: Long,
        canManageInviteLinks: Boolean,
        canEditProfile: Boolean,
        canManageAdmins: Boolean
    ): Result<Unit> {
        return try {
            val response = channelApi.upsertAdmin(
                channelId = channelId,
                userId = userId,
                request = UpsertChannelAdminRequestDto(
                    canManageInviteLinks = canManageInviteLinks,
                    canEditProfile = canEditProfile,
                    canManageAdmins = canManageAdmins
                )
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Upsert channel admin failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error upserting admin in channel $channelId", e)
            Result.failure(e)
        }
    }
    
    suspend fun removeAdmin(channelId: Long, userId: Long): Result<Unit> {
        return try {
            val response = channelApi.removeAdmin(channelId, userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Remove channel admin failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing admin in channel $channelId", e)
            Result.failure(e)
        }
    }
    
    private companion object {
        const val TAG = "ChannelAdminsRepository"
    }
}
