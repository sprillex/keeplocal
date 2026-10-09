package com.randolph.keeplocal.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageDecoder {
    suspend fun loadDownsampledBitmap(
        context: Context,
        uriString: String,
        reqWidth: Int,
        reqHeight: Int
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return@withContext null
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            bitmap?.asImageBitmap()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    suspend fun saveImageToInternalStorage(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        if (uri.scheme == "file" && uri.path?.startsWith(context.filesDir.absolutePath) == true) {
            return@withContext uri.toString()
        }

        try {
            val imagesDir = File(context.filesDir, "note_images").apply {
                if (!exists()) mkdirs()
            }
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val destFile = File(imagesDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            if (destFile.exists() && destFile.length() > 0) {
                Uri.fromFile(destFile).toString()
            } else {
                uri.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            uri.toString()
        }
    }
}
