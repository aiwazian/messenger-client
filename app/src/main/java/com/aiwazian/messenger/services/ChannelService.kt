package com.aiwazian.messenger.services

import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.ChannelInfo
import com.aiwazian.messenger.data.UserInfo
import com.aiwazian.messenger.types.EntityId
import javax.inject.Inject

class ChannelService @Inject constructor() {
    
    suspend fun create(channel: ChannelInfo): Long? {
        val response = RetrofitInstance.api.createChannel(channel)
        return response.body()?.message?.toLongOrNull()
    }
    
    suspend fun save(channel: ChannelInfo): Long? {
        val response = RetrofitInstance.api.saveChannel(channel.id, channel)
        return response.body()?.message?.toLongOrNull()
    }
    
    suspend fun delete(id: Long): Boolean {
        val response = RetrofitInstance.api.deleteChannel(id)
        return response.isSuccessful
    }
    
    suspend fun get(id: Long): ChannelInfo? {
        val response = RetrofitInstance.api.getChannel(id)
        return response.body()
    }
    
    suspend fun join(id: Long): Boolean {
        val response = RetrofitInstance.api.joinChannel(id)
        return response.isSuccessful
    }
    
    suspend fun leave(id: Long): Boolean {
        val response = RetrofitInstance.api.leaveChannel(id)
        return response.isSuccessful
    }
    
    suspend fun isBusyPublicLick(link:String): Boolean {
        val response = RetrofitInstance.api.checkChannelPublicLink(link)
        return !response.isSuccessful
    }
    
    suspend fun getSubscribers(id:Long): List<UserInfo>? {
        val response = RetrofitInstance.api.getChannelSubscribers(id)
        return response.body()
    }
    
}