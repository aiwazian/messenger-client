/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.ResultEffect
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.screens.chat.media.ShowInChatResult
import com.aiwazian.messenger.ui.screens.chat.pinned.PinnedMessageOpenAction
import com.aiwazian.messenger.ui.screens.chat.pinned.PinnedMessageResult
import com.aiwazian.messenger.utils.UiText
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

private const val BOTTOM_ITEM_INDEX = 0

@Composable
internal fun ChatScreenEffects(
    viewModel: ChatViewModel,
    notificationsViewModel: ChatNotificationsViewModel,
    stickersNotice: StickerPackNotice?,
    onConsumeStickersNotice: () -> Unit,
    navBackStack: NavBackStack<NavKey>,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    context: Context
) {
    var snackbarJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ChatUiEffect.NavigateBack -> navBackStack.removeLastOrNull()
                is ChatUiEffect.NavigateToMain -> {
                    navBackStack.clear()
                    navBackStack.add(AppRoute.Main)
                }

                is ChatUiEffect.ScrollToBottom -> {
                    listState.animateScrollToItem(BOTTOM_ITEM_INDEX)
                }

                is ChatUiEffect.ShowSnackbar -> {
                    snackbarJob?.cancel()
                    snackbarJob = scope.launch {
                        snackbarHostState.showSnackbar(
                            message = effect.message.asString(context),
                            duration = SnackbarDuration.Short
                        )
                    }
                }

                is ChatUiEffect.NavigateToChat -> {
                    navBackStack.add(
                        AppRoute.Chat(
                            chatId = effect.chatId,
                            scrollToMessageId = effect.scrollToMessageId
                        )
                    )
                }

                is ChatUiEffect.OpenScheduledMessages -> {
                    navBackStack.add(AppRoute.ScheduledMessages(effect.chatId))
                }

                is ChatUiEffect.OpenUrl -> {
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .setTranslateLocale(Locale.getDefault())
                        .build()
                        .launchUrl(context, effect.url.toUri())
                }

                is ChatUiEffect.OpenEmail -> {
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

                is ChatUiEffect.OpenPhone -> {
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
            }
        }
    }

    LaunchedEffect(Unit) {
        notificationsViewModel.snackbar.collect { message ->
            snackbarJob?.cancel()
            snackbarJob = scope.launch {
                snackbarHostState.showSnackbar(
                    message = message.asString(context),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    LaunchedEffect(stickersNotice) {
        val notice = stickersNotice ?: return@LaunchedEffect

        val message = notice.message.asString(context)

        onConsumeStickersNotice()

        snackbarJob?.cancel()
        snackbarJob = scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    ResultEffect<PinnedMessageResult> { result ->
        when (result.action) {
            PinnedMessageOpenAction.REPLY -> viewModel.startReply(result.message)
            PinnedMessageOpenAction.EDIT -> viewModel.startEditing(result.message)
        }
    }

    ResultEffect<ShowInChatResult> { result ->
        viewModel.jumpToMessage(result.messageId)
    }
}
