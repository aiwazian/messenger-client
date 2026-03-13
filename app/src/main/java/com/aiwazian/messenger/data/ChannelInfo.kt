package com.aiwazian.messenger.data

import androidx.annotation.Keep
import com.aiwazian.messenger.enums.ChannelType
import com.aiwazian.messenger.interfaces.Profile

@Keep
data class ChannelInfo(
    @Keep override val id: Long = 0,
    @Keep val ownerId: Long = 0,
    @Keep val name: String = "",
    @Keep val bio: String = "",
    @Keep val subscribers: Int = 0,
    @Keep val removedUser: Int = 0,
    @Keep var channelType: Int = ChannelType.PRIVATE.ordinal,
    @Keep var publicLink: String? = null,
    @Keep val isSubscribed: Boolean = false
) : Profile
