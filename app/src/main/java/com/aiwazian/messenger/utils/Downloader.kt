package com.aiwazian.messenger.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

suspend fun saveFileToApplicationFolder(
    context: Context,
    responseBody: ResponseBody,
    fileName: String
) {
    withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val appDir = context.getExternalFilesDir(null) ?: return@withContext
            
            val file = File(
                appDir,
                fileName
            )
            
            FileOutputStream(file).use { output ->
                responseBody.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(
                            buffer,
                            0,
                            bytesRead
                        )
                    }
                    output.flush()
                }
            }
        } else {
            val appDir = context.getExternalFilesDir(null) ?: return@withContext
            if (!appDir.exists()) appDir.mkdirs()
            
            val file = File(
                appDir,
                fileName
            )
            
            FileOutputStream(file).use { output ->
                responseBody.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(
                            buffer,
                            0,
                            bytesRead
                        )
                    }
                    output.flush()
                }
            }
        }
    }
}

fun saveFileToDownloadsFolder(
    context: Context,
    sourceFileName: String,
    destinationFileName: String
): Boolean {
    try {
        context.filesDir.listFiles()?.forEach {
            Log.d(
                "Downloader",
                "Файл: ${it.absolutePath}"
            )
        }
        
        Log.d(
            "Downloader",
            sourceFileName
        )
        
        val sourceFile = File(
            context.getExternalFilesDir(null),
            sourceFileName
        )
        
        if (!sourceFile.exists()) {
            Log.e(
                "Downloader",
                "Исходный файл не найден: ${sourceFile.absolutePath}"
            )
            //            return false
        }
        
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        
        val destinationFile = File(
            downloadsDir,
            destinationFileName
        )
        
        sourceFile.copyTo(destinationFile)
        
        return true
    } catch (e: IOException) {
        Log.e(
            "Downloader",
            "Ошибка при копировании файла",
            e
        )
        return false
    } catch (e: SecurityException) {
        Log.e(
            "Downloader",
            "Нет прав на доступ к файлам",
            e
        )
        return false
    } catch (e: Exception) {
        Log.e(
            "Downloader",
            "Ошибка при копировании файла",
            e
        )
        return false
    }
}
