/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import com.aiwazian.messenger.enums.GroupType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupResponseDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("bio") val bio: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("groupType") val groupType: GroupType = GroupType.PRIVATE,
    @SerialName("ownerId") val ownerId: String? = null,
    @SerialName("membersCount") val membersCount: Int? = null,
    @SerialName("isMember") val isMember: Boolean? = null,
    @SerialName("isOwner") val isOwner: Boolean? = null
)

@Serializable
data class CreateGroupRequestDto(
    @SerialName("name") val name: String,
    @SerialName("bio") val bio: String? = null
)

@Serializable
data class UpdateGroupRequestDto(
    @SerialName("name") val name: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("groupType") val groupType: GroupType? = null,
)

@Serializable
data class AddMembersRequestDto(
    @SerialName("userIds") val userIds: List<String>
)
