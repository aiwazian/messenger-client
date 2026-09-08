/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.stickers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.StickerCard
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddedStickerPacksScreen(viewModel: AddedStickerPacksViewModel = hiltViewModel()) {
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    val openedPack by viewModel.openedPack.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    val removeMessage = stringResource(R.string.sticker_pack_remove_message)
    
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is StickerPackListEffect.ShowMessage -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(
                            UiText.StringResource(effect.messageRes)
                                .asString(context)
                        )
                    }
                }
            }
        }
    }
    
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            PageTopBar(title = { Text(stringResource(R.string.sticker_packs_added)) })
        },
        snackbarHost = { AppSnackbar(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding.plus(PaddingValues(horizontal = 10.dp)),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item(key = SEARCH_ITEM_KEY) {
                SectionContainer(contentPadding = PaddingValues.Zero) {
                    FramelessTextBox(
                        placeholder = stringResource(R.string.search),
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange
                    )
                }
            }
            
            items(
                items = uiState.visiblePacks,
                key = { it.id }) { pack ->
                StickerCard(
                    pack = pack,
                    deleteMessage = removeMessage,
                    onClick = { viewModel.open(pack.id) },
                    onDelete = { viewModel.remove(pack.id) },
                    modifier = Modifier.clip(MaterialTheme.shapes.large)
                )
            }
            
            if (uiState.visiblePacks.isEmpty() && !uiState.isLoading) {
                item(key = EMPTY_ITEM_KEY) {
                    Text(
                        text = stringResource(R.string.sticker_packs_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    
    val pack = openedPack
    
    if (pack != null) {
        AppBottomSheet(onDismissRequest = viewModel::close) {
            Text(
                text = pack.name,
                modifier = Modifier.padding(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 8.dp
                ),
                style = MaterialTheme.typography.titleMedium
            )
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = STICKER_CELL_MIN_SIZE),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = SHEET_GRID_MAX_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = pack.stickers,
                    key = { it.id }) { sticker ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(sticker.url)
                            .memoryCacheKey(sticker.fileId)
                            .diskCacheKey(sticker.fileId)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.aspectRatio(1f),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

private const val SEARCH_ITEM_KEY = "search"
private const val EMPTY_ITEM_KEY = "empty"
private val STICKER_CELL_MIN_SIZE = 80.dp
private val SHEET_GRID_MAX_HEIGHT = 420.dp
