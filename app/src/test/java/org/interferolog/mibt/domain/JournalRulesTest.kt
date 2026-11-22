// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.domain

import org.interferolog.mibt.content.JournalCanon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalRulesTest {
    @Test
    fun etaMatchesChapter() {
        val ratio = eta(60_000_00, 20_000_00, 10_000_00, 110_000_00)
        assertEquals(0.45, ratio!!, 1e-9)
        assertEquals(EtaBand.OVERHEATED, etaBand(ratio))
        assertEquals(EtaBand.WORKING, etaBand(0.80))
        assertEquals(EtaBand.PINCH, etaBand(0.91))
    }

    @Test
    fun obligationsStayOutsideEta() {
        val afterDebt = eta(60_000_00, 20_000_00, 10_000_00, 85_000_00)
        assertEquals(90_000_00.toDouble() / 175_000_00.toDouble(), afterDebt!!, 1e-9)
    }

    @Test
    fun hourlyRatesAndResistanceMatchPortfolio() {
        assertEquals(400_000.0, hourlyRate(100_000_00, 0, 25.0, 0.0)!!, 0.01)
        assertEquals(180_000.0, hourlyRate(150_000_00, 15_000_00, 75.0, 0.0)!!, 0.01)
        assertEquals(0.16, resistanceByShares(2, 2.0, 5.0)!!, 1e-9)
        assertEquals(0.497777, resistanceByShares(4, 28.0, 45.0)!!, 1e-4)
        assertEquals(2.52, resistanceByShares(9, 70.0, 50.0)!!, 1e-9)
        assertEquals(FiZone.GREEN, fiZone(0.16))
        assertEquals(FiZone.YELLOW, fiZone(1.4))
        assertEquals(FiZone.RED, fiZone(2.52))
        assertNull(resistanceByShares(3, 10.0, 0.0))
    }

    @Test
    fun highResistanceForcesFrictionQuadrant() {
        assertEquals(Quadrant.FRICTION, quadrant(yield = 2.0, friction = 2, index = 2.0))
        assertEquals(Quadrant.CORE, quadrant(yield = 1.2, friction = 3, index = 0.4))
        assertEquals(Quadrant.GROWTH_POINT, quadrant(yield = 1.2, friction = 7, index = 1.2))
        assertEquals(Quadrant.HYGIENE, quadrant(yield = 0.4, friction = 2, index = 0.8))
    }

    @Test
    fun familyRemainderAndRunway() {
        val foundation = familyFoundation(100_000_00, 15_000_00, 25_000_00)
        assertEquals(140_000_00, foundation)
        assertEquals(150_000_00, familyRemainder(175_000_00, 115_000_00, foundation))
        val minimum = carryingMinimum(110_000_00, 15_000_00, 25_000_00)
        assertEquals(150_000_00, minimum)
        assertEquals(0.5, runwayMonths(75_000_00, minimum)!!, 1e-9)
        assertEquals(23_750_00, weeklyTranche(95_000_00))
    }

    @Test
    fun futureValueMatchesChapterIllustration() {
        assertEquals(417_818.0, futureValue(10_000_00, 0.10, 36) / 100.0, 1.0)
        assertEquals(7_593_688.0, futureValue(10_000_00, 0.10, 240) / 100.0, 1.0)
    }

    @Test
    fun cohesionNeedsEveryAxisForTheUpperCircle() {
        assertEquals(Circle.UPPER, circleOf(cohesion(8, 8, 8), 8, 8, 8))
        assertEquals(Circle.MIDDLE, circleOf(cohesion(9, 9, 6), 9, 9, 6))
        assertEquals(Circle.LOWER, circleOf(cohesion(4, 4, 4), 4, 4, 4))
    }

    @Test
    fun avalancheTargetsTheHighestRate() {
        val rows = amortize(
            listOf(
                DebtInput("карта", 120_000_00, 29.9, 6_000_00, 19_000_00),
                DebtInput("кредит", 450_000_00, 19.5, 14_000_00, 0),
            ),
            DebtStrategy.AVALANCHE,
            limit = 1,
        )
        assertEquals("карта", rows.first().focus)
    }

    @Test
    fun cleanSheetOpensMirrorAndUnfundedNoiseDoesNotSave() {
        assertEquals(NoiseBooking(true, true), bookNoise(cleanSheet = true, canFundMirror = false))
        assertEquals(NoiseBooking(true, true), bookNoise(cleanSheet = false, canFundMirror = true))
        assertEquals(NoiseBooking(false, false), bookNoise(cleanSheet = false, canFundMirror = false))
    }

    @Test
    fun routesFollowPartOneAndCautiousTieBreak() {
        val yes = List(7) { Letter.V }
        assertEquals(
            (1..12).toList(),
            planRoute(PhaseAnswers(true, true, yes), fullCycleCompleted = false).weeks,
        )
        assertEquals(
            listOf(1, 2, 8, 7, 3, 4, 5, 6, 9, 10, 11, 12),
            planRoute(PhaseAnswers(false, true, yes), fullCycleCompleted = true).weeks,
        )
        val tieAb = listOf(Letter.A, Letter.A, Letter.A, Letter.B, Letter.B, Letter.B, Letter.V)
        assertEquals(RouteKind.REPAIR_A, planRoute(PhaseAnswers(true, true, tieAb), true).kind)
        val tieBv = listOf(Letter.B, Letter.B, Letter.B, Letter.V, Letter.V, Letter.V, Letter.A)
        assertEquals(RouteKind.REPAIR_B, planRoute(PhaseAnswers(true, true, tieBv), true).kind)
        val mostlyV = listOf(Letter.V, Letter.V, Letter.V, Letter.V, Letter.A, Letter.B, Letter.B)
        assertEquals(
            listOf(10, 11, 12),
            planRoute(PhaseAnswers(true, true, mostlyV), true).weeks,
        )
    }

    @Test
    fun runwayZonesFollowTheJournalBands() {
        assertEquals(RunwayZone.CRITICAL, runwayZone(0.0, 6))
        assertEquals(RunwayZone.VULNERABLE, runwayZone(0.5, 12))
        assertEquals(RunwayZone.VULNERABLE, runwayZone(3.0, 6))
        assertEquals(RunwayZone.BELOW_NORM, runwayZone(4.0, 6))
        assertEquals(RunwayZone.HIRE, runwayZone(8.0, 6))
        assertEquals(RunwayZone.BELOW_NORM, runwayZone(8.0, 12))
        assertEquals(RunwayZone.FREELANCE, runwayZone(12.0, 12))
        assertEquals(RunwayZone.BELOW_NORM, runwayZone(14.0, 24))
        assertEquals(RunwayZone.BUSINESS, runwayZone(24.0, 24))
        assertTrue(runwayZoneLabel(RunwayZone.BELOW_NORM, 12).contains("фриланса"))
        assertFalse(runwayZoneLabel(RunwayZone.BELOW_NORM, 12).contains("найма"))
        assertEquals(1L, weeklyTrancheRemainder(95_000_01))
        assertEquals(23_750_00, weeklyTranche(95_000_01))
    }

    @Test
    fun routeExplanationNamesTheCautiousTie() {
        val tie = listOf(Letter.A, Letter.A, Letter.A, Letter.B, Letter.B, Letter.B, Letter.V)
        val text = routeExplanation(PhaseAnswers(true, true, tie), fullCycleCompleted = true)
        assertTrue(text.contains("более осторожный"))
        assertTrue(text.contains("1, 2, 3, 4, 7"))
        val emergency = routeExplanation(PhaseAnswers(false, true, List(7) { Letter.V }), true)
        assertTrue(emergency.contains("Аварийный контур"))
    }

    @Test
    fun canonTextsMatchTheJournalOnTheFlaggedLines() {
        assertEquals("Семейный контур: два оператора", JournalCanon.week(6).title)
        assertFalse(JournalCanon.weeks.joinToString { it.title }.contains("лодк"))
        assertTrue(JournalCanon.items[0].a.contains("надо быстрее нажать кнопку и не думать"))
        assertTrue(JournalCanon.items[2].a.contains("чтобы оправдать цифру"))
        assertTrue(JournalCanon.items[2].v.contains("не с кем встать в один ряд"))
        assertTrue(JournalCanon.items[4].b.contains("Планирование жёсткое"))
        assertTrue(JournalCanon.items[5].prompt.contains("с другими людьми"))
        assertTrue(JournalCanon.items[6].b.contains("надо закрыть ипотеку"))
        assertTrue(JournalCanon.week(3).calculation.contains("× 100%"))
        assertTrue(JournalCanon.week(5).calculation.contains("Реальная ставка часа S"))
        assertFalse(JournalCanon.week(5).calculation.contains("Настоящая цена"))
        assertTrue(JournalCanon.week(11).title.contains("потолка одиночки"))
        assertTrue(JournalCanon.week(12).title.contains("то, что остаётся после нас"))
        assertFalse(JournalCanon.scripts.any { it.body.contains("не приведён") })
        assertTrue(JournalCanon.scripts.first { it.title == "Я пока не знаю" }.quoted)
        assertTrue(JournalCanon.howToUse.first().startsWith("Журнал — рабочая тетрадь"))
        assertEquals(7, JournalCanon.items.size)
    }

    @Test
    fun markerRepairAddressesTheWeekThatBuiltTheNode() {
        assertEquals(1, repairWeekForMarker(1))
        assertEquals(3, repairWeekForMarker(2))
        assertEquals(4, repairWeekForMarker(3))
        assertEquals(7, repairWeekForMarker(4))
        assertEquals(12, repairWeekForMarker(5))
        assertTrue(formatRubles(4_700_00).contains("4"))
        assertFalse(formatRubles(900_00).startsWith("−"))
    }
}
