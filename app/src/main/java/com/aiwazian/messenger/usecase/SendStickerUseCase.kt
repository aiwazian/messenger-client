package com.aiwazian.messenger.usecase

import android.util.Log
import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.MessageApi
import com.aiwazian.messenger.network.dto.StickerMessageRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendStickerUseCase @Inject constructor(
    private val messageApi: MessageApi,
    private val messageDao: MessageDao
) {
    suspend operator fun invoke(
        chatId: Long,
        stickerId: Long,
        replyToId: Long? = null
    ): Result<Message> = withContext(Dispatchers.IO) {
        try {
            val response = messageApi.sendStickerMessage(
                chatId = chatId,
                request = StickerMessageRequestDto(
                    stickerId = stickerId.toString(),
                    replyToId = replyToId?.toString()
                ),
                socketId = ""
            )
            
            val body = response.body()
            
            if (!response.isSuccessful || body == null) {
                return@withContext Result.failure(
                    IllegalStateException("Sticker send failed: ${response.code()}")
                )
            }
            
            val message = body.toDomain()
            messageDao.saveMessages(listOf(message.toEntity()))
            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Sticker send error", e)
            Result.failure(e)
        }
    }
    
    private companion object {
        const val TAG = "SendStickerUseCase"
    }
}
