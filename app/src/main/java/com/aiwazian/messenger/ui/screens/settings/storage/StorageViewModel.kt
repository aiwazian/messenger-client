/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.settings.storage

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.UserHandle
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.aiwazian.messenger.utils.DialogController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor() : ViewModel() {
    
    val clearCacheDialog = DialogController()
    
    var cacheSize by mutableLongStateOf(0)
        private set
    
    var appSize by mutableLongStateOf(0)
        private set
    
    fun reload(context: Context) {
        getCacheSize(context)
        getAppSize(context)
    }
    
    private fun getCacheSize(context: Context) {
        val storageStatsManager =
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        val appInfo = context.applicationInfo
        val user = UserHandle.getUserHandleForUid(appInfo.uid)
        
        try {
            val stats = storageStatsManager.queryStatsForPackage(
                appInfo.storageUuid,
                context.packageName,
                user
            )
            cacheSize = stats.cacheBytes
        } catch (_: Exception) {
            cacheSize = 0
        }
    }
    
    private fun getAppSize(context: Context) {
        val storageStatsManager =
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        val appInfo = context.applicationInfo
        val user = UserHandle.getUserHandleForUid(appInfo.uid)
        
        try {
            val stats = storageStatsManager.queryStatsForPackage(
                appInfo.storageUuid,
                context.packageName,
                user
            )
            appSize = stats.appBytes + stats.dataBytes + stats.cacheBytes
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun clearAppData(context: Context) {
        withContext(Dispatchers.IO) {
            context.cacheDir?.deleteRecursively()
            
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir != null && externalDir.exists()) {
                externalDir.listFiles()?.forEach { file ->
                    Log.d(
                        "CLEANUP",
                        "Удаляю: ${file.absolutePath}"
                    )
                    file.deleteRecursively()
                }
            }
        }
    }
}