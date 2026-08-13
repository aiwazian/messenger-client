/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

/**
 * Сервер ответил, что файла аватарки больше нет (404/410).
 *
 * Отдельный тип нужен, чтобы отличать «файл удалён» от обычной сетевой ошибки:
 * в первом случае кэш надо почистить, во втором — обязательно сохранить, иначе
 * пользователь без сети остался бы без аватарок.
 */
class AvatarNotFoundException(val fileId: String) :
    Exception("Avatar file $fileId no longer exists on the server")
