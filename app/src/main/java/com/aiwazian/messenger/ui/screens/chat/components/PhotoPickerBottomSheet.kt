/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.DeviceMediaItem
import com.aiwazian.messenger.repository.DeviceMediaRepository
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.components.MediaPickerNotice
import com.aiwazian.messenger.ui.components.PICKER_GRID_COLUMNS
import com.aiwazian.messenger.ui.components.PickerMediaCellContent
import com.aiwazian.messenger.ui.components.formatDuration
import com.aiwazian.messenger.ui.components.hasMediaPermission
import com.aiwazian.messenger.ui.components.mediaPermissions
import com.aiwazian.messenger.ui.components.mediaTransitionOrigin
import com.aiwazian.messenger.ui.components.pickerMediaKey
import com.aiwazian.messenger.utils.media.EncodedVideo
import com.aiwazian.messenger.utils.media.VideoExportTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickerBottomSheet(
    maskShape: Shape,
    videoExportTarget: VideoExportTarget,
    onPhotoPicked: (Uri) -> Unit,
    onVideoPicked: (EncodedVideo) -> Unit,
    onDismissRequest: () -> Unit,
    clipsToMask: Boolean = false,
    viewModel: PhotoPickerViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val photos by viewModel.photos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var hasPermission by remember { mutableStateOf(context.hasMediaPermission()) }
    var pickedMedia by remember { mutableStateOf<DeviceMediaItem?>(null) }

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
                MediaPickerNotice(
                    text = stringResource(R.string.media_picker_permission),
                    actionText = stringResource(R.string.media_picker_permission_action),
                    onActionClick = { permissionLauncher.launch(mediaPermissions()) }
                )
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
                MediaPickerNotice(text = stringResource(R.string.media_picker_empty))
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(PICKER_GRID_COLUMNS),
                    modifier = Modifier
                        .fillMaxSize()
                        .heightIn(max = GRID_MAX_HEIGHT),
                    horizontalArrangement = Arrangement.spacedBy(CELL_SPACING),
                    verticalArrangement = Arrangement.spacedBy(CELL_SPACING)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        )
                    }

                    items(photos, key = { it.id }) { photo ->
                        PickerMediaCell(photo = photo, onClick = { pickedMedia = photo })
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }

    val picked = pickedMedia

    if (picked != null) {
        if (picked.isVideo) {
            VideoPickerCropDialog(
                uri = picked.uri,
                maskShape = maskShape,
                exportTarget = videoExportTarget,
                onConfirm = { videoUri ->
                    pickedMedia = null

                    onVideoPicked(videoUri)
                    onDismissRequest()
                },
                onDismiss = { pickedMedia = null }
            )
        } else {
            MediaPickerCropDialog(
                uri = picked.uri,
                maskShape = maskShape,
                onConfirm = { cropped ->
                    pickedMedia = null

                    onPhotoPicked(cropped)
                    onDismissRequest()
                },
                onDismiss = { pickedMedia = null },
                clipsToMask = clipsToMask
            )
        }
    }
}

@Composable
private fun PickerMediaCell(photo: DeviceMediaItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .mediaTransitionOrigin(pickerMediaKey(photo.uri))
    ) {
        PickerMediaCellContent(item = photo, modifier = Modifier.fillMaxSize())
    }
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

            _photos.value = deviceMediaRepository.getMedia().filter { !it.isGif }

            _isLoading.value = false
        }
    }
}

private val GRID_MAX_HEIGHT = 420.dp
private val CELL_SPACING = 2.dp
