package com.aiwazian.messenger.services

import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.PrivacySettings
import javax.inject.Inject

class PrivacyService @Inject constructor() {
    
    suspend fun getMyPrivacy(): PrivacySettings? {
        val response = RetrofitInstance.api.getMyPrivacy()
        return response.body()
    }
}