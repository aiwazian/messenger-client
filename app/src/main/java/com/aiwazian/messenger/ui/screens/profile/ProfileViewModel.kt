/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.MoreVert
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.InviteLinkRepository
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.usecase.DownloadAvatarUseCase
import com.aiwazian.messenger.usecase.JoinViaInviteLinkUseCase
import com.aiwazian.messenger.usecase.LeaveChatUseCase
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.RegexPatterns
import com.aiwazian.messenger.utils.ShortcutManager
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
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
    private val searchRepository: SearchRepository,
    private val inviteLinkRepository: InviteLinkRepository,
    private val shortcutManager: ShortcutManager,
    private val clipboardService: ClipboardService,
    private val vibrationManager: VibrationManager,
    private val downloadAvatarUseCase: DownloadAvatarUseCase,
    private val joinViaInviteLinkUseCase: JoinViaInviteLinkUseCase,
    private val leaveChatUseCase: LeaveChatUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<ProfileUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    private val downloadingAvatars = mutableSetOf<String>()
    
    private var isInit = false
    
    fun init(profileId: Long, profileName: String?, avatarUri: Uri?) {
        if (isInit) return
        isInit = true
        
        _uiState.update {
            it.copy(
                id = profileId,
                title = UiText.DynamicString(profileName.orEmpty()),
                avatars = if (avatarUri != null) listOf(avatarUri) else emptyList()
            )
        }
        
        viewModelScope.launch {
            when (ChatType.fromId(profileId)) {
                ChatType.CHANNEL -> channelRepository.fetchById(profileId)
                ChatType.GROUP -> groupRepository.fetchById(profileId)
                ChatType.PRIVATE -> userRepository.fetchById(profileId)
                else -> {}
            }
        }
        
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
                                    title = UiText.DynamicString("${user.firstName} ${user.lastName.orEmpty()}".trim()),
                                    subTitle = UiText.DynamicString("в сети недавно"),
                                    profile = profile,
                                    avatars = user.avatars.map { avatar -> avatar.uri }
                                )
                            }
                            recalculateActions()
                            user.avatars.filter { it.uri == null && downloadingAvatars.add(it.fileId) }
                                .forEach { avatar ->
                                    viewModelScope.launch {
                                        downloadAvatarUseCase(profileId, avatar.fileId)
                                            .onFailure {
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
                                    title = UiText.DynamicString("${user.firstName} ${user.lastName.orEmpty()}".trim()),
                                    subTitle = UiText.DynamicString("в сети недавно"),
                                    profile = profile,
                                    avatars = user.avatars.map { avatar -> avatar.uri }
                                )
                            }
                            recalculateActions()
                            
                            user.avatars.filter { it.uri == null && downloadingAvatars.add(it.fileId) }
                                .forEach { avatar ->
                                    viewModelScope.launch {
                                        downloadAvatarUseCase(profileId, avatar.fileId)
                                            .onFailure {
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
                                title = UiText.DynamicString(channel.name),
                                subTitle = UiText.PluralResource(
                                    R.plurals.subscribers_count,
                                    channel.subscribers,
                                    channel.subscribers
                                ),
                                profile = profile,
                                avatars = channel.avatars.map { avatar -> avatar.uri }
                            )
                        }
                        recalculateActions()
                        
                        channel.avatars.filter { it.uri == null && downloadingAvatars.add(it.fileId) }
                            .forEach { avatar ->
                                viewModelScope.launch {
                                    downloadAvatarUseCase(profileId, avatar.fileId)
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
                                    title = UiText.DynamicString(group.name),
                                    subTitle = UiText.PluralResource(
                                        R.plurals.members_count,
                                        group.members,
                                        group.members
                                    ),
                                    profile = profile,
                                    avatars = group.avatars.map { avatar -> avatar.uri }
                                )
                            }
                            recalculateActions()
                            
                            group.avatars.filter { it.uri == null && downloadingAvatars.add(it.fileId) }
                                .forEach { avatar ->
                                    viewModelScope.launch {
                                        downloadAvatarUseCase(profileId, avatar.fileId)
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
            DropdownMenuAction(
                icon = Icons.Rounded.AddHome,
                text = UiText.StringResource(R.string.add_to_home_screen),
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
            DropdownMenuAction(
                icon = Icons.Rounded.AddHome,
                text = UiText.StringResource(R.string.add_to_home_screen),
                onClick = {
                    createChatShortcut(_uiState.value.title.asString(context))
                }
            )
        )
        
        if (channel.isSubscribed && channel.ownerId != _uiState.value.myId) {
            dropdownActions.add(
                DropdownMenuAction(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    text = UiText.StringResource(R.string.leave_channel),
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
            DropdownMenuAction(
                icon = Icons.Rounded.AddHome,
                text = UiText.StringResource(R.string.add_to_home_screen),
                onClick = {
                    createChatShortcut(_uiState.value.title.asString(context))
                }
            )
        )
        
        if (group.ownerId != _uiState.value.myId) {
            dropdownActions.add(
                DropdownMenuAction(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    text = UiText.StringResource(R.string.leave_group),
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
    
    fun onLinkClicked(url: String) {
        val inviteLinkRegex = RegexPatterns.INVITE_LINK
        val match = inviteLinkRegex.find(url)
        
        if (match == null) {
            val normalizedUrl =
                if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
            viewModelScope.launch {
                _uiEffect.emit(ProfileUiEffect.OpenUrl(normalizedUrl))
            }
            return
        }
        
        val code = match.groupValues[2]
        
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingInvite = true) }
            
            inviteLinkRepository.getInviteLinkInfo(code).onSuccess { linkInfo ->
                if (_uiState.value.id == linkInfo.chatId) {
                    _uiState.update { it.copy(isProcessingInvite = false) }
                    _uiEffect.emit(ProfileUiEffect.ShowSnackbar(UiText.StringResource(R.string.you_are_already_in_this_chat)))
                    vibrationManager.vibrate(VibrationPattern.Error)
                } else if (linkInfo.isJoined != null) {
                    _uiState.update { it.copy(isProcessingInvite = false) }
                    _uiEffect.emit(ProfileUiEffect.NavigateToChat(linkInfo.chatId))
                } else if (linkInfo.isBanned != null) {
                    _uiState.update {
                        it.copy(
                            showBannedDialog = true,
                            isProcessingInvite = false
                        )
                    }
                    vibrationManager.vibrate(VibrationPattern.Error)
                } else {
                    _uiState.update {
                        it.copy(
                            inviteLinkInfo = linkInfo,
                            inviteLinkCode = code,
                            showInviteBottomSheet = true,
                            isProcessingInvite = false
                        )
                    }
                }
            }.onFailure {
                _uiState.update { it.copy(isProcessingInvite = false) }
                _uiEffect.emit(ProfileUiEffect.ShowSnackbar(UiText.StringResource(R.string.invalid_link)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun onUsernameClicked(username: String) {
        viewModelScope.launch {
            val cleanUsername = username.removePrefix("@")
            searchRepository.resolveUsername(cleanUsername).onSuccess { result ->
                if (result == null) {
                    _uiEffect.emit(ProfileUiEffect.ShowSnackbar(UiText.StringResource(R.string.chat_not_found)))
                    vibrationManager.vibrate(VibrationPattern.Error)
                } else if (result.isBanned) {
                    _uiState.update { it.copy(showBannedDialog = true) }
                    vibrationManager.vibrate(VibrationPattern.Error)
                } else {
                    _uiEffect.emit(ProfileUiEffect.NavigateToChat(result.chatId))
                }
            }.onFailure {
                _uiEffect.emit(ProfileUiEffect.ShowSnackbar(UiText.StringResource(R.string.error_searching_for_chat)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun onSubscribeViaInviteLink() {
        val info = _uiState.value.inviteLinkInfo ?: return
        val code = _uiState.value.inviteLinkCode ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingInvite = true) }
            
            val result = joinViaInviteLinkUseCase(code, info.chatId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isProcessingInvite = false,
                        showInviteBottomSheet = false,
                        inviteLinkInfo = null,
                        inviteLinkCode = null
                    )
                }
                _uiEffect.emit(ProfileUiEffect.NavigateToChat(info.chatId))
            } else {
                _uiState.update { it.copy(isProcessingInvite = false) }
                _uiEffect.emit(ProfileUiEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_join)))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun dismissInviteBottomSheet() {
        _uiState.update {
            it.copy(
                showInviteBottomSheet = false,
                inviteLinkInfo = null,
                inviteLinkCode = null,
                isProcessingInvite = false
            )
        }
    }
    
    fun dismissBannedDialog() {
        _uiState.update { it.copy(showBannedDialog = false) }
    }
}
