/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.comments

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.FileAction
import com.aiwazian.messenger.ui.app.AppDialog
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.chatMediaKey
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.screens.chat.ChatItem
import com.aiwazian.messenger.ui.screens.chat.ChatUiState
import com.aiwazian.messenger.ui.screens.chat.MediaPickerViewModel
import com.aiwazian.messenger.ui.screens.chat.components.ChatInputSection
import com.aiwazian.messenger.ui.screens.chat.components.DateSeparatorItem
import com.aiwazian.messenger.ui.screens.chat.components.FullScreenViewer
import com.aiwazian.messenger.ui.screens.chat.components.InviteLinkBottomSheet
import com.aiwazian.messenger.ui.screens.chat.components.JoinButton
import com.aiwazian.messenger.ui.screens.chat.components.MessageBubble
import com.aiwazian.messenger.ui.screens.chat.components.ViewerMediaItem
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PostCommentsScreen(
    chatId: Long,
    postId: Long,
    viewModel: PostCommentsViewModel = hiltViewModel()
) {
    LaunchedEffect(chatId, postId) {
        viewModel.init(chatId, postId)
    }

    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaPickerViewModel: MediaPickerViewModel = hiltViewModel()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    var viewerFileId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is PostCommentsUiEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(
                            message = effect.message.asString(context),
                            duration = SnackbarDuration.Short
                        )
                    }
                }

                is PostCommentsUiEffect.ShowMediaViewer -> viewerFileId = effect.fileId

                is PostCommentsUiEffect.ScrollToComment -> {
                    val index = uiState.commentItems.indexOfFirst {
                        it is ChatItem.MessageItem && it.message.id == effect.commentId
                    }

                    if (index >= 0) {
                        scope.launch { listState.animateScrollToItem(index) }
                    }
                }

                is PostCommentsUiEffect.OpenUrl -> {
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .setTranslateLocale(Locale.getDefault())
                        .build()
                        .launchUrl(context, effect.url.toUri())
                }

                is PostCommentsUiEffect.OpenEmail -> {
                    val mailIntent = Intent(
                        Intent.ACTION_SENDTO,
                        "mailto:${Uri.encode(effect.email)}".toUri()
                    )
                    val chooser = Intent.createChooser(
                        mailIntent,
                        UiText.StringResource(R.string.open_email_with).asString(context)
                    )

                    val launched = runCatching { context.startActivity(chooser) }.isSuccess
                    if (!launched) {
                        snackbarJob?.cancel()
                        snackbarJob = scope.launch {
                            snackbarHostState.showSnackbar(
                                message = UiText.StringResource(R.string.no_email_app)
                                    .asString(context),
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }

                is PostCommentsUiEffect.OpenPhone -> {
                    val dialIntent = Intent(
                        Intent.ACTION_DIAL,
                        "tel:${effect.phone}".toUri()
                    )
                    val chooser = Intent.createChooser(
                        dialIntent,
                        UiText.StringResource(R.string.open_phone_with).asString(context)
                    )

                    val launched = runCatching { context.startActivity(chooser) }.isSuccess
                    if (!launched) {
                        snackbarJob?.cancel()
                        snackbarJob = scope.launch {
                            snackbarHostState.showSnackbar(
                                message = UiText.StringResource(R.string.no_phone_app)
                                    .asString(context),
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }

                is PostCommentsUiEffect.NavigateToChat -> {
                    navBackStack.add(AppRoute.Chat(chatId = effect.chatId))
                }
            }
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo.visibleItemsInfo
            info.isEmpty() || info.last().index >= listState.layoutInfo.totalItemsCount - 2
        }
    }

    LaunchedEffect(uiState.commentCount) {
        if (uiState.commentCount > 0 && isAtBottom) {
            listState.animateScrollToItem(uiState.commentItems.lastIndex)
        }
    }

    val inputState = remember(
        uiState.chatId,
        uiState.commentText,
        uiState.keyboardHeight,
        uiState.replyToMessage,
        uiState.editingCommentId
    ) {
        ChatUiState(
            chatId = uiState.chatId,
            messageText = uiState.commentText,
            keyboardHeight = uiState.keyboardHeight,
            replyToMessage = uiState.replyToMessage,
            editingMessageId = uiState.editingCommentId,
            isOwner = true,
            isJoined = true
        )
    }

    Scaffold(
        snackbarHost = { AppSnackbar(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { navBackStack.removeLastOrNull() },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                title = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.comments_count,
                                    uiState.commentCount,
                                    uiState.commentCount
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            val bottomBarModifier = Modifier.windowInsetsPadding(
                WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
            )

            if (uiState.commentsEnabled) {
                if (uiState.canComment) {
                    ChatInputSection(
                        uiState = inputState,
                        actions = viewModel,
                        modifier = bottomBarModifier,
                        alwaysShowInput = true,
                        isVoiceEnabled = false,
                        sendOptionsEnabled = false,
                        onMediaSheetSend = { _ ->
                            val picker = mediaPickerViewModel.uiState.value
                            viewModel.sendCommentFiles(
                                uris = picker.selected,
                                videoQualities = picker.videoQualities,
                                mediaTransforms = picker.mediaTransforms,
                                mediaDrawings = picker.mediaDrawings.mapValues { it.value.bitmap }
                            )
                            mediaPickerViewModel.reset()
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                            .then(bottomBarModifier)
                    ) {
                        JoinButton(onClick = viewModel::join)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                overscrollEffect = rememberOverscrollEffect(),
                contentPadding = PaddingValues(
                    start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                    end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
            ) {
                items(
                    items = uiState.commentItems,
                    key = { item ->
                        when (item) {
                            is ChatItem.DateSeparator -> "comment_date_${item.text}"
                            is ChatItem.MessageItem -> "comment_${item.message.id}"
                            else -> "comment_other"
                        }
                    }
                ) { item ->
                    when (item) {
                        is ChatItem.DateSeparator -> DateSeparatorItem(
                            item.text, Modifier.animateItem()
                        )

                        is ChatItem.MessageItem -> MessageBubble(
                            modifier = Modifier.animateItem(),
                            item = item,
                            onFileAction = { file, action ->
                                viewModel.onFileAction(item.message, file, action)
                            },
                            onSenderNameClick = {
                                navBackStack.add(
                                    AppRoute.Profile(
                                        profileId = item.message.senderId,
                                        profileName = item.senderName
                                    )
                                )
                            },
                            onReplyPreviewClick = {
                                viewModel.onReplyPreviewClicked(item.message)
                            },
                            onSwipeThresholdReached = viewModel::vibrateTactile,
                            onSwipeToReply = {
                                viewModel.startReply(item.message.id)
                            },
                            onLinkClicked = viewModel::onLinkClicked,
                            onUsernameClicked = viewModel::onUsernameClicked,
                            onEmailClicked = viewModel::onEmailClicked,
                            onPhoneClicked = viewModel::onPhoneClicked
                        )

                        else -> Unit
                    }
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            }
        }
    }

    if (uiState.commentToDelete != null) {
        AppDialog(
            title = stringResource(R.string.delete_comment),
            onDismissRequest = viewModel::dismissDeleteDialog,
            content = { Text(stringResource(R.string.delete_comment_confirm)) },
            buttons = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = viewModel::confirmDeleteComment,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        )
    }

    if (uiState.showInviteBottomSheet && uiState.inviteLinkInfo != null) {
        val info = uiState.inviteLinkInfo!!

        InviteLinkBottomSheet(
            chatId = info.chatId,
            name = info.name.orEmpty(),
            description = info.description,
            count = info.membersCount ?: 0,
            requireApproval = info.requireApproval,
            isLoading = uiState.isProcessingInvite,
            onDismiss = viewModel::dismissInviteBottomSheet,
            onJoin = viewModel::onSubscribeViaInviteLink
        )
    }

    if (uiState.showBannedDialog) {
        AppDialog(
            title = stringResource(R.string.no_access),
            onDismissRequest = viewModel::dismissBannedDialog,
            buttons = {
                TextButton(onClick = viewModel::dismissBannedDialog) {
                    Text(stringResource(R.string.ok))
                }
            },
            content = {
                Text(stringResource(R.string.banned_message))
            })
    }

    if (viewerFileId != null) {
        val downloadedMedia = uiState.commentItems
            .filterIsInstance<ChatItem.MessageItem>()
            .flatMap { it.message.attachments }
            .filter { attachment ->
                attachment.localUri != null && (
                        attachment.type == AttachmentType.IMAGE ||
                                attachment.type == AttachmentType.VIDEO ||
                                attachment.type == AttachmentType.GIF
                        )
            }

        val viewerEntries = downloadedMedia.mapNotNull { attachment ->
            val uri = attachment.localUri ?: return@mapNotNull null
            attachment to ViewerMediaItem(
                uri = uri,
                isVideo = attachment.type == AttachmentType.VIDEO,
                messageId = attachment.messageId,
                originKey = chatMediaKey(attachment.messageId, uri)
            )
        }
        val viewerInitialPage = viewerEntries
            .indexOfFirst { (attachment, _) -> attachment.fileId == viewerFileId }
            .coerceAtLeast(0)

        if (viewerEntries.isNotEmpty()) {
            FullScreenViewer(
                media = viewerEntries.map { it.second },
                initialPage = viewerInitialPage,
                isVideoLooping = true,
                videoPlaybackSpeed = 1.0f,
                canDownloadMedia = false,
                onVideoLoopingChange = {},
                onVideoPlaybackSpeedChange = {},
                onSaveToGallery = {},
                onShowInChat = {},
                onDismiss = { viewerFileId = null }
            )
        }
    }
}
