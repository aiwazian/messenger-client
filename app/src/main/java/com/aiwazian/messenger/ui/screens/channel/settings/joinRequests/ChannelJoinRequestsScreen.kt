/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel.settings.joinRequests

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.JoinRequestCard
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.components.section.SectionContainer
import com.aiwazian.messenger.ui.components.topBar.PageTopBar
import com.aiwazian.messenger.utils.LottieAnimation
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun ChannelJoinRequestsScreen(
    channelId: Long,
    viewModel: ChannelJoinRequestsViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    val context = LocalContext.current
    
    LaunchedEffect(channelId) {
        viewModel.init(channelId)
    }
    
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChannelJoinRequestsEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(effect.message.asString(context))
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            PageTopBar(
                title = {
                    Text(stringResource(R.string.join_requests))
                },
            )
        },
        snackbarHost = {
            AppSnackbar(snackbarHostState)
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val composition by rememberLottieComposition(
                    spec = LottieCompositionSpec.Asset(LottieAnimation.INBOX)
                )
                
                LottieAnimation(
                    composition = composition,
                    modifier = Modifier.size(100.dp),
                    iterations = LottieConstants.IterateForever,
                    isPlaying = true
                )
                
                Text(
                    text = stringResource(R.string.requests_to_channel_join),
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            } else if (state.requests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_requests_to_join))
                }
            } else {
                SectionContainer {
                    LazyColumn {
                        items(state.requests, key = { it.id }) { user ->
                            val headlineText = if (user.lastName.isNullOrBlank()) {
                                user.firstName
                            } else {
                                "${user.firstName} ${user.lastName}"
                            }
                            
                            JoinRequestCard(
                                id = user.id,
                                headlineText = headlineText,
                                avatarUri = user.avatars.firstOrNull()?.uri,
                                supportingText = user.username?.let { "@$it" },
                                onAccept = { viewModel.acceptRequest(user.id) },
                                onReject = { viewModel.rejectRequest(user.id) },
                                onClick = {
                                    navBackStack.add(
                                        AppRoute.Profile(
                                            profileId = user.id,
                                            profileName = headlineText
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
