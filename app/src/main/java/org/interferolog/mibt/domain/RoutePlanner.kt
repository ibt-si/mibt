package org.interferolog.mibt.domain

enum class Letter { A, B, V }

enum class RouteKind { BASIC, EMERGENCY, REPAIR_A, REPAIR_B, REPAIR_V }

data class PhaseAnswers(
    val moneyCoversMonth: Boolean,
    val noOverdue: Boolean,
    val items: List<Letter>,
)

data class PlannedRoute(val kind: RouteKind, val weeks: List<Int>)

fun planRoute(answers: PhaseAnswers, fullCycleCompleted: Boolean): PlannedRoute {
    require(answers.items.size == 7) { "В тесте семь пунктов." }
    if (!answers.moneyCoversMonth || !answers.noOverdue) {
        return PlannedRoute(
            RouteKind.EMERGENCY,
            listOf(1, 2, 8, 7, 3, 4, 5, 6, 9, 10, 11, 12),
        )
    }
    if (!fullCycleCompleted) {
        return PlannedRoute(RouteKind.BASIC, (1..12).toList())
    }
    val counts = answers.items.groupingBy { it }.eachCount()
    val a = counts[Letter.A] ?: 0
    val b = counts[Letter.B] ?: 0
    val v = counts[Letter.V] ?: 0
    val top = maxOf(a, b, v)
    val kind = when {
        a == top -> RouteKind.REPAIR_A
        b == top -> RouteKind.REPAIR_B
        else -> RouteKind.REPAIR_V
    }
    val weeks = when (kind) {
        RouteKind.REPAIR_A -> listOf(1, 2, 3, 4, 7)
        RouteKind.REPAIR_B -> listOf(3, 5, 6, 8, 9)
        RouteKind.REPAIR_V -> listOf(10, 11, 12)
        else -> error("Ремонтный маршрут не выбран.")
    }
    return PlannedRoute(kind, weeks)
}

fun routeExplanation(answers: PhaseAnswers, fullCycleCompleted: Boolean): String {
    val planned = planRoute(answers, fullCycleCompleted)
    val partOne = when {
        answers.moneyCoversMonth && answers.noOverdue ->
            "Оба признака части 1 — «да»: денег хватает на месяц базовой жизни и просроченных обязательств нет."
        !answers.moneyCoversMonth && !answers.noOverdue ->
            "Оба признака части 1 — «нет»."
        !answers.moneyCoversMonth ->
            "Первый признак части 1 — «нет»: денег не хватает на месяц базовой жизни."
        else ->
            "Второй признак части 1 — «нет»: есть просроченные обязательства."
    }
    return when (planned.kind) {
        RouteKind.BASIC ->
            "$partOne Полный цикл ещё не пройден, поэтому выбран маршрут 1. Базовый цикл: недели 1–12 подряд."
        RouteKind.EMERGENCY ->
            "$partOne Хотя бы один признак части 1 — «нет», поэтому выбран маршрут 2. Аварийный контур: недели 1 → 2 → 8 → 7, затем возврат к неделе 3 и дальше по порядку."
        else -> {
            val counts = answers.items.groupingBy { it }.eachCount()
            val a = counts[Letter.A] ?: 0
            val b = counts[Letter.B] ?: 0
            val v = counts[Letter.V] ?: 0
            val top = maxOf(a, b, v)
            val tied = listOf(a, b, v).count { it == top } > 1
            val set = when (planned.kind) {
                RouteKind.REPAIR_A -> "недели 1, 2, 3, 4, 7"
                RouteKind.REPAIR_B -> "недели 3, 5, 6, 8, 9"
                else -> "недели 10, 11, 12"
            }
            val letters = when (planned.kind) {
                RouteKind.REPAIR_A -> "«А»"
                RouteKind.REPAIR_B -> "«Б»"
                else -> "«В»"
            }
            val choice = if (tied) {
                "Число ведущих ответов совпало. Если есть сомнение между двумя маршрутами, выбирается более осторожный. Поэтому $set."
            } else {
                "Больше ответов $letters: $set."
            }
            "$partOne Полный цикл уже пройден, нужен ремонт. $choice"
        }
    }
}

fun routeTitle(kind: RouteKind): String = when (kind) {
    RouteKind.BASIC -> "Маршрут 1. Базовый цикл"
    RouteKind.EMERGENCY -> "Маршрут 2. Аварийный контур"
    RouteKind.REPAIR_A -> "Маршрут 3. Точечный ремонт, больше ответов А"
    RouteKind.REPAIR_B -> "Маршрут 3. Точечный ремонт, больше ответов Б"
    RouteKind.REPAIR_V -> "Маршрут 3. Точечный ремонт, больше ответов В"
}

fun repairWeekForMarker(marker: Int): Int = when (marker) {
    1 -> 1
    2 -> 3
    3 -> 4
    4 -> 7
    5 -> 12
    else -> 1
}
