package com.example.carromassist.physics

import com.example.carromassist.model.*
import kotlin.math.*

/** Finds only direct, unbanked pot shots. This is geometry guidance, not a guarantee. */
class ShotPlanner {
    fun recommend(board: BoardState): List<ShotRecommendation> {
        val result = mutableListOf<ShotRecommendation>()
        val confidentCoins = board.coins.filter { it.confidence >= 0.45 }

        for (coin in confidentCoins) {
            for (pocket in board.pockets) {
                // For a direct pot, the target coin must have a clear line to the pocket.
                if (!CarromPhysics.pocketReachable(coin, pocket, confidentCoins)) continue

                // The striker must hit the target from the side opposite the chosen pocket.
                val pocketDirection = (pocket.center - coin.center).normalized()
                if (pocketDirection.length() <= 1e-9) continue
                val contact = coin.center - pocketDirection * (coin.radius + board.striker.radius)

                if (contact.x < board.striker.radius || contact.x > board.width - board.striker.radius ||
                    contact.y < board.striker.radius || contact.y > board.height - board.striker.radius) continue

                val blockers = confidentCoins.filterNot { it.id == coin.id }
                if (CarromPhysics.segmentBlocked(board.striker.center, contact, blockers, clearance = 1.0)) continue

                val strikerDistance = board.striker.center.distance(contact)
                val coinDistance = coin.center.distance(pocket.center)
                val totalDistance = strikerDistance + coinDistance
                val boardScale = board.width + board.height
                val distanceScore = (1.0 - totalDistance / (boardScale * 0.95)).coerceIn(0.0, 1.0)
                val alignmentScore = (1.0 - abs(contact.x - board.striker.center.x) / board.width * 0.18).coerceIn(0.0, 1.0)
                val confidence = (coin.confidence * (0.72 + 0.18 * distanceScore + 0.10 * alignmentScore)).coerceIn(0.0, 1.0)

                result += ShotRecommendation(
                    angleRadians = CarromPhysics.angle(board.striker.center, contact),
                    targetId = coin.id,
                    contactPoint = contact,
                    path = listOf(board.striker.center, contact, coin.center),
                    pocket = pocket,
                    confidence = confidence,
                    summary = "Direct shot to pocket · no bank"
                )
            }
        }
        return result.distinctBy { it.targetId to it.pocket?.center }
            .sortedByDescending { it.confidence }
            .take(5)
    }
}
