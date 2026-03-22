/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.network.api.SearchApi
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.domain.Search
import javax.inject.Inject

class SearchRepository @Inject constructor(
    private val searchApi: SearchApi
) {

    suspend fun checkUsernameAvailable(username: String): Boolean {
        return try {
            val response = searchApi.checkUsernameAvailable(username)
            if (response.isSuccessful) {
                response.body()?.available ?: true
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e("SearchRepository", "Ошибка при проверке username", e)
            true
        }
    }

    suspend fun search(query: String): List<Search> {
        return try {
            val response = searchApi.search(query)
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
