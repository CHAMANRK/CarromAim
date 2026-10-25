package com.example.carromassist.vision

import com.example.carromassist.model.Vec2

/** Platform-independent frame representation; pixel storage is row-major ARGB. */
data class RgbFrame(val width: Int, val height: Int, val argb: IntArray, val timestampNanos: Long) {
    init { require(width > 0 && height > 0); require(width.toLong() * height <= Int.MAX_VALUE); require(argb.size == width * height) }
    fun pixel(x: Int, y: Int): Int { require(x in 0 until width && y in 0 until height); return argb[y * width + x] }
}
data class Quad(val topLeft: Vec2, val topRight: Vec2, val bottomRight: Vec2, val bottomLeft: Vec2) {
    fun isValid(minArea: Double = 1.0): Boolean {
        val points = listOf(topLeft, topRight, bottomRight, bottomLeft)
        if (points.any { !it.x.isFinite() || !it.y.isFinite() }) return false
        val area = points.indices.sumOf { i ->
            val a = points[i]; val b = points[(i + 1) % points.size]; a.x * b.y - b.x * a.y
        }.let { kotlin.math.abs(it) / 2.0 }
        return area >= minArea && topRight.x > topLeft.x && bottomRight.y > topRight.y && bottomLeft.x < bottomRight.x && bottomLeft.y > topLeft.y
    }
}
data class Detection<T>(val value: T, val confidence: Double, val timestampNanos: Long) {
    init { require(confidence in 0.0..1.0); require(timestampNanos >= 0) }
}
data class CircleDetection(val center: Vec2, val radius: Double) { init { require(radius > 0 && radius.isFinite()) } }

/** Conservative, deterministic color/contrast baseline. It does not claim general game compatibility. */
object FramePreprocessor {
    fun downsample(frame: RgbFrame, maxDimension: Int = 320): RgbFrame {
        require(maxDimension > 0)
        val scale = minOf(1.0, maxDimension.toDouble() / maxOf(frame.width, frame.height))
        val w = maxOf(1, (frame.width * scale).toInt()); val h = maxOf(1, (frame.height * scale).toInt())
        val pixels = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) pixels[y * w + x] = frame.pixel(
            ((x / scale).toInt()).coerceIn(0, frame.width - 1), ((y / scale).toInt()).coerceIn(0, frame.height - 1))
        return RgbFrame(w, h, pixels, frame.timestampNanos)
    }
}

object BoardCalibrator {
    fun validateCorners(corners: Quad?): Boolean = corners?.isValid() == true
    fun normalize(point: Vec2, bounds: Quad): Vec2? {
        if (!bounds.isValid()) return null
        val left = bounds.topLeft.x; val top = bounds.topLeft.y
        val width = bounds.topRight.x - left; val height = bounds.bottomLeft.y - top
        if (width <= 0 || height <= 0) return null
        return Vec2((point.x - left) / width, (point.y - top) / height).takeIf { it.x in 0.0..1.0 && it.y in 0.0..1.0 }
    }
}

/** Keeps short-term positions stable and rejects stale/low-confidence measurements. */
class DetectionTracker(private val alpha: Double = 0.35, private val maxAgeNanos: Long = 500_000_000L) {
    private val points = mutableMapOf<String, Detection<Vec2>>()
    init { require(alpha in 0.0..1.0 && maxAgeNanos > 0) }
    fun update(id: String, detection: Detection<Vec2>, minConfidence: Double = 0.5): Detection<Vec2>? {
        require(id.isNotBlank() && minConfidence in 0.0..1.0)
        if (detection.confidence < minConfidence) return current(id, detection.timestampNanos)
        val old = points[id]
        val smoothed = if (old == null || detection.timestampNanos <= old.timestampNanos || detection.timestampNanos - old.timestampNanos > maxAgeNanos) detection.value
            else old.value * (1.0 - alpha) + detection.value * alpha
        return Detection(smoothed, detection.confidence, detection.timestampNanos).also { points[id] = it }
    }
    fun current(id: String, nowNanos: Long): Detection<Vec2>? = points[id]?.takeIf { nowNanos >= it.timestampNanos && nowNanos - it.timestampNanos <= maxAgeNanos }
    fun clear() = points.clear()
}

object ConfidenceEstimator {
    fun accept(confidence: Double, minimum: Double = 0.55): Boolean = confidence.isFinite() && minimum in 0.0..1.0 && confidence in minimum..1.0
}
