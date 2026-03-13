package com.aiwazian.messenger.data

import androidx.annotation.Keep
import com.aiwazian.messenger.interfaces.Profile

@Keep
data class UserInfo(
    @Keep override var id: Long = 0,
    @Keep var firstName: String = "",
    @Keep var lastName: String = "",
    @Keep var username: String? = null,
    @Keep var bio: String = "",
    @Keep var dateOfBirth: Long? = null,
): Profile