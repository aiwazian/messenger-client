/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository.group

import android.util.Log
import com.aiwazian.messenger.domain.InviteLink
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.network.api.GroupApi
import com.aiwazian.messenger.network.dto.AddMembersRequestDto
import com.aiwazian.messenger.network.dto.CreateInviteLinkRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Участники группы: состав, приглашения, блокировки, заявки и ссылки-приглашения.
 */
class GroupMembersRepository @Inject constructor(
    private val groupApi: GroupApi,
    private val crudRepository: GroupCrudRepository
) {
    
    fun getMembers(
        id: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Flow<List<User>> = flow {
        try {
            val response = groupApi.getGroupMembers(id, skip, take, search)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                emit(dtos.map { it.toDomain() })
            } else {
                Log.e(TAG, "Failed to get members for group $id: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching members for group $id", e)
        }
    }
    
    fun getAvailableUsersForInvite(id: Long): Flow<List<User>> = flow {
        try {
            val response = groupApi.getAvailableUsersForInvite(id)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                emit(dtos.map { it.toDomain() })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении доступных пользователей", e)
        }
    }
    
    suspend fun addMembers(groupId: Long, userIds: List<Long>): Result<Unit> {
        return try {
            val request = AddMembersRequestDto(
                userIds = userIds.map { it.toString() }
            )
            val response = groupApi.addMembers(groupId, request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Add members failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении участников", e)
            Result.failure(e)
        }
    }
    
    suspend fun getBannedUsers(
        id: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Result<List<User>> {
        return try {
            val response = groupApi.getBlackList(id, skip, take, search)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                Result.success(dtos.map { it.toDomain() })
            } else {
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching blacklist for group $id", e)
            Result.failure(e)
        }
    }
    
    suspend fun join(id: Long): Result<Unit> {
        return try {
            val response = groupApi.joinGroup(id)
            if (response.isSuccessful) {
                crudRepository.fetchById(id)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Join failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при присоединении к группе", e)
            Result.failure(e)
        }
    }
    
    suspend fun leave(id: Long): Result<Unit> {
        return try {
            val response = groupApi.leaveGroup(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Leave failed ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при выходе из группы", e)
            Result.failure(e)
        }
    }
    
    suspend fun kickUser(groupId: Long, userId: Long): Result<Unit> {
        return try {
            val response = groupApi.kickUser(groupId, userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Kick failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при исключении пользователя", e)
            Result.failure(e)
        }
    }
    
    suspend fun banUser(groupId: Long, userId: Long): Result<Unit> {
        return try {
            val response = groupApi.banUser(groupId, userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Ban failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при бане пользователя", e)
            Result.failure(e)
        }
    }
    
    suspend fun unban(groupId: Long, userId: Long): Result<Unit> {
        return try {
            val response = groupApi.unbanUser(groupId, userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unban failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при разбане пользователя", e)
            Result.failure(e)
        }
    }
    
    suspend fun getJoinRequests(
        groupId: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Result<List<User>> {
        return try {
            val response = groupApi.getJoinRequests(groupId, skip, take, search)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                Result.success(dtos.map { it.toDomain() })
            } else {
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching join requests for group $groupId", e)
            Result.failure(e)
        }
    }
    
    suspend fun acceptJoinRequest(groupId: Long, userId: Long): Result<Unit> {
        return try {
            val response = groupApi.acceptJoinRequest(groupId, userId)
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
    
    suspend fun rejectJoinRequest(groupId: Long, userId: Long): Result<Unit> {
        return try {
            val response = groupApi.rejectJoinRequest(groupId, userId)
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
    
    suspend fun getInviteLinks(groupId: Long): Result<List<InviteLink>> {
        return try {
            val response = groupApi.getInviteLinks(groupId)
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
        groupId: Long,
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
            val response = groupApi.createInviteLink(groupId, request)
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
    
    suspend fun deleteInviteLink(groupId: Long, inviteLinkId: Long): Result<Unit> {
        return try {
            val response = groupApi.deleteInviteLink(groupId, inviteLinkId)
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
        const val TAG = "GroupMembersRepository"
    }
}
