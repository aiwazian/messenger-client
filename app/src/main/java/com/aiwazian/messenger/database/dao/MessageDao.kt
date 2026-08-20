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

/**
 * Кэш сообщений, разделённый по аккаунтам.
 *
 * Условие по владельцу везде берётся подзапросом из таблицы account, а не
 * параметром: активный аккаунт там уже хранится, и это единственный источник
 * правды. Иначе каждый вызывающий обязан был бы таскать за собой свой id и
 * ошибка в одном месте снова показала бы чужую переписку.
 */
@Dao
interface MessageDao {
    
    /**
     * Сохранение сообщений с присвоением владельца.
     *
     * Upsert перезаписывает строку целиком и возвращает ownerId к нулю, поэтому
     * владелец проставляется сразу после записи — иначе свежее сообщение стало бы
     * невидимым для всех аккаунтов. Тот, кто последним получил сообщение с сервера,
     * и становится владельцем строки: остальные перечитают её при открытии чата.
     */
    @Transaction
    suspend fun saveMessages(messages: List<MessageEntity>) {
        upsertMessages(messages)
        claimUnownedMessages()
    }
    
    @Upsert
    suspend fun upsertMessages(messages: List<MessageEntity>)
    
    /**
     * Присвоить активному аккаунту строки без владельца.
     *
     * EXISTS защищает от записи NULL в NOT NULL колонку, когда в базе нет активного
     * аккаунта: сообщения тогда просто ждут ближайшего входа.
     */
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
        "SELECT * FROM (" +
                "SELECT * FROM message " +
                "WHERE ((senderId = :senderId AND chatId = :chatId) " +
                "OR (senderId = :chatId AND chatId = :senderId)) " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "ORDER BY sendTime DESC " +
                "LIMIT :limit OFFSET :offset" +
                ") " +
                "ORDER BY sendTime ASC"
    )
    fun getMessagesWithAttachments(
        senderId: Long, chatId: Long, limit: Int, offset: Int
    ): Flow<List<MessageWithAttachments>>

    @Transaction
    @Query(
        "SELECT * FROM (" +
                "SELECT * FROM message " +
                   "WHERE (chatId = :chatId " +
                   "OR senderId = :chatId) " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "ORDER BY sendTime DESC " +
                "LIMIT :limit OFFSET :offset" +
                ") " +
                "ORDER BY sendTime ASC"
    )
    fun getMessagesWithAttachments(
        chatId: Long, limit: Int, offset: Int
    ): Flow<List<MessageWithAttachments>>
    
    @Transaction
    @Query(
        "SELECT * FROM message " +
                "WHERE id = :messageId " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "LIMIT 1"
    )
    suspend fun getMessageById(messageId: Long): MessageWithAttachments?
    
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

    /**
     * Сохранить результат правки: новый текст, время и сам факт правки.
     *
     * isEdited проставляется здесь же и безусловно: запрос вызывается только по
     * факту изменения, а editedAt через трое суток придёт с сервера пустым и
     * больше не годится в качестве признака.
     */
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
    
    /** Чаты пропали из списка своего аккаунта: кэш чужих аккаунтов при этом не трогаем. */
    @Query(
        "DELETE FROM message " +
                "WHERE chatId NOT IN (:chatIds) " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)"
    )
    suspend fun deleteMessagesNotInChatIds(chatIds: List<Long>)
    
    @Query(
        "DELETE FROM message " +
                "WHERE ((senderId = :userId AND chatId = :chatId) " +
                "OR (senderId = :chatId AND chatId = :userId) " +
                "OR (chatId = :chatId)) " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "AND sendTime >= :minTime AND sendTime <= :maxTime " +
                "AND id > 0 AND id NOT IN (:ids)"
    )
    suspend fun deleteMessagesInRangeExcluding(
        userId: Long,
        chatId: Long,
        minTime: Long,
        maxTime: Long,
        ids: List<Long>
    )
    
    /** Полная очистка таблицы: используется при выходе, поэтому без фильтра по владельцу. */
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
    
    /**
     * id своих сообщений в чате до указанного времени отправки.
     *
     * Нужны, чтобы по событию о прочтении проставить время прочтения тем же
     * сообщениям, которым событие ставит вторую галочку.
     *
     * Условие по chatId работает и для личного чата, и для группы: у своих сообщений
     * там лежит собеседник и id группы соответственно — ровно то, что приходит
     * в событии.
     *
     * id > 0 отсекает локальные отправляемые сообщения: их ещё нет на сервере,
     * и прочитаны они быть не могут.
     */
    @Query(
        "SELECT id FROM message " +
                "WHERE senderId = :myId AND chatId = :chatId " +
                "AND ownerId = " +
                "(SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1) " +
                "AND id > 0 AND sendTime <= :upToSendTime"
    )
    suspend fun getOwnMessageIdsUpTo(chatId: Long, myId: Long, upToSendTime: Long): List<Long>
    
    /**
     * Отметить прочитанными все входящие сообщения до указанного времени.
     *
     * В группе отметка только по одному senderId оставляла бы сообщения
     * остальных участников непрочитанными.
     */
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
    
    /**
     * Окно сообщений личного чата по диапазону id.
     *
     * Пагинация через LIMIT/OFFSET не умеет показать «середину» истории, поэтому окно
     * задаётся границами id. toId = Long.MAX_VALUE означает «до конца чата», тогда новые
     * сообщения из сокета попадают в окно автоматически.
     *
     * includePending = 1 добавляет локальные отправляемые сообщения (у них отрицательные id).
     */
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
    
    /** Окно сообщений группы/канала по диапазону id. */
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
    
    /** id последних сообщений личного чата — нужны, чтобы открыть чат без сети. */
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
    
    /** id последних сообщений группы/канала. */
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
