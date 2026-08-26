/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.sharedElement
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.components.AnimatedDotsText
import com.aiwazian.messenger.ui.components.ChatAvatar
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.topBar.TopBarAction

@Composable
fun ChatTopBar(
    title: String,
    avatarUri: Uri?,
    subTitle: String,
    topBarActions: List<TopBarAction>,
    isConnected: Boolean,
    chatId: Long,
    myId: Long,
    isMuted: Boolean = false,
    onToggleNotifications: () -> Unit = {},
    onBackClick: () -> Unit
) {
    val navBackStack = LocalNavBackStack.current
    val isSavedMessages = chatId == myId
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        targetValue = if (isPressed) 0.96f else 1f,
        label = "card_scale_animation"
    )
    
    TopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(
                            interactionSource = interactionSource, indication = null, onClick = {
                                navBackStack.add(
                                    AppRoute.Profile(
                                        profileId = chatId,
                                        profileName = title,
                                        avatarUri = avatarUri?.toString()
                                    )
                                )
                            }), horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSavedMessages) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BookmarkBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            ChatAvatar(id = chatId, chatName = title, avatarUri = avatarUri)
                        }
                        
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    fontSize = 18.sp,
                                    lineHeight = 16.sp,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.sharedElement(key = "chat-name-$chatId")
                                )
                                
                                /* Тот же перечёркнутый колокольчик, что и в списке чатов. */
                                if (isMuted) {
                                    Icon(
                                        imageVector = Icons.Outlined.NotificationsOff,
                                        contentDescription = stringResource(R.string.chat_notifications_disabled),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            AnimatedContent(
                                targetState = isConnected, transitionSpec = {
                                    slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                                }, label = "connection_animation"
                            ) { connected ->
                                if (!connected) {
                                    AnimatedDotsText(
                                        text = stringResource(R.string.connecting),
                                        fontSize = 12.sp,
                                        lineHeight = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (subTitle.isNotBlank()) {
                                    Text(
                                        text = subTitle.lowercase(),
                                        fontSize = 12.sp,
                                        lineHeight = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.sharedElement(key = "chat-sub-title-$chatId")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }, navigationIcon = {
            IconButton(
                onClick = onBackClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
            }
        }, actions = {
            topBarActions.forEachIndexed { index, action ->
                var expand by remember { mutableStateOf(false) }
                IconButton(
                    onClick = {
                        expand = true
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Icon(action.icon, null)
                }
                AppDropdownMenu(expanded = expand, onDismissRequest = { expand = false }) {
                    /*
                     * Уведомления и медиа стоят первыми и только в последнем меню ряда —
                     * тех самых трёх точках, а не в каждом выпадающем списке шапки.
                     *
                     * Удаление чата и очистка истории уходят под них: спутать удаление с
                     * выключением звука дороже, чем сделать лишнее движение пальцем.
                     */
                    if (index == topBarActions.lastIndex) {
                        AppDropdownMenuItem(leadingIcon = {
                            Icon(
                                if (isMuted) Icons.Outlined.Notifications
                                else Icons.Outlined.NotificationsOff, null
                            )
                        }, text = stringResource(
                            if (isMuted) R.string.chat_enable_notifications
                            else R.string.chat_disable_notifications
                        ), onClick = {
                            onToggleNotifications()
                            expand = false
                        })
                        
                        AppDropdownMenuItem(leadingIcon = {
                            Icon(Icons.Outlined.PermMedia, null)
                        }, text = stringResource(R.string.chat_media), onClick = {
                            expand = false
                            navBackStack.add(
                                AppRoute.ChatMedia(chatId = chatId, chatName = title)
                            )
                        })
                    }
                    
                    action.dropdownActions.forEach { dropdownAction ->
                        AppDropdownMenuItem(leadingIcon = {
                            Icon(dropdownAction.icon, null)
                        }, text = dropdownAction.text.asString(), onClick = {
                            dropdownAction.onClick?.invoke()
                            expand = false
                        })
                    }
                }
            }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}
