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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.MoreVert
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.ChatAdminPermissions
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.PrivacyLevel
import com.aiwazian.messenger.repository.ChannelRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.InviteLinkRepository
import com.aiwazian.messenger.repository.PrivacyRepository
import com.aiwazian.messenger.repository.SearchRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.repository.channel.ChannelAdminsRepository
import com.aiwazian.messenger.repository.group.GroupAdminsRepository
import com.aiwazian.messenger.socket.OnlineUsersTracker
import com.aiwazian.messenger.ui.components.topBar.DropdownMenuAction
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import com.aiwazian.messenger.usecase.DownloadAvatarUseCase
import com.aiwazian.messenger.usecase.GetShareTargetsUseCase
import com.aiwazian.messenger.usecase.JoinViaInviteLinkUseCase
import com.aiwazian.messenger.usecase.LeaveChatUseCase
import com.aiwazian.messenger.utils.ActiveChatTracker
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.LastSeenHelper
import com.aiwazian.messenger.utils.MessageSendQueue
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
import kotlinx.coroutines.flow.combine
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
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val channelAdminsRepository: ChannelAdminsRepository,
    private val groupAdminsRepository: GroupAdminsRepository,
    private val searchRepository: SearchRepository,
    private val inviteLinkRepository: InviteLinkRepository,
    private val privacyRepository: PrivacyRepository,
    private val shortcutManager: ShortcutManager,
    private val clipboardService: ClipboardService,
    private val vibrationManager: VibrationManager,
    private val messageSendQueue: MessageSendQueue,
    private val downloadAvatarUseCase: DownloadAvatarUseCase,
    private val getShareTargetsUseCase: GetShareTargetsUseCase,
    private val joinViaInviteLinkUseCase: JoinViaInviteLinkUseCase,
    private val joinChannelUseCase: com.aiwazian.messenger.usecase.JoinChannelUseCase,
    private val joinGroupUseCase: com.aiwazian.messenger.usecase.JoinGroupUseCase,
    private val leaveChatUseCase: LeaveChatUseCase,
    private val onlineUsersTracker: OnlineUsersTracker
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
        
        loadMyPermissions(profileId)
        
        /*
         * loadProfile() выбирает ветку сравнением profileId с myId, поэтому
         * запускать его раньше, чем известен свой идентификатор, нельзя. Из-за
         * этого свой же профиль уходил в ветку «чужой пользователь», где статус
         * берётся из списка онлайна, а сам пользователь в этот список никогда не
         * попадает: сервер не рассылает человеку событие о его собственном
         * статусе. В итоге на своём профиле всегда было «в сети недавно».
         */
        viewModelScope.launch {
            userRepository.getMe().firstOrNull()?.let { user ->
                _uiState.update { it.copy(myId = user.id) }
                recalculateActions()
            }
            
            loadProfile()
        }
    }
    
    /**
     * Загружает мои права в чате, чтобы решить, показывать ли кнопку перехода в настройки.
     */
    private fun loadMyPermissions(profileId: Long) {
        viewModelScope.launch {
            val result = when (ChatType.fromId(profileId)) {
                ChatType.CHANNEL -> channelAdminsRepository.getMyPermissions(profileId)
                ChatType.GROUP -> groupAdminsRepository.getMyPermissions(profileId)
                else -> return@launch
            }
            
            result.onSuccess { permissions ->
                _uiState.update { it.copy(permissions = permissions) }
                recalculateActions()
            }.onFailure { error ->
                Log.e(TAG, "Error load my permissions: ", error)
                _uiState.update { it.copy(permissions = ChatAdminPermissions()) }
                recalculateActions()
            }
        }
    }
    
    /**
     * Подпись под своим именем.
     *
     * Свой статус не приходит по вебсокету и не попадает в список онлайна, зато
     * он и не нужен: приложение открыто, значит пользователь в сети. Показываем
     * ровно то, что видят собеседники, поэтому при приватности «Никто» вместо
     * «в сети» остаётся «в сети недавно».
     *
     * Если настройку получить не удалось, считаем статус видимым: на сервере это
     * тоже значение по умолчанию.
     */
    private suspend fun resolveMyPresenceSubtitle(): UiText {
        val lastSeenPrivacy = privacyRepository.getPrivacySettings().getOrNull()?.lastSeen
        
        return if (lastSeenPrivacy == PrivacyLevel.NOBODY) {
            UiText.StringResource(R.string.last_seen_recently)
        } else {
            UiText.StringResource(R.string.online)
        }
    }
    
    private fun createChatShortcut(chatName: String) {
        shortcutManager.createChatShortcut(
            chatId = _uiState.value.id,
            chatName
        )
    }
    
    /**
     * Начиная с Android 13 система сама показывает плашку о копировании, поэтому
     * свой снекбар не нужен: иначе об одном действии сообщают дважды.
     */
    fun copyToClipboard(text: String) {
        clipboardService.copy(text)
    }
    
    /**
     * «Поделиться» из меню у username: в выбранные чаты уйдёт текст «@username».
     */
    fun onShareUsername(username: String) {
        val text = if (username.startsWith("@")) username else "@$username"
        
        _uiState.update {
            it.copy(
                shareText = text,
                shareTargets = emptyList(),
                selectedShareChatIds = emptySet(),
                showShareBottomSheet = true
            )
        }
        
        viewModelScope.launch {
            val targets = getShareTargetsUseCase()
            _uiState.update { it.copy(shareTargets = targets) }
        }
    }
    
    fun toggleShareTarget(chatId: Long) {
        _uiState.update { state ->
            val selected = if (state.selectedShareChatIds.contains(chatId)) {
                state.selectedShareChatIds - chatId
            } else {
                state.selectedShareChatIds + chatId
            }
            
            state.copy(
                selectedShareChatIds = selected,
                shareTargets = state.shareTargets.map {
                    if (it.id == chatId) it.copy(isSelected = selected.contains(it.id)) else it
                }
            )
        }
    }
    
    /**
     * Отправка идёт через [MessageSendQueue]: шторка закрывается сразу, а сообщение
     * доедет, даже если пользователь тут же уйдёт с экрана.
     */
    fun sendShare() {
        val state = _uiState.value
        val text = state.shareText
        val chatIds = state.selectedShareChatIds
        
        if (text.isBlank() || chatIds.isEmpty()) return
        
        chatIds.forEach { chatId ->
            messageSendQueue.enqueueText(chatId = chatId, text = text)
        }
        
        dismissShareBottomSheet()
        
        viewModelScope.launch {
            _uiEffect.emit(ProfileUiEffect.ShowSnackbar(UiText.StringResource(R.string.share_sent)))
        }
    }
    
    fun dismissShareBottomSheet() {
        _uiState.update {
            it.copy(
                shareText = "",
                shareTargets = emptyList(),
                selectedShareChatIds = emptySet(),
                showShareBottomSheet = false
            )
        }
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
                                profileChannelId = user.profileChannelId,
                                isBlocked = user.isBlocked
                            )
                            _uiState.update {
                                it.copy(
                                    title = UiText.DynamicString("${user.firstName} ${user.lastName.orEmpty()}".trim()),
                                    subTitle = resolveMyPresenceSubtitle(),
                                    profile = profile,
                                    avatars = user.avatars.map { avatar -> avatar.uri }
                                )
                            }
                            recalculateActions()
                            loadProfileChannel(user.profileChannelId)
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
                        combine(
                            userRepository.getById(profileId),
                            onlineUsersTracker.onlineUsers
                        ) { user, onlineUsers ->
                            user to onlineUsers.contains(user.id)
                        }.collectLatest { (user, isOnline) ->
                            val subTitle =
                                LastSeenHelper.getSubtitle(context, isOnline, user.lastSeen)
                            val profile = Profile.User(
                                username = user.username,
                                bio = user.bio,
                                dateOfBirth = user.dateOfBirth,
                                lastSeen = user.lastSeen,
                                profileChannelId = user.profileChannelId,
                                isBlocked = user.isBlocked,
                                isBlockedByThem = user.isBlockedByThem
                            )
                            _uiState.update {
                                it.copy(
                                    title = UiText.DynamicString("${user.firstName} ${user.lastName.orEmpty()}".trim()),
                                    subTitle = subTitle,
                                    profile = profile,
                                    avatars = user.avatars.map { avatar -> avatar.uri }
                                )
                            }
                            recalculateActions()
                            loadProfileChannel(user.profileChannelId)
                            
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
    
    private fun loadProfileChannel(channelId: Long?) {
        if (channelId == null) {
            _uiState.update { it.copy(profileChannelInfo = null) }
            return
        }
        viewModelScope.launch {
            channelRepository.getById(channelId).firstOrNull()?.let { channel ->
                val avatarUri = channel.avatars.firstOrNull()?.uri
                _uiState.update {
                    it.copy(
                        profileChannelInfo = ProfileChannelInfo(
                            id = channel.id,
                            name = channel.name,
                            subscribers = channel.subscribers,
                            avatarUri = avatarUri
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            chatRepository.getLastMessageFlow(channelId).collectLatest { lastMessage ->
                _uiState.update {
                    it.copy(
                        profileChannelInfo = it.profileChannelInfo?.copy(
                            lastMessage = lastMessage
                        )
                    )
                }
            }
        }
    }
    
    private fun recalculateActions() {
        val newActions = when (val profile = _uiState.value.profile) {
            is Profile.User -> calculateUserActions(profile)
            
            is Profile.Channel -> calculateChannelActions(profile)
            
            is Profile.Group -> calculateGroupActions(profile)
            
            else -> emptyList()
        }
        
        _uiState.update { it.copy(actions = newActions) }
    }
    
    private fun calculateUserActions(user: Profile.User): List<TopBarAction> {
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
            dropdownActions.add(
                DropdownMenuAction(
                    icon = Icons.Rounded.Block,
                    text = UiText.StringResource(if (user.isBlocked) R.string.unblock else R.string.block_user),
                    onClick = {
                        _uiState.update {
                            it.copy(
                                showBlockDialog = true,
                                isBlockedStateForDialog = user.isBlocked
                            )
                        }
                    }
                )
            )
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
        
        val isOwner = channel.ownerId == _uiState.value.myId
        
        if (channel.isSubscribed && !isOwner) {
            dropdownActions.add(
                DropdownMenuAction(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    text = UiText.StringResource(R.string.leave_channel),
                    onClick = ::showLeaveDialog
                )
            )
        }
        
        return buildList {
            if (isOwner || _uiState.value.canOpenChatSettings) {
                add(
                    TopBarAction(
                        icon = Icons.Filled.Edit,
                        onClick = { navigateToChannelSettings(_uiState.value.id) }
                    )
                )
            }
            
            add(
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
        
        val isOwner = group.ownerId == _uiState.value.myId
        
        if (!isOwner && group.isMember) {
            dropdownActions.add(
                DropdownMenuAction(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    text = UiText.StringResource(R.string.leave_group),
                    onClick = ::showLeaveDialog
                )
            )
        }
        
        return buildList {
            if (isOwner || _uiState.value.canOpenChatSettings) {
                add(
                    TopBarAction(
                        icon = Icons.Filled.Edit,
                        onClick = { navigateToGroupSettings(_uiState.value.id) }
                    )
                )
            }
            
            add(
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
    
    fun onChatButtonClicked() {
        viewModelScope.launch {
            if (ActiveChatTracker.activeChatId.value == _uiState.value.id) {
                _uiEffect.emit(ProfileUiEffect.NavigateBack)
            } else {
                _uiEffect.emit(ProfileUiEffect.NavigateToChat(_uiState.value.id))
            }
        }
    }
    
    fun onJoinClicked() {
        viewModelScope.launch {
            val profileId = _uiState.value.id
            when (ChatType.fromId(profileId)) {
                ChatType.CHANNEL -> {
                    joinChannelUseCase(profileId).onSuccess {
                        channelRepository.fetchById(profileId)
                    }
                }
                
                ChatType.GROUP -> {
                    joinGroupUseCase(profileId).onSuccess {
                        groupRepository.fetchById(profileId)
                    }
                }
                
                else -> {}
            }
        }
    }
    
    fun dismissBlockDialog() {
        _uiState.