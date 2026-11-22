// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HolterWeekTest {
    private val zone = ZoneOffset.UTC
    private val start = LocalDate.of(2026, 11, 2)
    private val labels = (1..7).map { "День $it" }

    @Test
    fun dayAndWeekEtaExcludeObligationsAndIncome() {
        val weeks = holterWeeks(
            movements = listOf(
                move(0, "U1", 60_000_00),
                move(0, "U2", 20_000_00),
                move(0, "U3", 10_000_00),
                move(0, "U4", 110_000_00),
                move(0, "OBZ", 25_000_00),
                move(0, "INCOME", 200_000_00),
            ),
            mirrors = listOf(HolterMirror(at(0), 10_000_00)),
            zone = zone,
            today = start,
            dayLabels = labels,
            totalLabel = "Итого за неделю",
        )
        assertEquals(1, weeks.size)
        val day = weeks.single().days.first()
        assertEquals("День 1", day.label)
        assertEquals(200_000_00, day.incomeKopecks)
        assertEquals(25_000_00, day.obligationsKopecks)
        assertEquals(10_000_00, day.doubleTariffKopecks)
        assertEquals(0.45, day.etaRatio!!, 1e-9)
        assertNull(weeks.single().days[1].etaRatio)
        val total = weeks.single().total
        assertEquals("Итого за неделю", total.label)
        assertNull(total.date)
        assertEquals(0.45, total.etaRatio!!, 1e-9)
        assertEquals(25_000_00, total.obligationsKopecks)
    }

    @Test
    fun weekEtaUsesSumsRatherThanTheMeanOfDailyRatios() {
        val weeks = holterWeeks(
            movements = listOf(
                move(0, "U1", 10_000_00),
                move(0, "U4", 10_000_00),
                move(1, "U4", 10_000_00),
            ),
            mirrors = emptyList(),
            zone = zone,
            today = start.plusDays(1),
            dayLabels = labels,
            totalLabel = "Итого за неделю",
        )
        assertEquals(1, weeks.size)
        assertEquals(0.5, weeks.single().days[0].etaRatio!!, 1e-9)
        assertEquals(0.0, weeks.single().days[1].etaRatio!!, 1e-9)
        assertEquals(10_000_00.toDouble() / 30_000_00.toDouble(), weeks.single().total.etaRatio!!, 1e-9)
    }

    @Test
    fun eighthCalendarDayOpensTheNextWeek() {
        val weeks = holterWeeks(
            movements = listOf(move(0, "U1", 1_00), move(7, "U4", 4_00)),
            mirrors = emptyList(),
            zone = zone,
            today = start.plusDays(7),
            dayLabels = labels,
            totalLabel = "Итого за неделю",
        )
        assertEquals(2, weeks.size)
        assertEquals(start, weeks[0].days.first().date)
        assertEquals(start.plusDays(7), weeks[1].days.first().date)
        assertEquals(1_00, weeks[0].total.supportKopecks)
        assertEquals(0L, weeks[0].total.noiseKopecks)
        assertEquals(4_00, weeks[1].total.noiseKopecks)
        assertEquals(1.0, weeks[0].total.etaRatio!!, 1e-9)
        assertEquals(0.0, weeks[1].total.etaRatio!!, 1e-9)
    }

    private fun move(day: Long, node: String, amount: Long) =
        HolterMovement(at(day), node, amount)

    private fun at(day: Long): Long =
        start.plusDays(day).atTime(12, 0).toInstant(zone).toEpochMilli()
}
