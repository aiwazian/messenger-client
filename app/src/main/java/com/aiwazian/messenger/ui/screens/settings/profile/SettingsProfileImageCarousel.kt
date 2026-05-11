/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Avatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsProfileImageCarousel(
    avatars: List<Avatar>,
    onAddPhoto: (Uri) -> Unit,
    onDeletePhoto: (String) -> Unit
) {
    val carouselState = rememberCarouselState { avatars.size + 1 }
    
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAddPhoto(uri)
        }
    }
    
    HorizontalCenteredHeroCarousel(
        state = carouselState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 10.dp)
    ) { i ->
        if (i < avatars.size) {
            val avatar = avatars[i]
            Box {
                var expanded by remember { mutableStateOf(false) }
                val isCentered = carouselState.currentItem == i
                
                LaunchedEffect(carouselState.isScrollInProgress) {
                    expanded = false
                }
                
                AsyncImage(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                        .maskClip(MaterialTheme.shapes.large),
                    model = avatar.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = isCentered,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    AnimatedContent(
                        targetState = expanded,
                        transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() },
                        modifier = Modifier.align(Alignment.BottomCenter),
                        contentAlignment = Alignment.Center
                    ) { expand ->
                        if (expand) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(bottom = 4.dp)
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(
                                        start = 10.dp,
                                        top = 10.dp,
                                        end = 10.dp,
                                        bottom = 6.dp
                                    ), horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Удалить это фото?",
                                        modifier = Modifier.padding(6.dp)
                                    )
                                    Row {
                                        TextButton(onClick = { expanded = false }) {
                                            Text(stringResource(R.string.no))
                                        }
                                        TextButton(
                                            onClick = {
                                                onDeletePhoto(avatar.fileId)
                                                expanded = false
                                            }, colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text(stringResource(R.string.yes))
                                        }
                                    }
                                }
                            }
                        } else {
                            IconButton(
                                onClick = { expanded = true },
                                modifier = Modifier.align(Alignment.Center),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, null)
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                        targetValue = if (isPressed) 0.94f else 1f,
                        label = "add_photo_button_scale_animation"
                    )
                    Button(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        interactionSource = interactionSource,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
                    ) {
                        Text(stringResource(R.string.add_photo))
                        Icon(
                            Icons.Outlined.AddAPhoto, null,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(start = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
