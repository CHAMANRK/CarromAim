package com.example.carromassist.model

data class Vec2(val x: Double, val y: Double) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scale: Double) = Vec2(x * scale, y * scale)
    fun length() = kotlin.math.hypot(x, y)
    fun normalized(): Vec2 { val l = length(); return if (l <= 1e-9) Vec2(0.0, 0.0) else Vec2(x/l, y/l) }
    fun distance(other: Vec2) = (this - other).length()
}
data class Coin(val id: String, val center: Vec2, val radius: Double, val confidence: Double = 1.0)
data class Pocket(val center: Vec2, val radius: Double)
data class BoardState(val width: Double, val height: Double, val striker: Coin, val coins: List<Coin>, val pockets: List<Pocket>) {
    init { require(width > 0 && height > 0); require(striker.radius > 0) }
}
data class ShotRecommendation(val angleRadians: Double, val targetId: String, val contactPoint: Vec2, val path: List<Vec2>, val pocket: Pocket?, val confidence: Double, val summary: String)
