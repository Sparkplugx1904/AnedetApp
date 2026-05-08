package com.example.anemiadetector.utils

import android.content.Context
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

fun loadModelBuffer(context: Context, path: String): ByteBuffer {
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
}
