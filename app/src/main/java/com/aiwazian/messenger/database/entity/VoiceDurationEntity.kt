/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Длина голосового, посчитанная на устройстве.
 *
 * Сервер длину не хранит, поэтому она достаётся из скачанного файла и
 * остаётся здесь: разбирать те же файлы при каждом открытии вкладки —
 * заметная пауза на списке из тысячи записей.
 *
 * Ключ — файл, а не вложение: длина принадлежит записи, а не тому, сколько
 * раз её переслали, и у копий она появляется сразу.
 */
@Entity(tableName = "voice_duration")
data class VoiceDurationEntity(
    @PrimaryKey
    val fileId: String,
    val durationMs: Int
)
