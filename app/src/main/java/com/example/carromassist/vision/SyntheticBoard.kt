package com.example.carromassist.vision

import com.example.carromassist.model.*

object SyntheticBoard {
    fun create() = BoardState(
        width = 1000.0, height = 1000.0,
        striker = Coin("striker", Vec2(500.0, 820.0), 28.0),
        coins = listOf(
            Coin("red-1", Vec2(485.0, 520.0), 18.0, 0.98),
            Coin("white-1", Vec2(600.0, 420.0), 18.0, 0.96),
            Coin("black-1", Vec2(360.0, 380.0), 18.0, 0.95),
            Coin("white-2", Vec2(680.0, 650.0), 18.0, 0.91)
        ),
        pockets = listOf(
            Pocket(Vec2(35.0, 35.0), 48.0), Pocket(Vec2(965.0, 35.0), 48.0),
            Pocket(Vec2(35.0, 965.0), 48.0), Pocket(Vec2(965.0, 965.0), 48.0)
        )
    )
}
