/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * Положение кадра, заданное пользователем: повороты и отражение.
 *
 * Раньше это умел только экран обрезки аватарки, и умел единственным
 * способом: сразу пересобирал битмап. Видео так не повернуть — битмапа у него
 * нет вовсе, а пересборка файла стоит десятки секунд и до нажатия «Готово»
 * никому не нужна. Поэтому положение описывается числами, а во что их
 * превратить — в матрицу для битмапа, в слой предпросмотра или в эффект
 * кодека — решает уже вызывающая сторона.
 *
 * Порядок действий закреплён: сначала отражение, потом поворот. Обратный
 * порядок дал бы другой кадр, поэтому [mirrored] разворачивает и накопленный
 * поворот — подробности там же.
 */
data class MediaTransform(
    /** По часовой стрелке, как rotationZ у слоя: 0, 90, 180 либо 270. */
    val rotationDegrees: Int = 0,
    /** Отражение по горизонтали. Вертикальное — это оно же с поворотом на 180. */
    val isMirrored: Boolean = false
) {
    /** Кадр не тронут: и сбрасывать нечего, и пересобирать файл незачем. */
    val isIdentity: Boolean
        get() = rotationDegrees == 0 && !isMirrored
    
    /** Стороны поменялись местами: из 4 на 3 вышло 3 на 4. */
    val swapsSides: Boolean
        get() = rotationDegrees % HALF_TURN != 0
    
    /** Отражение как масштаб: слой и кодек принимают его именно так. */
    val mirrorScaleX: Float
        get() = if (isMirrored) -1f else 1f
    
    /** Ещё четверть против часовой — в ту сторону, куда смотрит иконка кнопки. */
    fun rotated(): MediaTransform = copy(
        rotationDegrees = (rotationDegrees - QUARTER_TURN).mod(FULL_TURN)
    )
    
    /**
     * Отражение по горизонтали — по экрану, а не по самому кадру.
     *
     * Поворот и отражение местами не переставляются, поэтому у повёрнутого
     * кадра отражение заодно разворачивает накопленный поворот: отразить
     * повёрнутое — это повернуть отражённое в другую сторону. Без этого
     * «Отзеркалить» у лежащего на боку кадра давало бы отражение по вертикали,
     * то есть не то, что нажали.
     */
    fun mirrored(): MediaTransform = MediaTransform(
        rotationDegrees = (-rotationDegrees).mod(FULL_TURN),
        isMirrored = !isMirrored
    )
    
    /**
     * Матрица для битмапа: отражение, затем поворот.
     *
     * Куда уедет кадр, здесь не считается: и [Bitmap.createBitmap], и
     * сжатие фотографии сами приводят повёрнутую рамку к началу координат.
     */
    fun toMatrix(): Matrix = Matrix().apply {
        if (isMirrored) {
            postScale(-1f, 1f)
        }
        
        if (rotationDegrees != 0) {
            postRotate(rotationDegrees.toFloat())
        }
    }
    
    companion object {
        /** Кадр как он есть: с этого начинается любой предпросмотр. */
        val None = MediaTransform()
        
        /** Шаг кнопки поворота, в градусах. */
        const val QUARTER_TURN = 90
        
        private const val HALF_TURN = 180
        private const val FULL_TURN = 360
    }
}

/**
 * Пересобирает кадр по [transform].
 *
 * Исходник не освобождается: у аватарки он же и остаётся на экране, пока
 * повёрнутый не встанет на его место.
 */
fun Bitmap.transformed(transform: MediaTransform): Bitmap {
    if (transform.isIdentity) {
        return this
    }
    
    return Bitmap.createBitmap(this, 0, 0, width, height, transform.toMatrix(), true)
}

/** Четверть против часовой — ровно то, что отыгрывает кнопка поворота. */
fun Bitmap.rotatedQuarter(): Bitmap = transformed(MediaTransform.None.rotated())

/** Отражение по горизонтали — ровно то, что отыгрывает кнопка отражения. */
fun Bitmap.mirrored(): Bitmap = transformed(MediaTransform(isMirrored = true))
