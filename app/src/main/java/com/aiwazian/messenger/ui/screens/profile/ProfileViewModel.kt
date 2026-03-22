/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.MoreVert
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.DropdownMenuAction
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.ShortcutManager
import com.aiwazian.messenger.utils.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val userManager: UserManager
) : ViewModel() {
    
    private var _profileId: Long = -1L
    
    private val _uiEffect = MutableSharedFlow<ProfileUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    private val _profile = MutableStateFlow<Profile?>(null)
    val profile = _profile.asStateFlow()
    
    private var isInitialized = false
    
    fun init(profileId: Long) {
        if (isInitialized) return
        isInitialized = true
        _profileId = profileId
        
        setupUserObserver()
        loadProfile(profileId)
    }
    
    private fun setupUserObserver() {
        viewModelScope.launch {
            userManager.user.collectLatest { user ->
                _uiState.update { it.copy(myId = user.id) }
                recalculateActions()
            }
        }
    }
    
    fun createChatShortcut(
        chatId: Long,
        chatName: String
    ) {
        shortcutManager.createChatShortcut(
            chatId,
            chatName
        )
    }
    
    fun copyToClipboard(text: String) {
        clipboardService.copy(text)
    }
    
    private fun loadProfile(profileId: Long) {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null
            )
        }
        
        when (ChatType.fromId(profileId)) {
            ChatType.PRIVATE -> {
                if (profileId == userManager.user.value.id) {
                    viewModelScope.launch {
                        userManager.user.collectLatest { user ->
                            val profile = Profile.User(
                                id = user.id,
                                firstName = user.firstName,
                                lastName = user.lastName,
                                username = user.username,
                                bio = user.bio,
                                dateOfBirth = user.dateOfBirth
                            )
                            _profile.update { profile }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    profile = profile
                                )
                            }
                            recalculateActions()
                        }
                    }
                } else {
                    viewModelScope.launch {
                        userRepository.getById(profileId).collectLatest { user ->
                            val profile = Profile.User(
                                id = user.id,
                                firstName = user.firstName,
                                lastName = user.lastName,
                                username = user.username,
                                bio = user.bio,
                                dateOfBirth = user.dateOfBirth
                            )
                            _profile.update { profile }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    profile = profile
                                )
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
                            publicLink = channel.username,
                            isSubscribed = channel.isSubscribed
                        )
                        _profile.update { profile }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                profile = profile
                            )
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
                        val profile = Profile.Group(
                            id = group.id,
                            ownerId = group.ownerId,
                            name = group.name,
                            bio = group.bio,
                            members = group.members
                        )
                        _profile.update { profile }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                profile = profile
                            )
                        }
                        recalculateActions()
                    }
                }
            }
            
            else -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Unknown profile type"
                    )
                }
            }
        }
    }
    
    private fun recalculateActions() {
        val profile = _profile.value ?: return
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
                    createChatShortcut(
                        user.id,
                        chatName
                    )
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
                    createChatShortcut(
                        channel.id,
                        channel.name
                    )
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
                    createChatShortcut(
                        group.id,
                        group.name
                    )
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
        icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            val profile = _profile.value ?: return@launch
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
