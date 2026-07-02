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
