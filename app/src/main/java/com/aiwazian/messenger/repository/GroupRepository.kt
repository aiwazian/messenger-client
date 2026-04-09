/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.network.api.GroupApi
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.domain.Group
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.domain.InviteLink
import com.aiwazian.messenger.database.dao.GroupDao
import com.aiwazian.messenger.network.dto.CreateGroupRequestDto
import com.aiwazian.messenger.network.dto.UpdateGroupRequestDto
import com.aiwazian.messenger.network.dto.CreateInviteLinkRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class GroupRepository @Inject constructor(
    private val groupApi: GroupApi,
    private val groupDao: GroupDao
) {

    suspend fun create(groupInfo: Group): Result<Long> {
        return try {
            val request = CreateGroupRequestDto(
                name = groupInfo.name,
                bio = groupInfo.bio
            )
            val response = groupApi.createGroup(request)
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    val group = dto.toDomain()
                    groupDao.insert(group.toEntity())
                    Result.success(group.id)
                } else {
                    Result.failure(Exception("No group returned"))
                }
            } else {
                Result.failure(Exception("Create failed"))
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Ошибка при создании группы", e)
            Result.failure(e)
        }
    }

    fun getById(id: Long): Flow<Group> = groupDao.get(id)
        .mapNotNull { it?.toDomain() }
        .onStart {
            try {
                val response = groupApi.getGroupById(id)
                if (response.isSuccessful) {
                    val dto = response.body()
                    if (dto != null) {
                        val group = dto.toDomain()
                        groupDao.insert(group.toEntity())
                    }
                }
            } catch (e: Exception) {
                Log.e("GroupRepository", "Ошибка при получении группы", e)
            }
        }

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
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Ошибка при получении участников группы", e)
        }
    }

    suspend fun update(group: Group): Result<Unit> {
        return try {
            val request = UpdateGroupRequestDto(
                name = group.name,
                bio = group.bio,
                groupType = group.groupType,
                username = group.username
            )
            val response = groupApi.updateGroup(group.id, request)
            if (response.isSuccessful) {
                groupDao.insert(group.toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("Update failed"))
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Ошибка при обновлении группы", e)
            Result.failure(e)
        }
    }

    suspend fun delete(id: Long): Result<Unit> {
        return try {
            val response = groupApi.deleteGroup(id)
            if (response.isSuccessful) {
                groupDao.delete(id)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete failed"))
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Ошибка при удалении группы", e)
            Result.failure(e)
        }
    }

    suspend fun join(id: Long): Result<Unit> {
        return try {
            val response = groupApi.joinGroup(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Join failed"))
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Ошибка при присоединении к группе", e)
            Result.failure(e)
        }
    }

    suspend fun leave(id: Long): Result<Unit> {
        return try {
            val response = groupApi.leaveGroup(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Leave failed"))
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Ошибка при выходе из группы", e)
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
            Log.e("GroupRepository", "Ошибка при выгонении пользователя", e)
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
            Log.e("GroupRepository", "Ошибка при бане пользователя", e)
            Result.failure(e)
        }
    }

    fun getBlackList(
        id: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Flow<List<User>> = flow {
        try {
            val response = groupApi.getBlackList(id, skip, take, search)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                emit(dtos.map { it.toDomain() })
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Ошибка при получении черного списка группы", e)
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
            Log.e("GroupRepository", "Ошибка при разбане пользователя", e)
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
            Log.e("GroupRepository", "Error getting invite links", e)
            Result.failure(e)
        }
    }

    suspend fun createInviteLink(groupId: Long, maxUses: Int?, expiresInSeconds: Int? = null): Result<InviteLink> {
        return try {
            val request = CreateInviteLinkRequestDto(
                maxUses = maxUses,
                expiresInSeconds = expiresInSeconds
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
            Log.e("GroupRepository", "Error creating invite link", e)
            Result.failure(e)
        }
    }
}
