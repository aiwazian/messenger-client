package com.aiwazian.messenger.services

import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.FolderInfo
import javax.inject.Inject

class FolderService @Inject constructor() {
    
    suspend fun getAll(): List<FolderInfo>? {
        val request = RetrofitInstance.api.getFolders()

        return request.body()
    }
    
    suspend fun save(folderInfo: FolderInfo): Int? {
        val request = RetrofitInstance.api.saveFolder(folderInfo)
        
        val savedFolderId = request.body()?.message?.toInt()
        
        return savedFolderId
    }
    
    suspend fun remove(folderId: Int): Boolean {
        val request = RetrofitInstance.api.deleteFolder(folderId)
        
        return request.isSuccessful
    }
}