package com.example.anemiadetector.ui.camera

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.anemiadetector.R
import com.example.anemiadetector.data.model.InferenceState
import com.example.anemiadetector.utils.PolygonUtils
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import androidx.camera.core.ImageProxy
import android.graphics.BitmapFactory
import android.util.Log

/**
 * Main camera screen with three modes:
 * 1. Live Segmentation (default)
 * 2. Single Capture
 * 3. Live Inference
 */
@Composable
fun CameraScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    Log.d("CameraScreen", "CameraScreen composing...")

    // Manual permission state (more reliable than Accompanist)
    var hasPermissions by remember { 
        mutableStateOf(com.example.anemiadetector.utils.PermissionUtils.hasAllPermissions(context))
    }
    
    // Permission launcher
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("CameraScreen", "Permission result: $permissions")
        hasPermissions = permissions.values.all { it }
        Log.d("CameraScreen", "hasPermissions updated to: $hasPermissions")
    }

    Log.d("CameraScreen", "hasPermissions: $hasPermissions")

    // State
    val inferenceState by viewModel.inferenceState.collectAsState()
    val torchEnabled by viewModel.torchEnabled.collectAsState()
    val liveInferenceEnabled by viewModel.liveInferenceEnabled.collectAsState()
    val resultBitmap by viewModel.resultBitmap.collectAsState()
    
    var showWarningDialog by remember { mutableStateOf(false) }
    var showResultSheet by remember { mutableStateOf(false) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Request permissions on first composition
    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            val permissions = com.example.anemiadetector.utils.PermissionUtils.REQUIRED_PERMISSIONS
            permissionLauncher.launch(permissions)
        }
    }

    // Re-check permissions when app resumes (user returns from Settings)
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    Log.d("CameraScreen", "App resumed, re-checking permissions")
                    val newPermissionState = com.example.anemiadetector.utils.PermissionUtils.hasAllPermissions(context)
                    Log.d("CameraScreen", "Manual check result: $newPermissionState")
                    hasPermissions = newPermissionState
                }
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    // Stop live inference when app goes to background
                    if (liveInferenceEnabled) {
                        Log.d("CameraScreen", "App stopped, pausing live inference")
                        viewModel.toggleLiveInference(false)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle inference state changes
    LaunchedEffect(inferenceState) {
        when (val state = inferenceState) {
            is InferenceState.Success -> {
                if (state.classificationResult != null && !liveInferenceEnabled) {
                    // Single capture mode - show result sheet
                    showResultSheet = true
                }
            }
            is InferenceState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Short
                )
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasPermissions) {
                // Camera preview
                key(cameraSelector) {
                    CameraPreview(
                        cameraSelector = cameraSelector,
                        onFrameAnalyzed = { bitmap ->
                            viewModel.processFrameForSegmentation(bitmap)
                        },
                        onCameraReady = { control, info ->
                            viewModel.setCameraControl(control, info)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Overlay canvas
                when (val state = inferenceState) {
                    is InferenceState.Success -> {
                        ConjunctivaOverlay(
                            detectionResult = state.detectionResult,
                            classificationResult = state.classificationResult,
                            frameWidth = CameraViewModel.FRAME_WIDTH,
                            frameHeight = CameraViewModel.FRAME_HEIGHT,
                            showClassificationOverlay = liveInferenceEnabled,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Label text overlay for live inference
                        if (liveInferenceEnabled && state.classificationResult != null) {
                            StatusChip(
                                classificationResult = state.classificationResult,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 80.dp)
                            )
                        }
                    }
                    is InferenceState.NoDetection -> {
                        GuideOverlay(
                            message = state.message,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Guide text
                        Text(
                            text = stringResource(R.string.no_conjunctiva),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                    is InferenceState.Processing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {}
                }

                // Bottom action bar
                BottomActionBar(
                    torchEnabled = torchEnabled,
                    liveInferenceEnabled = liveInferenceEnabled,
                    onTorchToggle = { viewModel.setTorch(!torchEnabled) },
                    onFlipCamera = {
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    },
                    onCapture = {
                        viewModel.captureAndClassify()
                    },
                    onLiveInferenceToggle = {
                        if (!liveInferenceEnabled) {
                            showWarningDialog = true
                        } else {
                            viewModel.toggleLiveInference(false)
                        }
                    },
                    onHistory = onNavigateToHistory,
                    onSettings = onNavigateToSettings,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                )

            } else {
                // Permission denied screen
                PermissionDeniedScreen(
                    onRequestPermission = {
                        val permissions = com.example.anemiadetector.utils.PermissionUtils.REQUIRED_PERMISSIONS
                        permissionLauncher.launch(permissions)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Warning dialog
    if (showWarningDialog) {
        LiveInferenceWarningDialog(
            onConfirm = {
                showWarningDialog = false
                viewModel.toggleLiveInference(true)
            },
            onDismiss = {
                showWarningDialog = false
            }
        )
    }

    // Result sheet
    if (showResultSheet) {
        val state = inferenceState as? InferenceState.Success
        state?.classificationResult?.let { classification ->
            CaptureResultSheet(
                resultBitmap = resultBitmap,
                classificationResult = classification,
                onSave = {
                    scope.launch {
                        val bitmap = resultBitmap
                        if (bitmap != null) {
                            val success = viewModel.saveExamination(context, bitmap, classification)
                            if (success) {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.saved_to_gallery)
                                )
                            } else {
                                snackbarHostState.showSnackbar(
                                    message = "Failed to save image"
                                )
                            }
                        } else {
                            snackbarHostState.showSnackbar(
                                message = "No image to save"
                            )
                        }
                        showResultSheet = false
                    }
                },
                onDismiss = {
                    showResultSheet = false
                    viewModel.resetState()
                }
            )
        }
    }
}

/**
 * Camera preview with ImageAnalysis
 */
@Composable
private fun CameraPreview(
    cameraSelector: CameraSelector,
    onFrameAnalyzed: (android.graphics.Bitmap) -> Unit,
    onCameraReady: (androidx.camera.core.CameraControl, androidx.camera.core.CameraInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            val bitmap = imageProxy.toBitmap()
                            onFrameAnalyzed(bitmap)
                            imageProxy.close()
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    
                    onCameraReady(camera.cameraControl, camera.cameraInfo)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

/**
 * Bottom action bar with camera controls
 */
@Composable
private fun BottomActionBar(
    torchEnabled: Boolean,
    liveInferenceEnabled: Boolean,
    onTorchToggle: () -> Unit,
    onFlipCamera: () -> Unit,
    onCapture: () -> Unit,
    onLiveInferenceToggle: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row - Settings button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.cd_settings_button),
                    tint = Color.White
                )
            }
        }
        
        // Main control row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Torch button
            IconButton(
                onClick = onTorchToggle,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (torchEnabled) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = stringResource(R.string.cd_torch_button),
                    tint = Color.White
                )
            }

            // Flip camera button
            IconButton(
                onClick = onFlipCamera,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = stringResource(R.string.cd_flip_camera_button),
                    tint = Color.White
                )
            }

            // Capture button (large)
            FloatingActionButton(
                onClick = onCapture,
                modifier = Modifier.size(72.dp),
                containerColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.cd_capture_button),
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Live inference toggle
            IconButton(
                onClick = onLiveInferenceToggle,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (liveInferenceEnabled) Color(0xFFFF3B30).copy(alpha = 0.8f) else Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = stringResource(R.string.cd_live_inference_button),
                    tint = Color.White
                )
            }

            // History button
            IconButton(
                onClick = onHistory,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = stringResource(R.string.cd_history_button),
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Status chip for live inference results
 */
@Composable
private fun StatusChip(
    classificationResult: com.example.anemiadetector.data.model.ClassificationResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (classificationResult.isAnemic) {
                Color(0xFFFF3B30)
            } else {
                Color(0xFF34C759)
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${classificationResult.label} ${(classificationResult.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

/**
 * Permission denied screen
 */
@Composable
private fun PermissionDeniedScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.permission_camera_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.permission_camera_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        // Try request permission first
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(stringResource(R.string.permission_request))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Open settings if permission was permanently denied
        OutlinedButton(
            onClick = {
                context.startActivity(
                    com.example.anemiadetector.utils.PermissionUtils.createAppSettingsIntent(context)
                )
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(stringResource(R.string.permission_open_settings))
        }
    }
}


/**
 * Extension function to convert ImageProxy to Bitmap
 * FIXED: Handle rowStride padding properly for real Android devices
 * 
 * CameraX RGBA_8888 format provides raw pixel bytes with potential row padding.
 * On real devices, planes[0].rowStride is often > width * 4 due to GPU alignment.
 * We must copy row-by-row, skipping padding bytes at the end of each row.
 */
private fun ImageProxy.toBitmap(): android.graphics.Bitmap {
    val plane = planes[0]
    val rowStride = plane.rowStride      // bytes per row including padding
    val pixelStride = plane.pixelStride  // bytes per pixel (4 for RGBA_8888)
    val buffer = plane.buffer
    
    // Fast path: no padding, direct copy
    if (rowStride == width * pixelStride) {
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)
        android.util.Log.d("ImageProxy", "Fast path: no padding, rowStride=$rowStride")
        return bitmap
    }
    
    // Slow path: has padding — copy row by row, skip padding
    android.util.Log.d("ImageProxy", "Slow path: rowStride=$rowStride, expected=${width * pixelStride}, padding=${rowStride - width * pixelStride} bytes/row")
    
    val cleanBuffer = java.nio.ByteBuffer.allocateDirect(width * height * pixelStride)
    buffer.rewind()
    
    for (row in 0 until height) {
        // Set position to start of this row (including offset from previous row padding)
        buffer.position(row * rowStride)
        // Copy only valid pixel bytes (without padding at end of row)
        val rowData = ByteArray(width * pixelStride)
        buffer.get(rowData)
        cleanBuffer.put(rowData)
    }
    cleanBuffer.rewind()
    
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(cleanBuffer)
    
    return bitmap
}


/**
 * Model not found error screen
 */
@Composable
private fun ModelNotFoundScreen(
    error: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.error_model_not_found),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please copy TFLite models to:\n" +
                    "• app/src/main/assets/models/segments/best_int8.tflite\n" +
                    "• app/src/main/assets/models/classify/best_float32.tflite\n\n" +
                    "See COPY_MODELS.md for instructions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error: $error",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}
