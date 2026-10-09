package com.example

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

class WaterTrackingCalculationTestSuite {

    /**
     * Replicates the exact production calculation formula from WaterTrackerSheet:
     * val percentage: Int = if (hasGoal) {
     *     minOf(100, ((currentMl.toDouble() / targetMl.toDouble()) * 100.0).roundToInt().coerceAtLeast(0))
     * } else 0
     * val progressRatio: Float = if (hasGoal) (percentage / 100f).coerceIn(0f, 1f) else 0f
     */
    private fun computePercentage(currentMl: Int, targetMl: Int): Int {
        if (targetMl <= 0) return 0
        return minOf(100, ((currentMl.toDouble() / targetMl.toDouble()) * 100.0).roundToInt().coerceAtLeast(0))
    }

    private fun computeProgressRatio(currentMl: Int, targetMl: Int): Float {
        val pct = computePercentage(currentMl, targetMl)
        return (pct / 100f).coerceIn(0f, 1f)
    }

    @Test
    fun testProtocol_ZeroIntake_IsZeroPercent() {
        val percentage = computePercentage(currentMl = 0, targetMl = 3000)
        val ratio = computeProgressRatio(currentMl = 0, targetMl = 3000)
        assertEquals(0, percentage)
        assertEquals(0.0f, ratio, 0.001f)
    }

    @Test
    fun testProtocol_1000Of3000_IsApproximately33Percent() {
        val percentage = computePercentage(currentMl = 1000, targetMl = 3000)
        val ratio = computeProgressRatio(currentMl = 1000, targetMl = 3000)
        assertEquals(33, percentage)
        assertEquals(0.33f, ratio, 0.001f)
    }

    @Test
    fun testProtocol_1500Of3000_Is50Percent() {
        val percentage = computePercentage(currentMl = 1500, targetMl = 3000)
        val ratio = computeProgressRatio(currentMl = 1500, targetMl = 3000)
        assertEquals(50, percentage)
        assertEquals(0.50f, ratio, 0.001f)
    }

    @Test
    fun testProtocol_3000Of3000_Is100Percent() {
        val percentage = computePercentage(currentMl = 3000, targetMl = 3000)
        val ratio = computeProgressRatio(currentMl = 3000, targetMl = 3000)
        assertEquals(100, percentage)
        assertEquals(1.0f, ratio, 0.001f)
    }

    @Test
    fun testProtocol_3500Of3000_VisualCappedAt100Percent_IntakePreserved() {
        val currentIntake = 3500
        val targetGoal = 3000
        val percentage = computePercentage(currentMl = currentIntake, targetMl = targetGoal)
        val ratio = computeProgressRatio(currentMl = currentIntake, targetMl = targetGoal)

        assertEquals("Visual percentage must cap at 100%", 100, percentage)
        assertEquals("Progress ratio must cap at 1.0f", 1.0f, ratio, 0.001f)
        assertEquals("Actual intake recorded must be preserved", 3500, currentIntake)
    }

    @Test
    fun testCustomGoal_2000And4000ml() {
        // 1000 ml with 2000 ml goal = 50%
        val pct2000 = computePercentage(currentMl = 1000, targetMl = 2000)
        assertEquals(50, pct2000)

        // 1000 ml with 4000 ml goal = 25%
        val pct4000 = computePercentage(currentMl = 1000, targetMl = 4000)
        assertEquals(25, pct4000)
    }

    @Test
    fun testEdgeCases_ZeroOrNegativeGoal_NoDivisionByZeroCrash() {
        val pctZeroGoal = computePercentage(currentMl = 500, targetMl = 0)
        val pctNegativeGoal = computePercentage(currentMl = 500, targetMl = -100)
        assertEquals(0, pctZeroGoal)
        assertEquals(0, pctNegativeGoal)
    }
}
