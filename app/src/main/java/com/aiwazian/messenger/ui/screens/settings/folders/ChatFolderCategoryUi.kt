/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.folders

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ChatFolderCategory
import com.aiwazian.messenger.utils.UiText

fun ChatFolderCategory.titleText(): UiText = when (this) {
    ChatFolderCategory.PRIVATE_CHATS -> UiText.StringResource(R.string.private_chats)
    ChatFolderCategory.CHANNELS -> UiText.StringResource(R.string.channels)
    ChatFolderCategory.GROUPS -> UiText.StringResource(R.string.groups)
}

fun ChatFolderCategory.icon(): ImageVector = when (this) {
    ChatFolderCategory.PRIVATE_CHATS -> Icons.Rounded.Person
    ChatFolderCategory.CHANNELS -> Icons.Rounded.Campaign
    ChatFolderCategory.GROUPS -> Icons.Rounded.Group
}
