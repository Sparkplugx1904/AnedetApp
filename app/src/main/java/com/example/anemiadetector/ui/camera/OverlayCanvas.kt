package com.example.anemiadetector.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.anemiadetector.data.model.ClassificationResult
import com.example.anemiadetector.data.model.DetectionResult
import kotlin.math.roundToInt

/**
 * Overlay canvas for drawing conjunctiva polygon with alpha fill
 * 
 * Color coding:
 * - Blue (#007AFF) - Segmentation only (no classification)
 * - Red (#FF3B30) - Anemia detected
 * - Green (#34C759) - Non-Anemia (healthy)
 */
@Composable
fun ConjunctivaOverlay(
    detectionResult: DetectionResult?,
    classificationResult: ClassificationResult?,
    frameWidth: Int,
    frameHeight: Int,
    showClassificationOverlay: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        detectionResult ?: return@Canvas

        // Determine color based on classification result
        val color = when {
            classificationResult == null -> Color(0xFF007AFF)  // Blue - segmentation only
            classificationResult.isAnemic -> Color(0xFFFF3B30) // Red - Anemia
            else -> Color(0xFF34C759)                          // Green - Non-Anemia
        }
        
        val fillAlpha = if (classificationResult == null) 0.25f else 0.30f

        // Scale factor: frame coordinates → screen coordinates
        val scaleX = size.width / frameWidth
        val scaleY = size.height / frameHeight

        // Build polygon path
        val path = Path()
        detectionResult.polygon.forEachIndexed { index, point ->
            val sx = point.x * scaleX
            val sy = point.y * scaleY
            if (index == 0) {
                path.moveTo(sx, sy)
            } else {
                path.lineTo(sx, sy)
            }
        }
        path.close()

        // Draw fill with alpha
        drawPath(
            path = path,
            color = color.copy(alpha = fillAlpha),
            style = Fill
        )

        // Draw stroke (border)
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw vertex dots
        detectionResult.polygon.forEach { point ->
            drawCircle(
                color = color,
                radius = 4.dp.toPx(),
                center = Offset(point.x * scaleX, point.y * scaleY)
            )
        }

        // Draw label box (only when classification is active)
        if (showClassificationOverlay && classificationResult != null) {
            val bbox = detectionResult.boundingBox
            val bx1 = bbox.left * scaleX
            val by1 = bbox.top * scaleY
            
            val labelText = "${classificationResult.label} ${(classificationResult.confidence * 100).roundToInt()}%"
            
            // Draw background rectangle
            drawRoundRect(
                color = color,
                topLeft = Offset(bx1, by1 - 36.dp.toPx()),
                size = Size(180.dp.toPx(), 28.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            
            // Note: Text drawing in Canvas requires TextMeasurer which is complex
            // For simplicity, we'll use a separate Text composable overlay in CameraScreen
        }
    }
}

/**
 * Guide box overlay when no detection
 */
@Composable
fun GuideOverlay(
    message: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val boxWidth = 300.dp.toPx()
        val boxHeight = 200.dp.toPx()

        // Draw dashed rectangle
        val dashPath = Path()
        val left = centerX - boxWidth / 2
        val top = centerY - boxHeight / 2
        val right = centerX + boxWidth / 2
        val bottom = centerY + boxHeight / 2

        // Top line
        dashPath.moveTo(left, top)
        dashPath.lineTo(right, top)
        
        // Right line
        dashPath.moveTo(right, top)
        dashPath.lineTo(right, bottom)
        
        // Bottom line
        dashPath.moveTo(right, bottom)
        dashPath.lineTo(left, bottom)
        
        // Left line
        dashPath.moveTo(left, bottom)
        dashPath.lineTo(left, top)

        drawPath(
            path = dashPath,
            color = Color.White.copy(alpha = 0.7f),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(10f, 10f),
                    0f
                )
            )
        )
    }
}
