/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.share

import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppDropdownMenuGroup
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.app.AppDropdownMenuPopup
import com.aiwazian.messenger.ui.components.BottomBarScrim
import com.aiwazian.messenger.ui.components.CustomEmojiViewModel
import com.aiwazian.messenger.ui.components.ShareForwardMenuItems
import com.aiwazian.messenger.ui.components.ShareForwardOptions
import com.aiwazian.messenger.ui.screens.chat.ChatEmojiViewModel
import com.aiwazian.messenger.ui.screens.chat.components.CustomEmojiTextField
import com.aiwazian.messenger.ui.screens.chat.components.EmojiInputPanel
import com.aiwazian.messenger.ui.screens.chat.components.focusMessageInput
import com.aiwazian.messenger.ui.screens.chat.components.hideKeyboardKeepFocus
import com.aiwazian.messenger.ui.screens.chat.components.insertCustomEmoji
import com.aiwazian.messenger.ui.screens.chat.components.insertSystemEmoji
import kotlinx.coroutines.launch

@Composable
fun ShareSendBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hasFiles: Boolean = false,
    enabled: Boolean = true,
    forwardOptions: ShareForwardOptions? = null
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    val emojiViewModel: ChatEmojiViewModel = hiltViewModel()
    val emojiState by emojiViewModel.uiState.collectAsState()
    
    val customEmojiViewModel: CustomEmojiViewModel = hiltViewModel()
    
    var inputView by remember { mutableStateOf<EditText?>(null) }
    var isEmojiPanelVisible by remember { mutableStateOf(false) }
    var measuredKeyboardHeight by remember { mutableStateOf(0.dp) }
    
    val bottomPadding = with(density) {
        WindowInsets.navigationBars.getBottom(density).toDp()
    }
    val imeHeight = with(density) {
        WindowInsets.ime.getBottom(density).toDp()
    }
    val isKeyboardVisible = imeHeight > KEYBOARD_VISIBILITY_THRESHOLD
    val emojiPanelHeight = (measuredKeyboardHeight - bottomPadding)
        .coerceAtLeast(EMOJI_PANEL_MIN_HEIGHT)
    
    LaunchedEffect(imeHeight) {
        if (imeHeight > measuredKeyboardHeight) {
            measuredKeyboardHeight = imeHeight
        }
    }
    
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            isEmojiPanelVisible = false
        }
    }
    
    LaunchedEffect(isEmojiPanelVisible, isKeyboardVisible) {
        if (isEmojiPanelVisible || isKeyboardVisible) {
            emojiViewModel.preloadPacks()
        }
    }
    
    BackHandler(enabled = isEmojiPanelVisible) {
        isEmojiPanelVisible = false
    }
    
    val onEmojiButtonClick: () -> Unit = {
        if (isEmojiPanelVisible) {
            isEmojiPanelVisible = false
            inputView?.let { view -> focusMessageInput(view) }
        } else {
            isEmojiPanelVisible = true
            inputView?.let { view -> hideKeyboardKeepFocus(view) }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clip(INPUT_SHAPE)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = onEmojiButtonClick) {
                AnimatedContent(
                    targetState = isEmojiPanelVisible,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInVertically { -it } + fadeIn() + scaleIn() togetherWith
                                    slideOutVertically { it } + fadeOut() + scaleOut()
                        } else {
                            slideInVertically { it } + fadeIn() + scaleIn() togetherWith
                                    slideOutVertically { -it } + fadeOut() + scaleOut()
                        }
                    }
                ) { isPanelVisible ->
                    Icon(
                        imageVector = if (isPanelVisible) {
                            Icons.Outlined.Keyboard
                        } else {
                            Icons.Outlined.EmojiEmotions
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            CustomEmojiTextField(
                text = text,
                hint = stringResource(
                    if (hasFiles) R.string.media_picker_caption else R.string.message
                ),
                onTextChange = onTextChange,
                onResolveEmoji = customEmojiViewModel::resolveEmoji,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                onViewReady = { view -> inputView = view }
            )
            
            ShareSendOptionsButton(
                enabled = enabled,
                forwardOptions = forwardOptions,
                onSend = onSendClick
            )
        }
        
        AnimatedVisibility(
            visible = isEmojiPanelVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(emojiPanelHeight)
                    .clip(EMOJI_PANEL_SHAPE)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                EmojiInputPanel(
                    packs = emojiState.addedPacks,
                    systemEmojis = emojiState.systemEmojis,
                    onEmojiClick = { pack, emoji ->
                        inputView?.let { view ->
                            scope.launch {
                                insertCustomEmoji(view, pack.id, emoji)
                            }
                        }
                    },
                    onSystemEmojiClick = { emoji ->
                        inputView?.let { view -> insertSystemEmoji(view, emoji) }
                    }
                )
                
                BottomBarScrim(height = bottomPadding)
            }
        }
    }
}

@Composable
private fun ShareSendOptionsButton(
    enabled: Boolean,
    forwardOptions: ShareForwardOptions?,
    onSend: (Boolean) -> Unit
) {
    var optionsExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val shape = CircleShape
    
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(IconButtonDefaults.smallContainerSize())
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(focusRingShape = shape),
                enabled = enabled,
                role = Role.Button,
                onLongClick = if (enabled) {
                    { optionsExpanded = true }
                } else {
                    null
                },
                onClick = { onSend(false) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Send,
            contentDescription = stringResource(R.string.send),
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        
        AppDropdownMenuPopup(
            expanded = optionsExpanded,
            onDismissRequest = { optionsExpanded = false },
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                MenuAnchorPosition.Above
            ),
            scrimColor = Color.Black.copy(alpha = 0.1f),
            properties = PopupProperties(focusable = false, clippingEnabled = false)
        ) {
            if (forwardOptions != null) {
                AppDropdownMenuGroup {
                    ShareForwardMenuItems(forwardOptions)
                }
                
                Spacer(Modifier.height(4.dp))
            }
            
            AppDropdownMenuGroup {
                AppDropdownMenuItem(
                    text = stringResource(R.string.send_without_sound),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsOff,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        optionsExpanded = false
                        onSend(true)
                    }
                )
            }
        }
    }
}

private val INPUT_SHAPE = RoundedCornerShape(24.dp)
private val EMOJI_PANEL_SHAPE = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
private val EMOJI_PANEL_MIN_HEIGHT = 280.dp
private val KEYBOARD_VISIBILITY_THRESHOLD: Dp = 100.dp
