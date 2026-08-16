/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FileClient

/**
 * Скоуп, живущий столько же, сколько процесс приложения.
 *
 * Отправка сообщения не должна умирать вместе с экраном: пользователь закрывает
 * чат или сворачивает приложение, а повторные попытки обязаны продолжаться.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
