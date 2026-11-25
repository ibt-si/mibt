// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.data

import org.interferolog.mibt.domain.CurrencyChoice
import org.interferolog.mibt.domain.formatMoney
import org.interferolog.mibt.domain.parseMoney

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
    fun toEntity(cycleId: Long, currency: CurrencyChoice = CurrencyChoice.RUB): FamilyPlanEntity = FamilyPlanEntity(
        cycleId = cycleId,
        income1 = minorOf(income1, currency),
        income2 = minorOf(income2, currency),
        foundation = minorOf(foundation, currency),
        targetFund = minorOf(targetFund, currency),
        personal1 = minorOf(personal1, currency),
        personal2 = minorOf(personal2, currency),
        threshold = minorOf(threshold, currency),
        phrase = phrase,
        saturdayNote = saturdayNote,
    )
}

fun FamilyPlanEntity.toDraft(currency: CurrencyChoice = CurrencyChoice.RUB): FamilyDraft = FamilyDraft(
    income1 = minorInput(income1, currency),
    income2 = minorInput(income2, currency),
    foundation = minorInput(foundation, currency),
    targetFund = minorInput(targetFund, currency),
    personal1 = minorInput(personal1, currency),
    personal2 = minorInput(personal2, currency),
    threshold = minorInput(threshold, currency),
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
    fun toEntity(
        cycleId: Long,
        existing: RestMassEntity?,
        currency: CurrencyChoice = CurrencyChoice.RUB,
    ): RestMassEntity = RestMassEntity(
        cycleId = cycleId,
        support = minorOf(support, currency),
        tool = minorOf(tool, currency),
        obligations = minorOf(obligations, currency),
        balance = minorOf(balance, currency),
        profile = profile,
        firstCutPercent = doubleOf(firstCutPercent),
        accountOpened = if (existing == null) true else accountOpened,
        strategy = strategy,
    )
}

fun RestMassEntity.toDraft(currency: CurrencyChoice = CurrencyChoice.RUB): RestDraft = RestDraft(
    support = minorInput(support, currency),
    tool = minorInput(tool, currency),
    obligations = minorInput(obligations, currency),
    balance = minorInput(balance, currency),
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
    fun toEntity(cycleId: Long, currency: CurrencyChoice = CurrencyChoice.RUB): CrisisMapEntity = CrisisMapEntity(
        cycleId = cycleId,
        accessible = minorOf(accessible, currency),
        staffMinimum = minorOf(staffMinimum, currency),
        emergencyMinimum = minorOf(emergencyMinimum, currency),
        cutList = cutList,
        supportRing = supportRing,
        offers = offers,
        drillDone = drillDone,
    )
}

fun CrisisMapEntity.toDraft(currency: CurrencyChoice = CurrencyChoice.RUB): CrisisDraft = CrisisDraft(
    accessible = minorInput(accessible, currency),
    staffMinimum = minorInput(staffMinimum, currency),
    emergencyMinimum = minorInput(emergencyMinimum, currency),
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

    fun toEntity(cycleId: Long, currency: CurrencyChoice = CurrencyChoice.RUB): InvestmentPassportEntity = InvestmentPassportEntity(
        cycleId = cycleId,
        massStatus = massStatus,
        contributionRule = contributionRule,
        horizonRule = horizonRule,
        allocation = allocation,
        riskLimit = riskLimit,
        stormRule = stormRule,
        hygiene = hygiene,
        accountBalance = minorOf(accountBalance, currency),
        monthlyContribution = minorOf(monthlyContribution, currency),
        annualRate = doubleOf(annualRatePercent) / 100.0,
        years = intOf(years),
        reinvestPercent = doubleOf(reinvestPercent),
        supportPercent = doubleOf(supportPercent),
        crystalPercent = doubleOf(crystalPercent),
        drawdownOk = drawdownOk,
    )
}

fun InvestmentPassportEntity.toDraft(currency: CurrencyChoice = CurrencyChoice.RUB): PassportDraft = PassportDraft(
    massStatus = massStatus,
    contributionRule = contributionRule,
    horizonRule = horizonRule,
    allocation = allocation,
    riskLimit = riskLimit,
    stormRule = stormRule,
    hygiene = hygiene,
    accountBalance = minorInput(accountBalance, currency),
    monthlyContribution = minorInput(monthlyContribution, currency),
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

private fun minorOf(text: String, currency: CurrencyChoice): Long = parseMoney(text, currency) ?: 0

private fun minorInput(minor: Long, currency: CurrencyChoice): String =
    formatMoney(minor, currency).removeSuffix(" ${currency.symbol}")

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
