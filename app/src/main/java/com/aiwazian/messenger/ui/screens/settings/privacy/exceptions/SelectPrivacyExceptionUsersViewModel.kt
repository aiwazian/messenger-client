/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.privacy.exceptions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.PrivacyExceptionKind
import com.aiwazian.messenger.enums.PrivacyField
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectPrivacyExceptionUsersViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    chatRepository: ChatRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectPrivacyExceptionUsersUiState())
    val uiState = _uiState.asStateFlow()

    private var allUsers: List<Chat> = emptyList()
    private var field: PrivacyField? = null
    private var kind: PrivacyExceptionKind? = null
    private var selectionRestored = false

    init {
        viewModelScope.launch {
            val selfId = userRepository.getMe().firstOrNull()?.id

            chatRepository.getAllChats().collectLatest { chats ->
                allUsers = chats.filter { chat ->
                    ChatType.fromId(chat.id) == ChatType.PRIVATE && chat.id != selfId
                }

                _uiState.update { state ->
                    state.copy(users = filterUsers(allUsers, state.query))
                }
            }
        }
    }

    fun restoreSelection(
        field: PrivacyField,
        kind: PrivacyExceptionKind,
        userIds: List<Long>
    ) {
        if (selectionRestored) {
            return
        }
        selectionRestored = true

        this.field = field
        this.kind = kind

        _uiState.update { state ->
            state.copy(selectedUserIds = userIds.toSet())
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(query = query, users = filterUsers(allUsers, query))
        }
    }

    fun toggleUser(userId: Long) {
        _uiState.update { state ->
            val selected = if (userId in state.selectedUserIds) {
                state.selectedUserIds - userId
            } else {
                state.selectedUserIds + userId
            }
            state.copy(selectedUserIds = selected)
        }
    }

    fun buildSelection(): PrivacyExceptionSelection {
        val state = _uiState.value
        return PrivacyExceptionSelection(
            field = requireNotNull(field),
            kind = requireNotNull(kind),
            userIds = state.selectedUserIds.toList()
        )
    }

    private fun filterUsers(users: List<Chat>, query: String): List<Chat> {
        if (query.isBlank()) {
            return users
        }

        return users.filter { user ->
            user.chatName.asString(context).contains(query.trim(), ignoreCase = true)
        }
    }
}
