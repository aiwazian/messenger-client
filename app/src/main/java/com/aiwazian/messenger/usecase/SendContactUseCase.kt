package com.aiwazian.messenger.usecase

import android.content.Context
import android.util.Log
import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageContact
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.MessageApi
import com.aiwazian.messenger.network.dto.ContactMessageRequestDto
import com.aiwazian.messenger.utils.PhoneLinks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendContactUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageApi: MessageApi,
    private val messageDao: MessageDao
) {
    suspend operator fun invoke(
        chatId: Long,
        contact: MessageContact
    ): Result<Message> = withContext(Dispatchers.IO) {
        try {
            val region = PhoneLinks.deviceRegion(context)

            val response = messageApi.sendContactMessage(
                chatId = chatId,
                request = ContactMessageRequestDto(
                    name = contact.name,
                    phones = contact.phones.map { PhoneLinks.formatE164(it, region) }.distinct(),
                    emails = contact.emails,
                    urls = contact.urls,
                    addresses = contact.addresses,
                    organization = contact.organization,
                    jobTitle = contact.jobTitle,
                    birthday = contact.birthday
                ),
                socketId = ""
            )

            val body = response.body()

            if (!response.isSuccessful || body == null) {
                return@withContext Result.failure(
                    IllegalStateException("Contact send failed: ${response.code()}")
                )
            }

            val message = body.toDomain()
            messageDao.saveMessages(listOf(message.toEntity()))
            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Contact send error", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "SendContactUseCase"
    }
}
