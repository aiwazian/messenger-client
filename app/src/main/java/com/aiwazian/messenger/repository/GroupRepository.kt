/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import androidx.core.net.toUri
import com.aiwazian.messenger.database.dao.AvatarDao
import com.aiwazian.messenger.database.dao.GroupDao
import com.aiwazian.messenger.domain.Group
import com.aiwazian.messenger.domain.InviteLink
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.mappers.toGroupEntity
import com.aiwazian.messenger.network.api.GroupApi
import com.aiwazian.messenger.network.dto.AddMembersRequestDto
import com.aiwazian.messenger.network.dto.CreateGroupRequestDto
import com.aiwazian.messenger.network.dto.CreateInviteLinkRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.UpdateGroupRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GroupRepository @Inject constructor(
    private val groupApi: GroupApi,
    private val groupDao: GroupDao,
    private val avatarDao: AvatarDao
) {
    
    fun getById(id: Long): Flow<Group> = getByIdOrNull(id).filterNotNull()
    
    fun getByIdOrNull(id: Long): Flow<Group?> =
        groupDao.getWithAvatarsFlow(id).map { groupWithAvatars ->
            groupWithAvatars ?: return@map null
            val avatars =
                groupWithAvatars.avatars.sortedBy { it.avatar.sortOrder }.map { avatarWithFile ->
                    val uri = if (!avatarWithFile.file?.path.isNullOrBlank()) {
                        avatarWithFile.file.path.toUri()
                    } else {
                        null
                    }
                    avatarWithFile.avatar.toDomain(uri)
                }
            groupWithAvatars.group.toDomain(avatars)
        }
    
    suspend fun fetchById(groupId: Long) {
        try {
            val response = groupApi.getGroupById(groupId)
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    val group = dto.toDomain()
                    groupDao.insert(group.toEntity())
                    
                    val avatars = dto.avatars.map { it.toGroupEntity(group.id) }
                    avatarDao.insertAvatars(avatars)
                }
            } else {
                Log.e("GroupRepository", "Failed to get group $groupId: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error fetching group $groupId", e)
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
            } else {
                Log.e(
                    "GroupRepository",
                    "Failed to get members for group $id: ${response.message()}"
                )
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error fetching members for group $id", e)
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
            Log.e("GroupRepository", "Ошибка при получении доступных пользователей", e)
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
            Log.e("GroupRepository", "Error fetching blacklist for group $id", e)
            Result.failure(e)
        }
    }
    
    suspend fun create(name: String, bio: String): Result<Long> {
        return try {
            val request = CreateGroupRequestDto(
                name = name,
                bio = bio
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
            Log.e("GroupRepository", "Ошибка при добавлении участников", e)
            Result.failure(e)
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
    
    suspend fun updateGroupType(
        groupId: Long,
        groupType: GroupType,
        username: String?
    ): Result<Unit> {
        return try {
            val request = UpdateGroupRequestDto(groupType = groupType, username = username)
            val response = groupApi.updateGroup(groupId, request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    groupDao.insert(body.toDomain().toEntity())
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Update failed"))
                }
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
                refreshGroup(id)
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
                Result.failure(Exception("Leave failed ${response.errorBody()}"))
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
            Log.e("GroupRepository", "Error fetching join requests for group $groupId", e)
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
            Log.e("GroupRepository", "Ошибка при принятии заявки", e)
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
            Log.e("GroupRepository", "Ошибка при отклонении заявки", e)
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
            Log.e("GroupRepository", "Error creating invite link", e)
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
            Log.e("GroupRepository", "Error deleting invite link", e)
            Result.failure(e)
        }
    }
    
    private suspend fun refreshGroup(groupId: Long) {
        try {
            val response = groupApi.getGroupById(groupId)
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val group = dto.toDomain()
                    groupDao.insert(group.toEntity())
                    
                    val avatars = dto.avatars.map { it.toGroupEntity(group.id) }
                    avatarDao.insertAvatars(avatars)
                }
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error refreshing group $groupId", e)
        }
    }
    
    suspend fun initUploadAvatar(
        groupId: Long,
        name: String,
        size: Long,
        mimeType: String
    ): Result<FileInitResponseDto> {
        return try {
            val response = groupApi.initUploadAvatar(
                groupId,
                com.aiwazian.messenger.network.dto.FileInitRequestDto(name, size, mimeType)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Init upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun confirmUploadAvatar(groupId: Long, fileId: String): Result<Unit> {
        return try {
            val response = groupApi.confirmUploadAvatar(groupId, fileId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Confirm upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteAvatar(groupId: Long, fileId: String): Result<Unit> {
        return try {
            val response = groupApi.deleteAvatar(groupId, fileId)
            if (response.isSuccessful) {
                avatarDao.deleteAvatarByFileId(fileId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete avatar failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAvatarDownloadUrl(fileId: String): Result<String> {
        return try {
            val response = groupApi.getAvatarDownloadUrl(fileId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.downloadUrl)
                } else {
                    Result.failure(Exception("Empty body"))
                }
            } else {
                Result.failure(Exception("Unsuccessful request: ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Ошибка при загрузке аватара", e)
            Result.failure(e)
        }
    }
}
