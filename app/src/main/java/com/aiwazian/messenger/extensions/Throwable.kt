/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.extensions

fun Throwable.isNetworkError() = when (this) {
    is java.net.UnknownHostException -> true
    is java.net.SocketTimeoutException -> true
    is java.net.ConnectException -> true
    is java.io.IOException -> true
    else -> false
}
