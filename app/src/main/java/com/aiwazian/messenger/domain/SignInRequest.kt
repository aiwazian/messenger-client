/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class SignInRequest(
    val login: String,
    val password: String,
    val deviceModel: String,
    val osVersion: String,
    val osName: String
)
