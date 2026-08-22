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
 * Свои копии вложений, которые ждут отправки.
 *
 * Системный выбор файла и выбор из галереи дают доступ к content://-ссылке
 * ровно на время жизни задачи приложения: ушёл из чата, свернул, вернулся
 * через полчаса — и читать уже нечего, отправка упирается в отобранный
 * доступ. Поэтому содержимое перекладывается к нам, а дальше по конвейеру
 * идёт file://-ссылка на свою копию.
 *
 * Кэш для этого не подходит: систему ничто не удержит от его очистки посреди
 * отправки большого видео. [SharedFileCache] делает то же самое для
 * системного «Поделиться», но живёт в cacheDir и работает со списками.
 */
@Singleton
class AttachmentOutbox @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    
    /**
     * @return ссылку на свою копию либо исходную ссылку, если копировать
     * незачем или не удалось: голосовые, кэш «Поделиться» и прошлые копии уже
     * лежат у нас, а про удалённый файл честнее доложит сама отправка.
     */
    suspend fun keep(uri: Uri, fileId: String): Uri = withContext(Dispatchers.IO) {
        if (uri.scheme == SCHEME_FILE) {
            return@withContext uri
        }
        
        val directory = File(context.filesDir, DIRECTORY_NAME)
        directory.mkdirs()
        
        // Имя берётся, пока ссылка ещё читаема: без него потеряется расширение,
        // а вместе с ним и mime-тип на следующей попытке.
        val name = uri.getFileName(context)?.replace('/', '_') ?: fileId
        val target = File(directory, "${fileId}_$name")
        
        if (target.exists() && target.length() > 0) {
            return@withContext Uri.fromFile(target)
        }
        
        try {
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            
            if (copied) Uri.fromFile(target) else uri
        } catch (e: Exception) {
            Log.e(TAG, "Unable to keep $uri", e)
            target.delete()
            uri
        }
    }
    
    /** Копия больше не нужна: сообщение ушло либо отправка окончательно отменена. */
    fun release(uri: Uri) {
        if (uri.scheme != SCHEME_FILE) {
            return
        }
        
        val file = File(uri.path ?: return)
        
        // Удаляем только своё: по этому же конвейеру идут голосовые и копии из
        // «Поделиться», их трогать нельзя.
        if (file.parentFile?.name == DIRECTORY_NAME) {
            file.delete()
        }
    }
    
    private companion object {
        const val TAG = "AttachmentOutbox"
        const val SCHEME_FILE = "file"
        const val DIRECTORY_NAME = "outbox"
    }
}
