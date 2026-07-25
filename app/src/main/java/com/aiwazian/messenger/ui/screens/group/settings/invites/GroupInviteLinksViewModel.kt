/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.group.settings.invites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.isNetworkError
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.GroupRepository
import com.aiwazian.messenger.ui.components.ShareItem
import com.aiwazian.messenger.usecase.SendMessageUseCase
import com.aiwazian.messenger.utils.ClipboardService
import com.aiwazian.messenger.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupInviteLinksViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val chatRepository: ChatRepository,
    private val clipboardService: ClipboardService,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GroupInviteLinkUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _uiEffect = MutableSharedFlow<GroupInviteLinkUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    
    fun init(groupId: Long) {
        _uiState.update { it.copy(groupId = groupId) }
        loadLinks()
    }
    
    private fun loadLinks() {
        viewModelScope.launch {
            groupRepository.getInviteLinks(_uiState.value.groupId).onSuccess { links ->
                val now = System.currentTimeMillis()
                
                val (active, inactive) = links.partition { link ->
                    val isExpired = link.expiresAt != null && link.expiresAt < now
                    val isExhausted =
                        (link.maxUses != null && link.uses != null) && link.uses >= link.maxUses
                    !isExpired && !isExhausted
                }
                
                _uiState.update { it.copy(activeLinks = active, inactiveLinks = inactive) }
            }.onFailure {
                if (it.isNetworkError()) {
                    _uiEffect.emit(GroupInviteLinkUiEffect.ShowSnackbar(UiText.StringResource(R.string.failed_to_connect)))
                }
            }
        }
    }
    
    fun showDeleteConfirmation(linkId: Long) {
        _uiState.update { it.copy(expandedMenuId = null, linkIdToDelete = linkId) }
    }
    
    fun confirmDelete() {
        val linkId = _uiState.value.linkIdToDelete ?: return
        viewModelScope.launch {
            groupRepository.deleteInviteLink(_uiState.value.groupId, linkId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            activeLinks = it.activeLinks.filter { link -> link.id != linkId },
                            inactiveLinks = it.inactiveLinks.filter { link -> link.id != linkId },
                            linkIdToDelete = null
                        )
                    }
                }
                .onFailure {
                    if (it.isNetworkError()) {
                        _uiEffect.emit(
                            GroupInviteLinkUiEffect.ShowSnackbar(
                                UiText.StringResource(
                                    R.string.failed_to_connect
                                )
                            )
                        )
                    }
                    _uiEffect.emit(
                        GroupInviteLinkUiEffect.ShowSnackbar(
                            UiText.StringResource(
                                R.string.unexpected_error
                            )
                        )
                    )
                    _uiState.update { state -> state.copy(linkIdToDelete = null) }
                }
        }
    }
    
    fun shareLink(linkId: Long) {
        _uiState.update { it.copy(expandedMenuId = null) }
        val link = _uiState.value.activeLinks.find { it.id == linkId }
            ?: _uiState.value.inactiveLinks.find { it.id == linkId } ?: return
        val fullLink = "aiwazian.ru/${link.code}"
        
        _uiState.update { it.copy(linkToShare = fullLink, showShareSheet = true) }
        loadAvailableChats()
    }
    
    private fun loadAvailableChats() {
        viewModelScope.launch {
            chatRepository.getAllChats().firstOrNull()?.let { chats ->
                val shareItems = chats.map { chat ->
                    ShareItem(
                        id = chat.id,
                        name = chat.chatName,
                        isSelected = _uiState.value.selectedChatIds.contains(chat.id),
                        avatarUri = chat.avatarUri
                    )
                }
                _uiState.update { it.copy(availableChats = shareItems) }
            }
        }
    }
    
    fun toggleChatSelection(chatId: Long) {
        _uiState.update { state ->
            val newSelected = if (state.selectedChatIds.contains(chatId)) {
                state.selectedChatIds - chatId
            } else {
                state.selectedChatIds + chatId
            }
            state.copy(
                selectedChatIds = newSelected,
                availableChats = state.availableChats.map {
                    if (it.id == chatId) it.copy(isSelected = newSelected.contains(it.id)) else it
                }
            )
        }
    }
    
    fun sendLink() {
        val link = _uiState.value.linkToShare ?: return
        val chatIds = _uiState.value.selectedChatIds
        if (chatIds.isEmpty()) return
        
        viewModelScope.launch {
            chatIds.forEach { chatId ->
                sendMessageUseCase(chatId, link)
            }
            val count = chatIds.size
            _uiState.update {
                it.copy(
                    showShareSheet = false,
                    linkToShare = null,
                    selectedChatIds = emptySet()
                )
            }
            _uiEffect.emit(GroupInviteLinkUiEffect.ShowSnackbar(UiText.DynamicString("Ссылка отправлена в $count чата")))
        }
    }
    
    fun hideShareSheet() {
        _uiState.update {
            it.copy(
                showShareSheet = false,
                linkToShare = null,
                selectedChatIds = emptySet(),
                availableChats = it.availableChats.map { chat -> chat.copy(isSelected = false) }
            )
        }
    }
    
    fun copyLink(linkId: Long) {
        _uiState.update { it.copy(expandedMenuId = null) }
        val link = _uiState.value.activeLinks.find { it.id == linkId }
            ?: _uiState.value.inactiveLinks.find { it.id == linkId } ?: return
        val fullLink = "aiwazian.ru/${link.code}"
        clipboardService.copy(fullLink)
    }
    
    fun hideDeleteDialog() {
        _uiState.update { it.copy(linkIdToDelete = null) }
    }
    
    fun setExpandedMenuId(menuId: Long?) {
        _uiState.update { it.copy(expandedMenuId = menuId) }
    }
}
