package com.example.anemiadetector.utils

import android.content.Context
import android.util.Log
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

fun loadModelBuffer(context: Context, path: String): ByteBuffer {
    try {
        val fileDescriptor = context.assets.openFd(path)
        fileDescriptor.use { fd ->
            val input = fd.createInputStream()
            input.use {
                return it.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fd.startOffset,
                    fd.declaredLength
                )
            }
        }
    } catch (e: FileNotFoundException) {
        Log.e("ModelUtils", "Model file not found: $path")
        Log.e("ModelUtils", "Please copy TFLite models to assets folder!")
        Log.e("ModelUtils", "See COPY_MODELS.md for instructions")
        throw RuntimeException("Model file not found: $path. Please copy TFLite models to assets/models/ folder.", e)
    } catch (e: Exception) {
        Log.e("ModelUtils", "Error loading model: $path", e)
        throw RuntimeException("Error loading model: $path", e)
    }
}
