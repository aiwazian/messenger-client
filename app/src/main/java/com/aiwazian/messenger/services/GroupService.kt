package com.aiwazian.messenger.services

import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.GroupInfo
import com.aiwazian.messenger.data.UserInfo
import javax.inject.Inject

class GroupService @Inject constructor() {
    
    suspend fun create(groupInfo: GroupInfo): Long? {
        val response = RetrofitInstance.api.createGroup(groupInfo)
        return response.body()?.message?.toLongOrNull()
    }
    
    suspend fun get(id: Long): GroupInfo? {
        val response = RetrofitInstance.api.getGroup(id)
        return response.body()
    }
    
    suspend fun delete(id: Long): Boolean {
        val response = RetrofitInstance.api.deleteGroup(id)
        return response.isSuccessful
    }
    
    suspend fun getMembers(id: Long): List<UserInfo>? {
        val response = RetrofitInstance.api.getGroupMembers(id)
        return response.body()
    }
    
    suspend fun inviteUserToGroup(
        groupId: Long,
        userId: Long
    ): Boolean {
        val response = RetrofitInstance.api.inviteUserToGroup(
            groupId,
            userId
        )
        return response.isSuccessful
    }
    
    suspend fun removeUserFromGroup(
        groupId: Long,
        userId: Long
    ): Boolean {
        val response = RetrofitInstance.api.removeUserFromGroup(
            groupId,
            userId
        )
        return response.isSuccessful
    }
}