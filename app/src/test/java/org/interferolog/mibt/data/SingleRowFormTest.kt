// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.data

import kotlinx.coroutines.runBlocking
import org.interferolog.mibt.content.JournalCanon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SingleRowFormTest {
    @Test
    fun familyResaveKeepsEveryField() {
        val saved = FamilyPlanEntity(
            cycleId = 7,
            income1 = 175_000_00,
            income2 = 115_000_50,
            foundation = 140_000_00,
            targetFund = 20_000_00,
            personal1 = 10_000_00,
            personal2 = 5_050_00,
            threshold = 3_000_00,
            phrase = "Своя фраза",
            saturdayNote = "Ещё одна сверка",
        )
        assertEquals(saved, saved.toDraft().toEntity(7))
        val edited = saved.toDraft().copy(saturdayNote = "тишина").toEntity(7)
        assertEquals(saved.copy(saturdayNote = "тишина"), edited)
    }

    @Test
    fun familyMoneyRoundTripsKopecks() {
        listOf(0L, 1L, 10L, 99L, 100L, 150_50L, 175_000_00L, -150_50L).forEach { kopecks ->
            val saved = FamilyPlanEntity(cycleId = 1, income1 = kopecks, threshold = kopecks)
            val again = saved.toDraft().toEntity(1)
            assertEquals(kopecks, again.income1)
            assertEquals(kopecks, again.threshold)
        }
    }

    @Test
    fun blankFamilyDraftUsesTheSameDefaultsAsANewRow() {
        assertEquals(FamilyPlanEntity(cycleId = 4), FamilyDraft().toEntity(4))
    }

    @Test
    fun restResaveKeepsClosedAccountAndCut() {
        val saved = RestMassEntity(
            cycleId = 3,
            support = 110_000_00,
            tool = 15_000_00,
            obligations = 25_000_50,
            balance = 75_000_00,
            profile = "BUSINESS",
            firstCutPercent = 7.5,
            accountOpened = false,
            strategy = "SNOWBALL",
        )
        assertEquals(saved, saved.toDraft().toEntity(3, saved))
        val edited = saved.toDraft().copy(balance = "80000").toEntity(3, saved)
        assertEquals(saved.copy(balance = 80_000_00), edited)
    }

    @Test
    fun firstRestSaveOpensTheAccountAndDoesNotInventACut() {
        assertEquals(
            RestMassEntity(cycleId = 4, firstCutPercent = 0.0, accountOpened = true),
            RestDraft().toEntity(4, null),
        )
    }

    @Test
    fun restPercentsRoundTrip() {
        listOf(0.0, 5.0, 7.5, 12.25, 0.1).forEach { cut ->
            val saved = RestMassEntity(cycleId = 1, firstCutPercent = cut, accountOpened = true)
            assertEquals(cut, saved.toDraft().toEntity(1, saved).firstCutPercent, 0.0)
        }
    }

    @Test
    fun crisisResaveKeepsDrillAndText() {
        val saved = CrisisMapEntity(
            cycleId = 8,
            accessible = 90_000_00,
            staffMinimum = 70_000_50,
            emergencyMinimum = 45_000_00,
            cutList = "подписки\nтакси",
            supportRing = "двое своих",
            offers = "три фразы",
            drillDone = true,
        )
        assertEquals(saved, saved.toDraft().toEntity(8))
        val marked = saved.toDraft().copy(drillDone = false).toEntity(8)
        assertEquals(saved.copy(drillDone = false), marked)
    }

    @Test
    fun blankCrisisDraftDoesNotDropTheDrillFlagDefault() {
        assertEquals(CrisisMapEntity(cycleId = 4), CrisisDraft().toEntity(4))
    }

    @Test
    fun passportResaveKeepsEveryFieldIncludingRate() {
        val typedRate = "12.5"
        val saved = InvestmentPassportEntity(
            cycleId = 3,
            massStatus = "собрана",
            contributionRule = "каждый месяц",
            horizonRule = "купон в опору",
            allocation = "широкий рынок",
            riskLimit = "без плеча",
            stormRule = "не продавать",
            hygiene = "без ленты",
            accountBalance = 250_000_50,
            monthlyContribution = 10_000_00,
            annualRate = typedRate.toDouble() / 100.0,
            years = 20,
            reinvestPercent = 50.0,
            supportPercent = 30.0,
            crystalPercent = 20.0,
            drawdownOk = true,
        )
        assertEquals(saved, saved.toDraft().toEntity(3))
        assertEquals(typedRate, saved.toDraft().annualRatePercent)
        val edited = saved.toDraft().withText(4, "плечо запрещено").toEntity(3)
        assertEquals(saved.copy(riskLimit = "плечо запрещено"), edited)
        val draft = saved.toDraft()
        JournalCanon.passportFields.indices.forEach { index ->
            val changed = draft.withText(index, "x")
            assertEquals("x", changed.textAt(index))
            JournalCanon.passportFields.indices.filter { it != index }.forEach { other ->
                assertEquals(draft.textAt(other), changed.textAt(other))
            }
        }
    }

    @Test
    fun typedPassportRatesRoundTrip() {
        listOf("0", "1", "7", "7.5", "10", "12.25", "12.5", "33.333", "50", "100", "0.5").forEach { typed ->
            val saved = InvestmentPassportEntity(cycleId = 1, annualRate = typed.toDouble() / 100.0)
            val again = saved.toDraft().toEntity(1)
            assertEquals(saved.annualRate, again.annualRate, 0.0)
        }
    }

    @Test
    fun blankPassportDraftDoesNotApplyEntityDefaults() {
        assertEquals(
            InvestmentPassportEntity(
                cycleId = 4,
                annualRate = 0.0,
                years = 0,
                reinvestPercent = 0.0,
                supportPercent = 0.0,
                crystalPercent = 0.0,
            ),
            PassportDraft().toEntity(4),
        )
    }

    @Test
    fun positionResaveKeepsEverySphereAndTheAnswer() {
        val saved = PositionAuditEntity(
            cycleId = 3,
            craft = "ремесло",
            surplus = "профицит",
            people = "круг",
            horizon = "десять лет",
            timeFeel = "ровно",
            answer = "Два предложения. Ещё одно.",
        )
        assertEquals(saved, saved.toDraft().toEntity(3))
        val edited = saved.toDraft().withSphere(1, "новый профицит").toEntity(3)
        assertEquals(saved.copy(surplus = "новый профицит"), edited)
        assertEquals(saved.craft, edited.craft)
        assertEquals(saved.answer, edited.answer)
        JournalCanon.positionSpheres.indices.forEach { index ->
            assertEquals(saved.toDraft().sphereAt(index), listOf(saved.craft, saved.surplus, saved.people, saved.horizon, saved.timeFeel)[index])
        }
    }

    @Test
    fun canonFieldCountsMatchTheDraftSlots() {
        assertEquals(7, JournalCanon.passportFields.size)
        assertEquals(5, JournalCanon.positionSpheres.size)
        assertEquals(3, JournalCanon.rings.size)
    }

    @Test
    fun ringDraftKeepsContentFields() {
        val saved = MeaningRingEntity(
            id = 8,
            cycleId = 3,
            ring = 2,
            what = "школа",
            growthShare = 12.5,
            hoursPerWeek = 4.0,
            firstStone = "первый урок",
        )
        val again = saved.toDraft().toEntity(3, 2)
        assertEquals(0L, again.id)
        assertEquals(saved.copy(id = 0), again)
        val edited = saved.toDraft().copy(hoursPerWeek = "6.5").toEntity(3, 2)
        assertEquals(saved.copy(id = 0, hoursPerWeek = 6.5), edited)
        assertEquals(saved.what, edited.what)
        assertEquals(saved.firstStone, edited.firstStone)
    }

    @Test
    fun savingTheSameRingUpdatesInsteadOfInserting() = runBlocking {
        val store = FakeRings()
        val first = MeaningRingEntity(
            cycleId = 4,
            ring = 1,
            what = "дом",
            growthShare = 20.0,
            hoursPerWeek = 3.0,
            firstStone = "порог",
        )
        suspend fun save(ring: MeaningRingEntity) {
            saveMeaningRing(store.rows.toList(), store::insert, store::update, ring)
        }
        save(first)
        save(first.copy(what = "дом и сад", growthShare = 30.0, hoursPerWeek = 6.5, firstStone = "окно"))
        save(first.copy(ring = 2, what = "школа", growthShare = 10.0, hoursPerWeek = 2.0, firstStone = "урок"))
        save(first.copy(cycleId = 9, what = "другой цикл"))

        assertEquals(1, store.rows.count { it.cycleId == 4L && it.ring == 1 })
        assertEquals(1, store.rows.count { it.cycleId == 4L && it.ring == 2 })
        assertEquals(1, store.rows.count { it.cycleId == 9L && it.ring == 1 })
        val kept = store.rows.single { it.cycleId == 4L && it.ring == 1 }
        assertEquals(1L, kept.id)
        assertEquals(first.copy(id = 1L, what = "дом и сад", growthShare = 30.0, hoursPerWeek = 6.5, firstStone = "окно"), kept)
        assertEquals(listOf(1, 2), latestRings(store.rows.filter { it.cycleId == 4L }).map { it.ring })
    }

    @Test
    fun existingRingDuplicatesDisplayOnlyTheLatest() {
        val older = MeaningRingEntity(
            id = 2,
            cycleId = 1,
            ring = 1,
            what = "старое",
            growthShare = 1.0,
            hoursPerWeek = 1.0,
            firstStone = "а",
        )
        val middle = older.copy(id = 4, what = "среднее")
        val newer = older.copy(id = 5, what = "новое", hoursPerWeek = 4.0)
        val second = older.copy(id = 3, ring = 2, what = "второе")
        val visible = latestRings(listOf(older, second, middle, newer))
        assertEquals(listOf(5L, 3L), visible.map { it.id })
        assertEquals("новое", visible.first().what)
        assertEquals(4.0, visible.first().hoursPerWeek, 0.0)

        val plan = planRingSave(listOf(older, middle, newer), newer.copy(what = "ещё новее", firstStone = "камень"))
        assertNull(plan.insert)
        assertEquals(5L, plan.update?.id)
        assertEquals("ещё новее", plan.update?.what)
        assertEquals("камень", plan.update?.firstStone)
        assertEquals(newer.growthShare, plan.update?.growthShare)
    }

    private class FakeRings {
        val rows = mutableListOf<MeaningRingEntity>()
        private var nextId = 1L

        suspend fun insert(ring: MeaningRingEntity) {
            val id = if (ring.id == 0L) nextId++ else ring.id
            rows += ring.copy(id = id)
        }

        suspend fun update(ring: MeaningRingEntity) {
            val index = rows.indexOfFirst { it.id == ring.id }
            check(index >= 0) { "update missed ${ring.id}" }
            rows[index] = ring
        }
    }
}
