/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.extensions.getFileName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Копии файлов, пришедших из системного «Поделиться».
 *
 * Права на content://-ссылку из чужого приложения живут ровно столько, сколько
 * живёт задача нашей Activity. Отправка же продолжается и после её закрытия,
 * а при плохой сети повторяется минутами, поэтому содержимое сразу
 * перекладывается в свой кэш, а дальше по конвейеру идёт file://-ссылка.
 */
@Singleton
class SharedFileCache @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    
    /**
     * @return ссылки на копии. Файлы, которые прочитать не удалось, просто
     * выпадают из списка: лучше отправить остальные, чем ничего.
     */
    suspend fun cache(uris: List<Uri>): List<Uri> = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, DIRECTORY_NAME)
        directory.mkdirs()
        
        val stamp = System.currentTimeMillis()
        
        uris.mapIndexedNotNull { index, uri ->
            try {
                // Имя берётся до копирования, пока content://-ссылка ещё читаема:
                // без него в чате вместо документа окажется безымянный file.pdf.
                val name = uri.getFileName(context)?.replace('/', '_') ?: "shared_$index"
                val target = File(directory, "${stamp}_${index}_$name")
                
                val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                    true
                } ?: false
                
                if (copied) Uri.fromFile(target) else null
            } catch (e: Exception) {
                Log.e(TAG, "Unable to cache shared file $uri", e)
                null
            }
        }
    }
    
    private companion object {
        const val TAG = "SharedFileCache"
        const val DIRECTORY_NAME = "shared"
    }
}
