package com.aiwazian.messenger.data

import androidx.annotation.Keep
import com.aiwazian.messenger.interfaces.Profile

@Keep
data class GroupInfo(
    @Keep override val id: Long = 0,
    @Keep val ownerId: Long = 0,
    @Keep val name: String = "",
    @Keep val bio: String = "",
    @Keep val members: Int = 0
) : Profile