/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.stickers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.chat.components.PhotoPickerBottomSheet

/**
 * Создание или правка набора стикеров.
 *
 * @param packId `null` для нового набора.
 */
@Composable
fun StickerPackEditorScreen(
    packId: Long? = null,
    viewModel: StickerPackEditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    var isPickerVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.load(packId)
    }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is StickerPackEditorEffect.ShowMessage -> {
                    /* Предыдущее сообщение уже неактуально — не держим очередь. */
                    snackbarHostState.currentSnackbarData?.dismiss()
                    
                    snackbarHostState.showSnackbar(context.getString(effect.messageRes))
                }
            }
        }
    }
    
    val isFabVisible = uiState.canSave || uiState.isSaving
    
    Scaffold(
        topBar = {
            PageTopBar(title = {
                Text(
                    stringResource(
                        if (uiState.packId == null) {
                            R.string.sticker_pack_new
                        } else {
                            R.string.sticker_pack
                        }
                    )
                )
            })
        },
        snackbarHost = { AppSnackbar(hostState = snackbarHostState) },
        floatingActionButton = {
            if (isFabVisible) {
                FloatingActionButton(onClick = viewModel::save) {
                    if (uiState.isSaving) {
                        CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Done,
                            contentDescription = null
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyVerticalGrid(
            /* На телефоне выйдет три столбца, на планшете больше. */
            columns = GridCells.Adaptive(minSize = CELL_MIN_SIZE),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 10.dp,
                end = 10.dp,
                bottom = 100.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    FramelessTextBox(
                        placeholder = stringResource(R.string.sticker_pack_name),
                        value = uiState.name,
                        onValueChange = viewModel::onNameChange,
                        trailingIcon = {
                            Text(
                                text = "${uiState.name.length}/${StickerPackEditorViewModel.MAX_NAME_LENGTH}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        })
                    
                    FramelessTextBox(
                        placeholder = stringResource(R.string.sticker_pack_username),
                        value = uiState.username,
                        onValueChange = viewModel::onUsernameChange,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                    )
                    
                    UsernameHint(status = uiState.usernameStatus)
                }
            }
            
            items(
                items = uiState.stickers,
                key = { it.key }) { slot ->
                StickerSlotCell(
                    slot = slot,
                    onRemove = { viewModel.removeSticker(slot.key) })
            }
            
            item(key = ADD_CELL_KEY) {
                AddStickerCell(
                    isBusy = uiState.isAddingSticker,
                    onClick = { isPickerVisible = true })
            }
        }
    }
    
    if (isPickerVisible) {
        PhotoPickerBottomSheet(
            /* Квадрат со скруглением: стикер — квадратная картинка, а не аватарка. */
            maskShape = MaterialTheme.shapes.large,
            onPhotoPicked = viewModel::addSticker,
            onDismissRequest = { isPickerVisible = false })
    }
}

@Composable
private fun UsernameHint(status: UsernameStatus) {
    val text = when (status) {
        UsernameStatus.Empty -> stringResource(R.string.sticker_pack_username_hint)
        UsernameStatus.TooShort -> stringResource(
            R.string.sticker_pack_username_too_short,
            StickerPackEditorViewModel.MIN_USERNAME_LENGTH
        )
        
        UsernameStatus.Checking -> stringResource(R.string.sticker_pack_username_checking)
        UsernameStatus.Available -> stringResource(R.string.sticker_pack_username_available)
        UsernameStatus.Taken -> stringResource(R.string.sticker_pack_username_taken)
        UsernameStatus.Unknown -> stringResource(R.string.sticker_pack_username_unknown)
    }
    
    val color = when (status) {
        UsernameStatus.Available -> MaterialTheme.colorScheme.primary
        UsernameStatus.Taken, UsernameStatus.Unknown -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Text(
        text = text,
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = 10.dp
        ),
        color = color,
        fontSize = 13.sp,
        lineHeight = 16.sp
    )
}

@Composable
private fun StickerSlotCell(slot: StickerSlot, onRemove: () -> Unit) {
    val context = LocalContext.current
    
    val model = when (slot) {
        is StickerSlot.Local -> ImageRequest.Builder(context).data(slot.sticker.uri).build()
        
        is StickerSlot.Remote -> ImageRequest.Builder(context)
            .data(slot.url)
            /* Ключ кеша — файл, а не адрес: смена домена CDN не сбросит картинки. */
            .memoryCacheKey(slot.fileId)
            .diskCacheKey(slot.fileId)
            .build()
    }
    
    Box(modifier = Modifier.aspectRatio(1f)) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Fit
        )
        
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddStickerCell(isBusy: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = !isBusy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isBusy) {
            CircularWavyProgressIndicator(modifier = Modifier.size(28.dp))
        } else {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.sticker_add),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val CELL_MIN_SIZE = 104.dp
private const val ADD_CELL_KEY = "add_sticker"
