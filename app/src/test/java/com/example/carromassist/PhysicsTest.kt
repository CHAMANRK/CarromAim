package com.example.carromassist

import com.example.carromassist.model.*
import com.example.carromassist.physics.CarromPhysics
import com.example.carromassist.physics.ShotPlanner
import com.example.carromassist.vision.SyntheticBoard
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class PhysicsTest {
    @Test fun contactPointStopsBeforeTarget() {
        val point = CarromPhysics.contactPoint(Vec2(0.0, 0.0), Vec2(100.0, 0.0), 10.0, 5.0)
        assertEquals(85.0, point.x, 1e-6)
    }
    @Test fun verticalReflectionIsSymmetric() {
        val reflected = CarromPhysics.reflectAcrossVertical(Vec2(20.0, 30.0), 100.0)
        assertEquals(180.0, reflected.x, 1e-6)
        assertEquals(30.0, reflected.y, 1e-6)
    }
    @Test fun pointSegmentDistanceWorks() {
        assertEquals(2.0, CarromPhysics.distancePointToSegment(Vec2(5.0, 2.0), Vec2(0.0,0.0), Vec2(10.0,0.0)), 1e-6)
    }
    @Test fun syntheticBoardProducesDeterministicRecommendations() {
        val board = SyntheticBoard.create()
        val planner = ShotPlanner()
        assertEquals(planner.recommend(board), planner.recommend(board))
    }
    @Test fun emptyCoinsProducesNoShots() {
        val b = BoardState(100.0,100.0,Coin("s",Vec2(50.0,80.0),4.0), emptyList(), emptyList())
        assertTrue(ShotPlanner().recommend(b).isEmpty())
    }
    @Test fun horizontalReflectionIsSymmetric() {
        val reflected = CarromPhysics.reflectAcrossHorizontal(Vec2(20.0, 30.0), 100.0)
        assertEquals(20.0, reflected.x, 1e-6)
        assertEquals(170.0, reflected.y, 1e-6)
    }
    @Test fun boundaryRayFindsFirstHit() {
        val hit = CarromPhysics.firstBoundaryHit(Vec2(50.0,50.0), Vec2(1.0,1.0), 100.0, 100.0)
        assertNotNull(hit); assertEquals(100.0, hit!!.x, 1e-6); assertEquals(100.0, hit.y, 1e-6)
    }
    @Test fun invalidBoardDimensionsRejected() {
        try { BoardState(0.0,100.0,Coin("s",Vec2(1.0,1.0),4.0), emptyList(), emptyList()); fail("Expected invalid dimensions") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun lowConfidenceCoinsAreIgnored() {
        val b = BoardState(100.0,100.0,Coin("s",Vec2(50.0,80.0),4.0), listOf(Coin("c",Vec2(50.0,30.0),4.0,0.1)), emptyList())
        assertTrue(ShotPlanner().recommend(b).isEmpty())
    }
}
