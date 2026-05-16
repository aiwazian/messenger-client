/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.profile

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularWavyProgressIndicator
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

@Composable
fun ProfileImageCarousel(
    profileId: Long,
    avatars: List<Uri?>
) {
    val carouselState = rememberCarouselState { avatars.size }
    val containerWidth = LocalWindowInfo.current.containerDpSize.width
    
    val itemWidth = if (containerWidth < 500.dp) {
        containerWidth
    } else {
        300.dp
    }
    
    HorizontalUncontainedCarousel(
        state = carouselState,
        itemWidth = itemWidth,
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
            val uri = avatars[index]
            if (uri == null) {
                CircularWavyProgressIndicator()
            } else {
                val modifier = if (index == 0) {
                    Modifier.sharedElement(key = "chat-avatar-$profileId")
                } else {
                    Modifier
                }
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = modifier.then(Modifier.fillMaxSize()),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
