/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.database.dao.AccountDao
import com.aiwazian.messenger.database.dao.ChatDao
import com.aiwazian.messenger.database.dao.NotificationSettingsDao
import com.aiwazian.messenger.database.entity.NotificationSettingsEntity
import com.aiwazian.messenger.domain.ChatNotificationException
import com.aiwazian.messenger.domain.NotificationSettings
import com.aiwazian.messenger.enums.ChatFolderCategory
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.network.api.NotificationSettingsApi
import com.aiwazian.messenger.network.dto.UpdateChatNotificationSettingRequestDto
import com.aiwazian.messenger.network.dto.UpdateNotificationSettingsRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Настройки уведомлений: Room как кэш, сервер как источник правды.
 *
 * Singleton не ради состояния, а ради единого экземпляра для экрана, слушателя сокета
 * и показа уведомлений.
 *
 * Здесь же живут исключения по отдельным чатам: для пользователя это одна и та же
 * настройка, просто с разной точностью, и решается всё в одном месте.
 */
@Singleton
class NotificationSettingsRepository @Inject constructor(
    private val notificationSettingsApi: NotificationSettingsApi,
    private val notificationSettingsDao: NotificationSettingsDao,
    private val chatDao: ChatDao,
    private val accountDao: AccountDao
) {
    
    /**
     * Кэш исключений в памяти: один на приложение, репозиторий — Singleton.
     *
     * В Room не кладётся, чтобы не заводить лишнюю таблицу и миграцию: строк
     * немного, а на старте и при коннекте список всё равно перечитывается с
     * сервера. Экран настроек, экран категории и список чатов читают его отсюда,
     * поэтому удаление исключения на одном экране сразу видно на другом.
     */
    private val chatExceptions = MutableStateFlow<List<ChatNotificationException>>(emptyList())
    
    /**
     * Текущие настройки активного аккаунта.
     *
     * Нет записи в базе — всё включено: это же состояние по умолчанию и на сервере.
     */
    fun observe(): Flow<NotificationSettings> = flow {
        val userId = currentUserId()
        
        if (userId == null) {
            emit(NotificationSettings())
            return@flow
        }
        
        emitAll(
            notificationSettingsDao.observe(userId)
                .map { it?.toDomain() ?: NotificationSettings() })
    }
    
    /** Подтянуть серверное состояние в кэш — настройку могли менять с другого устройства. */
    suspend fun refresh() {
        val userId = currentUserId() ?: return
        
        try {
            val response = notificationSettingsApi.getNotificationSettings()
            val settings = response.body()
            
            if (response.isSuccessful && settings != null) {
                notificationSettingsDao.upsert(settings.toDomain().toEntity(userId))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось загрузить настройки уведомлений", e)
        }
    }
    
    /**
     * Сохранить новое состояние переключателей.
     *
     * Сначала пишется в Room, и только потом уходит запрос: переключатель должен
     * срабатывать сразу, а не ждать сеть. Если запрос не ушёл, значение возвращается
     * обратно: иначе локально уведомления выключены, а сервер о этом не знает и
     * продолжает слать пуши.
     */
    suspend fun update(settings: NotificationSettings): Result<Unit> {
        val userId = currentUserId()
            ?: return Result.failure(IllegalStateException("Нет активного аккаунта"))
        
        val previous = notificationSettingsDao.get(userId)?.toDomain() ?: NotificationSettings()
        
        notificationSettingsDao.upsert(settings.toEntity(userId))
        
        return try {
            val response = notificationSettingsApi.updateNotificationSettings(
                UpdateNotificationSettingsRequestDto(
                    privateChats = settings.privateChats,
                    groups = settings.groups,
                    channels = settings.channels
                )
            )
            
            if (response.isSuccessful) {
                response.body()?.let {
                    notificationSettingsDao.upsert(it.toDomain().toEntity(userId))
                }
                
                Result.success(Unit)
            } else {
                notificationSettingsDao.upsert(previous.toEntity(userId))
                Result.failure(Exception("Failed to update notification settings: ${response.code()}"))
            }
        } catch (e: Exception) {
            notificationSettingsDao.upsert(previous.toEntity(userId))
            Result.failure(e)
        }
    }
    
    /** Настройку поменяли в другой сессии — событие пришло по сокету. */
    suspend fun applyRemote(settings: NotificationSettings) {
        val userId = currentUserId() ?: return
        notificationSettingsDao.upsert(settings.toEntity(userId))
    }
    
    /**
     * Молчит ли конкретный чат: колокольчик в шапке чата и надпись в меню.
     *
     * Флаг считает сервер и кладёт рядом с чатом, поэтому категорию здесь учитывать
     * не нужно: она уже учтена.
     */
    fun observeChatMuted(chatId: Long): Flow<Boolean> = flow {
        val userId = currentUserId()
        
        if (userId == null) {
            emit(false)
            return@flow
        }
        
        emitAll(chatDao.getChatByIdFlow(userId, chatId).map { it?.isMuted == true })
    }
    
    /** Исключения для экранов категорий и колокольчика в списке чатов. */
    fun observeChatExceptions(): Flow<List<ChatNotificationException>> = chatExceptions.asStateFlow()
    
    /** Перечитать исключения с сервера в общий кэш. */
    suspend fun refreshChatExceptions() {
        try {
            val response = notificationSettingsApi.getChatNotificationSettings()
            
            if (response.isSuccessful) {
                chatExceptions.value = response.body().orEmpty()
                    .map { ChatNotificationException(it.chatId, it.enabled) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось загрузить исключения чатов", e)
        }
    }
    
    /**
     * Добавить чат в исключения: уведомления по нему всегда включены или всегда
     * выключены, независимо от настройки его категории.
     *
     * Колокольчик рисуется до ответа сервера и возвращается назад, если запрос не
     * прошёл: иначе интерфейс обещает тишину, которой не будет.
     *
     * В кэш исключений новое состояние кладётся так же сразу: подпись строки и её
     * меню на экране категории читаются оттуда, и ждать ответа сервера, чтобы
     * перерисовать только что нажатую строку, незачем.
     */
    suspend fun setChatNotifications(chatId: Long, enabled: Boolean): Result<Unit> {
        val userId = currentUserId()
            ?: return Result.failure(IllegalStateException("Нет активного аккаунта"))
        
        val previousMuted = chatDao.isMuted(userId, chatId) ?: false
        val previousException = chatExceptions.value.firstOrNull { it.chatId == chatId }
        
        chatDao.setMuted(userId, chatId, !enabled)
        upsertException(chatId, enabled)
        
        return try {
            val response = notificationSettingsApi.setChatNotificationSetting(
                chatId, UpdateChatNotificationSettingRequestDto(enabled = enabled)
            )
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                rollbackChatSetting(userId, chatId, previousMuted, previousException)
                Result.failure(Exception("Failed to update chat notifications: ${response.code()}"))
            }
        } catch (e: Exception) {
            rollbackChatSetting(userId, chatId, previousMuted, previousException)
            Result.failure(e)
        }
    }
    
    /**
     * Все исключения разом — для экрана со списком чатов, выключенных или
     * включённых принудительно.
     */
    suspend fun getChatExceptions(): List<ChatNotificationException> {
        return try {
            val response = notificationSettingsApi.getChatNotificationSettings()
            
            if (response.isSuccessful) {
                response.body().orEmpty().map { ChatNotificationException(it.chatId, it.enabled) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось загрузить исключения чатов", e)
            emptyList()
        }
    }
    
    /**
     * Убрать чат из исключений: он снова следует настройке своей категории.
     *
     * Кроме кэша пересчитываем и колокольчик: свежий isMuted сервер пришлёт только
     * со следующим списком чатов, а строка в списке и шапка чата должны быть правы
     * уже сейчас.
     */
    suspend fun removeChatException(chatId: Long): Result<Unit> {
        return try {
            val response = notificationSettingsApi.deleteChatNotificationSetting(chatId)
            
            if (response.isSuccessful) {
                removeExceptionFromCache(chatId)
                resetChatMutedToCategory(chatId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete chat notification setting: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Убрать все исключения выбранной категории разом.
     *
     * Кнопка «Удалить все» на экране категории: одним запросом снимаем исключения
     * только этой категории, чужие не трогаем.
     */
    suspend fun removeAllChatExceptions(category: ChatFolderCategory): Result<Unit> {
        return try {
            val response =
                notificationSettingsApi.deleteAllChatNotificationSettings(category.name)
            
            if (response.isSuccessful) {
                val removed = chatExceptions.value.filter { category.matches(it.chatId) }
                chatExceptions.update { list -> list.filterNot { category.matches(it.chatId) } }
                removed.forEach { resetChatMutedToCategory(it.chatId) }
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete chat notification settings: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /** Чат выключили или включили в другой сессии — событие chat:notifications. */
    suspend fun applyRemoteChatSetting(chatId: Long, enabled: Boolean) {
        val userId = currentUserId() ?: return
        chatDao.setMuted(userId, chatId, !enabled)
        upsertException(chatId, enabled)
    }
    
    /**
     * Показывать ли уведомление из этого чата.
     *
     * Проверка дублирует серверную не от недоверия, а потому что между выключением
     * переключателя и его доездом до сервера есть окно, да и пуш мог уйти в полёт
     * раньше. Лучше отбросить его здесь, чем показать вопреки настройке.
     *
     * Исключение по чату сильнее категории, поэтому сначала смотрим на чат: там лежит
     * итоговое состояние с сервера, где категория уже учтена.
     *
     * Любая неопределённость толкуется в пользу показа: пропущенное уведомление
     * не вернёшь, а лишнее пользователь смахнёт.
     */
    suspend fun isEnabledFor(chatId: Long): Boolean {
        val userId = currentUserId() ?: return true
        
        chatDao.isMuted(userId, chatId)?.let { return !it }
        
        val settings = notificationSettingsDao.get(userId)?.toDomain() ?: return true
        
        return settings.isEnabledFor(ChatType.fromId(chatId))
    }
    
    /** Свежее исключение — наверх, как отдаёт сервер. */
    private fun upsertException(chatId: Long, enabled: Boolean) {
        chatExceptions.update { list ->
            listOf(ChatNotificationException(chatId, enabled)) +
                list.filterNot { it.chatId == chatId }
        }
    }
    
    private fun removeExceptionFromCache(chatId: Long) {
        chatExceptions.update { list -> list.filterNot { it.chatId == chatId } }
    }
    
    /**
     * Вернуть чат в состояние до неудавшегося запроса.
     *
     * Исключения не было — убираем добавленное; было — возвращаем прежнее
     * значение, иначе экран останется с исключением, которого на сервере нет.
     */
    private suspend fun rollbackChatSetting(
        userId: Long,
        chatId: Long,
        muted: Boolean,
        exception: ChatNotificationException?
    ) {
        chatDao.setMuted(userId, chatId, muted)
        
        if (exception == null) {
            removeExceptionFromCache(chatId)
        } else {
            upsertException(exception.chatId, exception.enabled)
        }
    }
    
    /**
     * Исключение снято — чат снова живёт по настройке своей категории, и колокольчик
     * считаем сами по тем же правилам, что и сервер.
     */
    private suspend fun resetChatMutedToCategory(chatId: Long) {
        val userId = currentUserId() ?: return
        val settings = notificationSettingsDao.get(userId)?.toDomain() ?: NotificationSettings()
        
        chatDao.setMuted(userId, chatId, !settings.isEnabledFor(ChatType.fromId(chatId)))
    }
    
    private suspend fun currentUserId(): Long? = accountDao.getCurrentAccount()?.userId
    
    private fun NotificationSettingsEntity.toDomain() = NotificationSettings(
        privateChats = privateChats,
        groups = groups,
        channels = channels
    )
    
    private fun NotificationSettings.toEntity(userId: Long) = NotificationSettingsEntity(
        userId = userId,
        privateChats = privateChats,
        groups = groups,
        channels = channels
    )
    
    private companion object {
        const val TAG = "NotificationSettings"
    }
}
