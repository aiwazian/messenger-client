/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aiwazian.messenger.extensions.sharedElement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileImageCarousel(
    id: Long,
    avatars: List<Uri?>
) {
    val carouselState = rememberCarouselState { avatars.size }
    val containerWidth = LocalWindowInfo.current.containerDpSize.width
    
    val width = if (containerWidth < 500.dp) {
        containerWidth
    } else {
        300.dp
    }
    
    HorizontalUncontainedCarousel(
        state = carouselState,
        itemWidth = width,
        flingBehavior = if (containerWidth < 500.dp) {
            CarouselDefaults.singleAdvanceFlingBehavior(carouselState)
        } else {
            CarouselDefaults.multiBrowseFlingBehavior(carouselState)
        }
    ) { index ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(avatars[index], label = "avatar_anim") { uri ->
                if (uri == null) {
                    CircularWavyProgressIndicator()
                } else {
                    val modifier = if (index == 1) {
                        Modifier.sharedElement(key = "chat-avatar-$id")
                    } else {
                        Modifier
                    }
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(modifier),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}
