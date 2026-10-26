package com.example.carromassist.physics

import com.example.carromassist.model.*
import kotlin.math.*

class ShotPlanner {
    fun recommend(board: BoardState): List<ShotRecommendation> {
        val result = mutableListOf<ShotRecommendation>()
        for (coin in board.coins.filter { it.confidence >= 0.45 }) {
            val contact = CarromPhysics.contactPoint(board.striker.center, coin.center, board.striker.radius, coin.radius)
            if (CarromPhysics.segmentBlocked(board.striker.center, contact, board.coins.filterNot { it.id == coin.id })) continue
            val pocket = board.pockets.filter { CarromPhysics.pocketReachable(coin, it, board.coins) }
                .minByOrNull { coin.center.distance(it.center) }
            val dist = board.striker.center.distance(contact)
            val confidence = (coin.confidence * (if (pocket != null) 0.85 else 0.55) * (1.0 - (dist / (board.width + board.height)).coerceIn(0.0, 0.4))).coerceIn(0.0, 1.0)
            result += ShotRecommendation(
                angleRadians = CarromPhysics.angle(board.striker.center, contact),
                targetId = coin.id, contactPoint = contact,
                path = listOf(board.striker.center, contact, coin.center),
                pocket = pocket, confidence = confidence,
                summary = if (pocket != null) "Possible pocket path" else "Contact shot; pocket path uncertain"
            )
        }
        return result.sortedByDescending { it.confidence }.take(5)
    }
}
