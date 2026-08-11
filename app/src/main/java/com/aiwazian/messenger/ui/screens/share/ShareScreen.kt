/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.share

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.ui.components.ShareBottomSheet

@Composable
fun ShareScreen(
    sharedText: String,
    onClose: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(sharedText) {
        viewModel.init(sharedText)
    }
    
    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ShareUiEffect.ShowToast -> {
                    Toast.makeText(
                        context,
                        effect.message.asString(context),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                is ShareUiEffect.Close -> onClose()
            }
        }
    }
    
    ShareBottomSheet(
        items = uiState.targets,
        onItemClick = viewModel::toggleChatSelection,
        onSendClick = viewModel::send,
        onDismiss = onClose
    )
}
