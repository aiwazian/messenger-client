/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.domain.MessageReplyPreview
import com.aiwazian.messenger.enums.AttachmentType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Отправка, которую не успели закончить до смерти процесса. */
data class PendingSend(
    val tempId: Long,
    val chatId: Long,
    val text: String?,
    val uris: List<Uri>,
    val replyTo: MessageReplyPreview?
)

/**
 * Незаконченные отправки на диске.
 *
 * Скоуп приложения переживает уход с экрана и сворачивание, но не смерть
 * процесса: свёрнутое приложение система вправе выгрузить, и сообщение
 * оставалось бы «отправляется» навсегда. Поэтому о каждой начатой отправке
 * остаётся запись, а [PendingSendResumer] поднимает их при следующем запуске.
 *
 * Ссылки хранятся уже на свои копии из [AttachmentOutbox]: content://-ссылка к
 * следующему запуску мертва.
 */
@Singleton
class PendingSendStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun remember(
        tempId: Long,
        chatId: Long,
        uris: List<Uri>,
        text: String?,
        replyTo: MessageReplyPreview?
    ) = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, DIRECTORY_NAME)
        directory.mkdirs()
        
        val record = Record(
            tempId = tempId,
            chatId = chatId,
            text = text,
            uris = uris.map { it.toString() },
            replyTo = replyTo?.let { reply ->
                ReplyRecord(
                    messageId = reply.messageId,
                    chatId = reply.chatId,
                    senderId = reply.senderId,
                    senderName = reply.senderName,
                    chatName = reply.chatName,
                    text = reply.text,
                    attachmentTypes = reply.attachmentTypes.map { it.name }
                )
            }
        )
        
        val target = File(directory, "$tempId$EXTENSION")
        val temp = File(directory, "$tempId$EXTENSION$TEMP_SUFFIX")
        
        try {
            temp.writeText(json.encodeToString(record))
            
            // Переименование атомарно: недописанный json после убийства процесса
            // выглядел бы как потерянная отправка.
            if (!temp.renameTo(target)) {
                target.delete()
                temp.renameTo(target)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to remember send #$tempId", e)
            temp.delete()
        }
        
        Unit
    }
    
    /**
     * Убирается первым делом после успеха — до остальных обновлений: отправить
     * одно и то же сообщение дважды хуже, чем оставить лишнюю запись.
     */
    suspend fun forget(tempId: Long) = withContext(Dispatchers.IO) {
        File(File(context.filesDir, DIRECTORY_NAME), "$tempId$EXTENSION").delete()
        
        Unit
    }
    
    suspend fun all(): List<PendingSend> = withContext(Dispatchers.IO) {
        val files = File(context.filesDir, DIRECTORY_NAME).listFiles()
            ?: return@withContext emptyList()
        
        files.filter { it.isFile && it.name.endsWith(EXTENSION) }.mapNotNull { file ->
            try {
                val record = json.decodeFromString<Record>(file.readText())
                
                PendingSend(
                    tempId = record.tempId,
                    chatId = record.chatId,
                    text = record.text,
                    uris = record.uris.map { Uri.parse(it) },
                    replyTo = record.replyTo?.let { reply ->
                        MessageReplyPreview(
                            messageId = reply.messageId,
                            chatId = reply.chatId,
                            senderId = reply.senderId,
                            senderName = reply.senderName,
                            chatName = reply.chatName,
                            text = reply.text,
                            attachmentTypes = reply.attachmentTypes.mapNotNull { type ->
                                runCatching { AttachmentType.valueOf(type) }.getOrNull()
                            }
                        )
                    }
                )
            } catch (e: Exception) {
                // Битую запись держать незачем: поднимать из неё нечего.
                Log.e(TAG, "Unable to read ${file.name}", e)
                file.delete()
                null
            }
        }
    }
    
    /**
     * Запись об одной отправке: нужна тем, кто её останавливает — вместе с
     * записью надо убрать и копии файлов, а их ссылки лежат только в ней.
     */
    suspend fun find(tempId: Long): PendingSend? = all().firstOrNull { it.tempId == tempId }
    
    @Serializable
    private data class Record(
        val tempId: Long,
        val chatId: Long,
        val text: String? = null,
        val uris: List<String> = emptyList(),
        val replyTo: ReplyRecord? = null
    )
    
    @Serializable
    private data class ReplyRecord(
        val messageId: Long,
        val chatId: Long? = null,
        val senderId: Long? = null,
        val senderName: String? = null,
        val chatName: String? = null,
        val text: String? = null,
        val attachmentTypes: List<String> = emptyList()
    )
    
    private companion object {
        const val TAG = "PendingSendStore"
        const val DIRECTORY_NAME = "pending_sends"
        const val EXTENSION = ".json"
        const val TEMP_SUFFIX = ".tmp"
    }
}
