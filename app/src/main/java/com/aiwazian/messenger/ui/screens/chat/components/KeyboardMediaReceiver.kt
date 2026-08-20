/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.components

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.inputmethod.InputContentInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.ui.Modifier
import androidx.core.os.BundleCompat
import androidx.core.view.inputmethod.InputConnectionCompat

private const val TAG = "KeyboardMediaReceiver"

/**
 * Разрешает вставлять в поле ввода медиа с клавиатуры: GIF, стикеры, картинки.
 *
 * Само наличие обработчика содержимого говорит клавиатуре, что поле принимает не
 * только текст. Без него GBoard на выбранный GIF отвечает «Тут нельзя вставить
 * такое содержимое», поэтому обработчик висит прямо на поле ввода.
 *
 * В текст медиа не вставляется: [onMedia] отправляет его отдельным сообщением.
 * Всё остальное — текст, ссылки — возвращается полю и вставляется как раньше.
 *
 * @param onMedia ссылки на медиа и функция, возвращающая клавиатуре права на них.
 * Вызывать её нужно после того, как файлы скопированы: пока права наши,
 * содержимое ещё можно прочитать.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.keyboardMediaReceiver(
    context: Context,
    onMedia: (uris: List<Uri>, releasePermission: () -> Unit) -> Unit
): Modifier = contentReceiver(ReceiveContentListener { content ->
    val media = mutableListOf<Uri>()
    
    val remaining = content.consume { item ->
        val uri = item.uri
        
        if (uri == null || !context.isMedia(uri)) {
            return@consume false
        }
        
        media += uri
        true
    }
    
    if (media.isNotEmpty()) {
        onMedia(media, content.holdPermission())
    }
    
    remaining
})

/**
 * Тип спрашиваем у поставщика содержимого: клавиатура отдаёт ссылки без
 * расширения, по имени файла тип не угадать.
 */
private fun Context.isMedia(uri: Uri): Boolean {
    val type = runCatching { contentResolver.getType(uri) }.getOrNull() ?: return false
    
    return type.startsWith("image/") || type.startsWith("video/")
}

/**
 * Клавиатура выдаёт права на своё содержимое только по запросу и забирает их, как
 * только закрывается сессия ввода. Запрашиваем их сами, иначе к моменту
 * копирования файл уже не прочитать.
 *
 * @return функция, возвращающая права обратно. Ничего не делает, если клавиатура
 * не приложила описание содержимого: значит, и права не нужны.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun TransferableContent.holdPermission(): () -> Unit {
    val extras = platformTransferableContent?.extras ?: return { }
    
    val info = runCatching {
        BundleCompat.getParcelable(
            extras, InputConnectionCompat.EXTRA_INPUT_CONTENT_INFO, InputContentInfo::class.java
        )
    }.getOrNull() ?: return { }
    
    runCatching { info.requestPermission() }.onFailure {
        Log.e(TAG, "Клавиатура не дала прав на своё содержимое", it)
    }
    
    return { runCatching { info.releasePermission() } }
}
