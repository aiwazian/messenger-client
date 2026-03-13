package com.aiwazian.messenger.data

import androidx.annotation.Keep

@Keep
data class AuthRequest(
    @Keep val login: String,
    @Keep val password: String,
    @Keep val deviceName: String
)
