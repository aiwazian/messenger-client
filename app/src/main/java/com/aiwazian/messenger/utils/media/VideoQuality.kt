/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import kotlin.math.roundToInt

/**
 * Ступени, до которых можно сжать видео перед отправкой.
 *
 * Цифра — короткая сторона кадра, а не длинная: у портретного видео 720p значит
 * 720 по ширине, у горизонтального — по высоте. Пользователь читает эти цифры
 * именно так, и так же их понимает Presentation из media3.
 *
 * Битрейт задан на ступень, а не считается от исходного файла. У видео с камеры
 * он завышен в разы, и унаследовать его значило бы уменьшить кадр, оставив вес
 * почти прежним.
 */
enum class VideoQuality(
    /** Короткая сторона кадра после сжатия, в пикселях. */
    val shortSide: Int,
    /** Битрейт видеодорожки после сжатия, бит/с. */
    val videoBitrate: Int
) {
    P360(360, 800_000),
    P480(480, 1_200_000),
    P720(720, 2_500_000),
    P1080(1080, 4_500_000);
    
    /** Подпись над делением слайдера: «720p». */
    val label: String
        get() = "${shortSide}p"
    
    companion object {
        /** Ступень, с которой уходит видео, если пользователь её не выбирал. */
        val DEFAULT = P720
        
        /**
         * Ступени, которые есть смысл предложить для видео с короткой стороной
         * [shortSide].
         *
         * Растягивать нечего, поэтому ступени выше исходника выброшены: у
         * 1280 на 720 остаётся три деления, у 854 на 480 — два. Пустой список
         * значит, что видео мельче самой низкой ступени: выбирать не из чего, и
         * кнопку качества показывать незачем.
         *
         * Неизвестный размер — единственный случай, когда возвращаются все
         * ступени. Сжатие всё равно не растянет кадр, а промолчать здесь значило
         * бы отобрать выбор совсем.
         */
        fun availableFor(shortSide: Int): List<VideoQuality> {
            if (shortSide <= 0) {
                return entries
            }
            
            return entries.filter { it.shortSide <= shortSide }
        }
        
        /**
         * Ступень, на которой открывается слайдер, если пользователь ещё ничего
         * не сохранял.
         *
         * null значит, что сжимать по размеру нечего: видео мельче самой низкой
         * ступени и уйдёт в своём разрешении.
         */
        fun defaultFor(shortSide: Int): VideoQuality? {
            val available = availableFor(shortSide)
            
            return available.lastOrNull { it.shortSide <= DEFAULT.shortSide }
                ?: available.firstOrNull()
        }
    }
}

/** Стороны кадра после сжатия. */
data class VideoFrame(
    val width: Int,
    val height: Int
)

/**
 * Кадр, который получится из [width] на [height] на этой ступени.
 *
 * Нужен предпросмотру: подпись сверху показывает не саму ступень, а настоящие
 * стороны будущего файла, и меняется вместе со слайдером.
 */
fun VideoQuality.frameFor(width: Int, height: Int): VideoFrame {
    if (width <= 0 || height <= 0) {
        return VideoFrame(width, height)
    }
    
    val sourceShortSide = minOf(width, height)
    
    // Кадр мельче ступени уходит своими сторонами: растягивать его незачем.
    if (sourceShortSide <= shortSide) {
        return VideoFrame(width, height)
    }
    
    val scale = shortSide.toDouble() / sourceShortSide
    
    return VideoFrame(
        width = toEvenSide((width * scale).roundToInt()),
        height = toEvenSide((height * scale).roundToInt())
    )
}

/**
 * Приблизительный вес видео после сжатия, в байтах.
 *
 * Считается по битрейту ступени и длительности — точнее без пробного прогона
 * кодека не выйдет, поэтому в интерфейсе это число идёт со знаком «~». Сверху
 * оно ограничено исходным весом: обещать файл тяжелее исходного нельзя, потому
 * что в такой ситуации сжатие просто отдаст оригинал.
 */
fun VideoQuality.estimateSizeBytes(durationMs: Long, sourceSizeBytes: Long): Long {
    if (durationMs <= 0) {
        return sourceSizeBytes
    }
    
    val bitrate = videoBitrate.toLong() + MediaCompressionConfig.VIDEO_AUDIO_BITRATE
    val estimated = bitrate * durationMs / (BITS_IN_BYTE * MILLIS_IN_SECOND)
    
    return if (sourceSizeBytes > 0) minOf(estimated, sourceSizeBytes) else estimated
}

/** H.264 не кодирует нечётные стороны кадра. */
private fun toEvenSide(value: Int): Int = value - value % 2

private const val BITS_IN_BYTE = 8
private const val MILLIS_IN_SECOND = 1000
