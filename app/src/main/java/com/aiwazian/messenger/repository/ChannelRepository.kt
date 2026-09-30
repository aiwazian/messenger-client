/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.domain.Comment
import com.aiwazian.messenger.domain.InviteLink
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.network.dto.AttachmentInputDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.repository.channel.ChannelCommentsRepository
import com.aiwazian.messenger.repository.channel.ChannelCrudRepository
import com.aiwazian.messenger.repository.channel.ChannelMembersRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChannelRepository @Inject constructor(
    private val crudRepository: ChannelCrudRepository,
    private val membersRepository: ChannelMembersRepository,
    private val commentsRepository: ChannelCommentsRepository
) {

    suspend fun getComments(channelId: Long, postId: Long): Result<List<Comment>> =
        commentsRepository.getComments(channelId, postId)

    suspend fun sendComment(
        channelId: Long,
        postId: Long,
        text: String?,
        stickerId: Long? = null
    ): Result<Comment> = commentsRepository.sendComment(channelId, postId, text, stickerId)

    suspend fun initCommentFileUpload(
        channelId: Long,
        postId: Long,
        request: FileInitRequestDto
    ): FileInitResponseDto? = commentsRepository.initFileUpload(channelId, postId, request)

    suspend fun confirmCommentFiles(
        channelId: Long,
        postId: Long,
        attachments: List<AttachmentInputDto>,
        text: String?
    ): Result<Comment> = commentsRepository.confirmFiles(channelId, postId, attachments, text)

    suspend fun getCommentFileDownloadUrl(
        channelId: Long,
        postId: Long,
        commentId: Long,
        fileId: String
    ): Result<String> =
        commentsRepository.getFileDownloadUrl(channelId, postId, commentId, fileId)

    suspend fun setCommentsEnabled(channel: Channel, enabled: Boolean): Result<Unit> =
        commentsRepository.setCommentsEnabled(channel, enabled)

    suspend fun setCommentsRestrictedToSubscribers(channel: Channel, restricted: Boolean): Result<Unit> =
        commentsRepository.setCommentsRestrictedToSubscribers(channel, restricted)

    suspend fun deleteAllComments(channelId: Long): Result<Unit> =
        commentsRepository.deleteAllComments(channelId)
    
    suspend fun create(name: String, bio: String): Result<Long> =
        crudRepository.create(name, bio)
    
    fun getById(channelId: Long): Flow<Channel> = crudRepository.getById(channelId)
    
    fun getByIdOrNull(channelId: Long): Flow<Channel?> = crudRepository.getByIdOrNull(channelId)
    
    suspend fun fetchById(channelId: Long) = crudRepository.fetchById(channelId)
    
    suspend fun update(channel: Channel): Result<Unit> = crudRepository.update(channel)
    
    suspend fun updateChannelType(
        channelId: Long,
        channelType: ChannelType,
        username: String?
    ): Result<Unit> = crudRepository.updateChannelType(channelId, channelType, username)
    
    suspend fun delete(channelId: Long): Result<Unit> = crudRepository.delete(channelId)
    
    suspend fun initUploadAvatar(
        channelId: Long,
        name: String,
        size: Long,
        mimeType: String
    ): Result<FileInitResponseDto> =
        crudRepository.initUploadAvatar(channelId, name, size, mimeType)
    
    suspend fun confirmUploadAvatar(channelId: Long, fileId: String): Result<Unit> =
        crudRepository.confirmUploadAvatar(channelId, fileId)
    
    suspend fun deleteAvatar(channelId: Long, fileId: String): Result<Unit> =
        crudRepository.deleteAvatar(channelId, fileId)
    
    suspend fun getAvatarDownloadUrl(fileId: String): Result<String> =
        crudRepository.getAvatarDownloadUrl(fileId)
    
    suspend fun getSubscribers(
        channelId: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Result<List<User>> = membersRepository.getSubscribers(channelId, skip, take, search)
    
    suspend fun join(channelId: Long): Result<Unit> = membersRepository.join(channelId)
    
    suspend fun leave(channelId: Long): Result<Unit> = membersRepository.leave(channelId)
    
    suspend fun kickUser(channelId: Long, userId: Long): Result<Unit> =
        membersRepository.kickUser(channelId, userId)
    
    suspend fun transferOwnership(channelId: Long, userId: Long): Result<Unit> =
        membersRepository.transferOwnership(channelId, userId)
    
    suspend fun getBannedUsers(
        channelId: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Result<List<User>> = membersRepository.getBannedUsers(channelId, skip, take, search)
    
    suspend fun banUser(channelId: Long, userId: Long): Result<Unit> =
        membersRepository.banUser(channelId, userId)
    
    suspend fun unbanUser(channelId: Long, userId: Long): Result<Unit> =
        membersRepository.unbanUser(channelId, userId)
    
    suspend fun getJoinRequests(
        channelId: Long,
        skip: Int = 0,
        take: Int = 100,
        search: String? = null
    ): Result<List<User>> = membersRepository.getJoinRequests(channelId, skip, take, search)
    
    suspend fun acceptJoinRequest(channelId: Long, userId: Long): Result<Unit> =
        membersRepository.acceptJoinRequest(channelId, userId)
    
    suspend fun rejectJoinRequest(channelId: Long, userId: Long): Result<Unit> =
        membersRepository.rejectJoinRequest(channelId, userId)
    
    suspend fun getInviteLinks(channelId: Long): Result<List<InviteLink>> =
        membersRepository.getInviteLinks(channelId)
    
    suspend fun createInviteLink(
        channelId: Long,
        maxUses: Int?,
        expiresAt: Long? = null,
        requireApproval: Boolean = false
    ): Result<InviteLink> =
        membersRepository.createInviteLink(channelId, maxUses, expiresAt, requireApproval)
    
    suspend fun deleteInviteLink(channelId: Long, inviteLinkId: Long): Result<Unit> =
        membersRepository.deleteInviteLink(channelId, inviteLinkId)
}
