/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.usecase

import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.ui.components.ShareItem
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class GetShareTargetsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        selectedChatIds: Set<Long> = emptySet(),
        canShareTo: suspend (Chat) -> Boolean = { true }
    ): List<ShareItem> {
        val myId = userRepository.getMe().first().id
        val chats = chatRepository.getAllChats().firstOrNull().orEmpty()
        
        val targets = mutableListOf(
            ShareItem(
                id = myId,
                name = UiText.StringResource(R.string.saved_messages),
                isSelected = selectedChatIds.contains(myId),
                isSavedMessages = true
            )
        )
        
        for (chat in chats) {
            if (chat.id == myId) continue
            if (!canShareTo(chat)) continue
            
            targets.add(
                ShareItem(
                    id = chat.id,
                    name = chat.chatName,
                    isSelected = selectedChatIds.contains(chat.id),
                    avatarUri = chat.avatarUri
                )
            )
        }
        
        return targets
    }
}
