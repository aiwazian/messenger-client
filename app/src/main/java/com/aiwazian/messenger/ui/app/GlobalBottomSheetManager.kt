/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.app

import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.InviteLinkInfo
import com.aiwazian.messenger.playback.MusicPlayerManager
import com.aiwazian.messenger.repository.InviteLinkRepository
import com.aiwazian.messenger.usecase.JoinViaInviteLinkUseCase
import com.aiwazian.messenger.utils.RegexPatterns
import com.aiwazian.messenger.utils.UiText
import com.aiwazian.messenger.utils.VibrationManager
import com.aiwazian.messenger.utils.VibrationPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalBottomSheetManager @Inject constructor(
    private val inviteLinkRepository: InviteLinkRepository,
    private val joinViaInviteLinkUseCase: JoinViaInviteLinkUseCase,
    private val vibrationManager: VibrationManager,
    val musicPlayerManager: MusicPlayerManager
) {
    private val _showMusicPlayerSheet = MutableStateFlow(false)
    val showMusicPlayerSheet = _showMusicPlayerSheet.asStateFlow()
    
    private val _showInviteBottomSheet = MutableStateFlow(false)
    val showInviteBottomSheet = _showInviteBottomSheet.asStateFlow()
    
    private val _inviteLinkInfo = MutableStateFlow<InviteLinkInfo?>(null)
    val inviteLinkInfo = _inviteLinkInfo.asStateFlow()
    
    private val _inviteLinkCode = MutableStateFlow<String?>(null)
    
    private val _isProcessingInvite = MutableStateFlow(false)
    val isProcessingInvite = _isProcessingInvite.asStateFlow()
    
    private val _showBannedDialog = MutableStateFlow(false)
    val showBannedDialog = _showBannedDialog.asStateFlow()
    
    fun showMusicPlayerSheet() {
        _showMusicPlayerSheet.value = true
    }
    
    fun hideMusicPlayerSheet() {
        _showMusicPlayerSheet.value = false
    }
    
    fun handleInviteLink(
        url: String,
        currentChatId: Long? = null,
        scope: CoroutineScope,
        onNavigateToChat: (Long) -> Unit,
        onOpenUrl: (String) -> Unit,
        onShowSnackbar: (UiText) -> Unit
    ) {
        val match = RegexPatterns.INVITE_LINK.find(url)
        if (match == null) {
            val normalized = if (url.startsWith("http")) url else "https://$url"
            onOpenUrl(normalized)
            return
        }
        
        val code = match.groupValues[2]
        scope.launch {
            _isProcessingInvite.value = true
            inviteLinkRepository.getInviteLinkInfo(code).onSuccess { linkInfo ->
                _isProcessingInvite.value = false
                when {
                    currentChatId != null && currentChatId == linkInfo.chatId -> {
                        onShowSnackbar(UiText.StringResource(R.string.you_are_already_in_this_chat))
                        vibrationManager.vibrate(VibrationPattern.Error)
                    }
                    
                    linkInfo.isJoined != null -> {
                        onNavigateToChat(linkInfo.chatId)
                    }
                    
                    linkInfo.isBanned != null -> {
                        _showBannedDialog.value = true
                        vibrationManager.vibrate(VibrationPattern.Error)
                    }
                    
                    else -> {
                        _inviteLinkInfo.value = linkInfo
                        _inviteLinkCode.value = code
                        _showInviteBottomSheet.value = true
                    }
                }
            }.onFailure {
                _isProcessingInvite.value = false
                onShowSnackbar(UiText.StringResource(R.string.invalid_link))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun subscribeViaInviteLink(
        scope: CoroutineScope,
        onNavigateToChat: (Long) -> Unit,
        onShowSnackbar: (UiText) -> Unit
    ) {
        val info = _inviteLinkInfo.value ?: return
        val code = _inviteLinkCode.value ?: return
        
        scope.launch {
            _isProcessingInvite.value = true
            joinViaInviteLinkUseCase(code, info.chatId).onSuccess {
                dismissInviteBottomSheet()
                if (info.requireApproval) {
                    onShowSnackbar(UiText.StringResource(R.string.join_request_sent))
                } else {
                    onNavigateToChat(info.chatId)
                }
            }.onFailure {
                _isProcessingInvite.value = false
                onShowSnackbar(UiText.StringResource(R.string.failed_to_join))
                vibrationManager.vibrate(VibrationPattern.Error)
            }
        }
    }
    
    fun dismissInviteBottomSheet() {
        _showInviteBottomSheet.value = false
        _inviteLinkInfo.value = null
        _inviteLinkCode.value = null
        _isProcessingInvite.value = false
    }
    
    fun hideBannedDialog() {
        _showBannedDialog.value = false
    }
}
