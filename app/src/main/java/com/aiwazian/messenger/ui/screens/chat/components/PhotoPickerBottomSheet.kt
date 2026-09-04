/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.DeviceMediaItem
import com.aiwazian.messenger.repository.DeviceMediaRepository
import com.aiwazian.messenger.ui.app.AppBottomSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Выбор ровно одной фотографии с кадрированием под маску.
 *
 * Отдельная шторка, а не режим [MediaPickerBottomSheet]: там вся логика строится
 * вокруг множественного выбора, подписи и отправки в чат. Здесь ничего этого
 * нет: нужен один кадр для стикера или аватарки, поэтому нет ни кружков
 * выбора, ни текстового поля, и тап по ячейке сразу ведёт к кадрированию.
 *
 * @param maskShape форма окна кадрирования: круг для аватарки, скруглённый квадрат
 * для стикера.
 * @param onPhotoPicked отдаёт уже обрезанный кадр в кеше, а не исходный файл галереи.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickerBottomSheet(
    maskShape: Shape,
    onPhotoPicked: (Uri) -> Unit,
    onDismissRequest: () -> Unit,
    viewModel: PhotoPickerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    val photos by viewModel.photos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var hasPermission by remember { mutableStateOf(context.hasPhotoPermission()) }
    var croppingUri by remember { mutableStateOf<Uri?>(null) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = result.values.any { it }
    }
    
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.load()
        }
    }
    
    AppBottomSheet(onDismissRequest = onDismissRequest, contentPadding = PaddingValues.Zero) {
        when {
            !hasPermission -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.media_picker_permission),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Button(onClick = { permissionLauncher.launch(photoPermissions()) }) {
                        Text(stringResource(R.string.media_picker_permission_action))
                    }
                }
            }
            
            isLoading && photos.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            }
            
            photos.isEmpty() -> {
                Text(
                    text = stringResource(R.string.media_picker_empty),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }
            
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(GRID_COLUMNS),
                    modifier = Modifier
                        .fillMaxSize()
                        .heightIn(max = GRID_MAX_HEIGHT),
                    horizontalArrangement = Arrangement.spacedBy(CELL_SPACING),
                    verticalArrangement = Arrangement.spacedBy(CELL_SPACING)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        PhotoCell(photo = photo, onClick = { croppingUri = photo.uri })
                    }
                }
            }
        }
    }
    
    val uri = croppingUri
    
    if (uri != null) {
        MediaPickerCropDialog(
            uri = uri,
            maskShape = maskShape,
            onConfirm = { cropped ->
                croppingUri = null
                
                onPhotoPicked(cropped)
                onDismissRequest()
            },
            onDismiss = { croppingUri = null })
    }
}

@Composable
private fun PhotoCell(photo: DeviceMediaItem, onClick: () -> Unit) {
    AsyncImage(
        model = photo.uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    )
}

@HiltViewModel
class PhotoPickerViewModel @Inject constructor(
    private val deviceMediaRepository: DeviceMediaRepository
) : ViewModel() {
    
    private val _photos = MutableStateFlow<List<DeviceMediaItem>>(emptyList())
    val photos = _photos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            
            _photos.value = deviceMediaRepository.getMedia().filter { !it.isVideo && !it.isGif }
            
            _isLoading.value = false
        }
    }
}

private fun photoPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    )
    
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES
    )
    
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun Context.hasPhotoPermission(): Boolean = photoPermissions().any { permission ->
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private const val GRID_COLUMNS = 3
private val GRID_MAX_HEIGHT = 420.dp
private val CELL_SPACING = 2.dp
