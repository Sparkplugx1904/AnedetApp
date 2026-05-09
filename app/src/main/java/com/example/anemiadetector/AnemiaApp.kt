package com.example.anemiadetector

import android.app.Application
import android.util.Log
import com.example.anemiadetector.utils.TFLiteHelper
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class AnemiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Force TensorFlow Lite to use bundled version only (disable GMS client)
        // This MUST be called before any TFLite initialization
        TFLiteHelper.disableGmsClient()
        
        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed")
        } else {
            Log.d("OpenCV", "OpenCV initialized successfully")
        }
    }
}
