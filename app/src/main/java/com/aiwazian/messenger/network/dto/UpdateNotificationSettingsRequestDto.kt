/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.Serializable

/**
 * Отправляем все три флага сразу, а не один изменённый.
 *
 * На сервере поля необязательные, но частичный объект потребовал бы отключать
 * сериализацию null — иначе в теле уедет privateChats: null и валидатор сочтёт это
 * попыткой записать пустое значение. Экран и так знает все три значения.
 */
@Serializable
data class UpdateNotificationSettingsRequestDto(
    val privateChats: Boolean,
    val groups: Boolean,
    val channels: Boolean
)
