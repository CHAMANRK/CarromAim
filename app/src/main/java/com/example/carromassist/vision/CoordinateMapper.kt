package com.example.carromassist.vision

import com.example.carromassist.model.Vec2

/** Rectangular mapping between coordinate spaces. Values are pixels unless documented otherwise. */
data class RectD(val left: Double, val top: Double, val width: Double, val height: Double) {
    init { require(left.isFinite() && top.isFinite() && width.isFinite() && height.isFinite()); require(width > 0 && height > 0) }
    val right get() = left + width
    val bottom get() = top + height
}

/** Fit board coordinates into a frame rectangle while preserving aspect ratio (letterboxing). */
class CoordinateMapper(
    private val boardWidth: Double,
    private val boardHeight: Double,
    private val frameRect: RectD,
    private val displayRect: RectD,
    private val overlayRect: RectD,
    private val rotationDegrees: Int = 0
) {
    init {
        require(boardWidth > 0 && boardHeight > 0 && boardWidth.isFinite() && boardHeight.isFinite())
        require(rotationDegrees in listOf(0, 90, 180, 270))
    }

    fun boardToFrame(p: Vec2): Vec2 {
        val scale = minOf(frameRect.width / boardWidth, frameRect.height / boardHeight)
        val w = boardWidth * scale; val h = boardHeight * scale
        return Vec2(frameRect.left + (frameRect.width - w) / 2 + p.x * scale,
            frameRect.top + (frameRect.height - h) / 2 + p.y * scale)
    }

    fun frameToDisplay(p: Vec2): Vec2 = mapRect(p, frameRect, displayRect)
    fun displayToOverlay(p: Vec2): Vec2 = mapRect(p, displayRect, overlayRect)
    fun boardToOverlay(p: Vec2): Vec2 = displayToOverlay(frameToDisplay(boardToFrame(p)))

    /** Convert a point in the unrotated captured frame into the displayed orientation. */
    fun rotateFramePoint(p: Vec2, frameWidth: Double, frameHeight: Double): Vec2 {
        require(frameWidth > 0 && frameHeight > 0)
        return when (rotationDegrees) {
            0 -> p
            90 -> Vec2(frameHeight - p.y, p.x)
            180 -> Vec2(frameWidth - p.x, frameHeight - p.y)
            else -> Vec2(p.y, frameWidth - p.x)
        }
    }

    private fun mapRect(p: Vec2, from: RectD, to: RectD) = Vec2(
        to.left + ((p.x - from.left) / from.width) * to.width,
        to.top + ((p.y - from.top) / from.height) * to.height
    )
}
