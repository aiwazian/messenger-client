/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.aiwazian.messenger.database.entity.MessageEntity
import com.aiwazian.messenger.database.entity.MessageWithAttachments
import com.aiwazian.messenger.enums.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Transaction
    suspend fun saveMessages(messages: List<MessageEntity>) {
        upsertMessages(messages)
        claimUnownedMessages()
    }
    
    @Upsert
    suspend fun upsertMessages(messages: List<MessageEntity>)
  
    @Query(
        "UPDATE message " +
                "SET ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "WHERE ownerId = 0 " +
                "AND EXISTS (SELECT 1 FROM account WHERE isCurrent = 1)"
    )
    suspend fun claimUnownedMessages()

    @Transaction
    @Query(
        "SELECT * FROM message " +
                "WHERE id = :messageId " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "LIMIT 1"
    )
    suspend fun getMessageById(messageId: Long): MessageWithAttachments?

    /** Сообщения по списку id — для панели закреплённых сообщений. */
    @Transaction
    @Query(
        "SELECT * FROM message " +
                "WHERE id IN (:ids) " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)"
    )
    fun getMessagesByIds(ids: List<Long>): Flow<List<MessageWithAttachments>>
    
    @Transaction
    @Query(
        "SELECT * FROM message " +
                "WHERE ((senderId = :userId AND chatId = :chatId) " +
                "OR (chatId = :userId AND senderId = :chatId)) " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "ORDER BY sendTime DESC " +
                "LIMIT 1"
    )
    fun getChatLastMessageFlow(userId: Long, chatId: Long): Flow<MessageWithAttachments?>
    
    @Transaction
    @Query(
        "SELECT * FROM message " +
                "WHERE chatId = :chatId " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "ORDER BY sendTime DESC " +
                "LIMIT 1"
    )
    fun getChatLastMessageFlow(chatId: Long): Flow<MessageWithAttachments?>
    
    @Query(
        "DELETE FROM message " +
                "WHERE ((senderId = :userId AND chatId = :chatId) " +
                "OR (senderId = :chatId AND chatId = :userId) " +
                "OR (senderId = :chatId AND chatId = :chatId)) " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)"
    )
    suspend fun clearChatHistory(userId: Long, chatId: Long)
    
    @Query(
        "DELETE FROM message " +
                "WHERE id = :id " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)"
    )
    suspend fun deleteMessageById(id: Long)

    @Query(
        "UPDATE message SET text = :text, editedAt = :editedAt, isEdited = 1 " +
                "WHERE id = :id " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)"
    )
    suspend fun updateMessageTextAndEditedAt(id: Long, text: String, editedAt: Long?)
    
    @Query(
        "UPDATE message SET status = :status " +
                "WHERE id = :id " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)"
    )
    suspend fun updateMessageStatus(id: Long, status: MessageStatus)

    @Query(
        "UPDATE message SET id = :newId " +
                "WHERE id = :oldId " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)"
    )
    suspend fun updateMessageId(oldId: Long, newId: Long)

    @Query(
        "UPDATE message SET commentsCount = commentsCount + 1 " +
                "WHERE id = :messageId " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)"
    )
    suspend fun incrementCommentCount(messageId: Long)

    @Query(
        "UPDATE message SET commentsCount = MAX(commentsCount - 1, 0) " +
                "WHERE id = :messageId " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)"
    )
    suspend fun decrementCommentCount(messageId: Long)

    @Query(
        "UPDATE message SET commentsCount = 0 " +
                "WHERE chatId = :chatId " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)"
    )
    suspend fun clearCommentCounts(chatId: Long)
    
    @Query(
        "DELETE FROM message " +
                "WHERE chatId NOT IN (:chatIds) " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)"
    )
    suspend fun deleteMessagesNotInChatIds(chatIds: List<Long>)
    
    @Query("DELETE FROM message")
    suspend fun deleteAll()
    
    @Query(
        "UPDATE message SET isRead = :isRead " +
                "WHERE id = :id " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)"
    )
    suspend fun updateMessageReadStatus(id: Long, isRead: Boolean)
    
    @Query(
        "UPDATE message SET isRead = :isRead " +
                "WHERE chatId = :chatId AND senderId = :senderId " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "AND sendTime <= :upToSendTime AND isRead = 0"
    )
    suspend fun updateReadStatusBySenderUpTo(
        chatId: Long, senderId: Long, upToSendTime: Long, isRead: Boolean
    )

    @Query(
        "SELECT id FROM message " +
                "WHERE senderId = :myId AND chatId = :chatId " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "AND id > 0 AND sendTime <= :upToSendTime"
    )
    suspend fun getOwnMessageIdsUpTo(chatId: Long, myId: Long, upToSendTime: Long): List<Long>

    @Query(
        """
        UPDATE message SET isRead = 1 
        WHERE chatId = :chatId AND senderId != :myId 
            AND ownerId = (SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) 
            AND sendTime <= :upToSendTime AND isRead = 0
    """
    )
    suspend fun markIncomingReadUpTo(chatId: Long, myId: Long, upToSendTime: Long)
    
    @Query(
        "UPDATE message SET isRead = 1 " +
                "WHERE chatId = :chatId AND senderId != :myId " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "AND isRead = 0"
    )
    suspend fun markAllIncomingRead(chatId: Long, myId: Long)

    @Transaction
    @Query(
        "SELECT * FROM message " +
                "WHERE ((senderId = :senderId AND chatId = :chatId) " +
                "OR (senderId = :chatId AND chatId = :senderId)) " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "AND ((id >= :fromId AND id <= :toId) OR (:includePending = 1 AND id < 0)) " +
                "ORDER BY sendTime ASC, id ASC"
    )
    fun getPrivateMessagesWindow(
        senderId: Long, chatId: Long, fromId: Long, toId: Long, includePending: Int
    ): Flow<List<MessageWithAttachments>>
    
    @Transaction
    @Query(
        "SELECT * FROM message " +
                "WHERE (chatId = :chatId OR senderId = :chatId) " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "AND ((id >= :fromId AND id <= :toId) OR (:includePending = 1 AND id < 0)) " +
                "ORDER BY sendTime ASC, id ASC"
    )
    fun getChatMessagesWindow(
        chatId: Long, fromId: Long, toId: Long, includePending: Int
    ): Flow<List<MessageWithAttachments>>
    
    @Query(
        "SELECT id FROM message " +
                "WHERE ((senderId = :senderId AND chatId = :chatId) " +
                "OR (senderId = :chatId AND chatId = :senderId)) " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "AND id > 0 " +
                "ORDER BY sendTime DESC, id DESC LIMIT :limit"
    )
    suspend fun getLastPrivateMessageIds(senderId: Long, chatId: Long, limit: Int): List<Long>
    
    @Query(
        "SELECT id FROM message " +
                "WHERE (chatId = :chatId OR senderId = :chatId) " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "AND id > 0 " +
                "ORDER BY sendTime DESC, id DESC LIMIT :limit"
    )
    suspend fun getLastChatMessageIds(chatId: Long, limit: Int): List<Long>
}
