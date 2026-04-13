/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.AddMembersRequestDto
import com.aiwazian.messenger.network.dto.CreateGroupRequestDto
import com.aiwazian.messenger.network.dto.CreateInviteLinkRequestDto
import com.aiwazian.messenger.network.dto.GroupResponseDto
import com.aiwazian.messenger.network.dto.InviteLinkResponseDto
import com.aiwazian.messenger.network.dto.UpdateGroupRequestDto
import com.aiwazian.messenger.network.dto.UserResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
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
}
