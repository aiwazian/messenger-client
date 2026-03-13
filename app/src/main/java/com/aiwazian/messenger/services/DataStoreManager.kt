package com.aiwazian.messenger.services

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aiwazian.messenger.enums.Language
import com.aiwazian.messenger.enums.PrimaryColorOption
import com.aiwazian.messenger.enums.ThemeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val USER_PREFERENCES_NAME = "data_store"

private val Context.dataStore by preferencesDataStore(USER_PREFERENCES_NAME)

private object Keys {
    val THEME = stringPreferencesKey("app_theme")
    val LANGUAGE = stringPreferencesKey("language")
    val TOKEN = stringPreferencesKey("token")
    val PRIMARY_COLOR = stringPreferencesKey("primary_color")
    val PASSCODE = stringPreferencesKey("passcode")
    val IS_LOCK_APP = booleanPreferencesKey("is_lock_app")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
}

class DataStoreManager private constructor(private val context: Context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: DataStoreManager? = null
        
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = DataStoreManager(context.applicationContext)
                    }
                }
            }
        }
        
        fun getInstance(): DataStoreManager {
            return INSTANCE ?: throw IllegalStateException("DataStoreManager is not initialized")
        }
    }
    
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
    
    suspend fun saveToken(token: String) = setValue(
        Keys.TOKEN,
        token
    )
    
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
    
    suspend fun saveLanguage(language: Language) = setValue(
        Keys.LANGUAGE,
        language.toString()
    )
    
    suspend fun saveTheme(theme: ThemeOption) = setValue(
        Keys.THEME,
        theme.toString()
    )
    
    suspend fun saveDynamicColor(dynamicColor: Boolean) = setValue(
        Keys.DYNAMIC_COLOR,
        dynamicColor
    )
    
    fun getToken() = getValue(
        Keys.TOKEN,
        ""
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
    
    fun getLanguage() = getValue(
        Keys.LANGUAGE,
        Language.EN.toString()
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
