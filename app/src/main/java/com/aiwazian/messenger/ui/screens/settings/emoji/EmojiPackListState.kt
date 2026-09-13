package com.aiwazian.messenger.ui.screens.settings.emoji

import androidx.annotation.StringRes
import com.aiwazian.messenger.domain.EmojiPack
import com.aiwazian.messenger.ui.components.ShareItem

data class EmojiPackListUiState(
    val packs: List<EmojiPack> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val sharingPack: EmojiPack? = null,
    val shareTargets: List<ShareItem> = emptyList(),
    val selectedShareChatIds: Set<Long> = emptySet()
) {
    val visiblePacks: List<EmojiPack>
        get() {
            val trimmed = query.trim()
            
            if (trimmed.isEmpty()) {
                return packs
            }
            
            return packs.filter { pack ->
                pack.name.contains(trimmed, ignoreCase = true) ||
                        pack.username.contains(trimmed, ignoreCase = true)
            }
        }
}

sealed interface EmojiPackListEffect {
    data class ShowMessage(@param:StringRes val messageRes: Int) : EmojiPackListEffect
}
