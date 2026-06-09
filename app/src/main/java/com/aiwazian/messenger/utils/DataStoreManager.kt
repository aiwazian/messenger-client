/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aiwazian.messenger.enums.AppPrimaryColor
import com.aiwazian.messenger.enums.ThemeOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private object Keys {
    val THEME = stringPreferencesKey("app_theme")
    val PRIMARY_COLOR = stringPreferencesKey("primary_color")
    val PASSCODE = stringPreferencesKey("passcode")
    val IS_LOCK_APP = booleanPreferencesKey("is_lock_app")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
    val BLOCKED_UNTIL = longPreferencesKey("blocked_until")
    val VIDEO_LOOPING = booleanPreferencesKey("video_looping")
    val VIDEO_PLAYBACK_SPEED = floatPreferencesKey("video_playback_speed")
    val AUTO_DOWNLOAD_MEDIA = booleanPreferencesKey("auto_download_media")
    val AUTO_DOWNLOAD_PHOTOS = booleanPreferencesKey("auto_download_photos")
    val AUTO_DOWNLOAD_VIDEOS = booleanPreferencesKey("auto_download_videos")
    val AUTO_DOWNLOAD_FILES = booleanPreferencesKey("auto_download_files")
}

@Singleton
class DataStoreManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val Context.dataStore by preferencesDataStore("data_store")
    
    private suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { settings ->
            settings[key] = value
        }
    }
    
    private fun <T> getValue(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return context.dataStore.data.map { pref ->
            pref[key] ?: defaultValue
        }
    }
    
    suspend fun savePasscode(passcode: String) = setValue(Keys.PASSCODE, passcode)
    
    suspend fun saveIsLockApp(isLock: Boolean) = setValue(Keys.IS_LOCK_APP, isLock)
    
    suspend fun saveFailedAttempts(attempts: Int) = setValue(Keys.FAILED_ATTEMPTS, attempts)
    
    suspend fun saveBlockedUntil(timestamp: Long) = setValue(Keys.BLOCKED_UNTIL, timestamp)
    
    suspend fun savePrimaryColor(color: AppPrimaryColor) = setValue(Keys.PRIMARY_COLOR, color.name)
    
    suspend fun saveTheme(theme: ThemeOption) = setValue(Keys.THEME, theme.name)
    
    suspend fun saveDynamicColor(dynamicColor: Boolean) = setValue(Keys.DYNAMIC_COLOR, dynamicColor)
    
    suspend fun saveVideoLooping(isLooping: Boolean) = setValue(Keys.VIDEO_LOOPING, isLooping)
    
    suspend fun saveVideoPlaybackSpeed(speed: Float) = setValue(Keys.VIDEO_PLAYBACK_SPEED, speed)
    
    suspend fun saveAutoDownloadMedia(enabled: Boolean) =
        setValue(Keys.AUTO_DOWNLOAD_MEDIA, enabled)
    
    suspend fun saveAutoDownloadPhotos(enabled: Boolean) =
        setValue(Keys.AUTO_DOWNLOAD_PHOTOS, enabled)
    
    suspend fun saveAutoDownloadVideos(enabled: Boolean) =
        setValue(Keys.AUTO_DOWNLOAD_VIDEOS, enabled)
    
    suspend fun saveAutoDownloadFiles(enabled: Boolean) =
        setValue(Keys.AUTO_DOWNLOAD_FILES, enabled)
    
    fun getPasscode() = getValue(Keys.PASSCODE, "")
    
    fun getIsLockApp() = getValue(Keys.IS_LOCK_APP, false)
    
    fun getFailedAttempts() = getValue(Keys.FAILED_ATTEMPTS, 0)
    
    fun getBlockedUntil() = getValue(Keys.BLOCKED_UNTIL, 0L)
    
    fun getPrimaryColor() = getValue(Keys.PRIMARY_COLOR, AppPrimaryColor.Blue.name)
    
    fun getTheme() = getValue(Keys.THEME, ThemeOption.SYSTEM.name)
    
    fun getDynamicColor() = getValue(Keys.DYNAMIC_COLOR, true)
    
    fun getVideoLooping() = getValue(Keys.VIDEO_LOOPING, false)
    
    fun getVideoPlaybackSpeed() = getValue(Keys.VIDEO_PLAYBACK_SPEED, 1.0f)
    
    fun getAutoDownloadMedia() = getValue(Keys.AUTO_DOWNLOAD_MEDIA, false)
    
    fun getAutoDownloadPhotos() = getValue(Keys.AUTO_DOWNLOAD_PHOTOS, true)
    
    fun getAutoDownloadVideos() = getValue(Keys.AUTO_DOWNLOAD_VIDEOS, true)
    
    fun getAutoDownloadFiles() = getValue(Keys.AUTO_DOWNLOAD_FILES, true)
    
    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
