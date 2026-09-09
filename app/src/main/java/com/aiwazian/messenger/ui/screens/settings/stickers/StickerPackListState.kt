package com.aiwazian.messenger.ui.screens.settings.stickers

import androidx.annotation.StringRes
import com.aiwazian.messenger.domain.StickerPack
import com.aiwazian.messenger.ui.components.ShareItem

data class StickerPackListUiState(
    val packs: List<StickerPack> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val sharingPack: StickerPack? = null,
    val shareTargets: List<ShareItem> = emptyList(),
    val selectedShareChatIds: Set<Long> = emptySet()
) {
    val visiblePacks: List<StickerPack>
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

sealed interface StickerPackListEffect {
    data class ShowMessage(@param:StringRes val messageRes: Int) : StickerPackListEffect
}
