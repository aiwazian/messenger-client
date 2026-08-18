/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.ChatFolder
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.push.PushRegistrar
import com.aiwazian.messenger.repository.ChatFolderRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.NotificationSettingsRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.socket.OnlineUsersTracker
import com.aiwazian.messenger.socket.WebSocketClient
import