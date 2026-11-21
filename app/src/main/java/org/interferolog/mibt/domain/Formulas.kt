package org.interferolog.mibt.domain

import kotlin.math.pow

enum class EtaBand { OVERHEATED, WORKING, PINCH }

enum class FiZone { GREEN, YELLOW, RED }

enum class Quadrant { CORE, GROWTH_POINT, HYGIENE, FRICTION }

enum class RunwayZone { CRITICAL, VULNERABLE, BELOW_NORM, HIRE, FREELANCE, BUSINESS }

enum class Circle { LOWER, MIDDLE, UPPER }

data class NoiseBooking(val savePurchase: Boolean, val openMirror: Boolean)

fun bookNoise(cleanSheet: Boolean, canFundMirror: Boolean): NoiseBooking {
    if (cleanSheet) return NoiseBooking(savePurchase = true, openMirror = true)
    return if (canFundMirror) {
        NoiseBooking(savePurchase = true, openMirror = true)
    } else {
        NoiseBooking(savePurchase = false, openMirror = false)
    }
}

fun chosenSpend(u1: Long, u2: Long, u3: Long, u4: Long): Long = u1 + u2 + u3 + u4

fun eta(u1: Long, u2: Long, u3: Long, u4: Long): Double? {
    val total = chosenSpend(u1, u2, u3, u4)
    if (total == 0L) return null
    return (u1 + u2 + u3).toDouble() / total.toDouble()
}

fun etaBand(ratio: Double): EtaBand = when {
    ratio < 0.70 -> EtaBand.OVERHEATED
    ratio <= 0.90 -> EtaBand.WORKING
    else -> EtaBand.PINCH
}

fun etaBandLabel(band: EtaBand): String = when (band) {
    EtaBand.OVERHEATED -> "меньше 70% — перегретый проводник"
    EtaBand.WORKING -> "70–90% — рабочий режим, норма от 80%"
    EtaBand.PINCH -> "больше 90% — проверка на зажим Опоры"
}

fun hourlyRate(
    netIncome: Long,
    compensatoryNoise: Long,
    contractHours: Double,
    frictionHours: Double,
): Double? {
    val hours = contractHours + frictionHours
    if (hours <= 0.0) return null
    return (netIncome - compensatoryNoise).toDouble() / hours
}

fun resistanceByShares(friction: Int, hoursShare: Double, incomeShare: Double): Double? {
    if (incomeShare == 0.0) return null
    return (friction * hoursShare) / (5.0 * incomeShare)
}

fun resistanceByRates(friction: Int, averageRate: Double, sourceRate: Double): Double? {
    if (sourceRate == 0.0) return null
    return (friction / 5.0) * (averageRate / sourceRate)
}

fun fiZone(index: Double): FiZone = when {
    index <= 1.0 -> FiZone.GREEN
    index < 2.0 -> FiZone.YELLOW
    else -> FiZone.RED
}

fun fiZoneLabel(zone: FiZone): String = when (zone) {
    FiZone.GREEN -> "Зелёная: оберегать"
    FiZone.YELLOW -> "Жёлтая: снизить трение или поднять ставку"
    FiZone.RED -> "Красная: пересобрать условия или спланировать выход"
}

fun quadrant(yield: Double, friction: Int, index: Double): Quadrant {
    if (index >= 2.0) return Quadrant.FRICTION
    val tense = friction >= 6
    val productive = yield >= 1.0
    return when {
        productive && !tense -> Quadrant.CORE
        productive && tense -> Quadrant.GROWTH_POINT
        !productive && !tense -> Quadrant.HYGIENE
        else -> Quadrant.FRICTION
    }
}

fun quadrantLabel(quadrant: Quadrant): String = when (quadrant) {
    Quadrant.CORE -> "I. Ядро"
    Quadrant.GROWTH_POINT -> "II. Точка роста"
    Quadrant.HYGIENE -> "III. Обязательная гигиена"
    Quadrant.FRICTION -> "IV. Трение"
}

fun familyFoundation(sharedSupport: Long, sharedTool: Long, obligations: Long): Long =
    sharedSupport + sharedTool + obligations

fun familyRemainder(income1: Long, income2: Long, foundation: Long): Long =
    income1 + income2 - foundation

fun carryingMinimum(support: Long, tool: Long, obligations: Long): Long =
    support + tool + obligations

fun runwayMonths(balance: Long, minimum: Long): Double? {
    if (minimum <= 0L) return null
    return balance.toDouble() / minimum.toDouble()
}

fun runwayZone(months: Double, profileMonths: Int): RunwayZone = when {
    months <= 0.0 -> RunwayZone.CRITICAL
    months <= 3.0 -> RunwayZone.VULNERABLE
    months >= profileMonths && profileMonths >= 24 -> RunwayZone.BUSINESS
    months >= profileMonths && profileMonths >= 12 -> RunwayZone.FREELANCE
    months >= profileMonths && profileMonths >= 6 -> RunwayZone.HIRE
    else -> RunwayZone.BELOW_NORM
}

fun runwayZoneLabel(zone: RunwayZone, profileMonths: Int): String = when (zone) {
    RunwayZone.CRITICAL -> "0 — критическая: любой сбой пробивает контур"
    RunwayZone.VULNERABLE -> "До 3 месяцев — уязвимая: защищает от мелких аварий, не от потери дохода"
    RunwayZone.HIRE -> "6 месяцев — рабочая норма для найма"
    RunwayZone.FREELANCE -> "12 месяцев — норма для фриланса и независимых специалистов"
    RunwayZone.BUSINESS -> "24 месяца — норма для владельца бизнеса"
    RunwayZone.BELOW_NORM -> {
        val norm = when {
            profileMonths >= 24 -> "24 месяца — норма для владельца бизнеса"
            profileMonths >= 12 -> "12 месяцев — норма для фриланса и независимых специалистов"
            else -> "6 месяцев — рабочая норма для найма"
        }
        "Выше уязвимой зоны (до 3 месяцев), до нормы профиля ещё не дошло: $norm"
    }
}

fun weeklyTranche(emergencyMinimum: Long): Long = emergencyMinimum / 4

fun weeklyTrancheRemainder(emergencyMinimum: Long): Long = emergencyMinimum % 4

fun echelonHours(weekHours: Double, share: Double): Double = weekHours * share

fun futureValue(payment: Long, annualRate: Double, periods: Int): Double {
    if (periods <= 0 || payment == 0L) return 0.0
    if (annualRate == 0.0) return payment.toDouble() * periods
    val periodRate = annualRate / 12.0
    return payment.toDouble() * (((1.0 + periodRate).pow(periods) - 1.0) / periodRate)
}

fun cohesion(transparency: Int, word: Int, gain: Int): Double =
    (transparency + word + gain) / 3.0

fun circleOf(index: Double, transparency: Int, word: Int, gain: Int): Circle = when {
    index >= 7.5 && transparency >= 7 && word >= 7 && gain >= 7 -> Circle.UPPER
    index >= 5.0 -> Circle.MIDDLE
    else -> Circle.LOWER
}

fun circleLabel(circle: Circle): String = when (circle) {
    Circle.LOWER -> "Нижний круг"
    Circle.MIDDLE -> "Средний круг"
    Circle.UPPER -> "Верхний круг"
}

data class DebtInput(
    val name: String,
    val balance: Long,
    val annualRatePercent: Double,
    val minPayment: Long,
    val extra: Long,
)

data class DebtMonth(val month: Int, val focus: String, val payment: Long, val remaining: Long)

enum class DebtStrategy { AVALANCHE, SNOWBALL }

fun amortize(debts: List<DebtInput>, strategy: DebtStrategy, limit: Int = 360): List<DebtMonth> {
    val state = debts.map { it.copy() }.toMutableList()
    val rows = mutableListOf<DebtMonth>()
    var month = 1
    while (state.any { it.balance > 0 } && month <= limit) {
        val focus = target(state, strategy) ?: break
        var payment = 0L
        state.forEachIndexed { index, debt ->
            if (debt.balance <= 0L) return@forEachIndexed
            val interest = (debt.balance.toDouble() * debt.annualRatePercent / 100.0 / 12.0).toLong()
            val extra = if (debt.name == focus.name) debt.extra else 0L
            val due = (debt.minPayment + extra).coerceAtMost(debt.balance + interest)
            val next = (debt.balance + interest - due).coerceAtLeast(0L)
            state[index] = debt.copy(balance = next)
            payment += due
        }
        rows += DebtMonth(month, focus.name, payment, state.sumOf { it.balance })
        month += 1
    }
    return rows
}

private fun target(debts: List<DebtInput>, strategy: DebtStrategy): DebtInput? {
    val open = debts.filter { it.balance > 0 }
    return when (strategy) {
        DebtStrategy.AVALANCHE -> open.maxByOrNull { it.annualRatePercent }
        DebtStrategy.SNOWBALL -> open.minByOrNull { it.balance }
    }
}

fun splitCoupon(inflow: Long, reinvest: Double, support: Double, crystal: Double): Triple<Long, Long, Long> {
    fun part(percent: Double) = (inflow.toDouble() * percent / 100.0).toLong()
    return Triple(part(reinvest), part(support), part(crystal))
}

fun profileMonths(profile: String): Int = when (profile) {
    "FREELANCE" -> 12
    "BUSINESS" -> 24
    else -> 6
}
