package com.example.anemiadetector.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility untuk menyimpan gambar ke MediaStore (Galeri Android)
 * Kompatibel dengan Android 10+ (Scoped Storage)
 */
object MediaStoreUtils {
    
    private const val TAG = "MediaStoreUtils"
    private const val RELATIVE_PATH = "Pictures/AnemiaDetector"
    private const val MIME_TYPE = "image/jpeg"
    private const val JPEG_QUALITY = 95
    
    /**
     * Simpan bitmap ke MediaStore dan return URI
     * 
     * @param context Application context
     * @param bitmap Bitmap yang akan disimpan
     * @param displayName Nama file (optional, akan di-generate jika null)
     * @return URI dari gambar yang tersimpan, atau null jika gagal
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        displayName: String? = null
    ): Uri? {
        return try {
            // Generate filename dengan timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = displayName ?: "anemia_${timestamp}.jpg"
            
            // Prepare content values
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE)
                
                // Android 10+ (API 29+) - Scoped Storage
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_PATH)
                    put(MediaStore.Images.Media.IS_PENDING, 1) // Mark as pending while writing
                }
            }
            
            // Insert to MediaStore
            val resolver = context.contentResolver
            val imageUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            
            if (imageUri == null) {
                Log.e(TAG, "Failed to create MediaStore entry")
                return null
            }
            
            // Write bitmap to output stream
            resolver.openOutputStream(imageUri)?.use { outputStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)) {
                    Log.e(TAG, "Failed to compress bitmap")
                    return null
                }
            } ?: run {
                Log.e(TAG, "Failed to open output stream")
                return null
            }
            
            // Mark as not pending (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            
            Log.d(TAG, "Image saved successfully: $imageUri")
            imageUri
            
        } catch (e: IOException) {
            Log.e(TAG, "IOException while saving image", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while saving image", e)
            null
        }
    }
    
    /**
     * Get file path dari URI (untuk disimpan di database)
     * 
     * @param uri URI dari MediaStore
     * @return String path atau URI string
     */
    fun getPathFromUri(uri: Uri): String {
        return uri.toString()
    }
}
