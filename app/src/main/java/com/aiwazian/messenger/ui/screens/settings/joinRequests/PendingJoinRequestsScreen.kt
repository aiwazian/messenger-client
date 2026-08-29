/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.joinRequests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.PendingJoinRequest
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppScaffold
import com.aiwazian.messenger.ui.components.ChatAvatar
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.utils.LottieAnimation

@Composable
fun PendingJoinRequestsScreen(
    viewModel: PendingJoinRequestsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    var requestToCancel by remember { mutableStateOf<PendingJoinRequest?>(null) }
    
    AppScaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.join_requests))
                }
            )
        }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val composition by rememberLottieComposition(
                spec = LottieCompositionSpec.Asset(LottieAnimation.OUTBOX)
            )
            
            LottieAnimation(
                composition = composition,
                modifier = Modifier.size(100.dp),
                iterations = LottieConstants.IterateForever,
                isPlaying = true
            )
            
            Text(
                text = stringResource(R.string.pending_join_requests_description),
                fontSize = 14.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator()
            }
        } else if (state.requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.pending_join_requests_empty),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        } else {
            SectionContainer {
                LazyColumn {
                    items(state.requests, key = { it.chatId }) { request ->
                        PendingJoinRequestCard(
                            request = request, onCancelClick = { requestToCancel = request })
                    }
                }
            }
        }
    }
    
    requestToCancel?.let { request ->
        AppDialog(
            onDismissRequest = { requestToCancel = null },
            title = stringResource(R.string.cancel),
            buttons = {
                TextButton(
                    onClick = {
                        viewModel.cancelRequest(request.chatId)
                        requestToCancel = null
                    }) {
                    Text(stringResource(R.string.yes))
                }
                
                TextButton(onClick = { requestToCancel = null }) {
                    Text(stringResource(R.string.no))
                }
            }) {
            Text("Вы уверены, что хотите отменить заявку на вступление в ${request.chatName}?")
        }
    }
}

@Composable
fun PendingJoinRequestCard(
    request: PendingJoinRequest, onCancelClick: () -> Unit
) {
    ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent), content = {
        Text(
            text = request.chatName,
            maxLines = 1,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            overflow = TextOverflow.Ellipsis
        )
    }, leadingContent = {
        ChatAvatar(
            id = request.chatId,
            chatName = request.chatName,
            avatarUri = null,
            size = 40.dp
        )
    }, trailingContent = {
        TextButton(onClick = onCancelClick) {
            Text(stringResource(R.string.cancel))
        }
    })
}
