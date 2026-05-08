package com.example.anemiadetector.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Utility functions for polygon operations
 */
object PolygonUtils {

    /**
     * Compute polygon area using Shoelace formula (identical to cv2.contourArea())
     */
    fun computeArea(polygon: List<PointF>): Float {
        if (polygon.size < 3) return 0f
        
        var area = 0f
        val n = polygon.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += polygon[i].x * polygon[j].y
            area -= polygon[j].x * polygon[i].y
        }
        return abs(area) / 2f
    }

    /**
     * Find index of polygon with maximum area
     */
    fun argMaxArea(polygons: List<List<PointF>>): Int {
        if (polygons.isEmpty()) return -1
        
        val areas = polygons.map { computeArea(it) }
        return areas.indices.maxByOrNull { areas[it] } ?: -1
    }

    /**
     * Adaptive Epsilon Polygon Reduction
     * Reduces polygon to 6-15 points using Douglas-Peucker algorithm
     * IDENTICAL to get_precision_points() from Python
     */
    fun getAdaptivePolygon(
        contourPoints: List<PointF>,
        minPts: Int = 6,
        maxPts: Int = 15
    ): List<PointF> {
        if (contourPoints.size <= maxPts) return contourPoints
        
        val arcLength = computeArcLength(contourPoints)
        val epsilonFactors = (0 until 100).map { i ->
            0.10 - (0.10 - 0.005) * i / 99.0
        }
        
        var bestApprox = contourPoints
        
        for (factor in epsilonFactors) {
            val epsilon = factor * arcLength
            val approx = douglasPeucker(contourPoints, epsilon)
            
            if (approx.size in minPts..maxPts) {
                return approx
            }
            
            bestApprox = approx
        }
        
        return bestApprox
    }

    /**
     * Compute arc length (perimeter) of polygon
     */
    private fun computeArcLength(points: List<PointF>): Double {
        if (points.size < 2) return 0.0
        
        var length = 0.0
        for (i in 0 until points.size) {
            val j = (i + 1) % points.size
            val dx = (points[j].x - points[i].x).toDouble()
            val dy = (points[j].y - points[i].y).toDouble()
            length += sqrt(dx * dx + dy * dy)
        }
        return length
    }

    /**
     * Douglas-Peucker algorithm for polygon simplification
     */
    private fun douglasPeucker(points: List<PointF>, epsilon: Double): List<PointF> {
        if (points.size < 3) return points
        
        // Find point with maximum distance from line segment
        var maxDist = 0.0
        var maxIndex = 0
        val end = points.size - 1
        
        for (i in 1 until end) {
            val dist = perpendicularDistance(points[i], points[0], points[end])
            if (dist > maxDist) {
                maxDist = dist
                maxIndex = i
            }
        }
        
        // If max distance is greater than epsilon, recursively simplify
        return if (maxDist > epsilon) {
            val left = douglasPeucker(points.subList(0, maxIndex + 1), epsilon)
            val right = douglasPeucker(points.subList(maxIndex, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(points.first(), points.last())
        }
    }

    /**
     * Calculate perpendicular distance from point to line segment
     */
    private fun perpendicularDistance(point: PointF, lineStart: PointF, lineEnd: PointF): Double {
        val dx = (lineEnd.x - lineStart.x).toDouble()
        val dy = (lineEnd.y - lineStart.y).toDouble()
        
        if (dx == 0.0 && dy == 0.0) {
            return distance(point, lineStart)
        }
        
        val t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / (dx * dx + dy * dy)
        
        val projection = when {
            t < 0 -> lineStart
            t > 1 -> lineEnd
            else -> PointF(
                (lineStart.x + t * dx).toFloat(),
                (lineStart.y + t * dy).toFloat()
            )
        }
        
        return distance(point, projection)
    }

    /**
     * Euclidean distance between two points
     */
    private fun distance(p1: PointF, p2: PointF): Double {
        val dx = (p2.x - p1.x).toDouble()
        val dy = (p2.y - p1.y).toDouble()
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Fill polygon on bitmap with alpha
     * Used for generating masked bitmap for saving
     */
    fun fillPolygonAlpha(
        bitmap: Bitmap,
        polygon: List<PointF>,
        color: Int,
        alpha: Int = 77  // ~30% opacity
    ): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.alpha = alpha
            style = Paint.Style.FILL
        }
        
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.alpha = 255
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        
        val path = Path()
        polygon.forEachIndexed { i, pt ->
            if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        path.close()
        
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
        
        return result
    }

    /**
     * Get color based on classification result
     */
    fun getStatusColor(isAnemic: Boolean?): Int {
        return when (isAnemic) {
            true -> Color.parseColor("#FFFF3B30")  // Red - Anemia
            false -> Color.parseColor("#FF34C759") // Green - Non-Anemia
            null -> Color.parseColor("#FF007AFF")  // Blue - Segmentation only
        }
    }
}
