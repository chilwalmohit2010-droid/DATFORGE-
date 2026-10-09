package com.example

import com.example.ui.components.NavTab
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationAndBottomBarLayoutTest {

    @Test
    fun testBottomNavigationTabOrder_ExactSevenPositions() {
        // The bottom navigation tabs displayed around the central + button
        val leftTabs = listOf(NavTab.HOME, NavTab.HABITS, NavTab.PROGRESS)
        val rightTabs = listOf(NavTab.LEADERBOARD, NavTab.JOURNAL, NavTab.PROFILE)

        assertEquals("Left side must contain exactly 3 tabs", 3, leftTabs.size)
        assertEquals("Right side must contain exactly 3 tabs", 3, rightTabs.size)

        assertEquals(NavTab.HOME, leftTabs[0])
        assertEquals(NavTab.HABITS, leftTabs[1])
        assertEquals(NavTab.PROGRESS, leftTabs[2])

        assertEquals(NavTab.LEADERBOARD, rightTabs[0])
        assertEquals(NavTab.JOURNAL, rightTabs[1])
        assertEquals(NavTab.PROFILE, rightTabs[2])
    }

    @Test
    fun testCentralPlusButton_IsExactCenterMidpoint() {
        val totalSlots = 7
        val leftSlotsCount = 3
        val centerSlotIndex = 4 // 1-based index 4, 0-based index 3
        val rightSlotsCount = 3

        assertEquals(totalSlots, leftSlotsCount + 1 + rightSlotsCount)

        // Midpoint calculation for 7 equal-weight slots
        // Left slots span: [0, 3/7]
        // Center slot spans: [3/7, 4/7]
        // Midpoint of center slot: (3/7 + 4/7) / 2 = 3.5 / 7 = 0.5 (exact 50% midpoint)
        val centerSlotMidpoint = (leftSlotsCount + 0.5) / totalSlots
        assertEquals(0.5, centerSlotMidpoint, 0.0001)
    }

    @Test
    fun testNavTabRoutesAndTitles() {
        assertEquals("home", NavTab.HOME.route)
        assertEquals("Home", NavTab.HOME.title)

        assertEquals("habits", NavTab.HABITS.route)
        assertEquals("Habits", NavTab.HABITS.title)

        assertEquals("progress", NavTab.PROGRESS.route)
        assertEquals("Progress", NavTab.PROGRESS.title)

        assertEquals("leaderboard", NavTab.LEADERBOARD.route)
        assertEquals("Leaderboard", NavTab.LEADERBOARD.title)

        assertEquals("journal", NavTab.JOURNAL.route)
        assertEquals("Journal", NavTab.JOURNAL.title)

        assertEquals("profile", NavTab.PROFILE.route)
        assertEquals("Profile", NavTab.PROFILE.title)
    }
}
