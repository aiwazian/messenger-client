/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.utils.SharedFileCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Медиа, которое клавиатура вставляет прямо в поле ввода: GIF, стикеры, картинки.
 *
 * Права на content://-ссылку клавиатура даёт только на время сессии ввода, а
 * отправка живёт дольше: она уходит в очередь и при плохой сети повторяется
 * минутами. Поэтому файл сразу перекладывается в свой кэш — тем же способом, что
 * и файлы из системного «Поделиться», — и дальше по конвейеру идёт file://-ссылка.
 *
 * Вынесено из ChatViewModel: копирование не зависит от чата, а viewModelScope
 * переживает пересоздание экрана, поэтому начатая копия не потеряется.
 */
@HiltViewModel
class KeyboardMediaViewModel @Inject constructor(
    private val sharedFileCache: SharedFileCache
) : ViewModel() {
    
    /**
     * @param onCached вызывается всегда, даже если прочитать ничего не удалось:
     * вызывающему нужно вернуть клавиатуре права на её содержимое.
     */
    fun cache(uris: List<Uri>, onCached: (List<Uri>) -> Unit) {
        viewModelScope.launch {
            val cached = sharedFileCache.cache(uris)
            
            if (cached.isEmpty()) {
                Log.e(TAG, "Не удалось скопировать медиа из клавиатуры: $uris")
            }
            
            onCached(cached)
        }
    }
    
    private companion object {
        const val TAG = "KeyboardMediaViewModel"
    }
}
