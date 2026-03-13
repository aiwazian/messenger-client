package com.aiwazian.messenger.viewModels

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.database.repository.ChannelRepository
import com.aiwazian.messenger.database.repository.GroupRepository
import com.aiwazian.messenger.database.repository.UserRepository
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.interfaces.Profile
import com.aiwazian.messenger.services.DialogController
import com.aiwazian.messenger.services.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val channelRepository: ChannelRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {
    
    private val _profile = MutableStateFlow<Profile?>(null)
    val profile = _profile.asStateFlow()
    
    val blockUserDialog = DialogController()
    
    val startSecretChatDialog = DialogController()
    
    suspend fun open(profileId: Long) {
        when (ChatType.fromId(profileId)) {
            ChatType.PRIVATE -> {
                if (profileId == UserManager.user.value.id) {
                    viewModelScope.launch {
                        UserManager.user.collectLatest { collect ->
                            _profile.update { collect }
                        }
                    }
                } else {
                    val user = userRepository.getById(profileId)

                    if (user != null) {
                        _profile.update { user }
                    }
                }
            }
            
            ChatType.CHANNEL -> {
                val channel = channelRepository.get(profileId)
                _profile.update { channel }
            }
            
            ChatType.GROUP -> {
                val group = groupRepository.get(profileId)
                _profile.update { group }
            }
            
            else -> {}
        }
    }
}