/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.invites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.InviteLink
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.InviteLinkRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.DialogController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupInviteLinksViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val chatRepository: ChatRepository,
    private val inviteLinkRepository: InviteLinkRepository,
    private val clipboardService: ClipboardService
) : ViewModel() {

    private val _activeInviteLinks = MutableStateFlow<List<InviteLink>>(emptyList())
    val activeInviteLinks = _activeInviteLinks.asStateFlow()

    private val _inactiveInviteLinks = MutableStateFlow<List<InviteLink>>(emptyList())
    val inactiveInviteLinks = _inactiveInviteLinks.asStateFlow()

    private var groupId: Long = -1

    val deleteDialog = DialogController()
    private var linkToDelete: Long? = null

    private val _expandedMenuId = MutableStateFlow<Long?>(null)
    val expandedMenuId = _expandedMenuId.asStateFlow()

    private val _isShareSheetVisible = MutableStateFlow(false)
    val isShareSheetVisible = _isShareSheetVisible.asStateFlow()

    private val _availableChats = MutableStateFlow<List<Chat>>(emptyList())
    val availableChats = _availableChats.asStateFlow()

    private val _selectedChatIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedChatIds = _selectedChatIds.asStateFlow()

    private val _linkToShare = MutableStateFlow<String?>(null)
    
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    fun init(groupId: Long) {
        this.groupId = groupId
        loadLinks()
    }

    private fun loadLinks() {
        viewModelScope.launch {
            val result = groupRepository.getInviteLinks(groupId)
            if (result.isSuccess) {
                val links = result.getOrNull() ?: emptyList()
                val now = System.currentTimeMillis()
                
                val (active, inactive) = links.partition { link ->
                    val isExpired = link.expiresAt != null && link.expiresAt.toLongOrNull()?.let { it < now } == true
                    val isExhausted = link.maxUses != null && link.uses >= link.maxUses
                    !isExpired && !isExhausted
                }
                
                _activeInviteLinks.value = active
                _inactiveInviteLinks.value = inactive
            }
        }
    }

    fun deleteLink(inviteLinkId: Long) {
        viewModelScope.launch {
            val result = inviteLinkRepository.deleteInviteLink(inviteLinkId)
            if (result.isSuccess) {
                loadLinks()
            }
        }
    }

    fun toggleMenu(inviteLinkId: Long?) {
        _expandedMenuId.value = inviteLinkId
    }

    fun showDeleteConfirmation(inviteLinkId: Long) {
        linkToDelete = inviteLinkId
        deleteDialog.show()
        toggleMenu(null)
    }

    fun confirmDelete() {
        linkToDelete?.let { id ->
            deleteLink(id)
            linkToDelete = null
            deleteDialog.hide()
        }
    }

    fun shareLink(link: String) {
        _linkToShare.value = link
        toggleMenu(null)
        loadAvailableChats()
        _isShareSheetVisible.value = true
    }

    private fun loadAvailableChats() {
        viewModelScope.launch {
            chatRepository.getAllChats().collect { chats ->
                val filtered = chats.filter { chat ->
                    val type = ChatType.fromId(chat.id)
                    when (type) {
                        ChatType.PRIVATE -> true
                        ChatType.GROUP -> true
                        ChatType.CHANNEL -> true
                        else -> false
                    }
                }
                _availableChats.value = filtered
            }
        }
    }

    fun toggleChatSelection(chatId: Long) {
        _selectedChatIds.value = if (_selectedChatIds.value.contains(chatId)) {
            _selectedChatIds.value - chatId
        } else {
            _selectedChatIds.value + chatId
        }
    }

    fun sendLink() {
        val link = _linkToShare.value ?: return
        val chatIds = _selectedChatIds.value
        if (chatIds.isEmpty()) return

        viewModelScope.launch {
            chatIds.forEach { chatId ->
                chatRepository.sendMessage(chatId, link)
            }
            val count = chatIds.size
            _isShareSheetVisible.value = false
            _selectedChatIds.value = emptySet()
            _snackbarMessage.emit("Ссылка отправлена в $count чата")
        }
    }

    fun hideShareSheet() {
        _isShareSheetVisible.value = false
        _selectedChatIds.value = emptySet()
    }

    fun copyLink(link: String) {
        clipboardService.copy(link)
        toggleMenu(null)
    }
}
