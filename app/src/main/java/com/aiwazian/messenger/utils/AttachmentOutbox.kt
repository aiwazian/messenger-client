/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.extensions.getFileName
import com.aiwazian.messenger.extensions.getFileType
import com.aiwazian.messenger.utils.media.ImageCompressor
import com.aiwazian.messenger.utils.media.MediaCompressionConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Свои копии вложений, которые ждут отправки.
 *
 * Системный выбор файла и выбор из галереи дают доступ к content://-ссылке ровно
 * на время жизни задачи приложения: ушёл из чата, свернул, вернулся через
 * полчаса — и читать уже нечего, отправка упирается в отобранный доступ.
 * Поэтому содержимое сразу перекладывается к нам, а дальше по конвейеру идёт
 * file://-ссылка на свою копию.
 *
 * Кэш для этого не подходит: систему ничто не удержит от его очистки посреди
 * отправки большого видео. [SharedFileCache] делает то же самое для системного
 * «Поделиться», но живёт в cacheDir и работает со списками.
 *
 * Каждая копия лежит в своей папке, названной по вложению. Так имя файла
 * остаётся исходным: на сервер и в чат оно уходит уже из пути копии, и
 * поднятая после перезапуска отправка не превращает photo.jpg в
 * temp_-17_0_photo.jpg.
 *
 * Здесь же сжимаются фотографии: копия всё равно делается, и дешевле сразу
 * положить в неё готовый к JPEG кадр, чем скопировать исходные десять
 * мегабайт и сжимать их шагом позже. Побочно из этого выходит главное:
 * повторы и досылка после перезапуска идут с уже сжатого файла, а не
 * сжимают его второй раз.
 */
@Singleton
class AttachmentOutbox @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageCompressor: ImageCompressor
) {
    
    /**
     * @param key имя вложения внутри отправки: по нему копия находится после
     * перезапуска.
     * @return ссылку на свою копию либо исходную ссылку, если копировать незачем
     * или не удалось: голосовые уже лежат у нас, а про удалённый файл честнее
     * доложит сама отправка.
     */
    suspend fun keep(uri: Uri, key: String): Uri = withContext(Dispatchers.IO) {
        // Уже наша копия: отправку подняли после перезапуска, и фото в ней
        // сжато ещё в прошлый раз — второй проход только срезал бы качество.
        if (directoryOf(uri) != null) {
            return@withContext uri
        }
        
        // Имя берётся, пока ссылка ещё читаема: без него потеряется расширение,
        // а с ним и mime-тип на следующей попытке.
        val name = uri.getFileName(context)?.replace('/', '_') ?: key
        
        // Фотографии копией служит результат сжатия: у него своё имя, свой
        // размер и свой формат, а метаданных нет вовсе.
        if (imageCompressor.isCompressible(uri.getFileType(context))) {
            val compressed = imageCompressor.compress(
                source = uri,
                directory = directoryFor(key),
                maxDimension = MediaCompressionConfig.PHOTO_MAX_DIMENSION,
                quality = MediaCompressionConfig.PHOTO_JPEG_QUALITY,
                name = name
            )
            
            if (compressed != null) {
                return@withContext compressed.uri
            }
            
            // Сжать не удалось: отправить исходник лучше, чем не отправить
            // ничего.
            Log.w(TAG, "Unable to compress $uri, keeping it as is")
        }
        
        if (uri.scheme == SCHEME_FILE) {
            return@withContext uri
        }
        
        val directory = directoryFor(key)
        directory.mkdirs()
        val target = File(directory, name)
        
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
    
    /** Копия больше не нужна: сообщение ушло либо отправка окончательно провалилась. */
    fun release(uri: Uri) {
        directoryOf(uri)?.deleteRecursively()
    }
    
    /**
     * Выбрасывает копии, за которыми не стоит ни одна отправка: мусор от
     * прошлых запусков, убитых посреди загрузки.
     *
     * @param keep ссылки, которые сейчас в работе.
     */
    suspend fun cleanUp(keep: List<Uri>) = withContext(Dispatchers.IO) {
        val directories = File(context.filesDir, DIRECTORY_NAME).listFiles()
            ?: return@withContext
        
        val kept = keep.mapNotNull { directoryOf(it)?.absolutePath }.toSet()
        val threshold = System.currentTimeMillis() - MIN_AGE_MS
        
        directories.forEach { directory ->
            // Свежие копии не трогаем: отправка могла начаться уже после того,
            // как сюда ушёл список.
            if (directory.absolutePath !in kept && directory.lastModified() < threshold) {
                directory.deleteRecursively()
            }
        }
    }
    
    /** Папка для копии одного вложения. */
    private fun directoryFor(key: String): File {
        return File(File(context.filesDir, DIRECTORY_NAME), key)
    }
    
    /** Папка копии либо null, если ссылка не наша. */
    private fun directoryOf(uri: Uri): File? {
        if (uri.scheme != SCHEME_FILE) {
            return null
        }
        
        val directory = File(uri.path ?: return null).parentFile ?: return null
        
        return if (directory.parentFile?.name == DIRECTORY_NAME) directory else null
    }
    
    private companion object {
        const val TAG = "AttachmentOutbox"
        const val SCHEME_FILE = "file"
        const val DIRECTORY_NAME = "outbox"
        
        /** Насколько свежие копии переживают уборку, даже если о них никто не помнит. */
        const val MIN_AGE_MS = 60L * 60 * 1000
    }
}
