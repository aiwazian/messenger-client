/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.content.Context
import android.util.Log
import com.aiwazian.messenger.di.FileClient
import com.aiwazian.messenger.utils.UnicodeEmojiParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemEmojiRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:FileClient private val fileClient: OkHttpClient
) {
    
    private val mutex = Mutex()
    
    private var cachedEmojis: List<String> = emptyList()
    
    suspend fun getEmojis(): List<String> = mutex.withLock {
        cachedEmojis.ifEmpty {
            loadEmojis().also { emojis -> cachedEmojis = emojis }
        }
    }
    
    private suspend fun loadEmojis(): List<String> = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, FILE_NAME)
        
        if (!file.isFile || file.length() == 0L) {
            download(file)
        }
        
        if (!file.isFile) {
            return@withContext emptyList()
        }
        
        try {
            file.bufferedReader().use { reader ->
                UnicodeEmojiParser.parse(reader.lineSequence())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading $FILE_NAME", e)
            
            emptyList()
        }
    }
    
    private fun download(target: File) {
        val temp = File(target.parentFile, "$FILE_NAME$TEMP_SUFFIX")
        val request = Request.Builder().url(EMOJI_LIST_URL).build()
        
        try {
            fileClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Emoji list request failed ${response.code}")
                }
                
                temp.outputStream().use { output ->
                    response.body.byteStream().copyTo(output)
                }
            }
            
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading $FILE_NAME", e)
        } finally {
            temp.delete()
        }
    }
    
    private companion object {
        const val TAG = "SystemEmojiRepository"
        
        const val FILE_NAME = "emoji-test.txt"
        
        const val TEMP_SUFFIX = ".tmp"
        
        const val EMOJI_LIST_URL = "https://www.unicode.org/Public/emoji/latest/emoji-test.txt"
    }
}
