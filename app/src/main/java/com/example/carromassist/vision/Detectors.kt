package com.example.carromassist.vision

import com.example.carromassist.model.Vec2

/** Pluggable detector contracts. The default image heuristics are deliberately conservative. */
fun interface BoardDetector { fun detect(frame: RgbFrame): Quad? }
fun interface PocketDetector { fun detect(frame: RgbFrame, board: Quad): List<Detection<CircleDetection>> }
fun interface StrikerDetector { fun detect(frame: RgbFrame, board: Quad): Detection<CircleDetection>? }
fun interface CoinDetector { fun detect(frame: RgbFrame, board: Quad): List<Detection<CircleDetection>> }

/** Baseline detector only identifies a plausible bright, axis-aligned board region. */
class ContrastBoardDetector(private val minimumBrightFraction: Double = 0.25) : BoardDetector {
    init { require(minimumBrightFraction in 0.0..1.0) }
    override fun detect(frame: RgbFrame): Quad? {
        if (frame.width < 16 || frame.height < 16) return null
        var bright = 0L
        for (pixel in frame.argb) {
            val r = pixel shr 16 and 255; val g = pixel shr 8 and 255; val b = pixel and 255
            if (r > 150 && g > 100 && b > 60 && r > b * 1.2) bright++
        }
        if (bright.toDouble() / frame.argb.size < minimumBrightFraction) return null
        val insetX = frame.width * 0.05; val insetY = frame.height * 0.05
        return Quad(Vec2(insetX, insetY), Vec2(frame.width - insetX, insetY),
            Vec2(frame.width - insetX, frame.height - insetY), Vec2(insetX, frame.height - insetY))
    }
}
