/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    sealed interface Error : ApiResult<Nothing> {
        data object BadRequest : Error          // 400
        data object Unauthorized : Error        // 401
        data object Forbidden : Error           // 403
        data object NotFound : Error            // 404
        data object TooManyRequests : Error     // 429
        data object NoInternet : Error
        data object Timeout : Error
        data class ServerError(val code: Int) : Error
        data class Unknown(val throwable: Throwable) : Error
    }
}

inline fun <T> ApiResult<T>.onSuccess(
    action: (T) -> Unit
): ApiResult<T> {
    if (this is ApiResult.Success) {
        action(data)
    }
    return this
}

inline fun <T> ApiResult<T>.onError(
    action: (ApiResult.Error) -> Unit
): ApiResult<T> {
    if (this is ApiResult.Error) {
        action(this)
    }
    return this
}