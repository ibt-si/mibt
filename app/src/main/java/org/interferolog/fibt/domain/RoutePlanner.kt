package org.interferolog.fibt.domain

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
