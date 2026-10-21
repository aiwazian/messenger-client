/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.enums.AppPrimaryColor
import com.aiwazian.messenger.ui.theme.LocalIsDarkTheme
import com.aiwazian.messenger.utils.PhoneLinks
import kotlin.math.abs

@Composable
fun MessageContact(
    message: Message,
    isMine: Boolean,
    onViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contact = message.contact ?: return
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        targetValue = if (isPressed) 0.98f else 1f,
        label = "reply_quote_scale_animation"
    )
    val region = PhoneLinks.deviceRegion(context)
    val displayPhones = remember(contact.phones, region) {
        contact.phones.map { PhoneLinks.formatInternational(it, region) }
    }
    val accentColor = if (isMine) {
        if (LocalIsDarkTheme.current) Color.White else MaterialTheme.colorScheme.primary
    } else {
        remember(contact.phones, contact.name) {
            contactAccentColor(contact.firstPhone ?: contact.name)
        }
    }
    val titleColor = when {
        !isMine -> accentColor
        LocalIsDarkTheme.current -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.small)
            .background(accentColor.copy(alpha = 0.1f))
            .combinedClickable(
                interactionSource = interactionSource,
                onClick = onViewClick,
                onLongClick = onViewClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(accentColor)
        )
        
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                text = contact.name,
                color = titleColor,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            displayPhones.forEach { phone ->
                ContactFieldText(text = phone)
            }
            
            contact.emails.forEach { email ->
                ContactFieldText(text = email)
            }
            
            contact.urls.forEach { url ->
                ContactFieldText(text = url)
            }
            
            Spacer(Modifier.height(4.dp))
            
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            
            Spacer(Modifier.height(4.dp))
            
            Text(
                text = stringResource(R.string.view_contact).uppercase(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                color = titleColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ContactFieldText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun contactAccentColor(seed: String): Color {
    return AppPrimaryColor.entries[abs(seed.hashCode()) % AppPrimaryColor.entries.size].color
}
