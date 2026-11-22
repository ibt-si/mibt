// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.domain

import kotlin.math.abs
import kotlin.math.round

fun parseRubles(text: String): Long? {
    val cleaned = text.trim()
        .replace(" ", "")
        .replace("\u00a0", "")
        .replace("₽", "")
        .replace(",", ".")
    if (cleaned.isEmpty() || cleaned == "-" || cleaned == ".") return null
    val value = cleaned.toDoubleOrNull() ?: return null
    return round(value * 100.0).toLong()
}

fun formatRubles(kopecks: Long): String {
    val negative = kopecks < 0
    val value = abs(kopecks)
    val rubles = value / 100
    val kop = (value % 100).toInt()
    val body = if (kop == 0) rubles.toString() else "%d,%02d".format(rubles, kop)
    return (if (negative) "−" else "") + body + " ₽"
}

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
