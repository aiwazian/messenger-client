/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.sharedElement
import com.aiwazian.messenger.ui.animations.expressiveScaleIn
import com.aiwazian.messenger.ui.animations.expressiveScaleOut
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
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onClearSearchQuery: () -> Unit = {},
    onStartSearch: () -> Unit = {},
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
    val searchFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) runCatching { searchFocusRequester.requestFocus() }
    }
    
    TopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSearchActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle.Default.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp,
                                fontSize = 16.sp
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.padding(
                                        start = 14.dp, top = 12.dp, bottom = 12.dp
                                    )
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.search),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp,
                                            fontSize = 16.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            })
                        
                        AnimatedVisibility(
                            visible = searchQuery.isNotEmpty(),
                            enter = expressiveScaleIn,
                            exit = expressiveScaleOut
                        ) {
                            IconButton(onClick = onClearSearchQuery) {
                                Icon(Icons.Rounded.Close, null)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onLongClick = onStartSearch,
                                onClick = {
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
                                    
                                    AnimatedContent(
                                        targetState = isMuted,
                                        transitionSpec = {
                                            expressiveScaleIn togetherWith expressiveScaleOut
                                        }
                                    ) { muted ->
                                        if (muted) {
                                            Icon(
                                                imageVector = Icons.Outlined.NotificationsOff,
                                                contentDescription = stringResource(R.string.chat_notifications_disabled),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
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
            }
        }, navigationIcon = {
            Box(
                modifier = Modifier.background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = CircleShape
                )
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.padding(1.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                }
            }
        }, actions = {
            if (isSearchActive) return@TopAppBar
            
            topBarActions.forEachIndexed { index, action ->
                var expand by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = CircleShape
                    )
                ) {
                    IconButton(
                        onClick = {
                            expand = true
                        },
                        modifier = Modifier.padding(1.dp),
                    ) {
                        Icon(action.icon, null)
                    }
                }
                AppDropdownMenu(expanded = expand, onDismissRequest = { expand = false }) {
                    if (index == topBarActions.lastIndex) {
                        AppDropdownMenuItem(leadingIcon = {
                            Icon(Icons.Outlined.PermMedia, null)
                        }, text = stringResource(R.string.chat_media), onClick = {
                            expand = false
                            navBackStack.add(
                                AppRoute.ChatMedia(chatId = chatId, chatName = title)
                            )
                        })
                        
                        AppDropdownMenuItem(leadingIcon = {
                            Icon(Icons.Rounded.Search, null)
                        }, text = stringResource(R.string.search), onClick = {
                            expand = false
                            onStartSearch()
                        })
                        
                        AppDropdownMenuItem(
                            leadingIcon = {
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
