/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Повторы без ограничения количества попыток.
 *
 * Раньше отправка сдавалась после трёх попыток и оставляла сообщение в статусе
 * «ошибка», а повтор нажимал пользователь. Теперь неудачная попытка только
 * откладывает следующую: задержка растёт с [INITIAL_DELAY] до [MAX_DELAY], чтобы
 * при долгом отсутствии сети не отправлять запрос каждую секунду.
 *
 * Отмена скоупа — единственный способ остановить цикл, поэтому вызывать это
 * стоит из скоупа приложения, а не экрана.
 */
object RetryPolicy {
    
    val INITIAL_DELAY = 1.seconds
    val MAX_DELAY = 30.seconds
    
    private const val FACTOR = 2.0
    private const val TAG = "RetryPolicy"
    
    /**
     * Выполняет [block], пока тот не вернёт успех.
     *
     * @param operation имя операции для логов.
     * @param isPermanent отказы, которые повтор не изменит. По умолчанию таких
     * нет: вызывающая сторона сама решает, что бессмысленно повторять.
     * @return успех либо безнадёжный отказ.
     */
    suspend fun <T> retryForever(
        operation: String,
        isPermanent: (Throwable) -> Boolean = { false },
        block: suspend (attempt: Int) -> Result<T>
    ): Result<T> {
        var attempt = 1
        var nextDelay = INITIAL_DELAY
        
        while (currentCoroutineContext().isActive) {
            val result = try {
                block(attempt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            
            if (result.isSuccess) {
                if (attempt > 1) {
                    Log.i(TAG, "$operation succeeded on attempt $attempt")
                }
                return result
            }
            
            val error = result.exceptionOrNull()
            
            if (error != null && isPermanent(error)) {
                Log.e(TAG, "$operation rejected for good on attempt $attempt", error)
                return result
            }
            
            Log.w(TAG, "$operation attempt $attempt failed, next try in $nextDelay", error)
            
            delay(nextDelay)
            nextDelay = increase(nextDelay)
            attempt++
        }
        
        throw CancellationException("$operation cancelled")
    }
    
    private fun increase(current: Duration): Duration {
        val increased = current * FACTOR
        return if (increased > MAX_DELAY) MAX_DELAY else increased
    }
}
