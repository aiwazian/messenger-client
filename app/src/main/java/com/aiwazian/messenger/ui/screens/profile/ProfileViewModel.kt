/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.ShortcutManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val channelRepository: ChannelRepository,
    private val groupRepository: GroupRepository,
    private val shortcutManager: ShortcutManager,
    private val clipboardService: ClipboardService,
    private val downloadManager: DownloaderManager,
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ProfileUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun init(profileId: Long) {
        setupUserObserver()
        loadProfile(profileId)
    }
    
    private fun setupUserObserver() {
        viewModelScope.launch {
            userRepository.getMe().collectLatest { user ->
                _uiState.update { it.copy(myId = user.id) }
                recalculateActions()
            }
        }
    }
    
    private fun createChatShortcut(chatName: String) {
        shortcutManager.createChatShortcut(
            chatId = _uiState.value.profile!!.id,
            chatName
        )
    }
    
    fun copyToClipboard(text: String) {
        clipboardService.copy(text)
    }
    
    private fun loadProfile(profileId: Long) {
        when (ChatType.fromId(profileId)) {
            ChatType.PRIVATE -> {
                viewModelScope.launch {
                    if (profileId == userRepository.getMe().first().id) {
                        userRepository.getMe().collectLatest { user ->
                            val profile = Profile.User(
                                id = user.id,
                                firstName = user.firstName,
                                lastName = user.lastName,
                                username = user.username,
                                bio = user.bio,
                                dateOfBirth = user.dateOfBirth,
                                avatars = user.avatars.mapNotNull { it.uri }
                            )
                            _uiState.update {
                                it.copy(profile = profile)
                            }
                            recalculateActions()
                            user.avatars.forEach { avatar ->
                                userRepository.getAvatarDownloadUrl(avatar.fileId)
                                    .onSuccess { downloadUrl ->
                                        Log.d("ProfileViewModel", "Download $downloadUrl")
                                        downloadManager.download(
                                            url = downloadUrl,
                                            fileId = avatar.fileId,
                                            fileName = avatar.fileId
                                        )
                                    }.onFailure {
                                        Log.e("ProfileViewModel", "Error download avatar: ", it)
                                    }
                            }
                        }
                    } else {
                        userRepository.getById(profileId).collectLatest { user ->
                            val profile = Profile.User(
                                id = user.id,
                                firstName = user.firstName,
                                lastName = user.lastName,
                                username = user.username,
                                bio = user.bio,
                                dateOfBirth = user.dateOfBirth
                            )
                            _uiState.update {
                                it.copy(profile = profile)
                            }
                            recalculateActions()
                        }
                    }
                }
            }
            
            ChatType.CHANNEL -> {
                viewModelScope.launch {
                    channelRepository.getByIdFlow(profileId).collectLatest { channel ->
                        val profile = Profile.Channel(
                            id = channel.id,
                            ownerId = channel.ownerId,
                            name = channel.name,
                            bio = channel.bio,
                            subscribers = channel.subscribers,
                            removedUser = channel.removedUser,
                            channelType = channel.channelType,
                            username = channel.username,
                            isSubscribed = channel.isSubscribed
                        )
                        _uiState.update {
                            it.copy(profile = profile)
                        }
                        recalculateActions()
                    }
                }
                
                viewModelScope.launch {
                    channelRepository.getById(profileId).collect {}
                }
            }
            
            ChatType.GROUP -> {
                viewModelScope.launch {
                    groupRepository.getById(profileId).collectLatest { group ->
                        group.let {
                            val profile = Profile.Group(
                                id = group.id,
                                ownerId = group.ownerId,
                                name = group.name,
                                bio = group.bio,
                                username = group.username,
                                members = group.members
                            )
                            _uiState.update {
                                it.copy(profile = profile)
                            }
                            recalculateActions()
                        }
                    }
                }
            }
            
            else -> {}
        }
    }
    
    private fun recalculateActions() {
        val profile = _uiState.value.profile ?: return
        val myId = _uiState.value.myId
        
        val newActions = when (profile) {
            is Profile.User -> calculateUserActions(
                profile,
                myId
            )
            
            is Profile.Channel -> calculateChannelActions(
                profile,
                myId
            )
            
            is Profile.Group -> calculateGroupActions(
                profile,
                myId
            )
        }
        
        _uiState.update { it.copy(actions = newActions) }
    }
    
    private fun calculateUserActions(
        user: Profile.User,
        myId: Long
    ): List<TopBarAction> {
        val dropdownActions = mutableListOf<DropdownMenuAction>()
        
        dropdownActions.add(
            createDropdownAction(
                icon = Icons.Rounded.AddHome,
                textResId = R.string.add_to_home_screen,
                onClick = {
                    val chatName = "${user.firstName} ${user.lastName}"
                    createChatShortcut(chatName)
                }
            )
        )
        
        return if (myId == user.id) {
            listOf(
                TopBarAction(
                    icon = Icons.Filled.Edit,
                    onClick = ::navigateToProfileSettings
                ),
                TopBarAction(
                    icon = Icons.Rounded.MoreVert,
                    dropdownActions = dropdownActions
                )
            )
        } else {
            listOf(
                TopBarAction(
                    icon = Icons.Rounded.MoreVert,
                    dropdownActions = dropdownActions
                )
            )
        }
    }
    
    private fun calculateChannelActions(
        channel: Profile.Channel,
        myId: Long
    ): List<TopBarAction> {
        val dropdownActions = mutableListOf<DropdownMenuAction>()
        
        dropdownActions.add(
            createDropdownAction(
                icon = Icons.Rounded.AddHome,
                textResId = R.string.add_to_home_screen,
                onClick = {
                    createChatShortcut(channel.name)
                }
            )
        )
        
        if (channel.isSubscribed && channel.ownerId != myId) {
            dropdownActions.add(
                createDropdownAction(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    textResId = R.string.leave_channel,
                    onClick = {
                        showLeaveDialog(
                            channel.name,
                            ChatType.CHANNEL
                        )
                    }
                )
            )
        }
        
        return if (channel.ownerId == myId) {
            listOf(
                TopBarAction(
                    icon = Icons.Filled.Edit,
                    onClick = { navigateToChannelSettings(channel.id) }
                ),
                TopBarAction(
                    icon = Icons.Rounded.MoreVert,
                    dropdownActions = dropdownActions
                )
            )
        } else {
            listOf(
                TopBarAction(
                    icon = Icons.Rounded.MoreVert,
                    dropdownActions = dropdownActions
                )
            )
        }
    }
    
    private fun calculateGroupActions(
        group: Profile.Group,
        myId: Long
    ): List<TopBarAction> {
        val dropdownActions = mutableListOf<DropdownMenuAction>()
        
        dropdownActions.add(
            createDropdownAction(
                icon = Icons.Rounded.AddHome,
                textResId = R.string.add_to_home_screen,
                onClick = {
                    createChatShortcut(group.name)
                }
            )
        )
        
        if (group.ownerId != myId) {
            dropdownActions.add(
                createDropdownAction(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    textResId = R.string.leave_group,
                    onClick = {
                        showLeaveDialog(
                            group.name,
                            ChatType.GROUP
                        )
                    }
                )
            )
        }
        
        return if (group.ownerId == myId) {
            listOf(
                TopBarAction(
                    icon = Icons.Filled.Edit,
                    onClick = { navigateToGroupSettings(group.id) }
                ),
                TopBarAction(
                    icon = Icons.Rounded.MoreVert,
                    dropdownActions = dropdownActions
                )
            )
        } else {
            listOf(
                TopBarAction(
                    icon = Icons.Rounded.MoreVert,
                    dropdownActions = dropdownActions
                )
            )
        }
    }
    
    private fun createDropdownAction(
        icon: ImageVector,
        @StringRes
        textResId: Int,
        onClick: () -> Unit
    ): DropdownMenuAction {
        return DropdownMenuAction(
            icon = icon,
            textResId = textResId,
            onClick = onClick
        )
    }
    
    private fun navigateToProfileSettings() {
        viewModelScope.launch {
            _uiEffect.emit(ProfileUiEffect.NavigateToUserSettings)
        }
    }
    
    private fun navigateToGroupSettings(groupId: Long) {
        viewModelScope.launch {
            _uiEffect.emit(ProfileUiEffect.NavigateToGroupSettings(groupId))
        }
    }
    
    private fun navigateToChannelSettings(channelId: Long) {
        viewModelScope.launch {
            _uiEffect.emit(ProfileUiEffect.NavigateToChannelSettings(channelId))
        }
    }
    
    fun showLeaveDialog(
        profileName: String,
        chatType: ChatType
    ) {
        viewModelScope.launch {
            _uiEffect.emit(
                ProfileUiEffect.ShowLeaveDialog(
                    profileName,
                    chatType
                )
            )
        }
    }
    
    fun hideLeaveDialog() {
        viewModelScope.launch {
            _uiEffect.emit(ProfileUiEffect.HideLeaveDialog)
        }
    }
    
    fun onLeaveConfirmed() {
        viewModelScope.launch {
            val profile = _uiState.value.profile ?: return@launch
            val chatId = profile.id
            val chatType = ChatType.fromId(chatId)
            
            val success = when (chatType) {
                ChatType.CHANNEL -> channelRepository.leave(chatId).isSuccess
                ChatType.GROUP -> groupRepository.leave(chatId).isSuccess
                else -> false
            }
            
            if (success) {
                hideLeaveDialog()
                _uiEffect.emit(ProfileUiEffect.NavigateToMain)
            }
        }
    }
}
