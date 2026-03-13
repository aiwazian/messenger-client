package com.aiwazian.messenger.viewModels

import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.data.ChannelInfo
import com.aiwazian.messenger.data.UserInfo
import com.aiwazian.messenger.database.repository.ChannelRepository
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.services.DialogController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(private val channelRepository: ChannelRepository) :
    ViewModel() {
    
    private val _channelInfo = MutableStateFlow(ChannelInfo())
    val channelInfo = _channelInfo.asStateFlow()
    
    val deleteDialog = DialogController()
    
    fun changeName(newName: String) {
        _channelInfo.update { it.copy(name = newName) }
    }
    
    fun changeBio(newBio: String) {
        _channelInfo.update { it.copy(bio = newBio) }
    }
    
    fun changeChannelType(channelType: ChannelType): Boolean {
        if (channelType == ChannelType.PUBLIC && _channelInfo.value.publicLink?.isBlank() == true) {
            return false
        }
        
        _channelInfo.update { it.copy(channelType = channelType.ordinal) }
        
        return true
    }
    
    fun changePublicLink(link: String?) {
        _channelInfo.update { it.copy(publicLink = link?.trim()) }
    }
    
    suspend fun checkIsBusyPublicLink(link: String): Boolean? {
        return channelRepository.checkIsBusyPublicLink(link)
    }
    
    fun open(channelInfo: ChannelInfo) {
        _channelInfo.update { channelInfo }
    }
    
    suspend fun getSubscribers(id: Long): List<UserInfo> {
        val subscribers = channelRepository.getSubscribers(id)
        return subscribers
    }
    
    suspend fun tryJoin(id: Long): Boolean {
        val isJoined = channelRepository.join(id)
        return isJoined
    }
    
    suspend fun tryLeave(id: Long): Boolean {
        val isLeaved = channelRepository.leave(id)
        return isLeaved
    }
    
    suspend fun trySaveOrCreate(): Long? {
        if (!checkValid()) {
            return null
        }
        
        if (_channelInfo.value.id == 0.toLong()) {
            return channelRepository.create(_channelInfo.value)
        }
        
        return channelRepository.save(_channelInfo.value)
    }
    
    suspend fun tryDelete(): Boolean {
        return channelRepository.delete(channelInfo.value.id)
    }
    
    fun cleanData() {
        _channelInfo.update { ChannelInfo() }
    }
    
    private fun checkValid(): Boolean {
        if (_channelInfo.value.name.isBlank()) {
            return false
        }
        
        if (_channelInfo.value.channelType == ChannelType.PUBLIC.ordinal && _channelInfo.value.publicLink?.isBlank() == true) {
            return false
        }
        
        return true
    }
    
}