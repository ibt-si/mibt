// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.delay
import org.interferolog.mibt.JournalViewModel
import org.interferolog.mibt.activeAttempt
import org.interferolog.mibt.content.CALC_DISCLAIMER
import org.interferolog.mibt.currentWeekNumber
import org.interferolog.mibt.content.JournalCanon
import org.interferolog.mibt.content.POINT_C
import org.interferolog.mibt.domain.Letter
import org.interferolog.mibt.domain.RunwayZone
import org.interferolog.mibt.domain.carryingMinimum
import org.interferolog.mibt.domain.circleLabel
import org.interferolog.mibt.domain.circleOf
import org.interferolog.mibt.domain.cohesion
import org.interferolog.mibt.domain.echelonHours
import org.interferolog.mibt.domain.eta
import org.interferolog.mibt.domain.etaBand
import org.interferolog.mibt.domain.etaBandLabel
import org.interferolog.mibt.domain.familyRemainder
import org.interferolog.mibt.domain.fiZone
import org.interferolog.mibt.domain.fiZoneLabel
import org.interferolog.mibt.domain.formatHours
import org.interferolog.mibt.domain.formatRatio
import org.interferolog.mibt.domain.formatRubles
import org.interferolog.mibt.domain.futureValue
import org.interferolog.mibt.domain.hourlyRate
import org.interferolog.mibt.domain.profileMonths
import org.interferolog.mibt.domain.quadrant
import org.interferolog.mibt.domain.quadrantLabel
import org.interferolog.mibt.domain.resistanceByRates
import org.interferolog.mibt.domain.resistanceByShares
import org.interferolog.mibt.domain.routeTitle
import org.interferolog.mibt.domain.RouteKind
import org.interferolog.mibt.domain.routeExplanation
import org.interferolog.mibt.domain.runwayMonths
import org.interferolog.mibt.domain.runwayZone
import org.interferolog.mibt.domain.runwayZoneLabel
import org.interferolog.mibt.domain.splitCoupon
import org.interferolog.mibt.domain.weeklyTranche
import org.interferolog.mibt.domain.weeklyTrancheRemainder
import org.interferolog.mibt.domain.PhaseAnswers
import org.interferolog.mibt.navigation.CapitalRoute
import org.interferolog.mibt.navigation.GuideRoute
import org.interferolog.mibt.navigation.MaintenanceRoute
import org.interferolog.mibt.navigation.ClusterRoute
import org.interferolog.mibt.navigation.CrisisRoute
import org.interferolog.mibt.navigation.CrystalRoute
import org.interferolog.mibt.navigation.ExportRoute
import org.interferolog.mibt.navigation.FamilyRoute
import org.interferolog.mibt.navigation.FanRoute
import org.interferolog.mibt.navigation.HolterRoute
import org.interferolog.mibt.navigation.IntakeRoute
import org.interferolog.mibt.navigation.PriceRoute
import org.interferolog.mibt.navigation.RestRoute
import org.interferolog.mibt.navigation.SourcesRoute
import org.interferolog.mibt.navigation.WeekRoute
import org.interferolog.mibt.navigation.WorkRoute
import org.interferolog.mibt.navigation.open
import org.interferolog.mibt.navigation.reveal

@Composable
fun GuideScreen(onStartTest: (() -> Unit)?) {
    ScreenColumn {
        JournalCanon.howToUse.forEach { BodyText(it) }
        QuietCard(CALC_DISCLAIMER)
        MetaText("В приложении тот же порядок: тест → маршрут → неделя → бланк → расчёт. К этому тексту можно вернуться из маршрута и из штатного обслуживания.")
        if (onStartTest != null) {
            CommitButton("Пройти входной тест", onClick = onStartTest)
        }
    }
}

@Composable
fun IntakeScreen(viewModel: JournalViewModel, onRecorded: () -> Unit = {}) {
    val prior by viewModel.priorFullCycle.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var money by remember { mutableStateOf<Boolean?>(null) }
    var clear by remember { mutableStateOf<Boolean?>(null) }
    var answers by remember { mutableStateOf(List<Letter?>(7) { null }) }
    var submitted by remember { mutableStateOf(false) }
    val started = remember { System.currentTimeMillis() }
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            elapsed = System.currentTimeMillis() - started
            delay(1000)
        }
    }
    LaunchedEffect(submitted, ui.cycle?.id) {
        if (submitted && ui.cycle != null) onRecorded()
    }
    val answered = (if (money != null) 1 else 0) + (if (clear != null) 1 else 0) + answers.count { it != null }
    val complete = money != null && clear != null && answers.all { it != null }
    ScreenColumn {
        JournalCanon.testPreamble.forEach { BodyText(it) }
        Text("Прошло ${elapsed / 1000 / 60}:${"%02d".format((elapsed / 1000) % 60)}. Отвечено $answered из 9.", style = MaterialTheme.typography.bodyLarge)
        LinearProgressIndicator(
            progress = { (elapsed / 300_000f).coerceAtMost(1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (prior) BodyText("Полный цикл уже был пройден. Этот тест может открыть точечный ремонт.")
        SectionTitle("Часть 1. Признаки маршрута")
        MetaText("Выбор по факту, не по желанию. Пока ответ не отмечен, он никуда не засчитывается.")
        JournalCanon.partOne.forEachIndexed { index, prompt ->
            val value = if (index == 0) money else clear
            BodyText(prompt)
            ChoiceChips(
                options = listOf("YES" to "Да", "NO" to "Нет"),
                selected = when (value) {
                    true -> "YES"
                    false -> "NO"
                    null -> ""
                },
                onSelect = { picked ->
                    val answer = picked == "YES"
                    if (index == 0) money = answer else clear = answer
                },
            )
        }
        SectionTitle("Часть 2. Пункты теста")
        JournalCanon.items.forEachIndexed { index, item ->
            SectionTitle("${item.number}. ${item.prompt}")
            Letter.entries.forEach { letter ->
                val label = when (letter) {
                    Letter.A -> "А. ${item.a}"
                    Letter.B -> "Б. ${item.b}"
                    Letter.V -> "В. ${item.v}"
                }
                ListItem(
                    selected = answers[index] == letter,
                    onClick = { answers = answers.toMutableList().also { it[index] = letter } },
                    modifier = Modifier.fillMaxWidth(),
                    content = { Text(label) },
                )
            }
        }
        if (!complete) MetaText("Кнопка откроется, когда отмечены оба признака части 1 и один вариант в каждом из семи пунктов.")
        CommitButton("Записать маршрут", enabled = complete) {
            val moneyAnswer = money ?: return@CommitButton
            val clearAnswer = clear ?: return@CommitButton
            val letters = answers.map { it ?: return@CommitButton }
            submitted = true
            viewModel.submitIntake(moneyAnswer, clearAnswer, letters, elapsed)
        }
    }
}

@Composable
fun MapScreen(viewModel: JournalViewModel, backStack: NavBackStack<NavKey>) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycle = ui.cycle
    ScreenColumn {
        TonalButton("Как пользоваться журналом") { backStack.add(GuideRoute) }
        if (cycle == null) {
            BodyText("Маршрут появится после входного теста. Порядок: тест → маршрут → неделя → бланк → расчёт.")
            CommitButton("К тесту") { backStack.add(IntakeRoute) }
            return@ScreenColumn
        }
        val kind = runCatching { RouteKind.valueOf(cycle.routeKind) }.getOrNull()
        Text(kind?.let(::routeTitle) ?: cycle.routeKind, style = MaterialTheme.typography.headlineSmallEmphasized)
        routeReason(ui)?.let { QuietCard(it) }
        val current = ui.currentWeekNumber()
        if (current == null) {
            BodyText("Все недели этого маршрута пройдены. Дальше — штатное обслуживание: четыре ритма и пять маркеров.")
            CommitButton("К штатному обслуживанию") { backStack.add(MaintenanceRoute) }
        } else {
            BodyText("Сейчас неделя $current. ${JournalCanon.week(current).title}. Внутри неё пять блоков по порядку: шаги, бланк, расчёт, готовность, калибровка.")
            CommitButton("Открыть неделю $current") { backStack.add(WeekRoute(current)) }
        }
        val done = ui.attempts.filter { it.status == "COMPLETED" }.map { it.weekNumber }.toSet()
        val scheme = MaterialTheme.colorScheme
        cycle.weekOrder.split(",").mapNotNull { it.toIntOrNull() }.forEach { number ->
            val title = JournalCanon.week(number).title
            val passed = number in done
            val now = number == current
            ListItem(
                onClick = { backStack.add(WeekRoute(number)) },
                modifier = Modifier.fillMaxWidth(),
                leadingContent = {
                    Icon(
                        if (passed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                    )
                },
                supportingContent = {
                    Text(
                        when {
                            passed -> "Пройдена"
                            now -> "Сейчас. Дальше — шаги, бланк и расчёт"
                            else -> "Впереди"
                        },
                    )
                },
                colors = if (now) {
                    ListItemDefaults.colors(
                        containerColor = scheme.tertiaryContainer,
                        contentColor = scheme.onTertiaryContainer,
                        leadingContentColor = scheme.onTertiaryContainer,
                        supportingContentColor = scheme.onTertiaryContainer,
                    )
                } else if (passed) {
                    ListItemDefaults.colors(
                        containerColor = scheme.primaryContainer,
                        contentColor = scheme.onPrimaryContainer,
                        leadingContentColor = scheme.onPrimaryContainer,
                        supportingContentColor = scheme.onPrimaryContainer,
                    )
                } else {
                    ListItemDefaults.colors()
                },
                content = { Text("Неделя $number. $title") },
            )
        }
    }
}

@Composable
fun SilenceGateScreen(onDone: () -> Unit) {
    var left by remember { mutableIntStateOf(60) }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000)
            left -= 1
        }
    }
    ScreenColumn(clearFab = false) {
        HeroNumber(left.toString(), "секунд дыхания")
        BodyText(POINT_C)
        BodyText("Вес в стопах. Выдох длиннее вдоха. Расчёт откроется, когда минута пройдёт.")
        if (left == 0) CommitButton("Открыть расчёт", onClick = onDone)
    }
}

@Composable
fun WeekScreen(viewModel: JournalViewModel, number: Int, backStack: NavBackStack<NavKey>) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val canon = JournalCanon.week(number)
    val attempt = ui.activeAttempt(number)
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Шаги", "Бланк", "Расчёт", "Готовность", "Калибровка")
    val scroll = rememberScrollState()
    LaunchedEffect(tab) { scroll.scrollTo(0) }
    var answers by remember(attempt?.id) {
        mutableStateOf(List(3) { index -> ui.calibration.firstOrNull { it.attemptId == attempt?.id && it.questionIndex == index }?.text.orEmpty() })
    }
    Column(Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { index, label ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            WeekTab(viewModel, number, backStack, canon, attempt, ui, tab, answers, scroll, { answers = it }) { tab = it }
        }
    }
}

@Composable
private fun WeekTab(
    viewModel: JournalViewModel,
    number: Int,
    backStack: NavBackStack<NavKey>,
    canon: org.interferolog.mibt.content.WeekCanon,
    attempt: org.interferolog.mibt.data.WeekAttemptEntity?,
    ui: org.interferolog.mibt.data.JournalUi,
    tab: Int,
    answers: List<String>,
    scroll: androidx.compose.foundation.ScrollState,
    onAnswers: (List<String>) -> Unit,
    onTab: (Int) -> Unit,
) {
    ScreenColumn(scrollState = scroll) {
        MetaText("Неделя $number. ${pathHint(tab)}")
        when (tab) {
            0 -> {
                if (ui.currentWeekNumber() == number) routeReason(ui)?.let { QuietCard(it) }
                if (canon.lead.isNotBlank()) QuietCard(canon.lead)
                canon.steps.forEachIndexed { index, step -> BodyText("${index + 1}. $step") }
                TonalButton("Дальше: бланк") { onTab(1) }
            }
            1 -> {
                BodyText("Рабочий бланк этой недели. Свои цифры, не пример из главы. После записи откройте расчёт.")
                BlankLinks(viewModel, number, backStack)
                TonalButton("Дальше: расчёт") { onTab(2) }
            }
            2 -> {
                BodyText(canon.calculation)
                BodyText(liveCalculation(ui, number))
                InstrumentButton(viewModel, number, backStack)
                TonalButton("Дальше: готовность") { onTab(3) }
            }
            3 -> {
                canon.readiness.forEach { BodyText("• $it") }
                if (number == 3) {
                    SwitchRow("На холтер выпал отпуск", ui.cycle?.vacation == true, viewModel::setVacation)
                    MetaText("Заполненных дней: ${filledEvenings(ui)}. Порог: ${if (ui.cycle?.vacation == true) 21 else 14} дней, готовность — 12 из 14 вечеров.")
                }
                if (number == 5) {
                    MetaText("Дней с записанными часами: ${loggedHourDays(ui)} из 14.")
                }
                val ready = markerReady(ui, number)
                if (!ready) QuietCard(markerBlocked(ui, number), CardTone.Error)
                if (attempt != null) {
                    TonalButton(
                        text = if (attempt.markerConfirmed) "Маркер подтверждён" else "Маркер готовности есть",
                        onClick = { viewModel.confirmMarker(attempt.id) },
                        enabled = ready && !attempt.markerConfirmed,
                    )
                    OutlinedButton(onClick = { viewModel.repeatWeek(attempt) }) {
                        Text("Повторить неделю. Это штатный режим.")
                    }
                    CommitButton("Дальше по маршруту") { viewModel.advance(attempt) }
                    TonalButton("К письменным ответам") { onTab(4) }
                }
            }
            else -> {
                canon.calibration.forEachIndexed { index, question ->
                    BodyText("${index + 1}. $question")
                    Field(
                        label = "Ответ ${index + 1}",
                        value = answers.getOrElse(index) { "" },
                        onValue = { text ->
                            onAnswers(answers.toMutableList().also { it[index] = text })
                            attempt?.let { viewModel.saveCalibration(it.id, index, text) }
                        },
                        singleLine = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun BlankLinks(viewModel: JournalViewModel, number: Int, backStack: NavBackStack<NavKey>) {
    when (number) {
        1 -> {
            BodyText("Бланк 1. Дневник фазы. Запись паузы — кнопка «Пауза». Она не проходит через минуту тишины.")
            TonalButton("Открыть паузу") { backStack.reveal(org.interferolog.mibt.navigation.PauseRoute) }
        }
        2 -> {
            BodyText("Бланк 2а. Колодец или Точка C. Три напоминания ставятся здесь и остаются тихими.")
            TonalButton("Открыть Точку C") { backStack.reveal(org.interferolog.mibt.navigation.PointCRoute) }
            ReminderEditor(viewModel)
        }
        3 -> WeekLink("Бланк 2. Холтер четырёх узлов") { backStack.open(HolterRoute) }
        4 -> {
            WeekLink("Бланк 3. Индекс сопротивления") { backStack.open(SourcesRoute) }
            WeekLink("Тренажёр цены") { backStack.add(PriceRoute) }
        }
        5 -> {
            WeekLink("Бланк 3н. Дела участка") { backStack.add(WorkRoute) }
            WeekLink("Тренажёр цены") { backStack.add(PriceRoute) }
        }
        6 -> WeekLink("Бланк 4. Два контура") { backStack.add(FamilyRoute) }
        7 -> WeekLink("Бланк 5. Масса покоя") { backStack.open(RestRoute) }
        8 -> WeekLink("Бланк 6. Карта кризиса") { backStack.open(CrisisRoute) }
        9 -> WeekLink("Бланк 7. Карта веера") { backStack.add(FanRoute) }
        10 -> WeekLink("Бланк 8. Инвестиционный паспорт") { backStack.open(CapitalRoute) }
        11 -> WeekLink("Бланк 9. Карта кластера") { backStack.add(ClusterRoute) }
        12 -> WeekLink("Бланки 10 и 11. Кристалл смысла") { backStack.add(CrystalRoute) }
    }
}

private fun pathHint(tab: Int): String = when (tab) {
    0 -> "Сейчас шаги. Дальше — бланк."
    1 -> "Сейчас бланк. Дальше — расчёт."
    2 -> "Сейчас расчёт по формуле журнала. Дальше — критерий готовности."
    3 -> "Сейчас критерий готовности. К следующей неделе нужны три письменных ответа и подтверждение маркера. Если критерий не выполнен, неделю повторяют: это штатный режим."
    else -> "Сейчас Точка калибровки: три письменных ответа. Затем вернитесь к готовности и подтвердите маркер."
}

internal fun routeReason(ui: org.interferolog.mibt.data.JournalUi): String? {
    val cycle = ui.cycle ?: return null
    val test = ui.test ?: return null
    val letters = test.answers.split(",").mapNotNull { runCatching { Letter.valueOf(it.trim()) }.getOrNull() }
    if (letters.size != 7) return null
    return routeExplanation(PhaseAnswers(test.moneyCoversMonth, test.noOverdue, letters), cycle.fullCycleCompleted)
}

@Composable
private fun WeekLink(label: String, onClick: () -> Unit) {
    TonalButton(text = label, modifier = Modifier.fillMaxWidth(), onClick = onClick)
}

@Composable
private fun InstrumentButton(viewModel: JournalViewModel, number: Int, backStack: NavBackStack<NavKey>) {
    BlankLinks(viewModel, number, backStack)
}

private fun liveCalculation(ui: org.interferolog.mibt.data.JournalUi, number: Int): String {
    return when (number) {
        1 -> {
            val all = ui.diary.size
            val empty = ui.diary.count { it.impulse == "EMPTY" }
            if (all == 0) "Записей пока нет." else "Доля импульсов из пустоты: ${formatRatio(empty.toDouble() / all)}."
        }
        2 -> {
            val all = ui.wells.size
            val well = ui.wells.count { it.question == "WELL" }
            if (all == 0) "Записей пока нет." else "Доля решений из колодца: ${formatRatio(well.toDouble() / all)}."
        }
        3 -> etaLine(ui)
        4 -> sourceLine(ui)
        5 -> workLine(ui)
        6 -> familyLine(ui)
        7 -> restLine(ui)
        8 -> crisisLine(ui)
        9 -> fanLine(ui)
        10 -> passportLine(ui)
        11 -> clusterLine(ui)
        12 -> ringLine(ui)
        else -> ""
    }
}

private fun sourceLine(ui: org.interferolog.mibt.data.JournalUi): String {
    if (ui.sources.isEmpty()) return "Источников пока нет. S и Fᵢ появятся после первой строки."
    val rates = ui.sources.map { source ->
        hourlyRate(source.netIncomeKopecks, source.compensatoryNoiseKopecks, source.contractHours, source.frictionHours)
    }
    val known = rates.filterNotNull()
    val average = if (known.isEmpty()) null else known.average()
    return ui.sources.mapIndexed { index, source ->
        val rate = rates[index]
        val resistance = if (rate != null && average != null) resistanceByRates(source.frictionScore, average, rate) else null
        val rateText = rate?.let { "${formatRubles(it.toLong())}/ч" } ?: "—"
        val indexText = resistance?.let { "${"%.2f".format(it)}. ${fiZoneLabel(fiZone(it))}" } ?: "—"
        "${source.name}: S $rateText, Fᵢ $indexText"
    }.joinToString("\n")
}

private fun workLine(ui: org.interferolog.mibt.data.JournalUi): String {
    val header = "Дней с часами: ${loggedHourDays(ui)} из 14."
    if (ui.work.isEmpty()) return "$header Квадранты появятся после первого дела."
    val lines = ui.work.joinToString("\n") { item ->
        val yield = if (item.hoursShare == 0.0) 0.0 else item.resultShare / item.hoursShare
        val index = resistanceByShares(item.frictionScore, item.hoursShare, item.resultShare) ?: 0.0
        "${item.name}: ${quadrantLabel(quadrant(yield, item.frictionScore, index))}, Fᵢ ${"%.2f".format(index)}"
    }
    return "$header\n$lines"
}

private fun familyLine(ui: org.interferolog.mibt.data.JournalUi): String {
    val plan = ui.family ?: return "Два контура ещё не сохранены."
    val left = familyRemainder(plan.income1, plan.income2, plan.foundation)
    return "Фундамент ${formatRubles(plan.foundation)}, целевой фонд ${formatRubles(plan.targetFund)}, личные доли ${formatRubles(plan.personal1)} и ${formatRubles(plan.personal2)}. Остаток после фундамента ${formatRubles(left)}."
}

private fun restLine(ui: org.interferolog.mibt.data.JournalUi): String {
    val rest = ui.rest ?: return "Масса покоя ещё не записана."
    val minimum = carryingMinimum(rest.support, rest.tool, rest.obligations)
    val months = runwayMonths(rest.balance, minimum)
    val profile = profileMonths(rest.profile)
    val zone = months?.let { runwayLabel(runwayZone(it, profile), profile) } ?: "—"
    val runway = months?.let { "%.1f мес.".format(it) } ?: "—"
    return "Emin ${formatRubles(minimum)}. M = B / Emin = $runway. $zone. Первое сечение ${"%.0f".format(rest.firstCutPercent)}%."
}

private fun crisisLine(ui: org.interferolog.mibt.data.JournalUi): String {
    val map = ui.crisis ?: return "Карта кризиса ещё не сохранена."
    val tranche = weeklyTranche(map.emergencyMinimum)
    val remainder = weeklyTrancheRemainder(map.emergencyMinimum)
    val leftover = if (remainder == 0L) "" else " Остаток ${formatRubles(remainder)} остаётся в запасе."
    val open = ui.quarantines.any { it.endsAt > System.currentTimeMillis() }
    val gate = if (open) "Карантин на резкие решения ещё идёт." else "Открытого карантина нет."
    return "Аварийный минимум ${formatRubles(map.emergencyMinimum)}. Недельный транш ${formatRubles(tranche)}.$leftover $gate"
}

private fun fanLine(ui: org.interferolog.mibt.data.JournalUi): String {
    fun hours(echelon: String) = ui.branches.filter { it.echelon == echelon && !it.closed }.sumOf { it.hoursPerWeek }
    val total = hours("CORE") + hours("GROWTH") + hours("SEARCH")
    val base = if (total == 0.0) 40.0 else total
    return "Ядро ${formatHours(hours("CORE"))} ч при цели ${formatHours(echelonHours(base, 0.60))}, рост ${formatHours(hours("GROWTH"))} ч при цели ${formatHours(echelonHours(base, 0.30))}, поиск ${formatHours(hours("SEARCH"))} ч при цели ${formatHours(echelonHours(base, 0.10))}."
}

private fun passportLine(ui: org.interferolog.mibt.data.JournalUi): String {
    val passport = ui.passport ?: return "Паспорт ещё не сохранён."
    val half = passport.accountBalance / 2
    val illustrated = futureValue(passport.monthlyContribution, passport.annualRate, passport.years * 12)
    val split = splitCoupon(
        passport.monthlyContribution,
        passport.reinvestPercent,
        passport.supportPercent,
        passport.crystalPercent,
    )
    return "Просадка 50% оставляет ${formatRubles(half)}. Иллюстрация счёта ${formatRubles(illustrated.toLong())}. Делёж взноса: реинвест ${formatRubles(split.first)}, Опора ${formatRubles(split.second)}, Кристалл ${formatRubles(split.third)}."
}

private fun clusterLine(ui: org.interferolog.mibt.data.JournalUi): String {
    if (ui.people.isEmpty()) return "В кластере пока никого нет."
    return ui.people.joinToString("\n") { person ->
        val index = cohesion(person.transparency, person.word, person.coherence)
        "${person.name}: Kᵢ ${"%.1f".format(index)}, ${circleLabel(circleOf(index, person.transparency, person.word, person.coherence))}"
    }
}

private fun ringLine(ui: org.interferolog.mibt.data.JournalUi): String {
    if (ui.rings.isEmpty()) return "Кольца ещё не записаны."
    return ui.rings.joinToString("\n") { ring ->
        "Кольцо ${ring.ring}: ${formatHours(ring.hoursPerWeek)} ч, доля узла Рост ${"%.0f".format(ring.growthShare)}%."
    }
}

internal fun runwayTone(zone: RunwayZone): CardTone = when (zone) {
    RunwayZone.CRITICAL, RunwayZone.VULNERABLE -> CardTone.Error
    RunwayZone.BELOW_NORM -> CardTone.Tertiary
    else -> CardTone.Primary
}

internal fun runwayLabel(zone: RunwayZone, profileMonths: Int): String = runwayZoneLabel(zone, profileMonths)

private fun loggedHourDays(ui: org.interferolog.mibt.data.JournalUi): Int =
    ui.hours.map { it.day }.toSet().size

private fun markerReady(ui: org.interferolog.mibt.data.JournalUi, number: Int): Boolean = when (number) {
    3 -> filledEvenings(ui) >= 12
    5 -> loggedHourDays(ui) >= 14
    else -> true
}

private fun markerBlocked(ui: org.interferolog.mibt.data.JournalUi, number: Int): String = when (number) {
    3 -> "Маркер откроется после 12 заполненных вечеров. Сейчас ${filledEvenings(ui)}."
    5 -> "Маркер откроется после 14 дней с часами. Сейчас ${loggedHourDays(ui)}."
    else -> ""
}

internal fun etaLine(ui: org.interferolog.mibt.data.JournalUi): String {
    val spends = ui.spends
    fun sum(node: String) = spends.filter { it.node == node }.sumOf { it.amountKopecks }
    val ratio = eta(sum("U1"), sum("U2"), sum("U3"), sum("U4"))
    return if (ratio == null) "η появится после первой траты вне обязательств."
    else "η = ${formatRatio(ratio)}. ${etaBandLabel(etaBand(ratio))}. Обязательства ${formatRubles(sum("OBZ"))} в формулу не входят."
}

private fun filledEvenings(ui: org.interferolog.mibt.data.JournalUi): Int {
    return ui.spends.map { java.time.Instant.ofEpochMilli(it.atMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }.toSet().size
}

@Suppress("unused")
private val exportHint = ExportRoute
