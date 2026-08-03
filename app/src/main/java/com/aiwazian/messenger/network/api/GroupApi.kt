/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.AddMembersRequestDto
import com.aiwazian.messenger.network.dto.ChatAdminPermissionsResponseDto
import com.aiwazian.messenger.network.dto.CreateGroupRequestDto
import com.aiwazian.messenger.network.dto.CreateInviteLinkRequestDto
import com.aiwazian.messenger.network.dto.FileDownloadResponseDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.GroupAdminResponseDto
import com.aiwazian.messenger.network.dto.GroupMemberTagResponseDto
import com.aiwazian.messenger.network.dto.GroupResponseDto
import com.aiwazian.messenger.network.dto.InviteLinkResponseDto
import com.aiwazian.messenger.network.dto.SetNoCopyRequestDto
import com.aiwazian.messenger.network.dto.UpdateGroupRequestDto
import com.aiwazian.messenger.network.dto.UpsertGroupAdminRequestDto
import com.aiwazian.messenger.network.dto.UserResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GroupApi {

    @POST("groups")
    suspend fun createGroup(@Body request: CreateGroupRequestDto): Response<GroupResponseDto>

    @GET("groups/{groupId}")
    suspend fun getGroupById(@Path("groupId") groupId: Long): Response<GroupResponseDto>

    @GET("groups/{groupId}/members")
    suspend fun getGroupMembers(
        @Path("groupId") groupId: Long,
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 100,
        @Query("search") search: String? = null
    ): Response<List<UserResponseDto>>

    /**
     * Теги участников группы.
     *
     * Доступно любому участнику: тег рисуется рядом с именем отправителя.
     */
    @GET("groups/{groupId}/member-tags")
    suspend fun getMemberTags(
        @Path("groupId") groupId: Long
    ): Response<List<GroupMemberTagResponseDto>>

    /** Список администраторов группы: только для владельца. */
    @GET("groups/{groupId}/admins")
    suspend fun getAdmins(@Path("groupId") groupId: Long): Response<List<GroupAdminResponseDto>>

    /** Права текущего пользователя в группе. */
    @GET("groups/{groupId}/admins/me")
    suspend fun getMyGroupPermissions(
        @Path("groupId") groupId: Long
    ): Response<ChatAdminPermissionsResponseDto>

    /** Назначить администратора или перезаписать его права и тег. */
    @PUT("groups/{groupId}/admins/{userId}")
    suspend fun upsertAdmin(
        @Path("groupId") groupId: Long,
        @Path("userId") userId: Long,
        @Body request: UpsertGroupAdminRequestDto
    ): Response<GroupAdminResponseDto>

    /** Снять администратора вместе с его тегом. */
    @DELETE("groups/{groupId}/admins/{userId}")
    suspend fun removeAdmin(
        @Path("groupId") groupId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>

    @GET("groups/{groupId}/available-users")
    suspend fun getAvailableUsersForInvite(
        @Path("groupId") groupId: Long
    ): Response<List<UserResponseDto>>

    @POST("groups/{groupId}/add-members")
    suspend fun addMembers(
        @Path("groupId") groupId: Long,
        @Body request: AddMembersRequestDto
    ): Response<Unit>

    @PATCH("groups/{groupId}")
    suspend fun updateGroup(
        @Path("groupId") groupId: Long,
        @Body request: UpdateGroupRequestDto
    ): Response<GroupResponseDto>

    /**
     * Включает или выключает запрет копирования контента группы.
     *
     * Сервер допускает вызов только владельцу группы.
     */
    @PATCH("groups/{groupId}/no-copy")
    suspend fun setNoCopy(
        @Path("groupId") groupId: Long,
        @Body request: SetNoCopyRequestDto
    ): Response<GroupResponseDto>

    @DELETE("groups/{groupId}")
    suspend fun deleteGroup(@Path("groupId") groupId: Long): Response<Unit>

    @POST("groups/{groupId}/join")
    suspend fun joinGroup(@Path("groupId") groupId: Long): Response<Unit>

    @POST("groups/{groupId}/leave")
    suspend fun leaveGroup(@Path("groupId") groupId: Long): Response<Unit>

    @POST("groups/{groupId}/kick/{userId}")
    suspend fun kickUser(
        @Path("groupId") groupId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>

    @POST("groups/{groupId}/ban/{userId}")
    suspend fun banUser(
        @Path("groupId") groupId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>

    @GET("groups/{groupId}/blacklist")
    suspend fun getBlackList(
        @Path("groupId") groupId: Long,
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 100,
        @Query("search") search: String? = null
    ): Response<List<UserResponseDto>>

    @POST("groups/{groupId}/unban/{userId}")
    suspend fun unbanUser(
        @Path("groupId") groupId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>
    
    @GET("groups/{groupId}/join-requests")
    suspend fun getJoinRequests(
        @Path("groupId") groupId: Long,
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 100,
        @Query("search") search: String? = null
    ): Response<List<UserResponseDto>>
    
    @POST("groups/{groupId}/join-requests/{userId}/accept")
    suspend fun acceptJoinRequest(
        @Path("groupId") groupId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>
    
    @POST("groups/{groupId}/join-requests/{userId}/reject")
    suspend fun rejectJoinRequest(
        @Path("groupId") groupId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>

    @GET("groups/{groupId}/invite-links")
    suspend fun getInviteLinks(@Path("groupId") groupId: Long): Response<List<InviteLinkResponseDto>>

    @POST("groups/{groupId}/invite-links")
    suspend fun createInviteLink(
        @Path("groupId") groupId: Long,
        @Body request: CreateInviteLinkRequestDto
    ): Response<InviteLinkResponseDto>

    @DELETE("groups/{groupId}/invite-links/{inviteLinkId}")
    suspend fun deleteInviteLink(
        @Path("groupId") groupId: Long,
        @Path("inviteLinkId") inviteLinkId: Long
    ): Response<Unit>
    
    @POST("groups/{groupId}/avatar/init")
    suspend fun initUploadAvatar(
        @Path("groupId") groupId: Long,
        @Body request: FileInitRequestDto
    ): Response<FileInitResponseDto>
    
    @POST("groups/{groupId}/avatar/confirm/{fileId}")
    suspend fun confirmUploadAvatar(
        @Path("groupId") groupId: Long,
        @Path("fileId") fileId: String
    ): Response<Unit>
    
    @DELETE("groups/{groupId}/avatars/{fileId}")
    suspend fun deleteAvatar(
        @Path("groupId") groupId: Long,
        @Path("fileId") fileId: String
    ): Response<Unit>
    
    @GET("groups/avatars/{fileId}")
    suspend fun getAvatarDownloadUrl(@Path("fileId") fileId: String): Response<FileDownloadResponseDto>
}
