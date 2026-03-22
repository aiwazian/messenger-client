/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aiwazian.messenger.enums.PrimaryColorOption
import com.aiwazian.messenger.enums.ThemeOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("data_store")

private object Keys {
    val THEME = stringPreferencesKey("app_theme")
    val PRIMARY_COLOR = stringPreferencesKey("primary_color")
    val PASSCODE = stringPreferencesKey("passcode")
    val IS_LOCK_APP = booleanPreferencesKey("is_lock_app")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
}

@Singleton
class DataStoreManager @Inject constructor(
    @param:ApplicationContext
    private val context: Context
) {
    private suspend fun <T> setValue(
        key: Preferences.Key<T>,
        value: T
    ) {
        context.dataStore.edit { settings ->
            settings[key] = value
        }
    }

    private fun <T> getValue(
        key: Preferences.Key<T>,
        defaultValue: T
    ): Flow<T> {
        return context.dataStore.data.map { pref ->
            pref[key] ?: defaultValue
        }
    }

    suspend fun savePasscode(passcode: String) = setValue(
        Keys.PASSCODE,
        passcode
    )

    suspend fun saveIsLockApp(isLock: Boolean) = setValue(
        Keys.IS_LOCK_APP,
        isLock
    )

    suspend fun savePrimaryColor(colorName: String) = setValue(
        Keys.PRIMARY_COLOR,
        colorName
    )

    suspend fun saveTheme(theme: ThemeOption) = setValue(
        Keys.THEME,
        theme.toString()
    )

    suspend fun saveDynamicColor(dynamicColor: Boolean) = setValue(
        Keys.DYNAMIC_COLOR,
        dynamicColor
    )

    fun getPasscode() = getValue(
        Keys.PASSCODE,
        ""
    )

    fun getIsLockApp() = getValue(
        Keys.IS_LOCK_APP,
        false
    )

    fun getPrimaryColor() = getValue(
        Keys.PRIMARY_COLOR,
        PrimaryColorOption.Blue.name
    )

    fun getTheme() = getValue(
        Keys.THEME,
        ThemeOption.SYSTEM.name
    )

    fun getDynamicColor() = getValue(
        Keys.DYNAMIC_COLOR,
        false
    )
}
