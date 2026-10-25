package com.example.carromassist.physics

import com.example.carromassist.model.*
import kotlin.math.*

object CarromPhysics {
    fun contactPoint(striker: Vec2, target: Vec2, strikerRadius: Double, targetRadius: Double): Vec2 {
        val direction = (target - striker).normalized()
        return target - direction * (strikerRadius + targetRadius)
    }
    /** Mirror a target across a wall to plan a single bank shot; result is a virtual target. */
    fun reflectAcrossVertical(point: Vec2, boardWidth: Double): Vec2 {
        require(boardWidth.isFinite() && boardWidth > 0 && point.x.isFinite() && point.y.isFinite())
        return Vec2(2.0 * boardWidth - point.x, point.y)
    }
    fun reflectAcrossHorizontal(point: Vec2, boardHeight: Double): Vec2 {
        require(boardHeight.isFinite() && boardHeight > 0 && point.x.isFinite() && point.y.isFinite())
        return Vec2(point.x, 2.0 * boardHeight - point.y)
    }

    fun reflectDirection(direction: Vec2, verticalWall: Boolean): Vec2 =
        if (verticalWall) Vec2(-direction.x, direction.y) else Vec2(direction.x, -direction.y)

    /** Returns the first wall intersection for a ray starting inside the board, or null. */
    fun firstBoundaryHit(origin: Vec2, direction: Vec2, width: Double, height: Double): Vec2? {
        require(width > 0 && height > 0 && width.isFinite() && height.isFinite())
        val d = direction.normalized()
        if (d.length() <= 1e-9 || origin.x !in 0.0..width || origin.y !in 0.0..height) return null
        val candidates = mutableListOf<Double>()
        if (d.x > 1e-9) candidates += (width - origin.x) / d.x
        if (d.x < -1e-9) candidates += (0.0 - origin.x) / d.x
        if (d.y > 1e-9) candidates += (height - origin.y) / d.y
        if (d.y < -1e-9) candidates += (0.0 - origin.y) / d.y
        val t = candidates.filter { it >= 0 && it.isFinite() }.minOrNull() ?: return null
        return origin + d * t
    }
    fun angle(from: Vec2, to: Vec2) = atan2(to.y - from.y, to.x - from.x)
    fun segmentBlocked(start: Vec2, end: Vec2, obstacles: List<Coin>, clearance: Double = 0.0): Boolean =
        obstacles.any { distancePointToSegment(it.center, start, end) < it.radius + clearance }
    fun distancePointToSegment(p: Vec2, a: Vec2, b: Vec2): Double {
        val ab = b - a
        val denom = ab.x * ab.x + ab.y * ab.y
        if (denom <= 1e-12) return p.distance(a)
        val t = (((p.x-a.x)*ab.x + (p.y-a.y)*ab.y) / denom).coerceIn(0.0, 1.0)
        return p.distance(a + ab * t)
    }
    fun pocketReachable(coin: Coin, pocket: Pocket, obstacles: List<Coin>): Boolean =
        !segmentBlocked(coin.center, pocket.center, obstacles.filterNot { it.id == coin.id }, 0.5)
}
