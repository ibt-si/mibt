// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.data

import org.interferolog.mibt.content.BlankCanon
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryMarkdownTest {
    @Test
    fun exportContainsEnteredAmount() {
        val text = exportTelemetry(
            TelemetrySnapshot(
                cycle = CycleEntity(
                    id = 1,
                    startedAt = 0,
                    routeKind = "BASIC",
                    weekOrder = "1,2,3",
                    status = "ACTIVE",
                    fullCycleCompleted = false,
                ),
                test = null,
                attempts = emptyList(),
                calibration = emptyList(),
                diary = emptyList(),
                wells = emptyList(),
                spends = listOf(
                    SpendEntryEntity(
                        cycleId = 1,
                        atMillis = 0,
                        node = "U4",
                        amountKopecks = 4_700_00,
                        note = "корзина",
                        cleanSheet = false,
                    ),
                ),
                bills = emptyList(),
                sources = emptyList(),
                rehearsals = emptyList(),
                work = emptyList(),
                hours = emptyList(),
                family = null,
                rest = null,
                debts = emptyList(),
                crisis = null,
                quarantines = emptyList(),
                tranches = emptyList(),
                branches = emptyList(),
                channels = emptyList(),
                passport = null,
                people = emptyList(),
                position = null,
                rings = emptyList(),
                maintenance = emptyList(),
                markers = emptyList(),
            ),
        )
        assertTrue(text.contains("4700"))
        assertTrue(text.contains("открытым текстом"))
        assertTrue(text.contains("## ${BlankCanon.phaseDiary.title}"))
        assertTrue(text.contains("## ${BlankCanon.pointC.title}"))
        assertTrue(text.contains("## ${BlankCanon.holter.title}"))
        assertTrue(text.contains("## ${BlankCanon.sources.title}"))
        assertTrue(text.contains("## ${BlankCanon.work.title}"))
    }

    @Test
    fun exportUsesTheCycleCurrency() {
        val text = exportTelemetry(
            TelemetrySnapshot(
                cycle = CycleEntity(
                    id = 1,
                    startedAt = 0,
                    routeKind = "BASIC",
                    weekOrder = "1,2,3",
                    status = "ACTIVE",
                    fullCycleCompleted = false,
                    currency = "BTC",
                ),
                test = null,
                attempts = emptyList(),
                calibration = emptyList(),
                diary = emptyList(),
                wells = emptyList(),
                spends = listOf(
                    SpendEntryEntity(
                        cycleId = 1,
                        atMillis = 0,
                        node = "U1",
                        amountKopecks = 50_000_000,
                        note = "",
                        cleanSheet = false,
                    ),
                ),
                bills = emptyList(),
                sources = emptyList(),
                rehearsals = emptyList(),
                work = emptyList(),
                hours = emptyList(),
                family = null,
                rest = null,
                debts = emptyList(),
                crisis = null,
                quarantines = emptyList(),
                tranches = emptyList(),
                branches = emptyList(),
                channels = emptyList(),
                passport = null,
                people = emptyList(),
                position = null,
                rings = emptyList(),
                maintenance = emptyList(),
                markers = emptyList(),
            ),
        )
        assertTrue(text.contains("0,50000000 ₿"))
    }
}
