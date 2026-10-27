/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem

@Composable
fun SendMessageOptionsButton(
    onClick: () -> Unit,
    onSendSilently: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    optionsEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var optionsExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val shape = CircleShape

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(IconButtonDefaults.smallContainerSize())
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(focusRingShape = shape),
                enabled = enabled,
                role = Role.Button,
                onLongClick = if (optionsEnabled) {
                    { optionsExpanded = true }
                } else {
                    null
                },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()

        AppDropdownMenu(
            expanded = optionsExpanded,
            onDismissRequest = { optionsExpanded = false },
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                MenuAnchorPosition.Above
            )
        ) {
            AppDropdownMenuItem(
                text = stringResource(R.string.send_without_sound),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsOff,
                        contentDescription = null
                    )
                },
                onClick = {
                    optionsExpanded = false
                    onSendSilently()
                }
            )
        }
    }
}
