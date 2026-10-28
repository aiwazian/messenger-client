package com.aiwazian.messenger.ui.screens.settings.stickers.created

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.StickerPack
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.AnimatedStickerImage
import com.aiwazian.messenger.ui.components.BottomBarScrim
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.ShareBottomSheet
import com.aiwazian.messenger.ui.components.TopBarScrim
import com.aiwazian.messenger.ui.components.isVideoMediaUrl
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionItem
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.ui.screens.settings.stickers.StickerPackListEffect
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun CreatedStickerPacksScreen(viewModel: CreatedStickerPacksViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    
    val deleteMessage = stringResource(R.string.sticker_pack_delete_message)
    
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
            PageTopBar(title = { Text(stringResource(R.string.sticker_packs_created)) })
        },
        snackbarHost = { AppSnackbar(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navBackStack.add(AppRoute.StickerPackEditor()) },
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null
                )
            }
        },
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
                item {
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
                            R.plurals.sticker_count,
                            pack.stickerCount,
                            pack.stickerCount
                        ),
                        leadingIcon = { StickerPackCover(pack) },
                        trailingIcon = {
                            StickerPackActionsMenu(
                                onShare = { viewModel.share(pack) },
                                onDelete = { isDeleteDialogVisible = true }
                            )
                        },
                        onClick = {
                            navBackStack.add(
                                AppRoute.StickerPackEditor(
                                    packId = pack.id,
                                    packName = pack.name,
                                    packUsername = pack.username
                                )
                            )
                        }
                    )
                    
                    if (isDeleteDialogVisible) {
                        AppDialog(
                            title = pack.name,
                            onDismissRequest = { isDeleteDialogVisible = false },
                            buttons = {
                                TextButton(onClick = { isDeleteDialogVisible = false }) {
                                    Text(stringResource(R.string.sticker_pack_delete_cancel))
                                }
                                
                                TextButton(onClick = {
                                    isDeleteDialogVisible = false
                                    
                                    viewModel.delete(pack.id)
                                }) {
                                    Text(
                                        text = stringResource(R.string.sticker_pack_delete_action),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }) {
                            Text(deleteMessage)
                        }
                    }
                }
                
                if (uiState.visiblePacks.isEmpty() && !uiState.isLoading) {
                    item {
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
    }
    
    if (uiState.sharingPack != null) {
        ShareBottomSheet(
            items = uiState.shareTargets,
            onItemClick = viewModel::toggleShareTarget,
            onSendClick = viewModel::sendShare,
            onDismiss = viewModel::dismissShare,
            text = uiState.shareText,
            onTextChange = viewModel::changeShareText
        )
    }
}

@Composable
private fun StickerPackCover(pack: StickerPack) {
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
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun StickerPackActionsMenu(
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
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
                text = stringResource(R.string.sticker_pack_share),
                onClick = {
                    isExpanded = false
                    onShare()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null
                    )
                })
            
            AppDropdownMenuItem(
                text = stringResource(R.string.sticker_pack_delete_action),
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
