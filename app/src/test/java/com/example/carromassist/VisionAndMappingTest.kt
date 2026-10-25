package com.example.carromassist

import com.example.carromassist.model.Vec2
import com.example.carromassist.vision.*
import org.junit.Assert.*
import org.junit.Test

class VisionAndMappingTest {
    @Test fun downsamplePreservesTimestampAndBounds() {
        val frame = RgbFrame(8, 4, IntArray(32) { it }, 123L)
        val small = FramePreprocessor.downsample(frame, 4)
        assertEquals(4, small.width); assertEquals(2, small.height); assertEquals(123L, small.timestampNanos)
    }
    @Test fun invalidQuadRejected() {
        assertFalse(BoardCalibrator.validateCorners(Quad(Vec2(0.0,0.0), Vec2(0.0,0.0), Vec2(0.0,0.0), Vec2(0.0,0.0))))
        assertTrue(BoardCalibrator.validateCorners(Quad(Vec2(0.0,0.0), Vec2(100.0,0.0), Vec2(100.0,100.0), Vec2(0.0,100.0))))
    }
    @Test fun normalizeBoardPoint() {
        val q = Quad(Vec2(10.0,20.0), Vec2(110.0,20.0), Vec2(110.0,220.0), Vec2(10.0,220.0))
        assertEquals(Vec2(0.5,0.5), BoardCalibrator.normalize(Vec2(60.0,120.0), q))
        assertNull(BoardCalibrator.normalize(Vec2(300.0,120.0), q))
    }
    @Test fun coordinateMappingAccountsForLetterboxAndInsets() {
        val m = CoordinateMapper(100.0,100.0, RectD(0.0,0.0,200.0,100.0), RectD(10.0,20.0,400.0,200.0), RectD(5.0,7.0,800.0,400.0))
        val p = m.boardToFrame(Vec2(50.0,50.0))
        assertEquals(100.0, p.x, 1e-6); assertEquals(50.0, p.y, 1e-6)
        val overlay = m.boardToOverlay(Vec2(50.0,50.0))
        assertEquals(405.0, overlay.x, 1e-6); assertEquals(207.0, overlay.y, 1e-6)
    }
    @Test fun rotationMappingHandlesQuarterTurns() {
        val m = CoordinateMapper(100.0,100.0,RectD(0.0,0.0,100.0,100.0),RectD(0.0,0.0,100.0,100.0),RectD(0.0,0.0,100.0,100.0), rotationDegrees = 90)
        assertEquals(Vec2(50.0,20.0), m.rotateFramePoint(Vec2(20.0,30.0), 100.0, 80.0))
    }
    @Test fun trackerSmoothsAndExpires() {
        val tracker = DetectionTracker(alpha = 0.5, maxAgeNanos = 100)
        tracker.update("coin", Detection(Vec2(0.0,0.0), 0.9, 10))
        val result = tracker.update("coin", Detection(Vec2(10.0,0.0), 0.9, 20))
        assertEquals(Vec2(5.0,0.0), result?.value)
        assertNull(tracker.current("coin", 200))
    }
    @Test fun confidenceRejectsNan() { assertFalse(ConfidenceEstimator.accept(Double.NaN)); assertTrue(ConfidenceEstimator.accept(0.8)) }
    @Test fun emptyFrameFailsSafely() {
        val frame = RgbFrame(4,4,IntArray(16),0)
        assertNull(ContrastBoardDetector().detect(frame))
    }
}
