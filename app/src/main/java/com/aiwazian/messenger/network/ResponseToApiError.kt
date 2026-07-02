/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network

import retrofit2.Response

fun Response<*>.toApiError(): ApiResult.Error = when (code()) {
    400 -> ApiResult.Error.BadRequest
    401 -> ApiResult.Error.Unauthorized
    403 -> ApiResult.Error.Forbidden
    404 -> ApiResult.Error.NotFound
    429 -> ApiResult.Error.TooManyRequests
    in 500..599 -> ApiResult.Error.ServerError(code())
    else -> ApiResult.Error.ServerError(code())
}
