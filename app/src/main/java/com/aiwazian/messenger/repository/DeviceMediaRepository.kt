/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.aiwazian.messenger.domain.DeviceMediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Лента фото и видео устройства.
 *
 * Системный выбор файлов отдаёт только то, что пользователь уже отметил, а
 * шторке вложений нужна вся галерея целиком, чтобы показать её сеткой.
 *
 * GIF для MediaStore — обычная картинка, поэтому он приходит той же выборкой,
 * что и фото, и отличается только типом файла.
 *
 * Миниатюры репозиторий не отдаёт: кадр видео и первый кадр GIF рисует Coil в
 * самой ячейке. Раньше здесь был loadThumbnail из MediaStore, но у него нет ни
 * кэша между прокрутками, ни отмены загрузки уехавшей ячейки, а файлам без
 * готовой миниатюры он возвращал пустоту.
 */
@Singleton
class DeviceMediaRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    suspend fun getMedia(limit: Int = DEFAULT_LIMIT): List<DeviceMediaItem> =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DURATION
            )
            
            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? " +
                    "OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
            
            val selectionArgs = arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )
            
            val media = mutableListOf<DeviceMediaItem>()
            
            context.contentResolver.query(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val typeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val mimeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val durationColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
                
                while (cursor.moveToNext() && media.size < limit) {
                    val id = cursor.getLong(idColumn)
                    val isVideo =
                        cursor.getInt(typeColumn) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    
                    val collection = if (isVideo) {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    
                    media.add(
                        DeviceMediaItem(
                            id = id,
                            uri = ContentUris.withAppendedId(collection, id),
                            isVideo = isVideo,
                            durationMs = cursor.getLong(durationColumn),
                            isGif = cursor.getString(mimeColumn) == GIF_MIME_TYPE
                        )
                    )
                }
            }
            
            media
        }
    
    private companion object {
        const val DEFAULT_LIMIT = 500
        const val GIF_MIME_TYPE = "image/gif"
    }
}
