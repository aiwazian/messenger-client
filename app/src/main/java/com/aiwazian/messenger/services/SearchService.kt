package com.aiwazian.messenger.services

import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.SearchInfo
import javax.inject.Inject

class SearchService @Inject constructor() {
    
    suspend fun searchUserByUsername(query: String): List<SearchInfo>? {
        val response = RetrofitInstance.api.searchUser(query)
        return response.body()
    }
}
