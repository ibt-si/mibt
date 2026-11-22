// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HolterMovement(
    val atMillis: Long,
    val node: String,
    val amountKopecks: Long,
)

data class HolterMirror(
    val openedAtMillis: Long,
    val amountKopecks: Long,
)

/**
 * One row of the weekly Holter table.
 * [etaRatio] uses only U₁–U₄. Income and obligations stay in their own fields
 * and never enter the ratio.
 */
data class HolterDayFigures(
    val label: String,
    val date: LocalDate?,
    val incomeKopecks: Long,
    val obligationsKopecks: Long,
    val supportKopecks: Long,
    val toolKopecks: Long,
    val growthKopecks: Long,
    val noiseKopecks: Long,
    val doubleTariffKopecks: Long,
) {
    val etaRatio: Double?
        get() = eta(supportKopecks, toolKopecks, growthKopecks, noiseKopecks)
}

data class HolterWeekFigures(
    val days: List<HolterDayFigures>,
    val total: HolterDayFigures,
)

/**
 * Groups movements into consecutive weeks of seven calendar days.
 * The first day is the earliest movement or mirror bill, or [today] when the log is empty.
 * Weeks continue through [today]. η of a week is the ratio of the summed nodes, not an average of daily ratios.
 */
fun holterWeeks(
    movements: List<HolterMovement>,
    mirrors: List<HolterMirror>,
    zone: ZoneId,
    today: LocalDate,
    dayLabels: List<String>,
    totalLabel: String,
): List<HolterWeekFigures> {
    require(dayLabels.size == 7) { "The journal week has seven day rows." }
    fun onDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val dates = movements.map { onDate(it.atMillis) } + mirrors.map { onDate(it.openedAtMillis) }
    val anchor = dates.minOrNull() ?: today
    val last = listOfNotNull(dates.maxOrNull(), today).max()
    val weeks = mutableListOf<HolterWeekFigures>()
    var start = anchor
    while (!start.isAfter(last)) {
        val days = dayLabels.mapIndexed { index, label ->
            val date = start.plusDays(index.toLong())
            val dayMoves = movements.filter { onDate(it.atMillis) == date }
            fun sum(node: String) = dayMoves.filter { it.node == node }.sumOf { it.amountKopecks }
            HolterDayFigures(
                label = label,
                date = date,
                incomeKopecks = sum("INCOME"),
                obligationsKopecks = sum("OBZ"),
                supportKopecks = sum("U1"),
                toolKopecks = sum("U2"),
                growthKopecks = sum("U3"),
                noiseKopecks = sum("U4"),
                doubleTariffKopecks = mirrors.filter { onDate(it.openedAtMillis) == date }.sumOf { it.amountKopecks },
            )
        }
        weeks += HolterWeekFigures(days, weekTotal(totalLabel, days))
        start = start.plusDays(7)
    }
    return weeks
}

private fun weekTotal(label: String, days: List<HolterDayFigures>): HolterDayFigures = HolterDayFigures(
    label = label,
    date = null,
    incomeKopecks = days.sumOf { it.incomeKopecks },
    obligationsKopecks = days.sumOf { it.obligationsKopecks },
    supportKopecks = days.sumOf { it.supportKopecks },
    toolKopecks = days.sumOf { it.toolKopecks },
    growthKopecks = days.sumOf { it.growthKopecks },
    noiseKopecks = days.sumOf { it.noiseKopecks },
    doubleTariffKopecks = days.sumOf { it.doubleTariffKopecks },
)
