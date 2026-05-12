/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import android.content.Context
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
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.usecase.LeaveChatUseCase
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.ShortcutManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val userRepository: UserRepository,
    private val channelRepository: ChannelRepository,
    private val groupRepository: GroupRepository,
    private val shortcutManager: ShortcutManager,
    private val clipboardService: ClipboardService,
    private val downloadManager: DownloaderManager,
    private val leaveChatUseCase: LeaveChatUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ProfileUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private val downloadingAvatars = mutableSetOf<String>()
    
    private var isInit = false
    
    fun init(profileId: Long) {
        if (isInit) return
        isInit = true
        _uiState.update { it.copy(id = profileId) }
        
        setupUserObserver()
        loadProfile()
    }
    
    private fun setupUserObserver() {
        viewModelScope.launch {
            userRepository.getMe().firstOrNull()?.let { user ->
                _uiState.update { it.copy(myId = user.id) }
                recalculateActions()
            }
        }
    }
    
    private fun createChatShortcut(chatName: String) {
        shortcutManager.createChatShortcut(
            chatId = _uiState.value.id,
            chatName
        )
    }
    
    fun copyToClipboard(text: String) {
        clipboardService.copy(text)
    }
    
    private fun loadProfile() {
        val profileId = _uiState.value.id
        when (ChatType.fromId(profileId)) {
            ChatType.PRIVATE -> {
                viewModelScope.launch {
                    if (profileId == _uiState.value.myId) {
                        userRepository.getMe().collectLatest { user ->
                            val profile = Profile.User(
                                username = user.username,
                                bio = user.bio,
                                dateOfBirth = user.dateOfBirth,
                            )
                            _uiState.update {
                                it.copy(
                                    profile = profile,
                                    avatars = user.avatars.map { avatar -> avatar.uri }
                                )
                            }
                            recalculateActions()
                            user.avatars.filter { it.uri == null && downloadingAvatars.add(it.fileId) }
                                .forEach { avatar ->
                                    viewModelScope.launch {
                                        userRepository.getAvatarDownloadUrl(avatar.fileId)
                                            .onSuccess { downloadUrl ->
                                                downloadManager.download(
                                                    url = downloadUrl,
                                                    fileId = avatar.fileId,
                                                    fileName = avatar.fileId
                                                )
                                            }.onFailure {
                                                downloadingAvatars.remove(avatar.fileId)
                                                Log.e(
                                                    "ProfileViewModel",
                                                    "Error download avatar: ",
                                                    it
                                                )
                                            }
                                    }
                                }
                        }
                    } else {
                        userRepository.getById(profileId).collectLatest { user ->
                            val profile = Profile.User(
                                username = user.username,
                                bio = user.bio,
                                dateOfBirth = user.dateOfBirth,
                            )
                            _uiState.update {
                                it.copy(
                                    profile = profile,
                                    avatars = user.avatars.map { avatar -> avatar.uri }
                                )
                            }
                            recalculateActions()
                            
                            user.avatars.filter { it.uri == null && downloadingAvatars.add(it.fileId) }
                                .forEach { avatar ->
                                    viewModelScope.launch {
                                        userRepository.getAvatarDownloadUrl(avatar.fileId)
                                            .onSuccess { downloadUrl ->
                                                downloadManager.download(
                                                    url = downloadUrl,
                                                    fileId = avatar.fileId,
                                                    fileName = avatar.fileId
                                                )
                                            }.onFailure {
                                                downloadingAvatars.remove(avatar.fileId)
                                                Log.e(
                                                    "ProfileViewModel",
                                                    "Error download avatar: ",
                                                    it
                                                )
                                            }
                                    }
                                }
                        }
                    }
                }
            }
            
            ChatType.CHANNEL -> {
                viewModelScope.launch {
                    channelRepository.getById(profileId).collectLatest { channel ->
                        val profile = Profile.Channel(
                            ownerId = channel.ownerId,
                            bio = channel.bio,
                            subscribers = channel.subscribers,
                            username = channel.username,
                            isSubscribed = channel.isSubscribed,
                        )
                        _uiState.update {
                            it.copy(
                                profile = profile,
                                avatars = channel.avatars.map { avatar -> avatar.uri }
                            )
                        }
                        recalculateActions()
                        
                        channel.avatars.filter { it.uri == null && downloadingAvatars.add(it.fileId) }
                            .forEach { avatar ->
                                viewModelScope.launch {
                                    channelRepository.getAvatarDownloadUrl(avatar.fileId)
                                        .onSuccess { downloadUrl ->
                                            downloadManager.download(
                                                url = downloadUrl,
                                                fileId = avatar.fileId,
                                                fileName = avatar.fileId
                                            )
                                        }
                                        .onFailure {
                                            downloadingAvatars.remove(avatar.fileId)
                                        }
                                }
                            }
                    }
                }
            }
            
            ChatType.GROUP -> {
                viewModelScope.launch {
                    groupRepository.getById(profileId).collectLatest { group ->
                        group.let {
                            val profile = Profile.Group(
                                ownerId = group.ownerId,
                                bio = group.bio,
                                username = group.username,
                                members = group.members,
                                isMember = group.isMember,
                            )
                            _uiState.update {
                                it.copy(
                                    profile = profile,
                                    avatars = group.avatars.map { avatar -> avatar.uri }
                                )
                            }
                            recalculateActions()
                            
                            group.avatars.filter { it.uri == null && downloadingAvatars.add(it.fileId) }
                                .forEach { avatar ->
                                    viewModelScope.launch {
                                        groupRepository.getAvatarDownloadUrl(avatar.fileId)
                                            .onSuccess { downloadUrl ->
                                                downloadManager.download(
                                                    url = downloadUrl,
                                                    fileId = avatar.fileId,
                                                    fileName = avatar.fileId
                                                )
                                            }
                                            .onFailure {
                                                downloadingAvatars.remove(avatar.fileId)
                                            }
                                    }
                                }
                        }
                    }
                }
            }
            
            else -> {}
        }
    }
    
    private fun recalculateActions() {
        val newActions = when (val profile = _uiState.value.profile) {
            is Profile.User -> calculateUserActions()
            
            is Profile.Channel -> calculateChannelActions(profile)
            
            is Profile.Group -> calculateGroupActions(profile)
            
            else -> emptyList()
        }
        
        _uiState.update { it.copy(actions = newActions) }
    }
    
    private fun calculateUserActions(): List<TopBarAction> {
        val dropdownActions = mutableListOf<DropdownMenuAction>()
        
        dropdownActions.add(
            createDropdownAction(
                icon = Icons.Rounded.AddHome,
                textResId = R.string.add_to_home_screen,
                onClick = {
                    createChatShortcut(_uiState.value.title.asString(context))
                }
            )
        )
        
        return if (_uiState.value.myId == _uiState.value.id) {
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
    
    private fun calculateChannelActions(channel: Profile.Channel): List<TopBarAction> {
        val dropdownActions = mutableListOf<DropdownMenuAction>()
        
        dropdownActions.add(
            createDropdownAction(
                icon = Icons.Rounded.AddHome,
                textResId = R.string.add_to_home_screen,
                onClick = {
                    createChatShortcut(_uiState.value.title.asString(context))
                }
            )
        )
        
        if (channel.isSubscribed && channel.ownerId != _uiState.value.myId) {
            dropdownActions.add(
                createDropdownAction(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    textResId = R.string.leave_channel,
                    onClick = ::showLeaveDialog
                )
            )
        }
        
        return if (channel.ownerId == _uiState.value.myId) {
            listOf(
                TopBarAction(
                    icon = Icons.Filled.Edit,
                    onClick = { navigateToChannelSettings(_uiState.value.id) }
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
    
    private fun calculateGroupActions(group: Profile.Group): List<TopBarAction> {
        val dropdownActions = mutableListOf<DropdownMenuAction>()
        
        dropdownActions.add(
            createDropdownAction(
                icon = Icons.Rounded.AddHome,
                textResId = R.string.add_to_home_screen,
                onClick = {
                    createChatShortcut(_uiState.value.title.asString(context))
                }
            )
        )
        
        if (group.ownerId != _uiState.value.myId) {
            dropdownActions.add(
                createDropdownAction(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    textResId = R.string.leave_group,
                    onClick = ::showLeaveDialog
                )
            )
        }
        
        return if (group.ownerId == _uiState.value.myId) {
            listOf(
                TopBarAction(
                    icon = Icons.Filled.Edit,
                    onClick = { navigateToGroupSettings(_uiState.value.id) }
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
    
    fun showLeaveDialog() {
        viewModelScope.launch {
            _uiEffect.emit(
                ProfileUiEffect.ShowLeaveDialog(
                    _uiState.value.title.asString(context),
                    ChatType.fromId(_uiState.value.id)
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
            leaveChatUseCase(_uiState.value.id).onSuccess {
                hideLeaveDialog()
                _uiEffect.emit(ProfileUiEffect.NavigateToMain)
            }
        }
    }
}
