// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.domain

data class SourceInput(
    val netIncomeKopecks: Long,
    val compensatoryNoiseKopecks: Long,
    val contractHours: Double,
    val frictionHours: Double,
    val incomeShare: Double,
    val hoursShare: Double,
    val frictionScore: Int,
)

fun SourceInput.rate(): Double? = hourlyRate(
    netIncomeKopecks,
    compensatoryNoiseKopecks,
    contractHours,
    frictionHours,
)

fun SourceInput.index(): Double? = resistanceByShares(frictionScore, hoursShare, incomeShare)

data class SourceTotals(
    val netIncomeKopecks: Long,
    val incomeShare: Double,
    val hours: Double,
    val hoursShare: Double,
    val averageRateKopecks: Double?,
    val rowsAtLeastTwo: Int,
)

/**
 * Portfolio rate is total clean money divided by total hours.
 * [rowsAtLeastTwo] counts sources whose Fᵢ is at least 2.
 */
fun sourceTotals(rows: List<SourceInput>): SourceTotals {
    val hours = rows.sumOf { it.contractHours + it.frictionHours }
    val clean = rows.sumOf { it.netIncomeKopecks - it.compensatoryNoiseKopecks }
    return SourceTotals(
        netIncomeKopecks = rows.sumOf { it.netIncomeKopecks },
        incomeShare = rows.sumOf { it.incomeShare },
        hours = hours,
        hoursShare = rows.sumOf { it.hoursShare },
        averageRateKopecks = if (hours <= 0.0) null else clean.toDouble() / hours,
        rowsAtLeastTwo = rows.count { (it.index() ?: Double.NEGATIVE_INFINITY) >= 2.0 },
    )
}

data class WorkInput(
    val resultShare: Double,
    val hours: Double,
    val hoursShare: Double,
    val frictionScore: Int,
) {
    val yieldRatio: Double
        get() = if (hoursShare == 0.0) 0.0 else resultShare / hoursShare

    val index: Double
        get() = resistanceByShares(frictionScore, hoursShare, resultShare) ?: 0.0
}

data class WorkTotals(
    val resultShare: Double,
    val hours: Double,
    val hoursShare: Double,
)

fun workTotals(rows: List<WorkInput>): WorkTotals = WorkTotals(
    resultShare = rows.sumOf { it.resultShare },
    hours = rows.sumOf { it.hours },
    hoursShare = rows.sumOf { it.hoursShare },
)

fun withDecision(label: String, decision: String): String {
    val own = decision.trim()
    return if (own.isEmpty()) label else "$label\n$own"
}
