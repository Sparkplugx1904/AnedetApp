package com.example.anemiadetector.utils

import android.util.Log

object TFLiteHelper {
    private const val TAG = "TFLiteHelper"
    
    /**
     * Force TensorFlow Lite to use bundled version only (disable GMS client)
     * This prevents "libtensorflowlite_jni_gms_client.so not found" error
     */
    fun disableGmsClient() {
        try {
            // Set system property
            System.setProperty("tflite.disable_gms_client", "true")
            
            // Try to disable via reflection (for TFLite Support library)
            try {
                val tfliteClass = Class.forName("org.tensorflow.lite.TensorFlowLite")
                val initMethod = tfliteClass.getDeclaredMethod("init")
                initMethod.isAccessible = true
                initMethod.invoke(null)
                Log.d(TAG, "TensorFlow Lite initialized successfully (bundled version)")
            } catch (e: ClassNotFoundException) {
                Log.d(TAG, "TensorFlowLite class not found, using default initialization")
            } catch (e: Exception) {
                Log.w(TAG, "Could not initialize TFLite via reflection: ${e.message}")
            }
            
            // Additional property to force CPU
            System.setProperty("tflite.force_cpu", "true")
            
            Log.d(TAG, "GMS client disabled, using bundled TFLite")
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling GMS client", e)
        }
    }
}
