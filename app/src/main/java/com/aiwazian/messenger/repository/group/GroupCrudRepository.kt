/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository.group

import android.util.Log
import androidx.core.net.toUri
import com.aiwazian.messenger.database.dao.AvatarDao
import com.aiwazian.messenger.database.dao.GroupDao
import com.aiwazian.messenger.domain.AvatarNotFoundException
import com.aiwazian.messenger.domain.Group
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.mappers.toGroupEntity
import com.aiwazian.messenger.network.api.GroupApi
import com.aiwazian.messenger.network.dto.CreateGroupRequestDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.UpdateGroupRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Создание, чтение, обновление и удаление группы, а также её аватары.
 *
 * Участники живут в [GroupMembersRepository], а запрет копирования —
 * в [GroupContentProtectionRepository].
 */
class GroupCrudRepository @Inject constructor(
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
                    
                    /*
                     * Сервер отдал полный список аватарок: пропавшие уберёт сам DAO, а
                     * случай «аватарку удалили совсем» приходит пустым списком и требует
                     * явной чистки: иначе в кэше осталась бы старая картинка.
                     */
                    if (avatars.isEmpty()) {
                        avatarDao.deleteAvatarsByGroupId(group.id)
                    } else {
                        avatarDao.insertAvatars(avatars)
                    }
                }
            } else {
                Log.e(TAG, "Failed to get group $groupId: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching group $groupId", e)
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
            Log.e(TAG, "Ошибка при создании группы", e)
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
            Log.e(TAG, "Ошибка при обновлении группы", e)
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
            Log.e(TAG, "Ошибка при обновлении группы", e)
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
            Log.e(TAG, "Ошибка при удалении группы", e)
            Result.failure(e)
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
                FileInitRequestDto(name, size, mimeType)
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
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
            } else if (response.code() == 404 || response.code() == 410) {
                /* Файла больше нет: DownloadAvatarUseCase уберёт аватарку из Room. */
                Result.failure(AvatarNotFoundException(fileId))
            } else {
                Result.failure(Exception("Unsuccessful request: ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке аватара", e)
            Result.failure(e)
        }
    }
    
    private companion object {
        const val TAG = "GroupCrudRepository"
    }
}
