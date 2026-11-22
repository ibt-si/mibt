// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.interferolog.mibt.JournalViewModel
import org.interferolog.mibt.activeAttempt
import org.interferolog.mibt.content.CALC_DISCLAIMER
import org.interferolog.mibt.content.BlankCanon
import org.interferolog.mibt.content.JournalCanon
import org.interferolog.mibt.content.POINT_C
import org.interferolog.mibt.content.WeekCanon
import org.interferolog.mibt.currentWeekNumber
import org.interferolog.mibt.data.JournalUi
import org.interferolog.mibt.data.WeekAttemptEntity
import org.interferolog.mibt.domain.EtaBand
import org.interferolog.mibt.domain.Letter
import org.interferolog.mibt.domain.PhaseAnswers
import org.interferolog.mibt.domain.RouteKind
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
import org.interferolog.mibt.domain.routeExplanation
import org.interferolog.mibt.domain.routeTitle
import org.interferolog.mibt.domain.runwayMonths
import org.interferolog.mibt.domain.runwayZone
import org.interferolog.mibt.domain.runwayZoneLabel
import org.interferolog.mibt.domain.splitCoupon
import org.interferolog.mibt.domain.weeklyTranche
import org.interferolog.mibt.domain.weeklyTrancheRemainder
import org.interferolog.mibt.navigation.CapitalRoute
import org.interferolog.mibt.navigation.ClusterRoute
import org.interferolog.mibt.navigation.CrisisRoute
import org.interferolog.mibt.navigation.CrystalRoute
import org.interferolog.mibt.navigation.FamilyRoute
import org.interferolog.mibt.navigation.FanRoute
import org.interferolog.mibt.navigation.HolterRoute
import org.interferolog.mibt.navigation.HomeRoute
import org.interferolog.mibt.navigation.IntakeRoute
import org.interferolog.mibt.navigation.MaintenanceRoute
import org.interferolog.mibt.navigation.PauseRoute
import org.interferolog.mibt.navigation.PointCRoute
import org.interferolog.mibt.navigation.PriceRoute
import org.interferolog.mibt.navigation.RestRoute
import org.interferolog.mibt.navigation.SourcesRoute
import org.interferolog.mibt.navigation.WeekRoute
import org.interferolog.mibt.navigation.WorkRoute
import org.interferolog.mibt.navigation.open
import org.interferolog.mibt.navigation.reveal
import org.interferolog.mibt.navigation.switchTopLevel
import java.time.Instant
import java.time.ZoneId

@Composable
fun GuideScreen(onStartTest: (() -> Unit)?) {
    val lines = JournalCanon.howToUse
    ScreenColumn {
        if (lines.isNotEmpty()) BodyText(lines.first())
        if (lines.size >= 6) {
            SectionCard {
                Text(lines[1], style = MaterialTheme.typography.titleMedium)
                lines.subList(2, 6).forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        ToneBadge("${index + 1}", CardTone.Secondary)
                        Box(modifier = Modifier.weight(1f)) {
                            BodyText(step)
                        }
                    }
                }
            }
        }
        if (lines.size >= 7) {
            QuietCard(lines[6], CardTone.Secondary)
        }
        QuietCard(CALC_DISCLAIMER, CardTone.Surface)
        MetaText("В приложении тот же порядок: тест → маршрут → неделя → бланк → расчёт.")
        if (onStartTest != null) {
            CommitButton("Пройти входной тест", onClick = onStartTest)
        }
    }
}

@Composable
fun IntakeScreen(viewModel: JournalViewModel, onRecorded: () -> Unit = {}) {
    val prior by viewModel.priorFullCycle.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var money by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var clear by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var answerCodes by rememberSaveable { mutableStateOf(List(7) { "" }) }
    var submitted by remember { mutableStateOf(false) }
    val started = rememberSaveable { System.currentTimeMillis() }
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            elapsed = (System.currentTimeMillis() - started).coerceAtLeast(0L)
            delay(1000)
        }
    }
    LaunchedEffect(submitted, ui.cycle?.id) {
        if (submitted && ui.cycle != null) onRecorded()
    }

    val answers = answerCodes.map { code -> Letter.entries.firstOrNull { it.name == code } }
    val answered = (if (money != null) 1 else 0) + (if (clear != null) 1 else 0) + answers.count { it != null }
    val complete = money != null && clear != null && answers.all { it != null }
    val scheme = MaterialTheme.colorScheme

    ScreenColumn {
        SectionCard {
            JournalCanon.testPreamble.forEach { BodyText(it) }
            if (prior) {
                QuietCard(
                    "Полный цикл уже был пройден. Этот тест может открыть точечный ремонт.",
                    CardTone.Secondary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Отвечено $answered из 9",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    MetaText("Время ${elapsed / 1000 / 60}:${"%02d".format((elapsed / 1000) % 60)}")
                }
                LinearProgressIndicator(
                    progress = { answered / 9f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SectionHeader(
            title = "Часть 1. Признаки маршрута",
            subtitle = "Выбор по факту, не по желанию. Пока ответ не отмечен, он никуда не засчитывается.",
        )
        JournalCanon.partOne.forEachIndexed { index, prompt ->
            val value = if (index == 0) money else clear
            SectionCard {
                Text(prompt, style = MaterialTheme.typography.titleMedium)
                ChoiceGroup(
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
        }

        SectionHeader("Часть 2. Пункты теста")
        JournalCanon.items.forEachIndexed { index, item ->
            SectionCard {
                Text(
                    "${item.number}. ${item.prompt}",
                    style = MaterialTheme.typography.titleMedium,
                )
                SegmentedGroup(count = Letter.entries.size) { optionIndex, shape ->
                    val letter = Letter.entries[optionIndex]
                    val label = when (letter) {
                        Letter.A -> "А. ${item.a}"
                        Letter.B -> "Б. ${item.b}"
                        Letter.V -> "В. ${item.v}"
                    }
                    val picked = answers[index] == letter
                    ListItem(
                        selected = picked,
                        onClick = {
                            answerCodes = answerCodes.toMutableList().also { it[index] = letter.name }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape),
                        leadingContent = {
                            RadioButton(selected = picked, onClick = null)
                        },
                        colors = if (picked) {
                            ListItemDefaults.colors(
                                containerColor = scheme.secondaryContainer,
                                contentColor = scheme.onSecondaryContainer,
                                leadingContentColor = scheme.onSecondaryContainer,
                            )
                        } else {
                            ListItemDefaults.colors(
                                containerColor = scheme.surfaceContainerLowest,
                            )
                        },
                        content = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                    )
                }
            }
        }

        if (!complete) {
            MetaText("Кнопка откроется, когда отмечены оба признака части 1 и один вариант в каждом из семи пунктов.")
        }
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
        if (cycle == null) {
            SectionCard {
                BodyText("Маршрут появится после входного теста. Порядок: тест → маршрут → неделя → бланк → расчёт.")
                CommitButton("К тесту") { backStack.reveal(IntakeRoute) }
            }
            return@ScreenColumn
        }
        val kind = runCatching { RouteKind.valueOf(cycle.routeKind) }.getOrNull()
        val order = cycle.weekOrder.split(",").mapNotNull { it.toIntOrNull() }
        val done = ui.attempts.filter { it.status == "COMPLETED" }.map { it.weekNumber }.toSet()
        val current = ui.currentWeekNumber()
        val completedCount = order.count { it in done }
        val progress = if (order.isEmpty()) 0f else completedCount.toFloat() / order.size

        SectionCard {
            Text(
                kind?.let(::routeTitle) ?: cycle.routeKind,
                style = MaterialTheme.typography.headlineSmallEmphasized,
            )
            routeReason(ui)?.let { MetaText(it) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Прогресс маршрута", style = MaterialTheme.typography.labelLarge)
                    MetaText("$completedCount из ${order.size}")
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (current == null) {
                BodyText("Все недели этого маршрута пройдены. Дальше — штатное обслуживание: четыре ритма и пять маркеров.")
                CommitButton("К штатному обслуживанию") { backStack.switchTopLevel(MaintenanceRoute) }
            } else {
                BodyText("Сейчас неделя $current. ${JournalCanon.week(current).title}.")
                CommitButton("Открыть неделю $current") { backStack.switchTopLevel(HomeRoute) }
            }
        }

        SectionHeader("Недели маршрута")
        val scheme = MaterialTheme.colorScheme
        SegmentedGroup(count = order.size) { index, shape ->
            val number = order[index]
            val title = JournalCanon.week(number).title
            val passed = number in done
            val now = number == current
            ListItem(
                onClick = {
                    if (now) backStack.switchTopLevel(HomeRoute) else backStack.reveal(WeekRoute(number))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape),
                leadingContent = {
                    Icon(
                        when {
                            now -> Icons.Filled.PlayArrow
                            passed -> Icons.Filled.CheckCircle
                            else -> Icons.Outlined.RadioButtonUnchecked
                        },
                        contentDescription = null,
                    )
                },
                supportingContent = {
                    Text(
                        when {
                            now -> "Сейчас. Дальше — шаги, бланк и расчёт"
                            passed -> "Пройдена"
                            else -> "Впереди"
                        },
                    )
                },
                colors = when {
                    now -> ListItemDefaults.colors(
                        containerColor = scheme.primaryContainer,
                        contentColor = scheme.onPrimaryContainer,
                        leadingContentColor = scheme.onPrimaryContainer,
                        supportingContentColor = scheme.onPrimaryContainer,
                    )
                    passed -> ListItemDefaults.colors(
                        containerColor = scheme.surfaceContainerLow,
                        contentColor = scheme.onSurface,
                        leadingContentColor = scheme.primary,
                        supportingContentColor = scheme.onSurfaceVariant,
                    )
                    else -> ListItemDefaults.colors(
                        containerColor = scheme.surfaceContainerLowest,
                        contentColor = scheme.onSurfaceVariant,
                        leadingContentColor = scheme.outline,
                        supportingContentColor = scheme.onSurfaceVariant,
                    )
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (60 - left) / 60f },
                    modifier = Modifier.size(164.dp),
                    strokeWidth = 6.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = left.toString(),
                        style = MaterialTheme.typography.displayMediumEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "секунд",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = POINT_C,
                style = MaterialTheme.typography.titleLargeEmphasized,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Вес в стопах. Выдох длиннее вдоха. Расчёт откроется, когда минута пройдёт.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            CommitButton(
                text = if (left == 0) "Открыть расчёт" else "Минута тишины ($left с)",
                enabled = left == 0,
                onClick = onDone,
            )
        }
    }
}

@Composable
fun WeekScreen(viewModel: JournalViewModel, number: Int, backStack: NavBackStack<NavKey>) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val canon = JournalCanon.week(number)
    val attempt = ui.activeAttempt(number)
    val tabs = listOf("Шаги", "Бланк", "Расчёт", "Готовность", "Калибровка")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    var answers by remember(attempt?.id) {
        mutableStateOf(
            List(3) { index ->
                ui.calibration.firstOrNull { it.attemptId == attempt?.id && it.questionIndex == index }?.text.orEmpty()
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 16.dp,
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(label) },
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            WeekTab(
                viewModel = viewModel,
                number = number,
                backStack = backStack,
                canon = canon,
                attempt = attempt,
                ui = ui,
                tab = page,
                answers = answers,
                onAnswers = { answers = it },
                onTab = { target -> scope.launch { pagerState.animateScrollToPage(target) } },
            )
        }
    }
}

@Composable
private fun WeekTab(
    viewModel: JournalViewModel,
    number: Int,
    backStack: NavBackStack<NavKey>,
    canon: WeekCanon,
    attempt: WeekAttemptEntity?,
    ui: JournalUi,
    tab: Int,
    answers: List<String>,
    onAnswers: (List<String>) -> Unit,
    onTab: (Int) -> Unit,
) {
    ScreenColumn {
        MetaText("Неделя $number. ${pathHint(tab)}")
        when (tab) {
            0 -> {
                if (ui.currentWeekNumber() == number) {
                    routeReason(ui)?.let { QuietCard(it, CardTone.Secondary) }
                }
                if (canon.lead.isNotBlank()) QuietCard(canon.lead, CardTone.Primary)
                SectionCard {
                    canon.steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            ToneBadge("${index + 1}", CardTone.Secondary)
                            Box(modifier = Modifier.weight(1f)) {
                                BodyText(step)
                            }
                        }
                    }
                }
                TonalButton("Дальше: бланк", modifier = Modifier.fillMaxWidth()) { onTab(1) }
            }
            1 -> {
                SectionCard {
                    BodyText("Рабочий бланк этой недели. Свои цифры, не пример из главы. После записи откройте расчёт.")
                    BlankLinks(viewModel, number, backStack, includeInlineEditor = true)
                }
                TonalButton("Дальше: расчёт", modifier = Modifier.fillMaxWidth()) { onTab(2) }
            }
            2 -> {
                QuietCard(canon.calculation, CardTone.Surface)
                LiveCalculationCard(ui, number)
                SectionCard {
                    MetaText("Чтобы обновить цифры, вернитесь к рабочему бланку недели.")
                    BlankLinks(viewModel, number, backStack, includeInlineEditor = false)
                }
                TonalButton("Дальше: готовность", modifier = Modifier.fillMaxWidth()) { onTab(3) }
            }
            3 -> {
                SectionCard {
                    canon.readiness.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                            Box(modifier = Modifier.weight(1f)) { BodyText(item) }
                        }
                    }
                }
                if (number == 3) {
                    val evenings = filledEvenings(ui)
                    val threshold = if (ui.cycle?.vacation == true) 21 else 14
                    SectionCard {
                        SwitchRow("На холтер выпал отпуск", ui.cycle?.vacation == true, viewModel::setVacation)
                        MetaText("Заполненных дней: $evenings. Порог: $threshold дней, готовность — 12 из 14 вечеров.")
                        LinearProgressIndicator(
                            progress = { (evenings / 12f).coerceAtMost(1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (number == 5) {
                    val days = loggedHourDays(ui)
                    SectionCard {
                        MetaText("Дней с записанными часами: $days из 14.")
                        LinearProgressIndicator(
                            progress = { (days / 14f).coerceAtMost(1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                val ready = markerReady(ui, number)
                if (!ready) QuietCard(markerBlocked(ui, number), CardTone.Error)
                if (attempt != null) {
                    SectionCard {
                        TonalButton(
                            text = if (attempt.markerConfirmed) "Маркер подтверждён" else "Маркер готовности есть",
                            enabled = ready && !attempt.markerConfirmed,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.confirmMarker(attempt.id) },
                        )
                        CommitButton("Дальше по маршруту") { viewModel.advance(attempt) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TonalButton(
                                text = "К ответам",
                                modifier = Modifier.weight(1f),
                                onClick = { onTab(4) },
                            )
                            OutlinedButton(
                                onClick = { viewModel.repeatWeek(attempt) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Повторить")
                            }
                        }
                        MetaText("Повторить неделю — это штатный режим.")
                    }
                }
            }
            else -> {
                val filled = answers.count { it.isNotBlank() }
                MetaText("Заполнено ответов: $filled из 3.")
                canon.calibration.forEachIndexed { index, question ->
                    SectionCard {
                        Text("${index + 1}. $question", style = MaterialTheme.typography.titleMedium)
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
                TonalButton("Вернуться к готовности", modifier = Modifier.fillMaxWidth()) { onTab(3) }
            }
        }
    }
}

@Composable
private fun LiveCalculationCard(ui: JournalUi, number: Int) {
    val tone = when (number) {
        3 -> {
            val ratio = currentEta(ui)
            if (ratio == null) CardTone.Surface else etaTone(etaBand(ratio))
        }
        7 -> {
            val rest = ui.rest
            val months = rest?.let { runwayMonths(it.balance, carryingMinimum(it.support, it.tool, it.obligations)) }
            val profile = rest?.let { profileMonths(it.profile) } ?: 6
            months?.let { runwayTone(runwayZone(it, profile)) } ?: CardTone.Surface
        }
        else -> CardTone.Secondary
    }
    QuietCard(liveCalculation(ui, number), tone)
}

@Composable
private fun BlankLinks(
    viewModel: JournalViewModel,
    number: Int,
    backStack: NavBackStack<NavKey>,
    includeInlineEditor: Boolean,
) {
    when (number) {
        1 -> {
            if (includeInlineEditor) {
                BodyText("Бланк 1. Дневник фазы. Запись паузы — кнопка «Пауза». Она не проходит через минуту тишины.")
            }
            WeekLink(BlankCanon.phaseDiary.title) { backStack.reveal(PauseRoute) }
        }
        2 -> {
            if (includeInlineEditor) {
                BodyText("Бланк 2а. Колодец или Точка C. Три напоминания ставятся здесь и остаются тихими.")
            }
            WeekLink(BlankCanon.pointC.title) { backStack.reveal(PointCRoute) }
            if (includeInlineEditor) {
                ReminderEditor(viewModel)
            }
        }
        3 -> WeekLink("Бланк 2. Холтер четырёх узлов") { backStack.open(HolterRoute) }
        4 -> {
            WeekLink("Бланк 3. Индекс сопротивления") { backStack.open(SourcesRoute) }
            WeekLink("Тренажёр цены") { backStack.reveal(PriceRoute) }
        }
        5 -> {
            WeekLink("Бланк 3н. Дела участка") { backStack.reveal(WorkRoute) }
            WeekLink("Тренажёр цены") { backStack.reveal(PriceRoute) }
        }
        6 -> WeekLink("Бланк 4. Два контура") { backStack.reveal(FamilyRoute) }
        7 -> WeekLink("Бланк 5. Масса покоя") { backStack.open(RestRoute) }
        8 -> WeekLink("Бланк 6. Карта кризиса") { backStack.open(CrisisRoute) }
        9 -> WeekLink("Бланк 7. Карта веера") { backStack.reveal(FanRoute) }
        10 -> WeekLink("Бланк 8. Инвестиционный паспорт") { backStack.open(CapitalRoute) }
        11 -> WeekLink("Бланк 9. Карта кластера") { backStack.reveal(ClusterRoute) }
        12 -> WeekLink("Бланки 10 и 11. Кристалл смысла") { backStack.reveal(CrystalRoute) }
    }
}

private fun pathHint(tab: Int): String = when (tab) {
    0 -> "Сейчас шаги. Дальше — бланк."
    1 -> "Сейчас бланк. Дальше — расчёт."
    2 -> "Сейчас расчёт по формуле журнала. Дальше — критерий готовности."
    3 -> "Сейчас критерий готовности. К следующей неделе нужны три письменных ответа и подтверждение маркера."
    else -> "Сейчас Точка калибровки: три письменных ответа. Затем вернитесь к готовности и подтвердите маркер."
}

internal fun routeReason(ui: JournalUi): String? {
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

internal fun impulseShareLine(entries: Int, empty: Int): String? {
    if (entries == 0) return null
    return "Доля импульсов из пустоты: ${formatRatio(empty.toDouble() / entries)}."
}

private fun liveCalculation(ui: JournalUi, number: Int): String {
    return when (number) {
        1 -> impulseShareLine(ui.diary.size, ui.diary.count { it.impulse == "EMPTY" }) ?: "Записей пока нет."
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

private fun sourceLine(ui: JournalUi): String {
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

private fun workLine(ui: JournalUi): String {
    val header = "Дней с часами: ${loggedHourDays(ui)} из 14."
    if (ui.work.isEmpty()) return "$header Квадранты появятся после первого дела."
    val lines = ui.work.joinToString("\n") { item ->
        val yield = if (item.hoursShare == 0.0) 0.0 else item.resultShare / item.hoursShare
        val index = resistanceByShares(item.frictionScore, item.hoursShare, item.resultShare) ?: 0.0
        "${item.name}: ${quadrantLabel(quadrant(yield, item.frictionScore, index))}, Fᵢ ${"%.2f".format(index)}"
    }
    return "$header\n$lines"
}

private fun familyLine(ui: JournalUi): String {
    val plan = ui.family ?: return "Два контура ещё не сохранены."
    val left = familyRemainder(plan.income1, plan.income2, plan.foundation)
    return "Фундамент ${formatRubles(plan.foundation)}, целевой фонд ${formatRubles(plan.targetFund)}, личные доли ${formatRubles(plan.personal1)} и ${formatRubles(plan.personal2)}. Остаток после фундамента ${formatRubles(left)}."
}

private fun restLine(ui: JournalUi): String {
    val rest = ui.rest ?: return "Масса покоя ещё не записана."
    val minimum = carryingMinimum(rest.support, rest.tool, rest.obligations)
    val months = runwayMonths(rest.balance, minimum)
    val profile = profileMonths(rest.profile)
    val zone = months?.let { runwayLabel(runwayZone(it, profile), profile) } ?: "—"
    val runway = months?.let { "%.1f мес.".format(it) } ?: "—"
    return "Emin ${formatRubles(minimum)}. M = B / Emin = $runway. $zone. Первое сечение ${"%.0f".format(rest.firstCutPercent)}%."
}

private fun crisisLine(ui: JournalUi): String {
    val map = ui.crisis ?: return "Карта кризиса ещё не сохранена."
    val tranche = weeklyTranche(map.emergencyMinimum)
    val remainder = weeklyTrancheRemainder(map.emergencyMinimum)
    val leftover = if (remainder == 0L) "" else " Остаток ${formatRubles(remainder)} остаётся в запасе."
    val open = ui.quarantines.any { it.endsAt > System.currentTimeMillis() }
    val gate = if (open) "Карантин на резкие решения ещё идёт." else "Открытого карантина нет."
    return "Аварийный минимум ${formatRubles(map.emergencyMinimum)}. Недельный транш ${formatRubles(tranche)}.$leftover $gate"
}

private fun fanLine(ui: JournalUi): String {
    fun hours(echelon: String) = ui.branches.filter { it.echelon == echelon && !it.closed }.sumOf { it.hoursPerWeek }
    val total = hours("CORE") + hours("GROWTH") + hours("SEARCH")
    val base = if (total == 0.0) 40.0 else total
    return "Ядро ${formatHours(hours("CORE"))} ч при цели ${formatHours(echelonHours(base, 0.60))}, рост ${formatHours(hours("GROWTH"))} ч при цели ${formatHours(echelonHours(base, 0.30))}, поиск ${formatHours(hours("SEARCH"))} ч при цели ${formatHours(echelonHours(base, 0.10))}."
}

private fun passportLine(ui: JournalUi): String {
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

private fun clusterLine(ui: JournalUi): String {
    if (ui.people.isEmpty()) return "В кластере пока никого нет."
    return ui.people.joinToString("\n") { person ->
        val index = cohesion(person.transparency, person.word, person.coherence)
        "${person.name}: Kᵢ ${"%.1f".format(index)}, ${circleLabel(circleOf(index, person.transparency, person.word, person.coherence))}"
    }
}

private fun ringLine(ui: JournalUi): String {
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

internal fun etaTone(band: EtaBand): CardTone = when (band) {
    EtaBand.OVERHEATED -> CardTone.Error
    EtaBand.WORKING -> CardTone.Primary
    EtaBand.PINCH -> CardTone.Tertiary
}

internal fun runwayLabel(zone: RunwayZone, profileMonths: Int): String = runwayZoneLabel(zone, profileMonths)

internal fun currentEta(ui: JournalUi): Double? {
    val spends = ui.spends
    fun sum(node: String) = spends.filter { it.node == node }.sumOf { it.amountKopecks }
    return eta(sum("U1"), sum("U2"), sum("U3"), sum("U4"))
}

private fun loggedHourDays(ui: JournalUi): Int =
    ui.hours.map { it.day }.toSet().size

private fun markerReady(ui: JournalUi, number: Int): Boolean = when (number) {
    3 -> filledEvenings(ui) >= 12
    5 -> loggedHourDays(ui) >= 14
    else -> true
}

private fun markerBlocked(ui: JournalUi, number: Int): String = when (number) {
    3 -> "Маркер откроется после 12 заполненных вечеров. Сейчас ${filledEvenings(ui)}."
    5 -> "Маркер откроется после 14 дней с часами. Сейчас ${loggedHourDays(ui)}."
    else -> ""
}

internal fun etaLine(ui: JournalUi): String {
    val spends = ui.spends
    fun sum(node: String) = spends.filter { it.node == node }.sumOf { it.amountKopecks }
    val ratio = eta(sum("U1"), sum("U2"), sum("U3"), sum("U4"))
    return if (ratio == null) "η появится после первой траты вне обязательств."
    else "η = ${formatRatio(ratio)}. ${etaBandLabel(etaBand(ratio))}. Обязательства ${formatRubles(sum("OBZ"))} в формулу не входят."
}

private fun filledEvenings(ui: JournalUi): Int {
    return ui.spends.map { Instant.ofEpochMilli(it.atMillis).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet().size
}
