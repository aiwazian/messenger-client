/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.profile

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.toInstance
import com.aiwazian.messenger.extensions.toPrettyDateWithYear
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.section.SectionDescription
import com.aiwazian.messenger.ui.components.section.SectionHeader
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.NavigationIcon
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.components.topBar.TopBarAction
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsProfileScreen(viewModel: SettingsProfileViewModel = hiltViewModel()) {
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is SettingsProfileSideEffect.NavigateBack -> navBackStack.removeLastOrNull()
            }
        }
    }
    
    val scrollState = rememberScrollState()
    var uri by remember { mutableStateOf<Uri?>(null) }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = { Text(stringResource(R.string.profile)) }, navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = navBackStack::removeLastOrNull
                ), actions = listOf(
                    TopBarAction(
                        icon = Icons.Rounded.Check, onClick = viewModel::onSaveAndBack
                    )
                )
            )
        }, modifier = Modifier.imePadding()
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            
            val d = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri2 ->
                if (uri2 != null) {
                    uri = uri2
                }
            }
            
            Box(modifier = Modifier.padding(start = 10.dp)) {
                SectionHeader(title = stringResource(R.string.profile_photos))
            }
            
            val carouselState = rememberCarouselState { uiState.user.avatars.size + 1 }
            
            HorizontalCenteredHeroCarousel(
                state = carouselState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                itemSpacing = 8.dp,
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) { i ->
                if (i < uiState.user.avatars.size) {
                    val avatar = uiState.user.avatars[i]
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
                                                        viewModel.deleteAvatar(avatar.fileId)
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
                                    d.launch(
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
            
            SectionContainer(header = {
                SectionHeader(title = stringResource(R.string.your_name))
            }) {
                FramelessTextBox(
                    placeholder = stringResource(R.string.first_name),
                    value = uiState.user.firstName,
                    onValueChange = viewModel::onChangeFirstName
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = 1.dp,
                )
                
                FramelessTextBox(
                    placeholder = stringResource(R.string.last_name),
                    value = uiState.user.lastName.orEmpty(),
                    onValueChange = viewModel::onChangeLastName
                )
            }
            
            SectionContainer(header = {
                SectionHeader(title = stringResource(R.string.bio))
            }, footer = {
                SectionDescription(text = "В настройках можно выбрать, кому они будут видны.")
            }) {
                FramelessTextBox(
                    placeholder = stringResource(R.string.write_about_me),
                    value = uiState.user.bio.orEmpty(),
                    onValueChange = viewModel::onChangeBio
                )
            }
            
            SectionContainer(header = {
                SectionHeader(title = stringResource(R.string.username))
            }, footer = {
                SectionDescription("Другие пользователи смогут найти Вас по такому имени и связаться.")
            }) {
                SectionItem(
                    headlineText = if (uiState.user.username != null) {
                        "@${uiState.user.username}"
                    } else {
                        "Задать имя пользователя"
                    }, onClick = {
                        navBackStack.add(AppRoute.SettingsUsername(uiState.user.username))
                    })
            }
            
            SectionContainer(header = {
                SectionHeader(title = stringResource(R.string.date_of_birth))
            }) {
                SectionItem(
                    headlineText = "Дата Вашего рождения",
                    trailingText = if (uiState.user.dateOfBirth != null) {
                        uiState.user.dateOfBirth!!.toInstance().toPrettyDateWithYear()
                    } else {
                        "Указать"
                    },
                    onClick = viewModel::showDatePicker
                )
                
                AnimatedContent(targetState = uiState.user.dateOfBirth) { dateOfBirth ->
                    if (dateOfBirth != null) {
                        SectionItem(
                            headlineText = stringResource(R.string.remove_date_of_birth),
                            onClick = {
                                viewModel.onChangeDateOfBirth(null)
                            },
                            contentColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            
            if (uiState.showDatePicker) {
                val datePickerState = rememberDatePickerState(uiState.user.dateOfBirth)
                DatePickerDialog(onDismissRequest = viewModel::hideDatePicker, confirmButton = {
                    TextButton(
                        onClick = {
                            val selected = datePickerState.selectedDateMillis
                            viewModel.onChangeDateOfBirth(selected)
                        },
                        modifier = Modifier.padding(end = 4.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                }, dismissButton = {
                    TextButton(
                        onClick = viewModel::hideDatePicker,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }) {
                    DatePicker(
                        title = { }, state = datePickerState
                    )
                }
            }
        }
    }
    
    if (uri != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AvatarCropScreen(
            imageUri = uri!!,
            onCropConfirmed = { bitmap ->
                val file =
                    File(context.cacheDir, "avatar_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                Log.d("FDS", contentUri.toString())
                
                viewModel.uploadAvatar(contentUri)
                uri = null
            },
            onDismiss = {
                uri = null
            }
        )
    }
}
