package com.aiwazian.messenger.ui.screens.settings.emoji.added

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.EmojiPack
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.AnimatedStickerImage
import com.aiwazian.messenger.ui.components.BottomBarScrim
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.TopBarScrim
import com.aiwazian.messenger.ui.components.isVideoMediaUrl
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.settings.emoji.EMOJI_CELL_MIN_SIZE
import com.aiwazian.messenger.ui.screens.settings.emoji.EMOJI_GRID_MAX_HEIGHT
import com.aiwazian.messenger.ui.screens.settings.emoji.EMOJI_GRID_SPACING
import com.aiwazian.messenger.ui.screens.settings.emoji.EmojiPackListEffect
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddedEmojiPacksScreen(viewModel: AddedEmojiPacksViewModel = hiltViewModel()) {
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    val openedPack by viewModel.openedPack.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is EmojiPackListEffect.ShowMessage -> {
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
            PageTopBar(title = { Text(stringResource(R.string.emoji_packs_added)) })
        },
        snackbarHost = { AppSnackbar(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box {
            TopBarScrim(height = innerPadding.calculateTopPadding())
            BottomBarScrim(height = innerPadding.calculateBottomPadding())
            
            LazyColumn(
                contentPadding = innerPadding.plus(
                    PaddingValues(
                        start = 10.dp,
                        end = 10.dp,
                        bottom = 10.dp
                    )
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item(key = SEARCH_ITEM_KEY) {
                    FramelessTextBox(
                        placeholder = stringResource(R.string.search),
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange
                    )
                }
                
                items(
                    items = uiState.visiblePacks,
                    key = { it.id }) { pack ->
                    var isDeleteDialogVisible by remember { mutableStateOf(false) }
                    
                    SectionItem(
                        headlineContent = { Text(pack.name) },
                        supportingText = pluralStringResource(
                            R.plurals.emoji_count,
                            pack.emojiCount,
                            pack.emojiCount
                        ),
                        leadingIcon = { EmojiPackCover(pack) },
                        trailingIcon = {
                            EmojiPackActionsMenu(
                                onDelete = { isDeleteDialogVisible = true }
                            )
                        },
                        onClick = { viewModel.open(pack.id) }
                    )
                    
                    if (isDeleteDialogVisible) {
                        AppDialog(
                            title = pack.name,
                            onDismissRequest = { isDeleteDialogVisible = false },
                            buttons = {
                                TextButton(onClick = { isDeleteDialogVisible = false }) {
                                    Text(stringResource(R.string.emoji_pack_delete_cancel))
                                }
                                
                                TextButton(onClick = {
                                    isDeleteDialogVisible = false
                                    
                                    viewModel.remove(pack.id)
                                }) {
                                    Text(
                                        text = stringResource(R.string.emoji_pack_delete_action),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }) {
                            Text(stringResource(R.string.emoji_pack_remove_message))
                        }
                    }
                }
                
                if (uiState.visiblePacks.isEmpty() && !uiState.isLoading) {
                    item(key = EMPTY_ITEM_KEY) {
                        Text(
                            text = stringResource(R.string.emoji_packs_empty),
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
                columns = GridCells.Adaptive(minSize = EMOJI_CELL_MIN_SIZE),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = EMOJI_GRID_MAX_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(EMOJI_GRID_SPACING),
                verticalArrangement = Arrangement.spacedBy(EMOJI_GRID_SPACING)
            ) {
                items(
                    items = pack.emojis,
                    key = { it.id }) { emoji ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(emoji.url)
                            .memoryCacheKey(emoji.fileId)
                            .diskCacheKey(emoji.fileId)
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

@Composable
private fun EmojiPackCover(pack: EmojiPack) {
    val coverUrl = pack.coverImageUrl
    val coverCacheKey = pack.coverCacheKey
    
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl == null) {
            Icon(
                imageVector = Icons.Rounded.Photo,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AnimatedStickerImage(
                data = coverUrl,
                isVideo = isVideoMediaUrl(coverUrl),
                cacheKey = coverCacheKey,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun EmojiPackActionsMenu(onDelete: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null
            )
        }
        
        AppDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }) {
            AppDropdownMenuItem(
                text = stringResource(R.string.emoji_pack_delete_action),
                onClick = {
                    isExpanded = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null
                    )
                })
        }
    }
}

private const val SEARCH_ITEM_KEY = "search"
private const val EMPTY_ITEM_KEY = "empty"
