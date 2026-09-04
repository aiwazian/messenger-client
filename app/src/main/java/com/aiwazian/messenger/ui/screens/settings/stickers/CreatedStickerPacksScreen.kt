/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.stickers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.FramelessTextBox
import com.aiwazian.messenger.ui.components.StickerCard
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.utils.UiText

/** Собственные наборы: отсюда же создаётся новый. */
@Composable
fun CreatedStickerPacksScreen(viewModel: CreatedStickerPacksViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    
    val uiState by viewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    val deleteMessage = stringResource(R.string.sticker_pack_delete_message)
    
    /* Перечитываем и при возврате с редактора: состав мог измениться. */
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is StickerPackListEffect.ShowMessage -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    
                    snackbarHostState.showSnackbar(
                        UiText.StringResource(effect.messageRes)
                            .asString(context)
                    )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item(key = SEARCH_KEY) {
                SectionContainer {
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
                    deleteMessage = deleteMessage,
                    onClick = {
                        navBackStack.add(AppRoute.StickerPackEditor(packId = pack.id))
                    },
                    onDelete = { viewModel.delete(pack.id) },
                    modifier = Modifier.clip(MaterialTheme.shapes.large)
                )
            }
            
            if (uiState.visiblePacks.isEmpty() && !uiState.isLoading) {
                item(key = EMPTY_KEY) {
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

private const val SEARCH_KEY = "search"
private const val EMPTY_KEY = "empty"
