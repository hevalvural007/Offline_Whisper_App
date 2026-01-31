package com.hevalvural.whisper_task

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object Utils {

    fun getModelPath(context: Context, modelName: String): String {
        val file = File(context.filesDir, modelName)

        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }

        try {
            context.assets.open(modelName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return file.absolutePath
    }
}