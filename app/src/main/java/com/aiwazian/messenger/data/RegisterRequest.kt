package com.aiwazian.messenger.data

import androidx.annotation.Keep

@Keep
data class RegisterRequest (
    @Keep val login: String,
    @Keep val password: String,
)