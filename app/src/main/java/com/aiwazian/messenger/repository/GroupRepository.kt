/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import com.aiwazian.messenger.domain.Group
import com.aiwazian.messenger.domain.InviteLink
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.enums.GroupType
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.repository.group.GroupCrudRepository
import com.aiwazian.messenger.repository.group.GroupMembersRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Единая точка входа для работы с группой.
 *
 * Реализация разделена на узкие репозитории:
 * [GroupCrudRepository] — данные самой группы и аватары,
 * [GroupMembersRepository] — участники, блокировки, заявки и ссылки-приглашения,
 * [com.aiwazian.messenger.repository.group.GroupContentProtectionRepository] —
 * запрет копирования.
 *
 * В новом коде лучше внедрять нужный узкий репозиторий напрямую, этот класс оставлен
 * для существующих экранов.
 */
class GroupRepository @Inject constructor(
    private val crudRepository: GroupCrudRepository,
    private val membersRepository: GroupMembersRepository
) {
    
    fun getById(id: Long): Flow<Group> = crudRepository.getById(id)
    
    fun getByIdOrNull(id: Long): Flow<Group?> = crudRepository.getByIdOrNull(id)
    
    suspend fun fetchById(groupId: Long) = crudRepository.fetchById(groupId)
    
    suspend fun create(name: String, bio: String): Result<Long> =
        crudRepository.create(name, bio)
    
    suspend fun update(group: Group): Result<Unit> = crudRepository.update(group)
    
    suspend fun updateGroupType(
        groupId: Long,
        groupType: GroupType,
        username: String?
    ): Result<Unit> = crudRepository.updateGroupType(groupId, groupType, username)
    
    suspend fun delete(id: Long): Result<Unit> = crudRepository.delete(id)
    
    suspend fun initUploadAvatar(
        groupId: Long,
        name: String,
        size: Long,
        mimeType: String
    ): Result<FileInitResponseDto> =
        crudRepository.initUploadAvatar(groupId, name, size, mimeType)
    
    suspend fun confirmUploadAvatar(groupId: Long, fileId: String): Result<Unit> =
        crudRepository.confirmUploadAvatar(groupId, fileId)
    
    suspend fun deleteAvatar(groupId: Long, fileId: String): Result<Unit> =
        crudRepository.deleteAvatar(groupId, fileId)
    
    suspend fun getAvatarDownloadUrl(fileId: String): Result<String> =
        crudRepository.getAvatarDownloadUrl(fileId)
    
    fun getMembers(
        id: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Flow<List<User>> = membersRepository.getMembers(id, skip, take, search)
    
    fun getAvailableUsersForInvite(id: Long): Flow<List<User>> =
        membersRepository.getAvailableUsersForInvite(id)
    
    suspend fun addMembers(groupId: Long, userIds: List<Long>): Result<Unit> =
        membersRepository.addMembers(groupId, userIds)
    
    suspend fun getBannedUsers(
        id: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Result<List<User>> = membersRepository.getBannedUsers(id, skip, take, search)
    
    suspend fun join(id: Long): Result<Unit> = membersRepository.join(id)
    
    suspend fun leave(id: Long): Result<Unit> = membersRepository.leave(id)
    
    suspend fun kickUser(groupId: Long, userId: Long): Result<Unit> =
        membersRepository.kickUser(groupId, userId)
    
    suspend fun banUser(groupId: Long, userId: Long): Result<Unit> =
        membersRepository.banUser(groupId, userId)
    
    suspend fun unban(groupId: Long, userId: Long): Result<Unit> =
        membersRepository.unban(groupId, userId)
    
    suspend fun getJoinRequests(
        groupId: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Result<List<User>> = membersRepository.getJoinRequests(groupId, skip, take, search)
    
    suspend fun acceptJoinRequest(groupId: Long, userId: Long): Result<Unit> =
        membersRepository.acceptJoinRequest(groupId, userId)
    
    suspend fun rejectJoinRequest(groupId: Long, userId: Long): Result<Unit> =
        membersRepository.rejectJoinRequest(groupId, userId)
    
    suspend fun getInviteLinks(groupId: Long): Result<List<InviteLink>> =
        membersRepository.getInviteLinks(groupId)
    
    suspend fun createInviteLink(
        groupId: Long,
        maxUses: Int?,
        expiresAt: Long? = null,
        requireApproval: Boolean = false
    ): Result<InviteLink> =
        membersRepository.createInviteLink(groupId, maxUses, expiresAt, requireApproval)
    
    suspend fun deleteInviteLink(groupId: Long, inviteLinkId: Long): Result<Unit> =
        membersRepository.deleteInviteLink(groupId, inviteLinkId)
}
