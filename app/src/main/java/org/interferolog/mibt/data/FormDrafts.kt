// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.data

import org.interferolog.mibt.domain.parseRubles

data class FamilyDraft(
    val income1: String = "",
    val income2: String = "",
    val foundation: String = "",
    val targetFund: String = "",
    val personal1: String = "",
    val personal2: String = "",
    val threshold: String = "",
    val phrase: String = "Я сейчас в сжатии",
    val saturdayNote: String = "",
) {
    fun toEntity(cycleId: Long): FamilyPlanEntity = FamilyPlanEntity(
        cycleId = cycleId,
        income1 = kopecksOf(income1),
        income2 = kopecksOf(income2),
        foundation = kopecksOf(foundation),
        targetFund = kopecksOf(targetFund),
        personal1 = kopecksOf(personal1),
        personal2 = kopecksOf(personal2),
        threshold = kopecksOf(threshold),
        phrase = phrase,
        saturdayNote = saturdayNote,
    )
}

fun FamilyPlanEntity.toDraft(): FamilyDraft = FamilyDraft(
    income1 = kopecksInput(income1),
    income2 = kopecksInput(income2),
    foundation = kopecksInput(foundation),
    targetFund = kopecksInput(targetFund),
    personal1 = kopecksInput(personal1),
    personal2 = kopecksInput(personal2),
    threshold = kopecksInput(threshold),
    phrase = phrase,
    saturdayNote = saturdayNote,
)

data class RestDraft(
    val support: String = "",
    val tool: String = "",
    val obligations: String = "",
    val balance: String = "",
    val profile: String = "HIRE",
    val firstCutPercent: String = "",
    val accountOpened: Boolean = false,
    val strategy: String = "AVALANCHE",
) {
    /**
     * A missing row is the first save from the blank form, which records the account as open.
     * A loaded row keeps [accountOpened] from the draft so a re-save does not flip it.
     */
    fun toEntity(cycleId: Long, existing: RestMassEntity?): RestMassEntity = RestMassEntity(
        cycleId = cycleId,
        support = kopecksOf(support),
        tool = kopecksOf(tool),
        obligations = kopecksOf(obligations),
        balance = kopecksOf(balance),
        profile = profile,
        firstCutPercent = doubleOf(firstCutPercent),
        accountOpened = if (existing == null) true else accountOpened,
        strategy = strategy,
    )
}

fun RestMassEntity.toDraft(): RestDraft = RestDraft(
    support = kopecksInput(support),
    tool = kopecksInput(tool),
    obligations = kopecksInput(obligations),
    balance = kopecksInput(balance),
    profile = profile,
    firstCutPercent = decimalInput(firstCutPercent),
    accountOpened = accountOpened,
    strategy = strategy,
)

data class CrisisDraft(
    val accessible: String = "",
    val staffMinimum: String = "",
    val emergencyMinimum: String = "",
    val cutList: String = "",
    val supportRing: String = "",
    val offers: String = "",
    val drillDone: Boolean = false,
) {
    fun toEntity(cycleId: Long): CrisisMapEntity = CrisisMapEntity(
        cycleId = cycleId,
        accessible = kopecksOf(accessible),
        staffMinimum = kopecksOf(staffMinimum),
        emergencyMinimum = kopecksOf(emergencyMinimum),
        cutList = cutList,
        supportRing = supportRing,
        offers = offers,
        drillDone = drillDone,
    )
}

fun CrisisMapEntity.toDraft(): CrisisDraft = CrisisDraft(
    accessible = kopecksInput(accessible),
    staffMinimum = kopecksInput(staffMinimum),
    emergencyMinimum = kopecksInput(emergencyMinimum),
    cutList = cutList,
    supportRing = supportRing,
    offers = offers,
    drillDone = drillDone,
)

data class PassportDraft(
    val massStatus: String = "",
    val contributionRule: String = "",
    val horizonRule: String = "",
    val allocation: String = "",
    val riskLimit: String = "",
    val stormRule: String = "",
    val hygiene: String = "",
    val accountBalance: String = "",
    val monthlyContribution: String = "",
    val annualRatePercent: String = "",
    val years: String = "",
    val reinvestPercent: String = "",
    val supportPercent: String = "",
    val crystalPercent: String = "",
    val drawdownOk: Boolean = false,
) {
    fun textAt(index: Int): String = when (index) {
        0 -> massStatus
        1 -> contributionRule
        2 -> horizonRule
        3 -> allocation
        4 -> riskLimit
        5 -> stormRule
        6 -> hygiene
        else -> ""
    }

    fun withText(index: Int, value: String): PassportDraft = when (index) {
        0 -> copy(massStatus = value)
        1 -> copy(contributionRule = value)
        2 -> copy(horizonRule = value)
        3 -> copy(allocation = value)
        4 -> copy(riskLimit = value)
        5 -> copy(stormRule = value)
        6 -> copy(hygiene = value)
        else -> this
    }

    fun toEntity(cycleId: Long): InvestmentPassportEntity = InvestmentPassportEntity(
        cycleId = cycleId,
        massStatus = massStatus,
        contributionRule = contributionRule,
        horizonRule = horizonRule,
        allocation = allocation,
        riskLimit = riskLimit,
        stormRule = stormRule,
        hygiene = hygiene,
        accountBalance = kopecksOf(accountBalance),
        monthlyContribution = kopecksOf(monthlyContribution),
        annualRate = doubleOf(annualRatePercent) / 100.0,
        years = intOf(years),
        reinvestPercent = doubleOf(reinvestPercent),
        supportPercent = doubleOf(supportPercent),
        crystalPercent = doubleOf(crystalPercent),
        drawdownOk = drawdownOk,
    )
}

fun InvestmentPassportEntity.toDraft(): PassportDraft = PassportDraft(
    massStatus = massStatus,
    contributionRule = contributionRule,
    horizonRule = horizonRule,
    allocation = allocation,
    riskLimit = riskLimit,
    stormRule = stormRule,
    hygiene = hygiene,
    accountBalance = kopecksInput(accountBalance),
    monthlyContribution = kopecksInput(monthlyContribution),
    annualRatePercent = percentInput(annualRate),
    years = years.toString(),
    reinvestPercent = decimalInput(reinvestPercent),
    supportPercent = decimalInput(supportPercent),
    crystalPercent = decimalInput(crystalPercent),
    drawdownOk = drawdownOk,
)

data class PositionDraft(
    val craft: String = "",
    val surplus: String = "",
    val people: String = "",
    val horizon: String = "",
    val timeFeel: String = "",
    val answer: String = "",
) {
    fun sphereAt(index: Int): String = when (index) {
        0 -> craft
        1 -> surplus
        2 -> people
        3 -> horizon
        4 -> timeFeel
        else -> ""
    }

    fun withSphere(index: Int, value: String): PositionDraft = when (index) {
        0 -> copy(craft = value)
        1 -> copy(surplus = value)
        2 -> copy(people = value)
        3 -> copy(horizon = value)
        4 -> copy(timeFeel = value)
        else -> this
    }

    fun toEntity(cycleId: Long): PositionAuditEntity = PositionAuditEntity(
        cycleId = cycleId,
        craft = craft,
        surplus = surplus,
        people = people,
        horizon = horizon,
        timeFeel = timeFeel,
        answer = answer,
    )
}

fun PositionAuditEntity.toDraft(): PositionDraft = PositionDraft(
    craft = craft,
    surplus = surplus,
    people = people,
    horizon = horizon,
    timeFeel = timeFeel,
    answer = answer,
)

data class RingDraft(
    val what: String = "",
    val growthShare: String = "",
    val hoursPerWeek: String = "",
    val firstStone: String = "",
) {
    fun toEntity(cycleId: Long, ring: Int): MeaningRingEntity = MeaningRingEntity(
        cycleId = cycleId,
        ring = ring,
        what = what,
        growthShare = doubleOf(growthShare),
        hoursPerWeek = doubleOf(hoursPerWeek),
        firstStone = firstStone,
    )
}

fun MeaningRingEntity.toDraft(): RingDraft = RingDraft(
    what = what,
    growthShare = decimalInput(growthShare),
    hoursPerWeek = decimalInput(hoursPerWeek),
    firstStone = firstStone,
)

private fun kopecksOf(text: String): Long = parseRubles(text) ?: 0

private fun kopecksInput(kopecks: Long): String {
    val sign = if (kopecks < 0) "-" else ""
    val value = kotlin.math.abs(kopecks)
    val rubles = value / 100
    val kop = (value % 100).toInt()
    val body = if (kop == 0) rubles.toString() else "$rubles,${kop.toString().padStart(2, '0')}"
    return sign + body
}

private fun doubleOf(text: String): Double = text.toDoubleOrNull() ?: 0.0

private fun intOf(text: String): Int = text.toIntOrNull() ?: 0

private fun decimalInput(value: Double): String {
    if (!value.isFinite()) return ""
    return value.toString().removeSuffix(".0")
}

private fun percentInput(fraction: Double): String {
    if (!fraction.isFinite()) return ""
    val percent = fraction * 100.0
    val rounded = kotlin.math.round(percent * 1_000_000.0) / 1_000_000.0
    return decimalInput(rounded)
}
