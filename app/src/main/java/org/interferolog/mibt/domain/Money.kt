// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/** Parses a typed amount into minor units of [currency]. Accepts both comma and dot decimals. */
fun parseMoney(text: String, currency: CurrencyChoice): Long? {
    val cleaned = text.trim()
        .replace(" ", "")
        .replace("\u00a0", "")
        .replace("\u202f", "")
        .replace(currency.symbol, "")
        .replace(currency.code, "", ignoreCase = true)
        .replace("₽", "")
        .replace("−", "-")
        .replace(",", ".")
    if (cleaned.isEmpty() || cleaned == "-" || cleaned == ".") return null
    val value = try {
        BigDecimal(cleaned).setScale(currency.fractionDigits, RoundingMode.HALF_UP)
    } catch (_: NumberFormatException) {
        return null
    }
    return try {
        value.movePointRight(currency.fractionDigits).longValueExact()
    } catch (_: ArithmeticException) {
        null
    }
}

/** Formats minor units of [currency] using a comma decimal separator and a symbol suffix. */
fun formatMoney(minor: Long, currency: CurrencyChoice): String {
    val value = abs(minor)
    val body = if (currency.fractionDigits == 0 || value % currency.unit == 0L) {
        (value / currency.unit).toString()
    } else {
        val whole = value / currency.unit
        val fraction = (value % currency.unit)
            .toString()
            .padStart(currency.fractionDigits, '0')
        "$whole,$fraction"
    }
    return (if (minor < 0) "−" else "") + body + " " + currency.symbol
}

fun parseRubles(text: String): Long? = parseMoney(text, CurrencyChoice.RUB)

fun formatRubles(kopecks: Long): String = formatMoney(kopecks, CurrencyChoice.RUB)

fun formatRatio(ratio: Double): String {
    val percent = ratio * 100.0
    val rounded = round(percent * 10.0) / 10.0
    val text = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.1f".format(rounded)
    return "$text%"
}

fun formatHours(hours: Double): String {
    val rounded = round(hours * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.1f".format(rounded)
}

fun formatPoints(points: Double): String {
    val rounded = round(points * 10.0) / 10.0
    val text = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.1f".format(Locale.US, rounded)
    return "$text%"
}

fun formatIndex(index: Double): String = "%.2f".format(Locale.US, index)

fun formatPerHour(kopecksPerHour: Double): String = formatPerHour(kopecksPerHour, CurrencyChoice.RUB)

fun formatPerHour(minorPerHour: Double, currency: CurrencyChoice): String =
    "${formatMoney(minorPerHour.toLong(), currency)}/ч"

fun formatYield(ratio: Double): String {
    val rounded = round(ratio * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.2f".format(Locale.US, rounded)
}
