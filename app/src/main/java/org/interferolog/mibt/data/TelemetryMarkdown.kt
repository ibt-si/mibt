package org.interferolog.mibt.data

import org.interferolog.mibt.domain.formatRubles
import org.interferolog.mibt.domain.routeTitle
import org.interferolog.mibt.domain.RouteKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TelemetrySnapshot(
    val cycle: CycleEntity?,
    val test: PhaseTestEntity?,
    val attempts: List<WeekAttemptEntity>,
    val calibration: List<CalibrationAnswerEntity>,
    val diary: List<PhaseDiaryEntryEntity>,
    val wells: List<WellEntryEntity>,
    val spends: List<SpendEntryEntity>,
    val bills: List<MirrorBillEntity>,
    val sources: List<IncomeSourceEntity>,
    val rehearsals: List<PriceRehearsalEntity>,
    val work: List<WorkItemEntity>,
    val hours: List<HourLogEntity>,
    val family: FamilyPlanEntity?,
    val rest: RestMassEntity?,
    val debts: List<DebtEntity>,
    val crisis: CrisisMapEntity?,
    val quarantines: List<QuarantineEntity>,
    val tranches: List<TrancheEntity>,
    val branches: List<FanBranchEntity>,
    val channels: List<FanChannelEntity>,
    val passport: InvestmentPassportEntity?,
    val people: List<ClusterPersonEntity>,
    val position: PositionAuditEntity?,
    val rings: List<MeaningRingEntity>,
    val maintenance: List<MaintenanceLogEntity>,
    val markers: List<MarkerCheckEntity>,
)

fun exportTelemetry(snapshot: TelemetrySnapshot, zone: ZoneId = ZoneId.systemDefault()): String {
    val time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone)
    fun at(millis: Long) = time.format(Instant.ofEpochMilli(millis))
    val lines = mutableListOf<String>()
    lines += "# Бортовой журнал — телеметрия"
    lines += ""
    lines += "Файл содержит суммы открытым текстом. Это читаемая копия журнала, не резервная копия базы."
    lines += ""
    val cycle = snapshot.cycle
    if (cycle == null) {
        lines += "Активного цикла нет."
        return lines.joinToString("\n")
    }
    val kind = runCatching { RouteKind.valueOf(cycle.routeKind) }.getOrNull()
    lines += "## Цикл"
    lines += "- Маршрут: ${kind?.let(::routeTitle) ?: cycle.routeKind}"
    lines += "- Порядок недель: ${cycle.weekOrder}"
    lines += "- Статус: ${cycle.status}"
    lines += "- Начат: ${at(cycle.startedAt)}"
    cycle.completedAt?.let { lines += "- Завершён: ${at(it)}" }
    lines += ""
    lines += "## Входной тест"
    val test = snapshot.test
    if (test == null) {
        lines += "Записей нет."
    } else {
        lines += "- Денег хватает на месяц базовой жизни: ${yesNo(test.moneyCoversMonth)}"
        lines += "- Просроченных обязательств нет: ${yesNo(test.noOverdue)}"
        lines += "- Ответы: ${test.answers}"
        lines += "- Длительность, минут: ${test.durationMillis / 60_000}"
    }
    lines += ""
    lines += "## Попытки недель"
    if (snapshot.attempts.isEmpty()) lines += "Записей нет."
    snapshot.attempts.forEach { attempt ->
        lines += "### Неделя ${attempt.weekNumber}, попытка ${attempt.attempt}"
        lines += "- Статус: ${attempt.status}"
        lines += "- Маркер готовности: ${yesNo(attempt.markerConfirmed)}"
        snapshot.calibration.filter { it.attemptId == attempt.id }.sortedBy { it.questionIndex }.forEach {
            lines += "- Калибровка ${it.questionIndex + 1}: ${it.text}"
        }
    }
    lines += ""
    lines += "## Бланк 1. Дневник фазы"
    tableOrEmpty(lines, snapshot.diary) { entry ->
        "| ${at(entry.atMillis)} | ${entry.amountKopecks?.let(::formatRubles) ?: "—"} | ${entry.impulse} | ${entry.bodyMarker} | ${entry.context} | ${entry.fact} |"
    }
    lines += ""
    lines += "## Бланк 2а. Колодец или Точка C"
    tableOrEmpty(lines, snapshot.wells) { entry ->
        "| ${at(entry.atMillis)} | ${entry.situation} | ${entry.question} | ${entry.spasm} | ${entry.pauseEffect} |"
    }
    lines += ""
    lines += "## Бланк 2. Холтер"
    tableOrEmpty(lines, snapshot.spends) { entry ->
        "| ${at(entry.atMillis)} | ${entry.node} | ${formatRubles(entry.amountKopecks)} | ${if (entry.cleanSheet) "чистый лист" else ""} | ${entry.note} |"
    }
    lines += ""
    lines += "## Зеркальные счета"
    tableOrEmpty(lines, snapshot.bills) { bill ->
        "| ${formatRubles(bill.amountKopecks)} | открыт ${at(bill.openedAt)} | ${bill.settledAt?.let { "перевод ${at(it)}" } ?: "открыт"} |"
    }
    lines += ""
    lines += "## Бланк 3. Источники"
    tableOrEmpty(lines, snapshot.sources) { source ->
        "| ${source.name} | ${formatRubles(source.netIncomeKopecks)} | ${source.contractHours}+${source.frictionHours} ч | R ${source.frictionScore} |"
    }
    lines += ""
    lines += "## Тренажёр цены"
    tableOrEmpty(lines, snapshot.rehearsals) { item ->
        "| ${at(item.createdAt)} | тишина ${yesNo(item.silenceHeld)} | ${item.outcome} |"
    }
    lines += ""
    lines += "## Бланк 3н и часы"
    tableOrEmpty(lines, snapshot.work) { item ->
        "| ${item.name} | ${item.mark} | D ${item.resultShare} | h ${item.hoursShare} | R ${item.frictionScore} |"
    }
    tableOrEmpty(lines, snapshot.hours) { item -> "| ${item.day} | ${item.hours} ч | ${item.note} |" }
    lines += ""
    lines += "## Бланк 4. Два контура"
    val family = snapshot.family
    if (family == null) lines += "Записей нет."
    else {
        lines += "- Доходы: ${formatRubles(family.income1)} и ${formatRubles(family.income2)}"
        lines += "- Фундамент: ${formatRubles(family.foundation)}"
        lines += "- Фонд / личные доли: ${formatRubles(family.targetFund)} / ${formatRubles(family.personal1)} / ${formatRubles(family.personal2)}"
        lines += "- Порог: ${formatRubles(family.threshold)}"
        lines += "- Фраза: ${family.phrase}"
        if (family.saturdayNote.isNotBlank()) lines += "- Субботняя сверка: ${family.saturdayNote}"
    }
    lines += ""
    lines += "## Бланк 5. Масса покоя"
    val rest = snapshot.rest
    if (rest == null) lines += "Записей нет."
    else {
        lines += "- Опора ${formatRubles(rest.support)}, Инструмент ${formatRubles(rest.tool)}, Обз ${formatRubles(rest.obligations)}"
        lines += "- Масса покоя: ${formatRubles(rest.balance)}"
        lines += "- Профиль: ${rest.profile}, первое сечение ${rest.firstCutPercent}%"
        lines += "- Счёт открыт: ${yesNo(rest.accountOpened)}, очередь: ${rest.strategy}"
    }
    tableOrEmpty(lines, snapshot.debts) { debt ->
        "| ${debt.name} | ${formatRubles(debt.balance)} | ${debt.ratePercent}% | минимум ${formatRubles(debt.minPayment)} |"
    }
    lines += ""
    lines += "## Бланк 6. Карта кризиса"
    val crisis = snapshot.crisis
    if (crisis == null) lines += "Записей нет."
    else {
        lines += "- Доступно: ${formatRubles(crisis.accessible)}"
        lines += "- Штатный минимум: ${formatRubles(crisis.staffMinimum)}"
        lines += "- Аварийный минимум: ${formatRubles(crisis.emergencyMinimum)}"
        lines += "- Отсечение: ${crisis.cutList}"
        lines += "- Кольцо опоры: ${crisis.supportRing}"
        lines += "- Быстрый контур: ${crisis.offers}"
        lines += "- Учебная тревога: ${yesNo(crisis.drillDone)}"
    }
    tableOrEmpty(lines, snapshot.quarantines) { item ->
        "| ${item.kind} | ${at(item.startedAt)} — ${at(item.endsAt)} | ${item.reason} |"
    }
    tableOrEmpty(lines, snapshot.tranches) { item ->
        "| ${at(item.atMillis)} | ${formatRubles(item.amount)} | запас ${item.runwayMonths} мес. |"
    }
    lines += ""
    lines += "## Бланк 7. Веер"
    tableOrEmpty(lines, snapshot.branches) { item ->
        "| ${item.echelon} | ${item.name} | ${item.hoursPerWeek} ч | горизонт ${item.horizonDays ?: "—"} | ${if (item.closed) "закрыта" else "открыта"} |"
    }
    tableOrEmpty(lines, snapshot.channels) { item -> "| ${item.echelon} ${item.slot} | ${item.failurePoint} |" }
    lines += ""
    lines += "## Бланк 8. Инвестиционный паспорт"
    val passport = snapshot.passport
    if (passport == null) lines += "Записей нет."
    else {
        lines += passport.massStatus
        lines += passport.contributionRule
        lines += passport.horizonRule
        lines += passport.allocation
        lines += passport.riskLimit
        lines += passport.stormRule
        lines += passport.hygiene
        lines += "- Счёт: ${formatRubles(passport.accountBalance)}, взнос ${formatRubles(passport.monthlyContribution)}"
        lines += "- Стресс-тест 50%: ${yesNo(passport.drawdownOk)}"
    }
    lines += ""
    lines += "## Бланк 9. Кластер"
    tableOrEmpty(lines, snapshot.people) { person ->
        "| ${person.name} | ${person.role} | ${person.transparency}/${person.word}/${person.coherence} | ${person.action} |"
    }
    lines += ""
    lines += "## Бланки 10 и 11"
    val position = snapshot.position
    if (position == null) lines += "Сверки позиции нет."
    else {
        lines += "- Дело: ${position.craft}"
        lines += "- Профицит: ${position.surplus}"
        lines += "- Люди: ${position.people}"
        lines += "- Горизонт: ${position.horizon}"
        lines += "- Время: ${position.timeFeel}"
        lines += "- Ответ: ${position.answer}"
    }
    tableOrEmpty(lines, snapshot.rings) { ring ->
        "| Кольцо ${ring.ring} | ${ring.what} | доля ${ring.growthShare} | ${ring.hoursPerWeek} ч | ${ring.firstStone} |"
    }
    lines += ""
    lines += "## Обслуживание"
    tableOrEmpty(lines, snapshot.maintenance) { item -> "| ${item.rhythm} | ${at(item.atMillis)} | ${item.note} |" }
    tableOrEmpty(lines, snapshot.markers) { item -> "| маркер ${item.marker} | ${yesNo(item.marked)} | ${at(item.atMillis)} |" }
    lines += ""
    return lines.joinToString("\n")
}

private fun yesNo(value: Boolean) = if (value) "да" else "нет"

private fun <T> tableOrEmpty(lines: MutableList<String>, rows: List<T>, render: (T) -> String) {
    if (rows.isEmpty()) lines += "Записей нет."
    else rows.forEach { lines += render(it) }
}
