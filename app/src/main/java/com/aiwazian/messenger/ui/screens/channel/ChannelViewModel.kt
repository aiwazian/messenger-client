/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.channel

import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : ViewModel() {

    suspend fun tryJoin(id: Long): Boolean {
        return channelRepository.join(id).isSuccess
    }
    
    suspend fun tryLeave(id: Long): Boolean {
        return channelRepository.leave(id).isSuccess
    }
}
