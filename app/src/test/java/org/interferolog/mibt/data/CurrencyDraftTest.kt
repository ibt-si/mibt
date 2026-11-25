// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.data

import org.interferolog.mibt.domain.CurrencyChoice
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyDraftTest {
    @Test
    fun draftsRoundTripHighPrecisionAmounts() {
        val btc = CurrencyChoice.fromCode("BTC")
        val draft = FamilyDraft(income1 = "0,00000001", foundation = "1,25")
        val saved = draft.toEntity(1, btc)
        assertEquals(1L, saved.income1)
        assertEquals(125_000_000L, saved.foundation)
        assertEquals("0,00000001", FamilyPlanEntity(cycleId = 1, income1 = 1L).toDraft(btc).income1)
    }

    @Test
    fun draftsKeepRubleDefaultForLegacyCallers() {
        val saved = FamilyDraft(income1 = "150,50").toEntity(1)
        assertEquals(15_050L, saved.income1)
        assertEquals("150,50", saved.toDraft().income1)
    }
}
