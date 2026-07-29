/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository.channel

import android.util.Log
import com.aiwazian.messenger.domain.InviteLink
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.network.api.ChannelApi
import com.aiwazian.messenger.network.dto.CreateInviteLinkRequestDto
import javax.inject.Inject

/**
 * Участники канала: подписки, блокировки, заявки на вступление и ссылки-приглашения.
 */
class ChannelMembersRepository @Inject constructor(
    private val channelApi: ChannelApi
) {
    
    suspend fun getSubscribers(
        channelId: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Result<List<User>> {
        return try {
            val response = channelApi.getChannelSubscribers(channelId, skip, take, search)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                Result.success(dtos.map { it.toDomain() })
            } else {
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subscribers", e)
            Result.failure(e)
        }
    }
    
    suspend fun join(channelId: Long): Result<Unit> {
        return try {
            val response = channelApi.joinChannel(channelId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Join failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error joining channel", e)
            Result.failure(e)
        }
    }
    
    suspend fun leave(channelId: Long): Result<Unit> {
        return try {
            val response = channelApi.leaveChannel(channelId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Leave failed ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving channel", e)
            Result.failure(e)
        }
    }
    
    suspend fun kickUser(channelId: Long, userId: Long): Result<Unit> {
        return try {
            val response = channelApi.kickUser(channelId, userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Kick failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error kicking user", e)
            Result.failure(e)
        }
    }
    
    suspend fun getBannedUsers(
        channelId: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Result<List<User>> {
        return try {
            val response = channelApi.getBannedUsers(channelId, skip, take, search)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                Result.success(dtos.map { it.toDomain() })
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении заблокированных пользователей", e)
            Result.failure(e)
        }
    }
    
    suspend fun banUser(channelId: Long, userId: Long): Result<Unit> {
        return try {
            val response = channelApi.banUser(channelId, userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Ban failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при блокировке пользователя", e)
            Result.failure(e)
        }
    }
    
    suspend fun unbanUser(channelId: Long, userId: Long): Result<Unit> {
        return try {
            val response = channelApi.unbanUser(channelId, userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unban failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при разблокировке пользователя", e)
            Result.failure(e)
        }
    }
    
    suspend fun getJoinRequests(
        channelId: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Result<List<User>> {
        return try {
            val response = channelApi.getJoinRequests(channelId, skip, take, search)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                Result.success(dtos.map { it.toDomain() })
            } else {
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching join requests for channel $channelId", e)
            Result.failure(e)
        }
    }
    
    suspend fun acceptJoinRequest(channelId: Long, userId: Long): Result<Unit> {
        return try {
            val response = channelApi.acceptJoinRequest(channelId, userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Accept join request failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при принятии заявки", e)
            Result.failure(e)
        }
    }
    
    suspend fun rejectJoinRequest(channelId: Long, userId: Long): Result<Unit> {
        return try {
            val response = channelApi.rejectJoinRequest(channelId, userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Reject join request failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при отклонении заявки", e)
            Result.failure(e)
        }
    }
    
    suspend fun getInviteLinks(channelId: Long): Result<List<InviteLink>> {
        return try {
            val response = channelApi.getInviteLinks(channelId)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                Result.success(dtos.map { it.toDomain() })
            } else {
                Result.failure(Exception("Failed to get invite links"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting invite links", e)
            Result.failure(e)
        }
    }
    
    suspend fun createInviteLink(
        channelId: Long,
        maxUses: Int?,
        expiresAt: Long? = null,
        requireApproval: Boolean = false
    ): Result<InviteLink> {
        return try {
            val request = CreateInviteLinkRequestDto(
                maxUses = maxUses,
                expiresAt = expiresAt,
                requireApproval = requireApproval
            )
            val response = channelApi.createInviteLink(channelId, request)
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    Result.success(dto.toDomain())
                } else {
                    Result.failure(Exception("No invite link returned"))
                }
            } else {
                Result.failure(Exception("Create invite link failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating invite link", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteInviteLink(channelId: Long, inviteLinkId: Long): Result<Unit> {
        return try {
            val response = channelApi.deleteInviteLink(channelId, inviteLinkId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete invite link failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting invite link", e)
            Result.failure(e)
        }
    }
    
    private companion object {
        const val TAG = "ChannelMembersRepository"
    }
}
