/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.domain.Search
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.network.api.SearchApi
import com.aiwazian.messenger.network.dto.ResolveUsernameResponseDto
import javax.inject.Inject

class SearchRepository @Inject constructor(
    private val searchApi: SearchApi
) {
    suspend fun checkUsernameAvailable(username: String): Result<Boolean> {
        return try {
            val response = searchApi.checkUsernameAvailable(username)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.available)
                } else {
                    Result.failure(Exception("Empty body"))
                }
            } else {
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("SearchRepository", "Ошибка при проверке username", e)
            Result.failure(e)
        }
    }
    
    suspend fun resolveUsername(username: String): Result<ResolveUsernameResponseDto?> {
        return try {
            val response = searchApi.resolveUsername(username)
            if (response.isSuccessful) {
                Result.success(response.body())
            } else if (response.code() == 404) {
                Result.success(null)
            } else {
                Result.failure(Exception("Failed to resolve username"))
            }
        } catch (e: Exception) {
            Log.e("SearchRepository", "Ошибка при resolve username", e)
            Result.failure(e)
        }
    }
    
    suspend fun search(
        query: String,
        limit: Int = 20,
        offset: Int = 0
    ): List<Search> {
        return try {
            val response = searchApi.search(query, limit, offset)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                dtos.map { it.toDomain() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SearchRepository", "Ошибка при поиске", e)
            emptyList()
        }
    }
}
