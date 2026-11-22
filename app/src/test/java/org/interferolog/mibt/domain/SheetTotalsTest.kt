// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SheetTotalsTest {
    @Test
    fun portfolioAverageAndRedRowsMatchTheChapter() {
        val rows = listOf(
            SourceInput(15_000_00, 0, 4.0, 0.0, 5.0, 2.0, 2),
            SourceInput(135_000_00, 0, 56.0, 0.0, 45.0, 28.0, 4),
            SourceInput(150_000_00, 0, 140.0, 0.0, 50.0, 70.0, 9),
        )
        assertEquals(0.16, rows[0].index()!!, 1e-9)
        assertEquals(2.52, rows[2].index()!!, 1e-9)
        val total = sourceTotals(rows)
        assertEquals(300_000_00, total.netIncomeKopecks)
        assertEquals(100.0, total.incomeShare, 1e-9)
        assertEquals(200.0, total.hours, 1e-9)
        assertEquals(100.0, total.hoursShare, 1e-9)
        assertEquals(1_500_00.toDouble(), total.averageRateKopecks!!, 1e-6)
        assertEquals(1, total.rowsAtLeastTwo)
    }

    @Test
    fun compensatoryNoiseLowersThePortfolioRate() {
        val row = SourceInput(150_000_00, 15_000_00, 75.0, 0.0, 100.0, 100.0, 10)
        assertEquals(2.0, row.index()!!, 1e-9)
        val total = sourceTotals(listOf(row))
        assertEquals(1_800_00.toDouble(), total.averageRateKopecks!!, 0.01)
        assertEquals(1, total.rowsAtLeastTwo)
    }

    @Test
    fun emptyPortfolioHasNoAverageRate() {
        assertNull(sourceTotals(emptyList()).averageRateKopecks)
        assertEquals(0, sourceTotals(emptyList()).rowsAtLeastTwo)
    }

    @Test
    fun workTotalsKeepSharesAndTheControlRuleStillForcesFriction() {
        val heavy = WorkInput(resultShare = 40.0, hours = 20.0, hoursShare = 10.0, frictionScore = 2)
        val light = WorkInput(resultShare = 60.0, hours = 30.0, hoursShare = 90.0, frictionScore = 3)
        assertEquals(Quadrant.FRICTION, quadrant(heavy.yieldRatio, heavy.frictionScore, 2.0))
        val total = workTotals(listOf(heavy, light))
        assertEquals(100.0, total.resultShare, 1e-9)
        assertEquals(50.0, total.hours, 1e-9)
        assertEquals(100.0, total.hoursShare, 1e-9)
    }

    @Test
    fun decisionStaysSeparateFromTheComputedLabel() {
        assertEquals("Зелёная: оберегать", withDecision("Зелёная: оберегать", "  "))
        assertEquals("I. Ядро\nоставить", withDecision("I. Ядро", " оставить "))
    }
}