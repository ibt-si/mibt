// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.ui

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.interferolog.mibt.JournalViewModel
import org.interferolog.mibt.content.BlankCanon
import org.interferolog.mibt.content.CALC_DISCLAIMER
import org.interferolog.mibt.content.JournalCanon
import org.interferolog.mibt.data.ClusterPersonEntity
import org.interferolog.mibt.data.CrisisDraft
import org.interferolog.mibt.data.DebtEntity
import org.interferolog.mibt.data.FamilyDraft
import org.interferolog.mibt.data.FanBranchEntity
import org.interferolog.mibt.data.FanChannelEntity
import org.interferolog.mibt.data.IncomeSourceEntity
import org.interferolog.mibt.data.PassportDraft
import org.interferolog.mibt.data.PositionDraft
import org.interferolog.mibt.data.RestDraft
import org.interferolog.mibt.data.RingDraft
import org.interferolog.mibt.data.HourLogEntity
import org.interferolog.mibt.data.WorkItemEntity
import org.interferolog.mibt.data.toDraft
import org.interferolog.mibt.domain.Circle
import org.interferolog.mibt.domain.DebtInput
import org.interferolog.mibt.domain.DebtStrategy
import org.interferolog.mibt.domain.amortize
import org.interferolog.mibt.domain.carryingMinimum
import org.interferolog.mibt.domain.circleLabel
import org.interferolog.mibt.domain.circleOf
import org.interferolog.mibt.domain.cohesion
import org.interferolog.mibt.domain.echelonHours
import org.interferolog.mibt.domain.familyRemainder
import org.interferolog.mibt.domain.fiZone
import org.interferolog.mibt.domain.fiZoneLabel
import org.interferolog.mibt.domain.SourceInput
import org.interferolog.mibt.domain.WorkInput
import org.interferolog.mibt.domain.formatHours
import org.interferolog.mibt.domain.formatIndex
import org.interferolog.mibt.domain.formatPerHour
import org.interferolog.mibt.domain.formatPoints
import org.interferolog.mibt.domain.formatRubles
import org.interferolog.mibt.domain.formatYield
import org.interferolog.mibt.domain.futureValue
import org.interferolog.mibt.domain.index
import org.interferolog.mibt.domain.rate
import org.interferolog.mibt.domain.sourceTotals
import org.interferolog.mibt.domain.withDecision
import org.interferolog.mibt.domain.workTotals
import org.interferolog.mibt.domain.parseRubles
import org.interferolog.mibt.domain.profileMonths
import org.interferolog.mibt.domain.quadrant
import org.interferolog.mibt.domain.quadrantLabel
import org.interferolog.mibt.domain.runwayMonths
import org.interferolog.mibt.domain.runwayZone
import org.interferolog.mibt.domain.splitCoupon
import org.interferolog.mibt.domain.weeklyTranche
import org.interferolog.mibt.domain.weeklyTrancheRemainder
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun circleTone(circle: Circle): CardTone = when (circle) {
    Circle.UPPER -> CardTone.Primary
    Circle.MIDDLE -> CardTone.Tertiary
    Circle.LOWER -> CardTone.Error
}

private fun echelonLabel(code: String): String = when (code) {
    "CORE" -> "Ядро"
    "GROWTH" -> "Рост"
    "SEARCH" -> "Поиск"
    else -> code
}

@Composable
fun SourcesScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id
    if (cycleId == null) {
        NoCycleNotice()
        return
    }
    var name by remember { mutableStateOf("") }
    var income by remember { mutableStateOf("") }
    var noise by remember { mutableStateOf("") }
    var contract by remember { mutableStateOf("") }
    var friction by remember { mutableStateOf("") }
    var dShare by remember { mutableStateOf("") }
    var hShare by remember { mutableStateOf("") }
    var score by remember { mutableIntStateOf(5) }
    var decision by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    val blank = BlankCanon.sources

    fun clearForm() {
        editingId = null
        name = ""
        income = ""
        noise = ""
        contract = ""
        friction = ""
        dShare = ""
        hShare = ""
        score = 5
        decision = ""
    }

    ScreenColumn {
        BlankHeader(blank)
        blank.notes.forEach { QuietCard(it, CardTone.Secondary) }
        SectionHeader(if (editingId == null) "Новый источник" else "Источник")
        SectionCard {
            Field("Источник", name, { name = it })
            Field("Чистый доход", income, { income = it }, number = true, suffix = "₽")
            Field("Компенсаторный Шум", noise, { noise = it }, number = true, suffix = "₽")
            Field("Часы по договору", contract, { contract = it }, number = true, suffix = "ч")
            Field("Часы трения", friction, { friction = it }, number = true, suffix = "ч")
            Field("Dᵢ, доля дохода", dShare, { dShare = it }, number = true, suffix = "%")
            Field("hᵢ, доля часов", hShare, { hShare = it }, number = true, suffix = "%")
            ScaleSlider("Rᵢ, сопротивление", score, { score = it })
            Field("Решение", decision, { decision = it }, singleLine = false)
            CommitButton(if (editingId == null) "Добавить источник" else "Сохранить") {
                viewModel.saveSource(
                    IncomeSourceEntity(
                        id = editingId ?: 0,
                        cycleId = cycleId,
                        name = name.ifBlank { "Источник" },
                        netIncomeKopecks = parseRubles(income) ?: 0,
                        compensatoryNoiseKopecks = parseRubles(noise) ?: 0,
                        contractHours = contract.toDoubleOrNull() ?: 0.0,
                        frictionHours = friction.toDoubleOrNull() ?: 0.0,
                        incomeShare = dShare.toDoubleOrNull() ?: 0.0,
                        hoursShare = hShare.toDoubleOrNull() ?: 0.0,
                        frictionScore = score,
                        decision = decision.trim(),
                    ),
                )
                clearForm()
            }
            if (editingId != null) {
                TonalButton("Отмена", modifier = Modifier.fillMaxWidth()) { clearForm() }
            }
        }
        val rows = ui.sources.map { it.toInput() }
        EntryList(
            columns = blank.columns,
            entries = ui.sources.mapIndexed { index, source ->
                EntryLine(source.id, sourceCells(index, source.toInput(), source.name, source.decision))
            },
            onDelete = viewModel::deleteSource,
            onEdit = { id ->
                val source = ui.sources.firstOrNull { it.id == id } ?: return@EntryList
                editingId = source.id
                name = source.name
                income = rublesField(source.netIncomeKopecks)
                noise = rublesField(source.compensatoryNoiseKopecks)
                contract = numberField(source.contractHours)
                friction = numberField(source.frictionHours)
                dShare = numberField(source.incomeShare)
                hShare = numberField(source.hoursShare)
                score = source.frictionScore
                decision = source.decision
            },
        )
        if (rows.isNotEmpty()) {
            ColumnCard(blank.rowLabels.single(), blank.columns, sourceTotalCells(blank, rows))
        }
    }
}

@Composable
fun PriceScreen(viewModel: JournalViewModel) {
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    if (ui.cycle == null) {
        NoCycleNotice()
        return
    }
    var recording by remember { mutableStateOf(false) }
    var silence by remember { mutableIntStateOf(-1) }
    var outcome by remember { mutableStateOf("") }
    var path by remember { mutableStateOf<String?>(null) }
    val recorder = remember { arrayOfNulls<MediaRecorder>(1) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder[0]?.stop() }
            runCatching { recorder[0]?.release() }
            recorder[0] = null
        }
    }
    LaunchedEffect(silence) {
        if (silence > 0) {
            delay(1000)
            silence -= 1
        }
    }

    ScreenColumn {
        QuietCard("Фраза с ценой, точка, десять секунд тишины. Диктофон не уходит с устройства.", CardTone.Secondary)
        SectionCard(tone = if (recording) CardTone.Error else CardTone.Surface) {
            if (recording) {
                ToneBadge("Идёт запись", CardTone.Error)
            }
            CommitButton(if (recording) "Точка" else "Начать дубль") {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    permission.launch(Manifest.permission.RECORD_AUDIO)
                    return@CommitButton
                }
                if (!recording) {
                    val file = File(context.filesDir, "rehearsals").apply { mkdirs() }
                        .resolve("${System.currentTimeMillis()}.m4a")
                    // MediaRecorder(Context) is only available from API 31; the no-arg
                    // constructor remains necessary on API 26-30 despite its deprecation.
                    @Suppress("DEPRECATION")
                    val media = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
                    media.setAudioSource(MediaRecorder.AudioSource.MIC)
                    media.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    media.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    media.setOutputFile(file.absolutePath)
                    media.prepare()
                    media.start()
                    recorder[0] = media
                    path = file.absolutePath
                    recording = true
                } else {
                    runCatching { recorder[0]?.stop() }
                    recorder[0]?.release()
                    recorder[0] = null
                    recording = false
                    silence = 10
                }
            }
        }
        if (silence >= 0) {
            MetricCard(
                value = silence.toString(),
                caption = "секунд тишины после точки",
                status = if (silence == 0) "Десять секунд выдержаны — зафиксируйте исход." else "Держите тишину.",
                tone = if (silence == 0) CardTone.Primary else CardTone.Tertiary,
            )
        }
        SectionCard {
            Field("Исход паузы", outcome, { outcome = it }, singleLine = false)
            ActionGroup(
                options = listOf(
                    "Тишина удержана" to {
                        path?.let {
                            viewModel.saveRehearsal(it, true, outcome)
                            outcome = ""
                            silence = -1
                        }
                    },
                    "После точки прозвучало слово" to {
                        path?.let {
                            viewModel.saveRehearsal(it, false, outcome)
                            outcome = ""
                            silence = -1
                        }
                    },
                ),
                enabled = path != null && silence == 0,
                stacked = true,
            )
        }
        val recent = ui.rehearsals.take(5)
        if (recent.isNotEmpty()) {
            SectionHeader("Последние дубли")
            SegmentedGroup(count = recent.size) { index, shape ->
                val item = recent[index]
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape),
                    leadingContent = {
                        ToneBadge(
                            text = if (item.silenceHeld) "Тишина" else "Слово",
                            tone = if (item.silenceHeld) CardTone.Primary else CardTone.Tertiary,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    content = { Text(item.outcome.ifBlank { "Без заметки" }) },
                )
            }
        }
    }
}

@Composable
fun WorkScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id
    if (cycleId == null) {
        NoCycleNotice()
        return
    }
    var name by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var score by remember { mutableIntStateOf(5) }
    var mark by remember { mutableStateOf("FRICTION") }
    var decision by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var day by remember { mutableStateOf(LocalDate.now().toString()) }
    var dayHours by remember { mutableStateOf("") }
    val blank = BlankCanon.work

    fun clearForm() {
        editingId = null
        name = ""
        result = ""
        hours = ""
        share = ""
        score = 5
        mark = "FRICTION"
        decision = ""
    }

    ScreenColumn {
        BlankHeader(blank)
        blank.referenceTables.forEach { ReferenceTable(it) }
        blank.notes.forEach { QuietCard(it, CardTone.Secondary) }
        SectionHeader(
            title = "Учёт часов",
            subtitle = "Записано дней с часами: ${ui.hours.map { it.day }.toSet().size} из 14.",
        )
        SectionCard {
            DatePickerField("День", day) { day = it }
            Field("Часы за день", dayHours, { dayHours = it }, number = true, suffix = "ч")
            CommitButton("Записать часы", enabled = dayHours.toDoubleOrNull() != null) {
                viewModel.saveHour(day, dayHours.toDoubleOrNull() ?: 0.0, "")
                dayHours = ""
            }
        }
        ui.hours.sortedBy { it.day }.forEach { entry ->
            ColumnCard(
                title = formatLoggedDay(entry.day),
                columns = hourColumns(entry),
                cells = hourCells(entry),
            )
        }
        SectionHeader(if (editingId == null) "Дело" else name.ifBlank { "Дело" })
        SectionCard {
            Field("Дело", name, { name = it })
            Field("Dᵢ, доля результата", result, { result = it }, number = true, suffix = "%")
            Field("Часы в месяц", hours, { hours = it }, number = true, suffix = "ч")
            Field("hᵢ, доля часов", share, { share = it }, number = true, suffix = "%")
            ScaleSlider("Rᵢ, сопротивление", score, { score = it })
            ChoiceGroup(
                options = listOf("CREATE" to "Созидание", "FRICTION" to "Трение"),
                selected = mark,
                onSelect = { mark = it },
            )
            Field("Решение", decision, { decision = it }, singleLine = false)
            CommitButton(if (editingId == null) "Добавить дело" else "Сохранить") {
                viewModel.saveWork(
                    WorkItemEntity(
                        id = editingId ?: 0,
                        cycleId = cycleId,
                        name = name.ifBlank { "Дело" },
                        resultShare = result.toDoubleOrNull() ?: 0.0,
                        hours = hours.toDoubleOrNull() ?: 0.0,
                        hoursShare = share.toDoubleOrNull() ?: 0.0,
                        frictionScore = score,
                        mark = mark,
                        decision = decision.trim(),
                    ),
                )
                clearForm()
            }
            if (editingId != null) {
                TonalButton("Отмена", modifier = Modifier.fillMaxWidth()) { clearForm() }
            }
        }
        val rows = ui.work.map { it.toInput() }
        EntryList(
            columns = blank.columns,
            entries = ui.work.map { item ->
                EntryLine(item.id, workCells(item.toInput(), item.name, item.mark, item.decision))
            },
            onDelete = viewModel::deleteWork,
            onEdit = { id ->
                val item = ui.work.firstOrNull { it.id == id } ?: return@EntryList
                editingId = item.id
                name = item.name
                result = numberField(item.resultShare)
                hours = numberField(item.hours)
                share = numberField(item.hoursShare)
                score = item.frictionScore
                mark = item.mark
                decision = item.decision
            },
        )
        if (rows.isNotEmpty()) {
            ColumnCard(blank.rowLabels.single(), blank.columns, workTotalCells(blank, rows))
        }
    }
}

private fun IncomeSourceEntity.toInput() = SourceInput(
    netIncomeKopecks = netIncomeKopecks,
    compensatoryNoiseKopecks = compensatoryNoiseKopecks,
    contractHours = contractHours,
    frictionHours = frictionHours,
    incomeShare = incomeShare,
    hoursShare = hoursShare,
    frictionScore = frictionScore,
)

private fun sourceCells(index: Int, source: SourceInput, name: String, decision: String): List<String> {
    val zone = source.index()?.let { fiZoneLabel(fiZone(it)) } ?: "—"
    return listOf(
        (index + 1).toString(),
        name.ifBlank { "—" },
        formatRubles(source.netIncomeKopecks),
        formatPoints(source.incomeShare),
        hoursAndShare(source.contractHours + source.frictionHours, source.hoursShare),
        source.rate()?.let(::formatPerHour) ?: "—",
        source.frictionScore.toString(),
        source.index()?.let(::formatIndex) ?: "—",
        withDecision(zone, decision),
    )
}

private fun sourceTotalCells(blank: BlankCanon, rows: List<SourceInput>): List<String> {
    val total = sourceTotals(rows)
    return listOf(
        blank.totalLabels[0],
        "—",
        formatRubles(total.netIncomeKopecks),
        printedShare(total.incomeShare, blank.totalLabels[1]),
        hoursAndShare(total.hours, total.hoursShare),
        total.averageRateKopecks?.let { "${blank.totalLabels[2]} ${formatPerHour(it)}" }
            ?: "${blank.totalLabels[2]} —",
        "—",
        "${blank.totalLabels[3]} ${total.rowsAtLeastTwo}",
        "—",
    )
}

private fun WorkItemEntity.toInput() = WorkInput(
    resultShare = resultShare,
    hours = hours,
    hoursShare = hoursShare,
    frictionScore = frictionScore,
)

private fun workCells(item: WorkInput, name: String, mark: String, decision: String): List<String> {
    val place = quadrant(item.yieldRatio, item.frictionScore, item.index)
    return listOf(
        name.ifBlank { "—" },
        formatPoints(item.resultShare),
        hoursAndShare(item.hours, item.hoursShare),
        item.frictionScore.toString(),
        formatYield(item.yieldRatio),
        formatIndex(item.index),
        markLabel(mark),
        withDecision(quadrantLabel(place), decision),
    )
}

private fun workTotalCells(blank: BlankCanon, rows: List<WorkInput>): List<String> {
    val total = workTotals(rows)
    return listOf(
        blank.totalLabels[0],
        printedShare(total.resultShare, blank.totalLabels[1]),
        hoursAndShare(total.hours, total.hoursShare),
        "—",
        "—",
        "—",
        "—",
        "—",
    )
}

private fun hoursAndShare(hours: Double, share: Double): String =
    "${formatHours(hours)} ч, ${formatPoints(share)}"

private fun printedShare(points: Double, printedHundred: String): String {
    val actual = formatPoints(points)
    return if (actual == printedHundred) printedHundred else actual
}

private fun markLabel(mark: String): String = when (mark) {
    "CREATE" -> "Созидание"
    "FRICTION" -> "Трение"
    else -> mark
}

private fun rublesField(kopecks: Long): String = formatRubles(kopecks).removeSuffix(" ₽")

private fun numberField(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

private val loggedDay: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private fun formatLoggedDay(iso: String): String =
    runCatching { LocalDate.parse(iso).format(loggedDay) }.getOrDefault(iso)

private fun hourColumns(entry: HourLogEntity): List<String> =
    if (entry.note.isBlank()) listOf("Часы за день") else listOf("Часы за день", "Заметка")

private fun hourCells(entry: HourLogEntity): List<String> =
    if (entry.note.isBlank()) listOf("${formatHours(entry.hours)} ч") else listOf("${formatHours(entry.hours)} ч", entry.note)

@Composable
fun FamilyScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id
    if (cycleId == null) {
        NoCycleNotice()
        return
    }
    val saved = ui.family
    var draft by remember(cycleId, saved) { mutableStateOf(saved?.toDraft() ?: FamilyDraft()) }
    var left by remember { mutableIntStateOf(0) }
    var purchase by remember { mutableStateOf("") }

    LaunchedEffect(left) {
        if (left > 0) {
            delay(1000)
            left -= 1
        }
    }
    val now = System.currentTimeMillis()
    val quarantine = ui.quarantines.firstOrNull { it.kind == "H24" && it.endsAt > now }
    val anyMoney = listOf(
        draft.income1,
        draft.income2,
        draft.foundation,
        draft.targetFund,
        draft.personal1,
        draft.personal2,
    ).any { it.isNotBlank() }

    ScreenColumn {
        QuietCard(JournalCanon.week(6).lead, CardTone.Secondary)
        MetaText("Обмен между двумя телефонами в эту версию не входит. Запись остаётся на этом устройстве.")

        SectionHeader("Поле боя или приборная панель")
        SegmentedGroup(count = JournalCanon.battlefield.size) { index, shape ->
            val row = JournalCanon.battlefield[index]
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape),
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MetaText("Поле боя: ${row.battle}")
                        Text("Приборная панель: ${row.panel}", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                content = { Text(row.parameter, style = MaterialTheme.typography.titleMedium) },
            )
        }

        SectionHeader(
            title = "Два контура",
            subtitle = "Общая Опора и обязательства не урезаются ни в одном споре. Общий Инструмент сверх порога ждёт 24 часа. Целевой фонд меняется только через спокойный разговор. Личные доли — без отчётов.",
        )
        SectionCard {
            Field("Доход 1", draft.income1, { draft = draft.copy(income1 = it) }, number = true, suffix = "₽")
            Field("Доход 2", draft.income2, { draft = draft.copy(income2 = it) }, number = true, suffix = "₽")
            Field("Фундамент Ф", draft.foundation, { draft = draft.copy(foundation = it) }, number = true, suffix = "₽")
            Field("Целевой фонд", draft.targetFund, { draft = draft.copy(targetFund = it) }, number = true, suffix = "₽")
            Field("Личная доля 1", draft.personal1, { draft = draft.copy(personal1 = it) }, number = true, suffix = "₽")
            Field("Личная доля 2", draft.personal2, { draft = draft.copy(personal2 = it) }, number = true, suffix = "₽")
            Field("Порог общей траты", draft.threshold, { draft = draft.copy(threshold = it) }, number = true, suffix = "₽")
            val f = parseRubles(draft.foundation) ?: 0
            val remainder = familyRemainder(parseRubles(draft.income1) ?: 0, parseRubles(draft.income2) ?: 0, f)
            if (anyMoney) {
                val sumCheck = f + (parseRubles(draft.targetFund) ?: 0) + (parseRubles(draft.personal1) ?: 0) + (parseRubles(draft.personal2) ?: 0)
                MetricCard(
                    value = formatRubles(remainder),
                    caption = "Свободный остаток С",
                    status = "Проверка: Ф + Ц + Л₁ + Л₂ = ${formatRubles(sumCheck)}",
                    tone = if (remainder < 0) CardTone.Error else CardTone.Primary,
                )
            } else {
                MetaText("Свободный остаток С появится, когда будут вписаны свои суммы. Пример из главы в поля не подставлен.")
            }
            CommitButton("Сохранить контуры") {
                viewModel.saveFamily(draft.toEntity(cycleId))
            }
        }

        SectionHeader("Субботняя сверка")
        SectionCard {
            TonalButton(
                text = if (left > 0) "Сверка: ${left / 60}:${"%02d".format(left % 60)}" else "Субботняя сверка, 15 минут",
                modifier = Modifier.fillMaxWidth(),
            ) {
                left = 15 * 60
            }
            JournalCanon.saturdayQuestions.forEachIndexed { idx, q ->
                BodyText("${idx + 1}. $q")
            }
            Field("Запись сверки", draft.saturdayNote, { draft = draft.copy(saturdayNote = it) }, singleLine = false)
            QuietCard("Фраза стоп-сигнала: «Я сейчас в сжатии».", CardTone.Secondary)
        }

        SectionHeader("Порог общей траты (24 часа)")
        SectionCard {
            Field("Общая трата сверх порога", purchase, { purchase = it }, number = true, suffix = "₽")
            if (quarantine != null) {
                QuietCard("Карантин 24 часа ещё идёт. Общая покупка сверх порога подождёт.", CardTone.Error)
            }
            TonalButton(
                text = "Проверить порог",
                modifier = Modifier.fillMaxWidth(),
                enabled = parseRubles(purchase) != null,
            ) {
                val amount = parseRubles(purchase) ?: return@TonalButton
                val limit = parseRubles(draft.threshold) ?: ui.family?.threshold ?: 0
                if (amount > limit && quarantine == null) {
                    viewModel.startQuarantine("H24", 24, "Общая трата ${formatRubles(amount)}")
                }
            }
        }
    }
}

@Composable
fun RestScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id
    if (cycleId == null) {
        NoCycleNotice()
        return
    }
    val saved = ui.rest
    var draft by remember(cycleId, saved) { mutableStateOf(saved?.toDraft() ?: RestDraft()) }
    var income by remember { mutableStateOf("") }
    var withdrawal by remember { mutableStateOf("") }
    var debtName by remember { mutableStateOf("") }
    var debtBalance by remember { mutableStateOf("") }
    var debtRate by remember { mutableStateOf("") }
    var debtMin by remember { mutableStateOf("") }
    var debtExtra by remember { mutableStateOf("") }

    ScreenColumn {
        val minimum = carryingMinimum(parseRubles(draft.support) ?: 0, parseRubles(draft.tool) ?: 0, parseRubles(draft.obligations) ?: 0)
        val months = runwayMonths(parseRubles(draft.balance) ?: 0, minimum)
        val target = profileMonths(draft.profile)
        val zone = months?.let { runwayZone(it, target) }

        MetricCard(
            value = months?.let { "%.1f мес.".format(it) } ?: "—",
            caption = "Запас хода · Emin ${formatRubles(minimum)}",
            status = zone?.let { runwayLabel(it, target) } ?: "Цель профиля: $target месяцев.",
            tone = zone?.let(::runwayTone) ?: CardTone.Surface,
        )

        SectionHeader("Масса покоя")
        SectionCard {
            Field("Опора U₁", draft.support, { draft = draft.copy(support = it) }, number = true, suffix = "₽")
            Field("Инструмент U₂", draft.tool, { draft = draft.copy(tool = it) }, number = true, suffix = "₽")
            Field("Обязательства", draft.obligations, { draft = draft.copy(obligations = it) }, number = true, suffix = "₽")
            Field("Масса покоя B", draft.balance, { draft = draft.copy(balance = it) }, number = true, suffix = "₽")
            Field(
                label = "Первое сечение",
                value = draft.firstCutPercent,
                onValue = { draft = draft.copy(firstCutPercent = it) },
                number = true,
                suffix = "%",
                supportingText = "1–5% при долгах; 10–20% без долгов.",
            )
            ChoiceGroup(
                options = listOf("HIRE" to "Найм", "FREELANCE" to "Фриланс", "BUSINESS" to "Бизнес"),
                selected = draft.profile,
                onSelect = { draft = draft.copy(profile = it) },
            )
            MetaText("Цель профиля: $target месяцев.")
            CommitButton("Записать массу покоя") {
                viewModel.saveRest(draft.toEntity(cycleId, saved))
            }
        }

        SectionHeader("Движение по счёту")
        SectionCard {
            Field("Приход", income, { income = it }, number = true, suffix = "₽")
            TonalButton(
                text = "Первое сечение с этого прихода",
                modifier = Modifier.fillMaxWidth(),
                enabled = parseRubles(income) != null,
            ) {
                parseRubles(income)?.let {
                    viewModel.applyFirstCut(it)
                    income = ""
                }
            }
            Field("Изъятие", withdrawal, { withdrawal = it }, number = true, suffix = "₽")
            OutlinedButton(
                onClick = {
                    parseRubles(withdrawal)?.let {
                        viewModel.withdraw(it)
                        withdrawal = ""
                    }
                },
                enabled = parseRubles(withdrawal) != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Изъятие из массы покоя")
            }
        }

        SectionHeader("Одна мишень")
        ChoiceGroup(
            options = listOf("AVALANCHE" to "Лавина", "SNOWBALL" to "Снежный ком"),
            selected = draft.strategy,
            onSelect = { draft = draft.copy(strategy = it) },
        )
        if (ui.debts.isNotEmpty()) {
            SegmentedGroup(count = ui.debts.size) { index, shape ->
                val debt = ui.debts[index]
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape),
                    supportingContent = {
                        Text(
                            "Остаток ${formatRubles(debt.balance)} · ${"%.1f".format(debt.ratePercent)}% · мин. ${formatRubles(debt.minPayment)}",
                        )
                    },
                    trailingContent = {
                        DeleteIconButton("Убрать ${debt.name}") { viewModel.deleteDebt(debt.id) }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    content = { Text(debt.name, style = MaterialTheme.typography.titleMedium) },
                )
            }
        }
        val schedule = amortize(
            ui.debts.map { DebtInput(it.name, it.balance, it.ratePercent, it.minPayment, it.extra) },
            if (draft.strategy == "SNOWBALL") DebtStrategy.SNOWBALL else DebtStrategy.AVALANCHE,
        )
        if (schedule.isNotEmpty()) {
            SectionCard {
                Text("График погашения (первые 6 мес.)", style = MaterialTheme.typography.titleMedium)
                schedule.take(6).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        MetaText("Мес. ${row.month} · ${row.focus}")
                        Text(formatRubles(row.remaining), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        SectionCard {
            Field("Долг", debtName, { debtName = it })
            Field("Остаток", debtBalance, { debtBalance = it }, number = true, suffix = "₽")
            Field("Ставка", debtRate, { debtRate = it }, number = true, suffix = "%")
            Field("Минимальный платёж", debtMin, { debtMin = it }, number = true, suffix = "₽")
            Field("Сверх минимума", debtExtra, { debtExtra = it }, number = true, suffix = "₽")
            TonalButton(
                text = "Добавить обязательство",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.saveDebt(
                        DebtEntity(
                            cycleId = cycleId,
                            name = debtName.ifBlank { "Долг" },
                            balance = parseRubles(debtBalance) ?: 0,
                            ratePercent = debtRate.toDoubleOrNull() ?: 0.0,
                            minPayment = parseRubles(debtMin) ?: 0,
                            extra = parseRubles(debtExtra) ?: 0,
                        ),
                    )
                    debtName = ""
                    debtBalance = ""
                    debtRate = ""
                    debtMin = ""
                    debtExtra = ""
                },
            )
        }
    }
}

@Composable
fun CrisisScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id
    if (cycleId == null) {
        NoCycleNotice()
        return
    }
    val saved = ui.crisis
    var draft by remember(cycleId, saved) { mutableStateOf(saved?.toDraft() ?: CrisisDraft()) }
    var tranche by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val quarantine = ui.quarantines.firstOrNull { it.kind == "H72" && it.endsAt > now }
    val emergencyValue = parseRubles(draft.emergencyMinimum) ?: 0
    val profile = profileMonths(ui.rest?.profile ?: "HIRE")
    val runway = runwayMonths(parseRubles(draft.accessible) ?: 0, emergencyValue)
    val crisisZone = runway?.let { runwayZone(it, profile) }
    val trancheRemainder = weeklyTrancheRemainder(emergencyValue)
    val trancheNote = if (trancheRemainder == 0L) "" else " Остаток ${formatRubles(trancheRemainder)} остаётся в запасе."

    ScreenColumn {
        MetricCard(
            value = runway?.let { "%.0f дн.".format(it * 30) } ?: "—",
            caption = "Аварийный запас хода",
            status = (crisisZone?.let { "${runwayLabel(it, profile)} · " }.orEmpty()) +
                "Недельный транш: ${formatRubles(weeklyTranche(emergencyValue))}.$trancheNote",
            tone = crisisZone?.let(::runwayTone) ?: CardTone.Surface,
        )

        SectionHeader("Карта кризиса")
        SectionCard {
            Field("Масса покоя в доступе", draft.accessible, { draft = draft.copy(accessible = it) }, number = true, suffix = "₽")
            Field("Штатный минимум", draft.staffMinimum, { draft = draft.copy(staffMinimum = it) }, number = true, suffix = "₽")
            Field("Аварийный минимум", draft.emergencyMinimum, { draft = draft.copy(emergencyMinimum = it) }, number = true, suffix = "₽")
            Field("Список отсечения", draft.cutList, { draft = draft.copy(cutList = it) }, singleLine = false)
            Field("Кольцо опоры", draft.supportRing, { draft = draft.copy(supportRing = it) }, singleLine = false)
            Field("Три предложения рынку", draft.offers, { draft = draft.copy(offers = it) }, singleLine = false)
            CommitButton("Сохранить карту") {
                viewModel.saveCrisis(draft.toEntity(cycleId))
            }
        }

        SectionHeader("Учебная тревога и карантин")
        SectionCard {
            if (ui.crisis?.drillDone == true) {
                ToneBadge("Учебная тревога 24 ч проведена", CardTone.Primary)
            }
            OutlinedButton(
                onClick = {
                    val marked = draft.copy(drillDone = true)
                    draft = marked
                    viewModel.saveCrisis(marked.toEntity(cycleId))
                },
                enabled = ui.crisis?.drillDone != true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (ui.crisis?.drillDone == true) "Учебная тревога отмечена" else "Учебная тревога на 24 часа проведена",
                )
            }
            if (quarantine != null) {
                QuietCard("72 часа без необратимых решений. Карантин ещё идёт.", CardTone.Error)
            }
            TonalButton(
                text = "Начать карантин 72 часа",
                modifier = Modifier.fillMaxWidth(),
            ) {
                viewModel.startQuarantine("H72", 72, "Карантин на резкие решения")
            }
        }

        SectionHeader("Недельные транши")
        SectionCard {
            Field("Недельный транш", tranche, { tranche = it }, number = true, suffix = "₽")
            TonalButton(
                text = "Записать транш",
                modifier = Modifier.fillMaxWidth(),
                enabled = parseRubles(tranche) != null,
            ) {
                val amount = parseRubles(tranche) ?: return@TonalButton
                viewModel.saveTranche(amount, runway ?: 0.0)
                tranche = ""
            }
        }
        if (ui.tranches.isNotEmpty()) {
            SegmentedGroup(count = ui.tranches.size) { index, shape ->
                val item = ui.tranches[index]
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape),
                    supportingContent = { Text("Запас ${"%.1f".format(item.runwayMonths)} мес.") },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    content = { Text(formatRubles(item.amount), style = MaterialTheme.typography.titleMedium) },
                )
            }
        }
    }
}

@Composable
fun FanScreen(viewModel: JournalViewModel) {
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id
    if (cycleId == null) {
        NoCycleNotice()
        return
    }
    var weekHours by remember { mutableStateOf("40") }
    var name by remember { mutableStateOf("") }
    var echelon by remember { mutableStateOf("CORE") }
    var hours by remember { mutableStateOf("") }
    var horizon by remember { mutableStateOf("60") }
    var signals by remember { mutableStateOf("") }
    var failure by remember { mutableStateOf("") }
    var slot by remember { mutableStateOf("1") }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) insertFanHold(context, weekHours.toDoubleOrNull() ?: 40.0)
    }

    ScreenColumn {
        SectionHeader("Эшелоны недели")
        SectionCard {
            Field("Рабочие часы недели", weekHours, { weekHours = it }, number = true, suffix = "ч")
            val total = weekHours.toDoubleOrNull() ?: 0.0
            MetaText(
                "Ядро ${formatHours(echelonHours(total, 0.60))} ч · Рост ${formatHours(echelonHours(total, 0.30))} ч · Поиск ${formatHours(echelonHours(total, 0.10))} ч",
            )
            TonalButton("Поставить часы веера в календарь", modifier = Modifier.fillMaxWidth()) {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                if (granted) insertFanHold(context, total) else calendarPermission.launch(Manifest.permission.WRITE_CALENDAR)
            }
        }

        SectionHeader("Ветви дохода")
        SectionCard {
            ChoiceGroup(
                options = listOf("CORE" to "Ядро", "GROWTH" to "Рост", "SEARCH" to "Поиск"),
                selected = echelon,
                onSelect = { echelon = it },
            )
            Field("Направление", name, { name = it })
            Field("Часы в неделю", hours, { hours = it }, number = true, suffix = "ч")
            if (echelon != "CORE") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Горизонт тишины, суток", style = MaterialTheme.typography.labelLarge)
                    ChoiceGroup(
                        options = listOf("30" to "30 сут.", "60" to "60 сут.", "90" to "90 сут."),
                        selected = horizon,
                        onSelect = { horizon = it },
                    )
                }
            }
            Field("Три опережающих сигнала", signals, { signals = it }, singleLine = false)
            CommitButton("Добавить ветвь") {
                val days = horizon.toIntOrNull()
                val review = days?.let { System.currentTimeMillis() + it * 86_400_000L }
                viewModel.saveBranch(
                    FanBranchEntity(
                        cycleId = cycleId,
                        echelon = echelon,
                        name = name.ifBlank { "Ветвь" },
                        hoursPerWeek = hours.toDoubleOrNull() ?: 0.0,
                        horizonDays = if (echelon == "CORE") null else days,
                        reviewAt = if (echelon == "CORE") null else review,
                        signals = signals,
                        closed = false,
                    ),
                )
                name = ""
                hours = ""
                signals = ""
            }
        }
        if (ui.branches.isNotEmpty()) {
            SegmentedGroup(count = ui.branches.size) { index, shape ->
                val branch = ui.branches[index]
                val locked = branch.reviewAt != null && branch.reviewAt > System.currentTimeMillis() && !branch.closed
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape),
                    leadingContent = {
                        ToneBadge(echelonLabel(branch.echelon), CardTone.Secondary)
                    },
                    supportingContent = {
                        Text(
                            "${formatHours(branch.hoursPerWeek)} ч/нед." +
                                if (branch.closed) " · закрыта" else if (locked) " · ревизия впереди" else "",
                        )
                    },
                    trailingContent = if (!branch.closed) {
                        {
                            OutlinedButton(
                                onClick = { viewModel.closeBranch(branch.id) },
                                enabled = !locked,
                            ) {
                                Text("Закрыть")
                            }
                        }
                    } else null,
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    content = { Text(branch.name, style = MaterialTheme.typography.titleMedium) },
                )
            }
        }

        SectionHeader("Каналы и точки отказа")
        SectionCard {
            Field("Точка отказа канала", failure, { failure = it })
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Номер канала", style = MaterialTheme.typography.labelLarge)
                ChoiceGroup(
                    options = listOf("1" to "Канал 1", "2" to "Канал 2", "3" to "Канал 3"),
                    selected = slot,
                    onSelect = { slot = it },
                )
            }
            TonalButton("Добавить канал", modifier = Modifier.fillMaxWidth()) {
                viewModel.saveChannel(
                    FanChannelEntity(
                        cycleId = cycleId,
                        echelon = echelon,
                        slot = slot.toIntOrNull() ?: 1,
                        failurePoint = failure,
                    ),
                )
                failure = ""
            }
        }
        val duplicated = ui.channels
            .groupBy { it.failurePoint.trim().lowercase() }
            .filter { it.key.isNotBlank() && it.value.size > 1 }
            .keys
        if (ui.channels.isNotEmpty()) {
            SegmentedGroup(count = ui.channels.size) { index, shape ->
                val channel = ui.channels[index]
                val same = channel.failurePoint.trim().lowercase() in duplicated
                val scheme = MaterialTheme.colorScheme
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape),
                    leadingContent = {
                        ToneBadge(
                            "${echelonLabel(channel.echelon)} ${channel.slot}",
                            if (same) CardTone.Error else CardTone.Secondary,
                        )
                    },
                    supportingContent = if (same) {
                        { Text("Совпадает с другим каналом — это один канал.") }
                    } else null,
                    colors = if (same) {
                        ListItemDefaults.colors(
                            containerColor = scheme.errorContainer,
                            contentColor = scheme.onErrorContainer,
                            supportingContentColor = scheme.onErrorContainer,
                        )
                    } else {
                        ListItemDefaults.colors(containerColor = scheme.surfaceContainerLow)
                    },
                    content = { Text(channel.failurePoint.ifBlank { "Без точки отказа" }) },
                )
            }
        }
    }
}

private fun insertFanHold(context: android.content.Context, weekHours: Double) {
    val calendarId = context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        arrayOf(CalendarContract.Calendars._ID),
        null,
        null,
        null,
    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null } ?: return
    val zone = ZoneId.systemDefault()
    val start = LocalDate.now().plusDays(1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
    listOf(
        "MiBT ядро" to echelonHours(weekHours, 0.60),
        "MiBT рост" to echelonHours(weekHours, 0.30),
        "MiBT поиск" to echelonHours(weekHours, 0.10),
    ).forEach { (title, hours) ->
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, start + (hours * 3_600_000L).toLong())
            put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
        }
        context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
    }
}

@Composable
fun PassportScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id
    if (cycleId == null) {
        NoCycleNotice()
        return
    }
    val saved = ui.passport
    var draft by remember(cycleId, saved) { mutableStateOf(saved?.toDraft() ?: PassportDraft()) }
    var coupon by remember { mutableStateOf("") }

    ScreenColumn {
        QuietCard(CALC_DISCLAIMER, CardTone.Tertiary)
        QuietCard(JournalCanon.passportExample, CardTone.Surface)
        MetaText("Поля ниже пустые до первой записи. Пример из главы в них не подставляется и прогнозом не является.")

        SectionHeader("Семь полей паспорта")
        SectionCard {
            JournalCanon.passportFields.forEachIndexed { index, label ->
                Field(label, draft.textAt(index), { draft = draft.withText(index, it) }, singleLine = false)
            }
        }

        SectionHeader("Иллюстрация сложного процента")
        SectionCard {
            Field("Счёт", draft.accountBalance, { draft = draft.copy(accountBalance = it) }, number = true, suffix = "₽")
            Field("Взнос в месяц", draft.monthlyContribution, { draft = draft.copy(monthlyContribution = it) }, number = true, suffix = "₽")
            Field("Доходность в год", draft.annualRatePercent, { draft = draft.copy(annualRatePercent = it) }, number = true, suffix = "%")
            Field("Срок", draft.years, { draft = draft.copy(years = it) }, integer = true, suffix = "лет")
            val paymentKop = parseRubles(draft.monthlyContribution)
            val rateValue = draft.annualRatePercent.toDoubleOrNull()
            val yearValue = draft.years.toIntOrNull()
            if (draft.monthlyContribution.isNotBlank() && draft.annualRatePercent.isNotBlank() && draft.years.isNotBlank() &&
                paymentKop != null && rateValue != null && yearValue != null && yearValue > 0
            ) {
                val value = futureValue(paymentKop, rateValue / 100.0, yearValue * 12)
                MetricCard(
                    value = formatRubles(value.toLong()),
                    caption = "Иллюстрация счёта к концу срока по введённым полям",
                    status = "Иллюстрация расчёта, не прогноз и не рекомендация.",
                    tone = CardTone.Primary,
                )
            } else {
                MetaText("Впишите взнос, доходность и срок — тогда появится иллюстрация по вашим полям. Пока они пустые, крупной цифры нет.")
            }
        }

        SectionHeader("Стресс-тест переборки")
        SectionCard {
            val half = (parseRubles(draft.accountBalance) ?: 0) / 2
            if (draft.accountBalance.isNotBlank()) {
                BodyText("Просадка 50% оставляет на счёте ${formatRubles(half)}. Опора, рабочие инструменты и сон при этом остаются отдельной Массой покоя.")
            }
            SwitchRow("Переборка выдержала стресс-тест", draft.drawdownOk) { draft = draft.copy(drawdownOk = it) }
        }

        SectionHeader(
            title = "Правило купонного притока",
            subtitle = "Доли реинвеста, Опоры и Кристалла журнал числами не задаёт. Впишите своё правило.",
        )
        SectionCard {
            Field("Купонный приток", coupon, { coupon = it }, number = true, suffix = "₽")
            Field("Реинвест", draft.reinvestPercent, { draft = draft.copy(reinvestPercent = it) }, number = true, suffix = "%")
            Field("В Опору", draft.supportPercent, { draft = draft.copy(supportPercent = it) }, number = true, suffix = "%")
            Field("В Кристалл", draft.crystalPercent, { draft = draft.copy(crystalPercent = it) }, number = true, suffix = "%")
            val couponKop = parseRubles(coupon)
            if (coupon.isNotBlank() && couponKop != null && listOf(draft.reinvestPercent, draft.supportPercent, draft.crystalPercent).all { it.isNotBlank() }) {
                val split = splitCoupon(
                    couponKop,
                    draft.reinvestPercent.toDoubleOrNull() ?: 0.0,
                    draft.supportPercent.toDoubleOrNull() ?: 0.0,
                    draft.crystalPercent.toDoubleOrNull() ?: 0.0,
                )
                QuietCard(
                    "Реинвест ${formatRubles(split.first)}, Опора ${formatRubles(split.second)}, Кристалл ${formatRubles(split.third)}.",
                    CardTone.Secondary,
                )
            }
        }

        CommitButton("Сохранить паспорт") {
            viewModel.savePassport(draft.toEntity(cycleId))
        }
    }
}

@Composable
fun ClusterScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id
    if (cycleId == null) {
        NoCycleNotice()
        return
    }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var transparency by remember { mutableStateOf<Int?>(null) }
    var word by remember { mutableStateOf<Int?>(null) }
    var gain by remember { mutableStateOf<Int?>(null) }
    var action by remember { mutableStateOf("") }

    ScreenColumn {
        if (ui.people.isNotEmpty()) {
            SectionHeader("Люди контура")
            SegmentedGroup(count = ui.people.size) { index, shape ->
                val person = ui.people[index]
                val ki = cohesion(person.transparency, person.word, person.coherence)
                val circle = circleOf(ki, person.transparency, person.word, person.coherence)
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape),
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ToneBadge(circleLabel(circle), circleTone(circle))
                                MetaText("Kᵢ ${"%.1f".format(ki)}")
                            }
                            if (person.action.isNotBlank()) MetaText(person.action)
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    content = {
                        Text(
                            if (person.role.isBlank()) person.name else "${person.name} · ${person.role}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                )
            }
        }
        SectionHeader(
            title = "Добавить человека",
            subtitle = "Оси 1–10 пустые, пока не выбраны свои оценки. Семёрка по умолчанию не ставится.",
        )
        SectionCard {
            Field("Человек", name, { name = it })
            Field("Роль", role, { role = it })
            UnsettableScaleSelector("Прозрачность", transparency) { transparency = it }
            UnsettableScaleSelector("Слово", word) { word = it }
            UnsettableScaleSelector("Прирост связности", gain) { gain = it }
            Field("Одно действие", action, { action = it })
            val axesReady = transparency != null && word != null && gain != null
            CommitButton("Добавить", enabled = axesReady) {
                viewModel.savePerson(
                    ClusterPersonEntity(
                        cycleId = cycleId,
                        name = name.ifBlank { "Человек" },
                        role = role,
                        transparency = transparency ?: return@CommitButton,
                        word = word ?: return@CommitButton,
                        coherence = gain ?: return@CommitButton,
                        action = action,
                    ),
                )
                name = ""
                role = ""
                transparency = null
                word = null
                gain = null
                action = ""
            }
        }
    }
}

@Composable
fun CrystalScreen(viewModel: JournalViewModel) {
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id
    if (cycleId == null) {
        NoCycleNotice()
        return
    }
    val savedAudit = ui.position
    var positionDraft by remember(cycleId, savedAudit) { mutableStateOf(savedAudit?.toDraft() ?: PositionDraft()) }
    var ring by remember { mutableIntStateOf(1) }
    val savedRing = ui.rings.firstOrNull { it.ring == ring }
    var ringDraft by remember(cycleId, ring, savedRing) { mutableStateOf(savedRing?.toDraft() ?: RingDraft()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    ScreenColumn {
        SectionHeader("Бланк 10. Сверка позиции")
        SectionCard {
            JournalCanon.positionSpheres.forEachIndexed { index, label ->
                Field(label, positionDraft.sphereAt(index), { positionDraft = positionDraft.withSphere(index, it) })
            }
            Field("Ответ в двух-трёх предложениях", positionDraft.answer, { positionDraft = positionDraft.copy(answer = it) }, singleLine = false)
            CommitButton("Сохранить сверку позиции") {
                viewModel.savePosition(positionDraft.toEntity(cycleId))
            }
        }

        SectionHeader("Бланк 11. Чертёж трёх колец")
        SectionCard {
            ChoiceGroup(
                options = (1..3).map { index -> index.toString() to "Кольцо $index" },
                selected = ring.toString(),
                onSelect = { ring = it.toInt() },
            )
            QuietCard(JournalCanon.rings[ring - 1], CardTone.Secondary)
            Field("Что создаётся", ringDraft.what, { ringDraft = ringDraft.copy(what = it) }, singleLine = false)
            Field("Доля узла Рост", ringDraft.growthShare, { ringDraft = ringDraft.copy(growthShare = it) }, number = true, suffix = "%")
            Field("Часы в неделю", ringDraft.hoursPerWeek, { ringDraft = ringDraft.copy(hoursPerWeek = it) }, number = true, suffix = "ч")
            Field("Первый камень за 30 дней", ringDraft.firstStone, { ringDraft = ringDraft.copy(firstStone = it) })
            TonalButton("Записать кольцо", modifier = Modifier.fillMaxWidth()) {
                viewModel.saveRing(ringDraft.toEntity(cycleId, ring))
            }
            OutlinedButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        permission.launch(Manifest.permission.WRITE_CALENDAR)
                    } else {
                        insertCrystal(context, ringDraft.hoursPerWeek.toDoubleOrNull() ?: 2.0)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Поставить часы Кристалла в календарь")
            }
        }
        if (ui.rings.isNotEmpty()) {
            SegmentedGroup(count = ui.rings.size) { index, shape ->
                val item = ui.rings[index]
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape),
                    leadingContent = { ToneBadge("Кольцо ${item.ring}", CardTone.Primary) },
                    supportingContent = {
                        Text("${formatHours(item.hoursPerWeek)} ч/нед. · Рост ${"%.0f".format(item.growthShare)}%")
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    content = { Text(item.what.ifBlank { "Без описания" }) },
                )
            }
        }
    }
}

private fun insertCrystal(context: android.content.Context, hours: Double) {
    val calendarId = context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        arrayOf(CalendarContract.Calendars._ID),
        null,
        null,
        null,
    )?.use { if (it.moveToFirst()) it.getLong(0) else null } ?: return
    val start = LocalDate.now().plusDays((8 - LocalDate.now().dayOfWeek.value).toLong()).atTime(9, 0)
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val values = ContentValues().apply {
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.TITLE, "MiBT Кристалл смысла")
        put(CalendarContract.Events.DTSTART, start)
        put(CalendarContract.Events.DTEND, start + (hours * 3_600_000L).toLong())
        put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
    }
    context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
}
