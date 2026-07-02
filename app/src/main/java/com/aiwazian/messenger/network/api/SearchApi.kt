/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.api

import com.aiwazian.messenger.network.dto.ResolveUsernameResponseDto
import com.aiwazian.messenger.network.dto.SearchResponseDto
import com.aiwazian.messenger.network.dto.UsernameAvailableResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SearchApi {

    @GET("search/check/{username}")
    suspend fun checkUsernameAvailable(@Path("username") username: String): Response<UsernameAvailableResponseDto>
    
    @GET("search/resolve/{username}")
    suspend fun resolveUsername(@Path("username") username: String): Response<ResolveUsernameResponseDto>

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<List<SearchResponseDto>>
}
