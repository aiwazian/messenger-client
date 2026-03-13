package com.aiwazian.messenger.ui.element

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MinimizeChatCard(
    chatName: String,
    selected: Boolean = false,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = { }
) {
    ListItem(
        modifier = Modifier.clickable {
            onClick()
        },
        leadingContent = {
            Box(modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    AnimatedContent(targetState = selected) { isVisible ->
                        if (isVisible) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .size(16.dp)
                                    .background(Color.Green),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        trailingContent = trailingContent,
        headlineContent = {
            Text(chatName)
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}
