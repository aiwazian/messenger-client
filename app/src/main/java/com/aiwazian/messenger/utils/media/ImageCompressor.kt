/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.aiwazian.messenger.extensions.getFileName
import com.aiwazian.messenger.extensions.getFileType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Пересборка фотографии в JPEG перед отправкой.
 *
 * Отправлять снимок как есть дорого дважды: восемь мегапикселей едут по сети
 * целиком, а вместе с ними уезжает и всё, что камера записала в EXIF: время
 * съёмки, модель телефона, координаты. Поэтому кадр не правится, а
 * собирается заново: декодируется, уменьшается и кодируется в новый файл. У
 * нового файла метаданных нет вообще — переносить их просто нечем.
 *
 * Поворот из EXIF при этом впекается в пиксели. Без этого шага портретные
 * снимки легли бы на бок: тег с поворотом уехал бы вместе с остальными
 * метаданными, а прямо такие кадры выглядели только благодаря ему.
 *
 * Туда же впекаются правки из предпросмотра — поворот и отражение: отдавать
 * их тегом нельзя по той же причине, по которой снимается EXIF.
 *
 * Формат один — JPEG: он есть везде, и одно расширение вместо шести избавляет
 * и сервер, и чат от догадок. Прозрачность JPEG не умеет, поэтому альфа
 * заливается фоном из [MediaCompressionConfig.TRANSPARENCY_BACKGROUND_COLOR],
 * а не уходит чёрным.
 *
 * Стикеры — единственное исключение из обоих правил, см. [compressSticker]: они
 * собираются в WebP и сохраняют прозрачность — без неё стикер ездил бы по
 * чату белым прямоугольником.
 *
 * Пределы задаются в [MediaCompressionConfig]. Здесь их нет: и вложения, и
 * аватарки просят свой размер сами.
 *
 * Видео этот класс не трогает — [isCompressible] пропускает только картинки.
 */
@Singleton
class ImageCompressor @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * Готовый файл. С исходником он больше никак не связан: ни именем, ни
     * размером, ни форматом, поэтому отдаётся всё сразу — тому, кто заводит
     * запись о вложении, спрашивать второй раз нечего.
     */
    data class CompressedImage(
        val uri: Uri,
        val name: String,
        val size: Long,
        val mimeType: String = MIME_TYPE_JPEG
    )

    /**
     * Стоит ли пересобирать файл с таким mime-типом.
     *
     * Видео, звук и документы отсеиваются здесь же: сжатие для них не
     * реализовано, и трогать их нельзя.
     */
    fun isCompressible(mimeType: String): Boolean {
        if (!mimeType.startsWith(IMAGE_MIME_PREFIX, ignoreCase = true)) {
            return false
        }

        return mimeType.lowercase() !in MediaCompressionConfig.KEEP_AS_IS_IMAGE_MIME_TYPES
    }

    /**
     * Собирает уменьшенную копию без метаданных в [directory].
     *
     * @param maxDimension предел по самой длинной стороне. Меньший кадр
     * остаётся как есть: растягивать 100 на 100 до предела значит раздуть файл
     * впустую и размыть картинку.
     * @param name имя, от которого берётся основа: расширение всегда меняется
     * на jpg. Если имени нет, берётся из ссылки, а в крайнем случае — подставляется
     * своё.
     * @param transform поворот и отражение, выбранные в предпросмотре. Кладутся в ту
     * же матрицу, что и поворот из EXIF, и ничего не стоят сверх самого сжатия.
     * @return готовый файл либо null, если пересобрать не удалось. Null здесь —
     * не поломка отправки: вызывающая сторона уходит с исходником.
     */
    suspend fun compress(
        source: Uri,
        directory: File,
        maxDimension: Int,
        quality: Int,
        name: String? = null,
        transform: MediaTransform = MediaTransform.None
    ): CompressedImage? = withContext(Dispatchers.IO) {
        val fileName = renamed(name ?: source.getFileName(context), JPEG_EXTENSION)
        val target = File(directory, fileName)

        // Готовый файл уже лежит: отправку подняли заново после перезапуска, и
        // второе сжатие только срезало бы качество ещё раз.
        if (target.exists() && target.length() > 0) {
            return@withContext CompressedImage(
                uri = Uri.fromFile(target),
                name = fileName,
                size = target.length()
            )
        }

        try {
            val bounds = readBounds(source) ?: return@withContext null
            val decoded = decode(source, bounds, maxDimension) ?: return@withContext null

            val prepared = try {
                prepare(decoded, readOrientation(source), maxDimension, transform)
            } catch (e: Exception) {
                decoded.recycle()
                throw e
            }

            try {
                write(prepared, target, quality)
            } finally {
                if (prepared !== decoded) {
                    prepared.recycle()
                }

                decoded.recycle()
            }

            val size = target.length()

            if (size <= 0) {
                target.delete()
                return@withContext null
            }

            Log.i(TAG, "Compressed $fileName into $size bytes")

            CompressedImage(uri = Uri.fromFile(target), name = fileName, size = size)
        } catch (e: OutOfMemoryError) {
            // Кадр не помещается в память даже прореженным. Ронять из-за этого
            // отправку незачем: уйдёт исходник.
            Log.e(TAG, "Not enough memory to compress $source", e)
            target.delete()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unable to compress $source", e)
            target.delete()
            null
        }
    }

    /**
     * Аватарка профиля, группы или канала: та же пересборка, но до
     * [MediaCompressionConfig.AVATAR_MAX_DIMENSION].
     *
     * Файл кладётся в кэш: он нужен ровно на время загрузки, и переживать
     * перезапуск ему незачем — аватарку ждут на экране, а не досылают потом.
     *
     * Поворот здесь не передаётся: экран обрезки отдаёт уже повёрнутый
     * битмап — крутить его второй раз было бы неверно.
     *
     * @return ссылку на готовый файл либо null, если пересобрать не удалось
     * или картинку трогать нельзя.
     */
    suspend fun compressAvatar(source: Uri): Uri? = withContext(Dispatchers.IO) {
        if (!isCompressible(source.getFileType(context))) {
            return@withContext null
        }

        val root = File(context.cacheDir, AVATAR_DIRECTORY_NAME)
        dropStale(root)

        // Своя папка на каждую загрузку: имя файла остаётся исходным, а
        // аватарка, которая ещё грузится, не перетирается следующей.
        val directory = File(root, System.currentTimeMillis().toString())

        compress(
            source = source,
            directory = directory,
            maxDimension = MediaCompressionConfig.AVATAR_MAX_DIMENSION,
            quality = MediaCompressionConfig.AVATAR_JPEG_QUALITY,
            name = source.getFileName(context) ?: DEFAULT_AVATAR_NAME
        )?.uri
    }

    /**
     * Стикер: кадр ложится в квадрат [MediaCompressionConfig.STICKER_SIZE] и
     * уходит в WebP с сохранённой прозрачностью.
     *
     * Отличий от [compress] три, и каждое принципиальное. Формат WebP, а не
     * JPEG: стикер без альфы превратился бы в плитку на фоне чата. Размер
     * всегда ровно 512 на 512, даже если картинка прямоугольная или мелкая:
     * один размер у всех стикеров значит, что набор в сетке не расползается.
     * И файл всегда собирается заново: готовый ответ из кэша здесь был бы
     * вредом — одну и ту же картинку могли обрезать или повернуть иначе.
     *
     * Файл ложится в кэш и живёт до отправки набора: стикеры копятся на
     * экране редактора и уходят на сервер одним действием, а не по одному
     * сразу после выбора.
     *
     * @param transform поворот и отражение из предпросмотра.
     * @return готовый файл либо null, если картинку не удалось разобрать.
     * Здесь null — уже отказ: стикер исходником не уйдёт, сервер принимает
     * только WebP.
     */
    suspend fun compressSticker(
        source: Uri,
        transform: MediaTransform = MediaTransform.None
    ): CompressedImage? = withContext(Dispatchers.IO) {
        val side = MediaCompressionConfig.STICKER_SIZE
        val root = File(context.cacheDir, STICKER_DIRECTORY_NAME)
        dropStale(root)

        // Своя папка на каждый стикер: в набор могут добавить два кадра из
        // одного снимка, и общее имя файла стёрло бы первый вторым.
        val directory = File(root, System.nanoTime().toString())
        val fileName = renamed(source.getFileName(context) ?: DEFAULT_STICKER_NAME, WEBP_EXTENSION)
        val target = File(directory, fileName)

        try {
            val bounds = readBounds(source) ?: return@withContext null
            val decoded = decode(source, bounds, side) ?: return@withContext null

            val prepared = try {
                square(decoded, readOrientation(source), transform, side)
            } catch (e: Exception) {
                decoded.recycle()
                throw e
            }

            try {
                write(
                    bitmap = prepared,
                    target = target,
                    quality = MediaCompressionConfig.STICKER_WEBP_QUALITY,
                    format = Bitmap.CompressFormat.WEBP_LOSSY
                )
            } finally {
                if (prepared !== decoded) {
                    prepared.recycle()
                }

                decoded.recycle()
            }

            val size = target.length()

            if (size <= 0) {
                target.delete()
                return@withContext null
            }

            Log.i(TAG, "Compressed sticker $fileName into $size bytes")

            CompressedImage(
                uri = Uri.fromFile(target),
                name = fileName,
                size = size,
                mimeType = MIME_TYPE_WEBP
            )
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Not enough memory to compress sticker $source", e)
            target.delete()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unable to compress sticker $source", e)
            target.delete()
            null
        }
    }

    /** Размеры исходника: по ним считается, во сколько раз прореживать декод. */
    private fun readBounds(source: Uri): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val stream = openStream(source) ?: return null

        stream.use { BitmapFactory.decodeStream(it, null, options) }

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null
        }

        return options.outWidth to options.outHeight
    }

    /**
     * Декодирует кадр сразу прореженным.
     *
     * Полный снимок в памяти не нужен и опасен: двадцать мегапикселей в
     * ARGB_8888 — это восемьдесят мегабайт, и до уменьшения дело может не
     * дойти.
     */
    private fun decode(source: Uri, bounds: Pair<Int, Int>, maxDimension: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.first, bounds.second, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val stream = openStream(source) ?: return null

        return stream.use { BitmapFactory.decodeStream(it, null, options) }
    }

    /**
     * Во сколько раз прореживать декод.
     *
     * Прореживание идёт степенями двойки и никогда не заходит за предел: после
     * него длинная сторона всё ещё не меньше нужной, а точный размер даёт
     * масштабирование в [prepare].
     */
    private fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        val longest = max(width, height)

        while (longest / (sample * 2) >= maxDimension) {
            sample *= 2
        }

        return sample
    }

    /**
     * Поворот, уменьшение и заливка фона — одним проходом.
     *
     * Поворот и отражение из EXIF, правки из предпросмотра и масштаб
     * складываются в одну матрицу: иначе на каждый шаг приходился бы свой
     * промежуточный кадр в памяти.
     *
     * Правки ложатся после EXIF: в предпросмотре крутили уже прямой кадр, а
     * не то, как он лежит в файле.
     */
    private fun prepare(
        source: Bitmap,
        orientation: Int,
        maxDimension: Int,
        transform: MediaTransform
    ): Bitmap {
        val longest = max(source.width, source.height)
        val scale = if (longest > maxDimension) maxDimension.toFloat() / longest else 1f
        val matrix = orientationMatrix(orientation)

        if (transform.isMirrored) {
            matrix.postScale(-1f, 1f)
        }

        matrix.postRotate(transform.rotationDegrees.toFloat())

        // Ни поворачивать, ни уменьшать, ни заливать фон не нужно: такому кадру
        // достаточно самой пересборки в JPEG, чтобы метаданные с него слетели.
        if (matrix.isIdentity && scale == 1f && !source.hasAlpha()) {
            return source
        }

        matrix.postScale(scale, scale)

        // Поворот уводит кадр в отрицательные координаты, а отражение — за
        // правый край. Куда именно, считает сама матрица: так и все восемь
        // положений EXIF, и любая правка из предпросмотра обрабатываются одним кодом.
        val frame = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        matrix.mapRect(frame)
        matrix.postTranslate(-frame.left, -frame.top)

        val width = frame.width().roundToInt().coerceAtLeast(1)
        val height = frame.height().roundToInt().coerceAtLeast(1)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // JPEG прозрачности не знает: без заливки альфа ушла бы чёрным.
        if (source.hasAlpha()) {
            canvas.drawColor(MediaCompressionConfig.TRANSPARENCY_BACKGROUND_COLOR)
        }

        canvas.drawBitmap(
            source,
            matrix,
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        )

        return result
    }

    /**
     * Кладёт кадр в середину квадрата [side] на [side].
     *
     * Квадрат получается всегда, даже если картинка вытянутая или мелкая:
     * лишнее место остаётся прозрачным. Заливать его нечем и незачем —
     * стикер должен ложиться на любой фон чата.
     *
     * Мелкий кадр не растягивается: картинка 200 на 200 такой и останется
     * внутри квадрата, иначе её только размыло бы вдвое и утяжелило файл.
     */
    private fun square(
        source: Bitmap,
        orientation: Int,
        transform: MediaTransform,
        side: Int
    ): Bitmap {
        val matrix = orientationMatrix(orientation)

        if (transform.isMirrored) {
            matrix.postScale(-1f, 1f)
        }

        matrix.postRotate(transform.rotationDegrees.toFloat())

        val bounds = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        val turned = RectF(bounds)
        matrix.mapRect(turned)

        val longest = max(turned.width(), turned.height())
        val scale = if (longest > side) side / longest else 1f
        matrix.postScale(scale, scale)

        // После поворота и масштаба кадр лежит где угодно, включая
        // отрицательные координаты, поэтому сначала спрашиваем у матрицы, где
        // он оказался, и уже потом сдвигаем его в середину.
        val placed = RectF(bounds)
        matrix.mapRect(placed)

        matrix.postTranslate(
            (side - placed.width()) / 2f - placed.left,
            (side - placed.height()) / 2f - placed.top
        )

        val result = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)

        Canvas(result).drawBitmap(
            source,
            matrix,
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        )

        return result
    }

    /**
     * Поворот и отражение, записанные камерой в EXIF.
     *
     * Читать их нужно до пересборки: в новом файле тега не будет, и кадр
     * обязан быть повёрнут уже пикселями.
     */
    private fun orientationMatrix(orientation: Int): Matrix {
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        return matrix
    }

    /**
     * @return поворот из EXIF либо [ExifInterface.ORIENTATION_NORMAL], если
     * тега нет или файл его не поддерживает. Нечитаемый EXIF — не повод
     * отказываться от сжатия: большинство снимков и так лежат прямо.
     */
    private fun readOrientation(source: Uri): Int {
        return try {
            val stream = openStream(source) ?: return ExifInterface.ORIENTATION_NORMAL

            stream.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read orientation of $source: ${e.message}")
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    /**
     * Кладёт готовый кадр на диск.
     *
     * Пишется он рядом и переносится на место одним движением: оборванная
     * запись не должна остаться под правильным именем — её потом приняли бы за
     * удачное сжатие и отправили обрезанной.
     *
     * @param format кодек. JPEG для вложений и аватарок, WebP — для стикеров,
     * которым нужна прозрачность.
     */
    private fun write(
        bitmap: Bitmap,
        target: File,
        quality: Int,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ) {
        target.parentFile?.mkdirs()

        val partial = File(target.parentFile, "${target.name}$PARTIAL_SUFFIX")

        try {
            partial.outputStream().use { output ->
                if (!bitmap.compress(format, quality, output)) {
                    throw IOException("${format.name} encoder refused ${target.name}")
                }
            }

            if (!partial.renameTo(target)) {
                throw IOException("Unable to move ${partial.name} to ${target.name}")
            }
        } finally {
            partial.delete()
        }
    }

    /**
     * ContentResolver не умеет запрашивать file://-ссылки, а именно такие идут
     * от кэша системного «Поделиться» и от своих же копий.
     */
    private fun openStream(source: Uri): InputStream? {
        return try {
            if (source.scheme == SCHEME_FILE) {
                source.path?.let { FileInputStream(it) }
            } else {
                context.contentResolver.openInputStream(source)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to open $source: ${e.message}")
            null
        }
    }

    /** Основа имени остаётся исходной, расширение становится [extension]. */
    private fun renamed(name: String?, extension: String): String {
        val safe = name?.replace('/', '_')?.trim().orEmpty()
        val base = safe.substringBeforeLast('.', safe).ifBlank { DEFAULT_NAME }

        return "$base.$extension"
    }

    /**
     * Выбрасывает аватарки и стикеры, которые уже никто не грузит.
     *
     * Кэш чистит и система, но её очередь может не дойти, а мусор здесь
     * накапливается с каждой сменой аватарки и каждым брошенным набором.
     */
    private fun dropStale(root: File) {
        val threshold = System.currentTimeMillis() - PENDING_MAX_AGE_MS

        root.listFiles()?.forEach { entry ->
            if (entry.lastModified() < threshold) {
                entry.deleteRecursively()
            }
        }
    }

    companion object {
        const val MIME_TYPE_JPEG = "image/jpeg"
        const val JPEG_EXTENSION = "jpg"
        const val MIME_TYPE_WEBP = "image/webp"
        const val WEBP_EXTENSION = "webp"

        private const val TAG = "ImageCompressor"
        private const val IMAGE_MIME_PREFIX = "image/"
        private const val SCHEME_FILE = "file"
        private const val PARTIAL_SUFFIX = ".part"
        private const val DEFAULT_NAME = "image"
        private const val DEFAULT_AVATAR_NAME = "avatar"
        private const val DEFAULT_STICKER_NAME = "sticker"
        private const val AVATAR_DIRECTORY_NAME = "avatar_uploads"
        private const val STICKER_DIRECTORY_NAME = "sticker_uploads"

        /** Сколько живёт сжатый кадр, который так и не догрузили. */
        private const val PENDING_MAX_AGE_MS = 60L * 60 * 1000
    }
}
