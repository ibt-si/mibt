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
import androidx.compose.runtime.mutableStateListOf
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
import org.interferolog.mibt.content.CALC_DISCLAIMER
import org.interferolog.mibt.content.JournalCanon
import org.interferolog.mibt.data.ClusterPersonEntity
import org.interferolog.mibt.data.CrisisMapEntity
import org.interferolog.mibt.data.DebtEntity
import org.interferolog.mibt.data.FamilyPlanEntity
import org.interferolog.mibt.data.FanBranchEntity
import org.interferolog.mibt.data.FanChannelEntity
import org.interferolog.mibt.data.IncomeSourceEntity
import org.interferolog.mibt.data.InvestmentPassportEntity
import org.interferolog.mibt.data.MeaningRingEntity
import org.interferolog.mibt.data.PositionAuditEntity
import org.interferolog.mibt.data.RestMassEntity
import org.interferolog.mibt.data.WorkItemEntity
import org.interferolog.mibt.domain.Circle
import org.interferolog.mibt.domain.DebtInput
import org.interferolog.mibt.domain.DebtStrategy
import org.interferolog.mibt.domain.FiZone
import org.interferolog.mibt.domain.Quadrant
import org.interferolog.mibt.domain.amortize
import org.interferolog.mibt.domain.carryingMinimum
import org.interferolog.mibt.domain.circleLabel
import org.interferolog.mibt.domain.circleOf
import org.interferolog.mibt.domain.cohesion
import org.interferolog.mibt.domain.echelonHours
import org.interferolog.mibt.domain.familyRemainder
import org.interferolog.mibt.domain.fiZone
import org.interferolog.mibt.domain.fiZoneLabel
import org.interferolog.mibt.domain.formatHours
import org.interferolog.mibt.domain.formatRubles
import org.interferolog.mibt.domain.futureValue
import org.interferolog.mibt.domain.hourlyRate
import org.interferolog.mibt.domain.parseRubles
import org.interferolog.mibt.domain.profileMonths
import org.interferolog.mibt.domain.quadrant
import org.interferolog.mibt.domain.quadrantLabel
import org.interferolog.mibt.domain.resistanceByShares
import org.interferolog.mibt.domain.runwayMonths
import org.interferolog.mibt.domain.runwayZone
import org.interferolog.mibt.domain.splitCoupon
import org.interferolog.mibt.domain.weeklyTranche
import org.interferolog.mibt.domain.weeklyTrancheRemainder
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

private fun formatKopecksInput(kopecks: Long): String {
    if (kopecks == 0L) return ""
    val rubles = kopecks / 100
    val rem = kotlin.math.abs(kopecks % 100)
    return if (rem == 0L) rubles.toString() else "$rubles.${"%02d".format(rem)}"
}

private fun formatDoubleInput(value: Double): String {
    if (value == 0.0) return ""
    val rounded = value.roundToLong()
    return if (kotlin.math.abs(value - rounded) < 1e-6) rounded.toString() else "%.2f".format(value)
}

private fun fiTone(zone: FiZone): CardTone = when (zone) {
    FiZone.GREEN -> CardTone.Primary
    FiZone.YELLOW -> CardTone.Tertiary
    FiZone.RED -> CardTone.Error
}

private fun quadrantTone(q: Quadrant): CardTone = when (q) {
    Quadrant.CORE -> CardTone.Primary
    Quadrant.GROWTH_POINT -> CardTone.Secondary
    Quadrant.HYGIENE -> CardTone.Tertiary
    Quadrant.FRICTION -> CardTone.Error
}

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

    ScreenColumn {
        if (ui.sources.isNotEmpty()) {
            SectionHeader("Источники дохода")
            SegmentedGroup(count = ui.sources.size) { index, shape ->
                val source = ui.sources[index]
                val rate = hourlyRate(
                    source.netIncomeKopecks,
                    source.compensatoryNoiseKopecks,
                    source.contractHours,
                    source.frictionHours,
                )
                val fi = resistanceByShares(source.frictionScore, source.hoursShare, source.incomeShare)
                val zone = fi?.let(::fiZone)
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape),
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("S: ${rate?.let { "${formatRubles(it.toLong())}/ч" } ?: "—"} · Fᵢ: ${fi?.let { "%.2f".format(it) } ?: "—"}")
                            if (zone != null) ToneBadge(fiZoneLabel(zone), fiTone(zone))
                        }
                    },
                    trailingContent = {
                        DeleteIconButton("Убрать ${source.name}") { viewModel.deleteSource(source.id) }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    content = { Text(source.name, style = MaterialTheme.typography.titleMedium) },
                )
            }
        }
        SectionHeader("Новый источник")
        SectionCard {
            Field("Источник", name, { name = it })
            Field("Чистый доход", income, { income = it }, number = true, suffix = "₽")
            Field("Компенсаторный Шум", noise, { noise = it }, number = true, suffix = "₽")
            Field("Часы по договору", contract, { contract = it }, number = true, suffix = "ч")
            Field("Часы трения", friction, { friction = it }, number = true, suffix = "ч")
            Field("Dᵢ, доля дохода", dShare, { dShare = it }, number = true, suffix = "%")
            Field("hᵢ, доля часов", hShare, { hShare = it }, number = true, suffix = "%")
            ScaleSlider("Rᵢ, сопротивление", score, { score = it })
            CommitButton("Добавить источник") {
                viewModel.saveSource(
                    IncomeSourceEntity(
                        cycleId = cycleId,
                        name = name.ifBlank { "Источник" },
                        netIncomeKopecks = parseRubles(income) ?: 0,
                        compensatoryNoiseKopecks = parseRubles(noise) ?: 0,
                        contractHours = contract.toDoubleOrNull() ?: 0.0,
                        frictionHours = friction.toDoubleOrNull() ?: 0.0,
                        incomeShare = dShare.toDoubleOrNull() ?: 0.0,
                        hoursShare = hShare.toDoubleOrNull() ?: 0.0,
                        frictionScore = score,
                    ),
                )
                name = ""
                income = ""
                noise = ""
                contract = ""
                friction = ""
                dShare = ""
                hShare = ""
                score = 5
            }
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
    var day by remember { mutableStateOf(LocalDate.now().toString()) }
    var dayHours by remember { mutableStateOf("") }

    ScreenColumn {
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

        SectionHeader("Дела участка")
        if (ui.work.isNotEmpty()) {
            SegmentedGroup(count = ui.work.size) { index, shape ->
                val item = ui.work[index]
                val yield = if (item.hoursShare == 0.0) 0.0 else item.resultShare / item.hoursShare
                val fi = resistanceByShares(item.frictionScore, item.hoursShare, item.resultShare) ?: 0.0
                val q = quadrant(yield, item.frictionScore, fi)
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape),
                    supportingContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ToneBadge(quadrantLabel(q), quadrantTone(q))
                            MetaText("Fᵢ ${"%.2f".format(fi)}")
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    content = { Text(item.name, style = MaterialTheme.typography.titleMedium) },
                )
            }
        }
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
            TonalButton(
                text = "Добавить дело",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.saveWork(
                        WorkItemEntity(
                            cycleId = cycleId,
                            name = name.ifBlank { "Дело" },
                            resultShare = result.toDoubleOrNull() ?: 0.0,
                            hours = hours.toDoubleOrNull() ?: 0.0,
                            hoursShare = share.toDoubleOrNull() ?: 0.0,
                            frictionScore = score,
                            mark = mark,
                        ),
                    )
                    name = ""
                    result = ""
                    hours = ""
                    share = ""
                    score = 5
                },
            )
        }
    }
}

@Composable
fun FamilyScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id
    if (cycleId == null) {
        NoCycleNotice()
        return
    }
    val saved = ui.family
    var income1 by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.income1) }.orEmpty()) }
    var income2 by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.income2) }.orEmpty()) }
    var foundation by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.foundation) }.orEmpty()) }
    var fund by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.targetFund) }.orEmpty()) }
    var personal1 by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.personal1) }.orEmpty()) }
    var personal2 by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.personal2) }.orEmpty()) }
    var threshold by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.threshold) }.orEmpty()) }
    var note by remember(saved) { mutableStateOf(saved?.saturdayNote.orEmpty()) }
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
    val anyMoney = listOf(income1, income2, foundation, fund, personal1, personal2).any { it.isNotBlank() }

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
            Field("Доход 1", income1, { income1 = it }, number = true, suffix = "₽")
            Field("Доход 2", income2, { income2 = it }, number = true, suffix = "₽")
            Field("Фундамент Ф", foundation, { foundation = it }, number = true, suffix = "₽")
            Field("Целевой фонд", fund, { fund = it }, number = true, suffix = "₽")
            Field("Личная доля 1", personal1, { personal1 = it }, number = true, suffix = "₽")
            Field("Личная доля 2", personal2, { personal2 = it }, number = true, suffix = "₽")
            Field("Порог общей траты", threshold, { threshold = it }, number = true, suffix = "₽")
            val f = parseRubles(foundation) ?: 0
            val remainder = familyRemainder(parseRubles(income1) ?: 0, parseRubles(income2) ?: 0, f)
            if (anyMoney) {
                val sumCheck = f + (parseRubles(fund) ?: 0) + (parseRubles(personal1) ?: 0) + (parseRubles(personal2) ?: 0)
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
                viewModel.saveFamily(
                    FamilyPlanEntity(
                        cycleId = cycleId,
                        income1 = parseRubles(income1) ?: 0,
                        income2 = parseRubles(income2) ?: 0,
                        foundation = f,
                        targetFund = parseRubles(fund) ?: 0,
                        personal1 = parseRubles(personal1) ?: 0,
                        personal2 = parseRubles(personal2) ?: 0,
                        threshold = parseRubles(threshold) ?: 0,
                        saturdayNote = note,
                    ),
                )
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
            Field("Запись сверки", note, { note = it }, singleLine = false)
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
                val limit = parseRubles(threshold) ?: ui.family?.threshold ?: 0
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
    var support by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.support) }.orEmpty()) }
    var tool by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.tool) }.orEmpty()) }
    var obligations by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.obligations) }.orEmpty()) }
    var balance by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.balance) }.orEmpty()) }
    var profile by remember(saved) { mutableStateOf(saved?.profile ?: "HIRE") }
    var cut by remember(saved) { mutableStateOf(saved?.let { formatDoubleInput(it.firstCutPercent) }.orEmpty()) }
    var strategy by remember(saved) { mutableStateOf(saved?.strategy ?: "AVALANCHE") }
    var income by remember { mutableStateOf("") }
    var withdrawal by remember { mutableStateOf("") }
    var debtName by remember { mutableStateOf("") }
    var debtBalance by remember { mutableStateOf("") }
    var debtRate by remember { mutableStateOf("") }
    var debtMin by remember { mutableStateOf("") }
    var debtExtra by remember { mutableStateOf("") }

    ScreenColumn {
        val minimum = carryingMinimum(parseRubles(support) ?: 0, parseRubles(tool) ?: 0, parseRubles(obligations) ?: 0)
        val months = runwayMonths(parseRubles(balance) ?: 0, minimum)
        val target = profileMonths(profile)
        val zone = months?.let { runwayZone(it, target) }

        MetricCard(
            value = months?.let { "%.1f мес.".format(it) } ?: "—",
            caption = "Запас хода · Emin ${formatRubles(minimum)}",
            status = zone?.let { runwayLabel(it, target) } ?: "Цель профиля: $target месяцев.",
            tone = zone?.let(::runwayTone) ?: CardTone.Surface,
        )

        SectionHeader("Масса покоя")
        SectionCard {
            Field("Опора U₁", support, { support = it }, number = true, suffix = "₽")
            Field("Инструмент U₂", tool, { tool = it }, number = true, suffix = "₽")
            Field("Обязательства", obligations, { obligations = it }, number = true, suffix = "₽")
            Field("Масса покоя B", balance, { balance = it }, number = true, suffix = "₽")
            Field(
                label = "Первое сечение",
                value = cut,
                onValue = { cut = it },
                number = true,
                suffix = "%",
                supportingText = "1–5% при долгах; 10–20% без долгов.",
            )
            ChoiceGroup(
                options = listOf("HIRE" to "Найм", "FREELANCE" to "Фриланс", "BUSINESS" to "Бизнес"),
                selected = profile,
                onSelect = { profile = it },
            )
            MetaText("Цель профиля: $target месяцев.")
            CommitButton("Записать массу покоя") {
                viewModel.saveRest(
                    RestMassEntity(
                        cycleId = cycleId,
                        support = parseRubles(support) ?: 0,
                        tool = parseRubles(tool) ?: 0,
                        obligations = parseRubles(obligations) ?: 0,
                        balance = parseRubles(balance) ?: ui.rest?.balance ?: 0,
                        profile = profile,
                        firstCutPercent = cut.toDoubleOrNull() ?: 0.0,
                        accountOpened = true,
                        strategy = strategy,
                    ),
                )
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
            selected = strategy,
            onSelect = { strategy = it },
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
            if (strategy == "SNOWBALL") DebtStrategy.SNOWBALL else DebtStrategy.AVALANCHE,
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
    var accessible by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.accessible) }.orEmpty()) }
    var staff by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.staffMinimum) }.orEmpty()) }
    var emergency by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.emergencyMinimum) }.orEmpty()) }
    var cuts by remember(saved) { mutableStateOf(saved?.cutList.orEmpty()) }
    var ring by remember(saved) { mutableStateOf(saved?.supportRing.orEmpty()) }
    var offers by remember(saved) { mutableStateOf(saved?.offers.orEmpty()) }
    var tranche by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val quarantine = ui.quarantines.firstOrNull { it.kind == "H72" && it.endsAt > now }
    val emergencyValue = parseRubles(emergency) ?: 0
    val profile = profileMonths(ui.rest?.profile ?: "HIRE")
    val runway = runwayMonths(parseRubles(accessible) ?: 0, emergencyValue)
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
            Field("Масса покоя в доступе", accessible, { accessible = it }, number = true, suffix = "₽")
            Field("Штатный минимум", staff, { staff = it }, number = true, suffix = "₽")
            Field("Аварийный минимум", emergency, { emergency = it }, number = true, suffix = "₽")
            Field("Список отсечения", cuts, { cuts = it }, singleLine = false)
            Field("Кольцо опоры", ring, { ring = it }, singleLine = false)
            Field("Три предложения рынку", offers, { offers = it }, singleLine = false)
            CommitButton("Сохранить карту") {
                viewModel.saveCrisis(
                    CrisisMapEntity(
                        cycleId = cycleId,
                        accessible = parseRubles(accessible) ?: 0,
                        staffMinimum = parseRubles(staff) ?: 0,
                        emergencyMinimum = emergencyValue,
                        cutList = cuts,
                        supportRing = ring,
                        offers = offers,
                        drillDone = ui.crisis?.drillDone == true,
                    ),
                )
            }
        }

        SectionHeader("Учебная тревога и карантин")
        SectionCard {
            if (ui.crisis?.drillDone == true) {
                ToneBadge("Учебная тревога 24 ч проведена", CardTone.Primary)
            }
            OutlinedButton(
                onClick = {
                    viewModel.saveCrisis((ui.crisis ?: CrisisMapEntity(cycleId)).copy(drillDone = true))
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
    val fields = remember(saved) {
        mutableStateListOf(
            saved?.massStatus.orEmpty(),
            saved?.contributionRule.orEmpty(),
            saved?.horizonRule.orEmpty(),
            saved?.allocation.orEmpty(),
            saved?.riskLimit.orEmpty(),
            saved?.stormRule.orEmpty(),
            saved?.hygiene.orEmpty(),
        )
    }
    var balance by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.accountBalance) }.orEmpty()) }
    var payment by remember(saved) { mutableStateOf(saved?.let { formatKopecksInput(it.monthlyContribution) }.orEmpty()) }
    var rate by remember(saved) { mutableStateOf(saved?.let { formatDoubleInput(it.annualRate * 100.0) }.orEmpty()) }
    var years by remember(saved) { mutableStateOf(saved?.takeIf { it.years > 0 }?.years?.toString().orEmpty()) }
    var reinvest by remember(saved) { mutableStateOf(saved?.let { formatDoubleInput(it.reinvestPercent) }.orEmpty()) }
    var support by remember(saved) { mutableStateOf(saved?.let { formatDoubleInput(it.supportPercent) }.orEmpty()) }
    var crystal by remember(saved) { mutableStateOf(saved?.let { formatDoubleInput(it.crystalPercent) }.orEmpty()) }
    var drawdown by remember(saved) { mutableStateOf(saved?.drawdownOk ?: false) }
    var coupon by remember { mutableStateOf("") }

    ScreenColumn {
        QuietCard(CALC_DISCLAIMER, CardTone.Tertiary)
        QuietCard(JournalCanon.passportExample, CardTone.Surface)
        MetaText("Поля ниже пустые до первой записи. Пример из главы в них не подставляется и прогнозом не является.")

        SectionHeader("Семь полей паспорта")
        SectionCard {
            JournalCanon.passportFields.forEachIndexed { index, label ->
                Field(label, fields[index], { fields[index] = it }, singleLine = false)
            }
        }

        SectionHeader("Иллюстрация сложного процента")
        SectionCard {
            Field("Счёт", balance, { balance = it }, number = true, suffix = "₽")
            Field("Взнос в месяц", payment, { payment = it }, number = true, suffix = "₽")
            Field("Доходность в год", rate, { rate = it }, number = true, suffix = "%")
            Field("Срок", years, { years = it }, integer = true, suffix = "лет")
            val paymentKop = parseRubles(payment)
            val rateValue = rate.toDoubleOrNull()
            val yearValue = years.toIntOrNull()
            if (payment.isNotBlank() && rate.isNotBlank() && years.isNotBlank() &&
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
            val half = (parseRubles(balance) ?: 0) / 2
            if (balance.isNotBlank()) {
                BodyText("Просадка 50% оставляет на счёте ${formatRubles(half)}. Опора, рабочие инструменты и сон при этом остаются отдельной Массой покоя.")
            }
            SwitchRow("Переборка выдержала стресс-тест", drawdown) { drawdown = it }
        }

        SectionHeader(
            title = "Правило купонного притока",
            subtitle = "Доли реинвеста, Опоры и Кристалла журнал числами не задаёт. Впишите своё правило.",
        )
        SectionCard {
            Field("Купонный приток", coupon, { coupon = it }, number = true, suffix = "₽")
            Field("Реинвест", reinvest, { reinvest = it }, number = true, suffix = "%")
            Field("В Опору", support, { support = it }, number = true, suffix = "%")
            Field("В Кристалл", crystal, { crystal = it }, number = true, suffix = "%")
            val couponKop = parseRubles(coupon)
            if (coupon.isNotBlank() && couponKop != null && listOf(reinvest, support, crystal).all { it.isNotBlank() }) {
                val split = splitCoupon(
                    couponKop,
                    reinvest.toDoubleOrNull() ?: 0.0,
                    support.toDoubleOrNull() ?: 0.0,
                    crystal.toDoubleOrNull() ?: 0.0,
                )
                QuietCard(
                    "Реинвест ${formatRubles(split.first)}, Опора ${formatRubles(split.second)}, Кристалл ${formatRubles(split.third)}.",
                    CardTone.Secondary,
                )
            }
        }

        CommitButton("Сохранить паспорт") {
            viewModel.savePassport(
                InvestmentPassportEntity(
                    cycleId = cycleId,
                    massStatus = fields[0],
                    contributionRule = fields[1],
                    horizonRule = fields[2],
                    allocation = fields[3],
                    riskLimit = fields[4],
                    stormRule = fields[5],
                    hygiene = fields[6],
                    accountBalance = parseRubles(balance) ?: 0,
                    monthlyContribution = parseRubles(payment) ?: 0,
                    annualRate = (rate.toDoubleOrNull() ?: 0.0) / 100.0,
                    years = years.toIntOrNull() ?: 0,
                    reinvestPercent = reinvest.toDoubleOrNull() ?: 0.0,
                    supportPercent = support.toDoubleOrNull() ?: 0.0,
                    crystalPercent = crystal.toDoubleOrNull() ?: 0.0,
                    drawdownOk = drawdown,
                ),
            )
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
    val spheres = remember(savedAudit) {
        mutableStateListOf(
            savedAudit?.craft.orEmpty(),
            savedAudit?.surplus.orEmpty(),
            savedAudit?.people.orEmpty(),
            savedAudit?.horizon.orEmpty(),
            savedAudit?.timeFeel.orEmpty(),
        )
    }
    var answer by remember(savedAudit) { mutableStateOf(savedAudit?.answer.orEmpty()) }
    var what by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var stone by remember { mutableStateOf("") }
    var ring by remember { mutableIntStateOf(1) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    ScreenColumn {
        SectionHeader("Бланк 10. Сверка позиции")
        SectionCard {
            JournalCanon.positionSpheres.forEachIndexed { index, label ->
                Field(label, spheres[index], { spheres[index] = it })
            }
            Field("Ответ в двух-трёх предложениях", answer, { answer = it }, singleLine = false)
            CommitButton("Сохранить сверку позиции") {
                viewModel.savePosition(
                    PositionAuditEntity(
                        cycleId = cycleId,
                        craft = spheres[0],
                        surplus = spheres[1],
                        people = spheres[2],
                        horizon = spheres[3],
                        timeFeel = spheres[4],
                        answer = answer,
                    ),
                )
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
            Field("Что создаётся", what, { what = it }, singleLine = false)
            Field("Доля узла Рост", share, { share = it }, number = true, suffix = "%")
            Field("Часы в неделю", hours, { hours = it }, number = true, suffix = "ч")
            Field("Первый камень за 30 дней", stone, { stone = it })
            TonalButton("Записать кольцо", modifier = Modifier.fillMaxWidth()) {
                viewModel.saveRing(
                    MeaningRingEntity(
                        cycleId = cycleId,
                        ring = ring,
                        what = what,
                        growthShare = share.toDoubleOrNull() ?: 0.0,
                        hoursPerWeek = hours.toDoubleOrNull() ?: 0.0,
                        firstStone = stone,
                    ),
                )
                what = ""
                share = ""
                hours = ""
                stone = ""
            }
            OutlinedButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                    if (!granted) permission.launch(Manifest.permission.WRITE_CALENDAR) else insertCrystal(context, hours.toDoubleOrNull() ?: 2.0)
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
