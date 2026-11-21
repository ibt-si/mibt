package org.interferolog.mibt.ui

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.interferolog.mibt.JournalViewModel
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
import org.interferolog.mibt.domain.DebtInput
import org.interferolog.mibt.domain.DebtStrategy
import org.interferolog.mibt.domain.amortize
import org.interferolog.mibt.domain.carryingMinimum
import org.interferolog.mibt.domain.circleLabel
import org.interferolog.mibt.domain.circleOf
import org.interferolog.mibt.domain.cohesion
import org.interferolog.mibt.domain.echelonHours
import org.interferolog.mibt.domain.familyFoundation
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

@Composable
fun SourcesScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var income by remember { mutableStateOf("") }
    var noise by remember { mutableStateOf("") }
    var contract by remember { mutableStateOf("") }
    var friction by remember { mutableStateOf("") }
    var dShare by remember { mutableStateOf("") }
    var hShare by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("5") }
    val cycleId = ui.cycle?.id
    ScreenColumn {
        ui.sources.forEach { source ->
            val rate = hourlyRate(source.netIncomeKopecks, source.compensatoryNoiseKopecks, source.contractHours, source.frictionHours)
            val index = resistanceByShares(source.frictionScore, source.hoursShare, source.incomeShare)
            MetaText("${source.name}: S ${rate?.let { formatRubles(it.toLong()) } ?: "—"} / ч, Fᵢ ${index?.let { "%.2f".format(it) } ?: "—"} ${index?.let { fiZoneLabel(fiZone(it)) } ?: ""}")
            RemoveButton("Убрать ${source.name}") { viewModel.deleteSource(source.id) }
        }
        Field("Источник", name, { name = it })
        Field("Чистый доход, ₽", income, { income = it }, number = true)
        Field("Компенсаторный Шум, ₽", noise, { noise = it }, number = true)
        Field("Часы по договору", contract, { contract = it }, number = true)
        Field("Часы трения", friction, { friction = it }, number = true)
        Field("Dᵢ, % дохода", dShare, { dShare = it }, number = true)
        Field("hᵢ, % часов", hShare, { hShare = it }, number = true)
        Field("Rᵢ, 1–10", score, { score = it }, number = true)
        CommitButton("Добавить источник", enabled = cycleId != null, onClick = {
            val id = cycleId ?: return@CommitButton
            viewModel.saveSource(
                IncomeSourceEntity(
                    cycleId = id,
                    name = name.ifBlank { "Источник" },
                    netIncomeKopecks = parseRubles(income) ?: 0,
                    compensatoryNoiseKopecks = parseRubles(noise) ?: 0,
                    contractHours = contract.toDoubleOrNull() ?: 0.0,
                    frictionHours = friction.toDoubleOrNull() ?: 0.0,
                    incomeShare = dShare.toDoubleOrNull() ?: 0.0,
                    hoursShare = hShare.toDoubleOrNull() ?: 0.0,
                    frictionScore = score.toIntOrNull()?.coerceIn(1, 10) ?: 5,
                ),
            )
            name = ""
        })
    }
}

@Composable
fun PriceScreen(viewModel: JournalViewModel) {
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var recording by remember { mutableStateOf(false) }
    var silence by remember { mutableIntStateOf(-1) }
    var outcome by remember { mutableStateOf("") }
    var path by remember { mutableStateOf<String?>(null) }
    val recorder = remember { arrayOfNulls<MediaRecorder>(1) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(silence) {
        if (silence > 0) {
            delay(1000)
            silence -= 1
        }
    }
    ScreenColumn {
        BodyText("Фраза с ценой, точка, десять секунд тишины. Диктофон не уходит с устройства.")
        CommitButton(if (recording) "Точка" else "Начать дубль", onClick = {
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
        })
        if (silence >= 0) HeroNumber(silence.toString(), "секунд тишины после точки")
        Field("Исход паузы", outcome, { outcome = it }, singleLine = false)
        ActionGroup(
            options = listOf(
                "Тишина удержана" to { path?.let { viewModel.saveRehearsal(it, true, outcome) } },
                "После точки прозвучало слово" to { path?.let { viewModel.saveRehearsal(it, false, outcome) } },
            ),
            enabled = path != null && silence == 0,
        )
        ui.rehearsals.take(5).forEach { item ->
            MetaText("${if (item.silenceHeld) "тишина" else "слово после точки"} · ${item.outcome}")
        }
    }
}

@Composable
fun WorkScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id
    var name by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("5") }
    var mark by remember { mutableStateOf("FRICTION") }
    var day by remember { mutableStateOf(LocalDate.now().toString()) }
    var dayHours by remember { mutableStateOf("") }
    ScreenColumn {
        MetaText("Записано дней с часами: ${ui.hours.map { it.day }.toSet().size} из 14.")
        Field("День", day, { day = it })
        Field("Часы за день", dayHours, { dayHours = it }, number = true)
        CommitButton("Записать часы", enabled = cycleId != null) {
            viewModel.saveHour(day, dayHours.toDoubleOrNull() ?: 0.0, "")
        }
        ui.work.forEach { item ->
            val yield = if (item.hoursShare == 0.0) 0.0 else item.resultShare / item.hoursShare
            val index = org.interferolog.mibt.domain.resistanceByShares(item.frictionScore, item.hoursShare, item.resultShare) ?: 0.0
            MetaText("${item.name}: ${quadrantLabel(quadrant(yield, item.frictionScore, index))}, Fᵢ ${"%.2f".format(index)}")
        }
        Field("Дело", name, { name = it })
        Field("Dᵢ, % результата", result, { result = it }, number = true)
        Field("Часы в месяц", hours, { hours = it }, number = true)
        Field("hᵢ, % часов", share, { share = it }, number = true)
        Field("Rᵢ", score, { score = it }, number = true)
        ChoiceGroup(
            options = listOf("CREATE" to "Созидание", "FRICTION" to "Трение"),
            selected = mark,
            onSelect = { mark = it },
        )
        TonalButton("Добавить дело", enabled = cycleId != null, onClick = {
            val id = cycleId ?: return@TonalButton
            viewModel.saveWork(
                WorkItemEntity(
                    cycleId = id,
                    name = name.ifBlank { "Дело" },
                    resultShare = result.toDoubleOrNull() ?: 0.0,
                    hours = hours.toDoubleOrNull() ?: 0.0,
                    hoursShare = share.toDoubleOrNull() ?: 0.0,
                    frictionScore = score.toIntOrNull() ?: 5,
                    mark = mark,
                ),
            )
        })
    }
}

@Composable
fun FamilyScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id ?: return
    var income1 by remember { mutableStateOf("") }
    var income2 by remember { mutableStateOf("") }
    var foundation by remember { mutableStateOf("") }
    var fund by remember { mutableStateOf("") }
    var personal1 by remember { mutableStateOf("") }
    var personal2 by remember { mutableStateOf("") }
    var threshold by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(ui.family?.saturdayNote.orEmpty()) }
    var left by remember { mutableIntStateOf(0) }
    var purchase by remember { mutableStateOf("") }
    LaunchedEffect(left) { if (left > 0) { delay(1000); left -= 1 } }
    val now = System.currentTimeMillis()
    val quarantine = ui.quarantines.firstOrNull { it.kind == "H24" && it.endsAt > now }
    val anyMoney = listOf(income1, income2, foundation, fund, personal1, personal2).any { it.isNotBlank() }
    ScreenColumn {
        BodyText("Обмен между двумя телефонами в эту версию не входит. Запись остаётся на этом устройстве.")
        QuietCard(JournalCanon.week(6).lead)
        SectionTitle("Поле боя или приборная панель")
        JournalCanon.battlefield.forEach { row ->
            BodyText(row.parameter)
            MetaText("Поле боя: ${row.battle}")
            MetaText("Приборная панель: ${row.panel}")
        }
        MetaText("Общая Опора и обязательства не урезаются ни в одном споре. Общий Инструмент сверх порога ждёт 24 часа. Целевой фонд меняется только через спокойный разговор. Личные доли — без отчётов.")
        Field("Доход 1, ₽", income1, { income1 = it }, number = true)
        Field("Доход 2, ₽", income2, { income2 = it }, number = true)
        Field("Фундамент Ф, ₽", foundation, { foundation = it }, number = true)
        Field("Целевой фонд, ₽", fund, { fund = it }, number = true)
        Field("Личная доля 1, ₽", personal1, { personal1 = it }, number = true)
        Field("Личная доля 2, ₽", personal2, { personal2 = it }, number = true)
        Field("Порог общей траты, ₽", threshold, { threshold = it }, number = true)
        val f = parseRubles(foundation) ?: 0
        val remainder = familyRemainder(parseRubles(income1) ?: 0, parseRubles(income2) ?: 0, f)
        if (anyMoney) {
            HeroNumber(formatRubles(remainder), "Свободный остаток С")
            BodyText("Проверка: Ф + Ц + Л₁ + Л₂ = ${formatRubles(f + (parseRubles(fund) ?: 0) + (parseRubles(personal1) ?: 0) + (parseRubles(personal2) ?: 0))}")
        } else {
            MetaText("Свободный остаток С появится, когда будут вписаны свои суммы. Пример из главы в поля не подставлен.")
        }
        CommitButton("Сохранить контуры", onClick = {
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
        })
        TonalButton(if (left > 0) "Сверка: ${left / 60}:${"%02d".format(left % 60)}" else "Субботняя сверка, 15 минут") {
            left = 15 * 60
        }
        JournalCanon.saturdayQuestions.forEach { BodyText(it) }
        Field("Запись сверки", note, { note = it }, singleLine = false)
        Field("Общая трата сверх порога, ₽", purchase, { purchase = it }, number = true)
        if (quarantine != null) {
            QuietCard("Карантин 24 часа ещё идёт. Общая покупка сверх порога подождёт.", CardTone.Error)
        }
        TonalButton("Проверить порог") {
            val amount = parseRubles(purchase) ?: return@TonalButton
            val limit = parseRubles(threshold) ?: ui.family?.threshold ?: 0
            if (amount > limit && quarantine == null) {
                viewModel.startQuarantine("H24", 24, "Общая трата ${formatRubles(amount)}")
            }
        }
        BodyText("Фраза стоп-сигнала: «Я сейчас в сжатии».")
    }
}

@Composable
fun RestScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id ?: return
    var support by remember { mutableStateOf("") }
    var tool by remember { mutableStateOf("") }
    var obligations by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf(ui.rest?.profile ?: "HIRE") }
    var cut by remember { mutableStateOf("") }
    var strategy by remember { mutableStateOf(ui.rest?.strategy ?: "AVALANCHE") }
    var income by remember { mutableStateOf("") }
    var withdrawal by remember { mutableStateOf("") }
    var debtName by remember { mutableStateOf("") }
    var debtBalance by remember { mutableStateOf("") }
    var debtRate by remember { mutableStateOf("") }
    var debtMin by remember { mutableStateOf("") }
    var debtExtra by remember { mutableStateOf("") }
    ScreenColumn {
        Field("Опора U₁, ₽", support, { support = it }, number = true)
        Field("Инструмент U₂, ₽", tool, { tool = it }, number = true)
        Field("Обязательства, ₽", obligations, { obligations = it }, number = true)
        Field("Масса покоя B, ₽", balance, { balance = it }, number = true)
        Field("Первое сечение, %", cut, { cut = it }, number = true)
        MetaText("Первое сечение: 1–5% при долгах; 10–20% без долгов. Поле пустое, пока не вписана своя доля.")
        ChoiceGroup(
            options = listOf("HIRE" to "Найм", "FREELANCE" to "Фриланс", "BUSINESS" to "Бизнес"),
            selected = profile,
            onSelect = { profile = it },
        )
        val minimum = carryingMinimum(parseRubles(support) ?: 0, parseRubles(tool) ?: 0, parseRubles(obligations) ?: 0)
        val months = runwayMonths(parseRubles(balance) ?: 0, minimum)
        val target = profileMonths(profile)
        val zone = months?.let { runwayZone(it, target) }
        HeroNumber(months?.let { "%.1f".format(it) } ?: "—", "Запас хода, месяцев. Emin ${formatRubles(minimum)}")
        if (zone != null) QuietCard(runwayLabel(zone, target), runwayTone(zone))
        BodyText("Цель профиля: ${profileMonths(profile)} месяцев.")
        CommitButton("Записать массу покоя", onClick = {
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
        })
        Field("Приход, ₽", income, { income = it }, number = true)
        TonalButton("Первое сечение с этого прихода") { parseRubles(income)?.let(viewModel::applyFirstCut) }
        Field("Изъятие, ₽", withdrawal, { withdrawal = it }, number = true)
        OutlinedButton(onClick = { parseRubles(withdrawal)?.let(viewModel::withdraw) }) { Text("Изъятие из массы покоя") }
        SectionTitle("Одна мишень")
        ChoiceGroup(
            options = listOf("AVALANCHE" to "Лавина", "SNOWBALL" to "Снежный ком"),
            selected = strategy,
            onSelect = { strategy = it },
        )
        Field("Долг", debtName, { debtName = it })
        Field("Остаток, ₽", debtBalance, { debtBalance = it }, number = true)
        Field("Ставка, %", debtRate, { debtRate = it }, number = true)
        Field("Минимальный платёж, ₽", debtMin, { debtMin = it }, number = true)
        Field("Сверх минимума, ₽", debtExtra, { debtExtra = it }, number = true)
        TonalButton("Добавить обязательство", onClick = {
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
        })
        val schedule = amortize(
            ui.debts.map { DebtInput(it.name, it.balance, it.ratePercent, it.minPayment, it.extra) },
            if (strategy == "SNOWBALL") DebtStrategy.SNOWBALL else DebtStrategy.AVALANCHE,
        )
        schedule.take(6).forEach { row ->
            MetaText("Месяц ${row.month}: мишень ${row.focus}, остаток ${formatRubles(row.remaining)}")
        }
        ui.debts.forEach { debt ->
            RemoveButton("Убрать ${debt.name}") { viewModel.deleteDebt(debt.id) }
        }
    }
}

@Composable
fun CrisisScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id ?: return
    var accessible by remember { mutableStateOf("") }
    var staff by remember { mutableStateOf("") }
    var emergency by remember { mutableStateOf("") }
    var cuts by remember { mutableStateOf("") }
    var ring by remember { mutableStateOf("") }
    var offers by remember { mutableStateOf("") }
    var tranche by remember { mutableStateOf("") }
    val now = System.currentTimeMillis()
    val quarantine = ui.quarantines.firstOrNull { it.kind == "H72" && it.endsAt > now }
    ScreenColumn {
        Field("Масса покоя в доступе, ₽", accessible, { accessible = it }, number = true)
        Field("Штатный минимум, ₽", staff, { staff = it }, number = true)
        Field("Аварийный минимум, ₽", emergency, { emergency = it }, number = true)
        Field("Список отсечения", cuts, { cuts = it }, singleLine = false)
        Field("Кольцо опоры", ring, { ring = it }, singleLine = false)
        Field("Три предложения рынку", offers, { offers = it }, singleLine = false)
        val emergencyValue = parseRubles(emergency) ?: 0
        val profile = profileMonths(ui.rest?.profile ?: "HIRE")
        val runway = runwayMonths(parseRubles(accessible) ?: 0, emergencyValue)
        val crisisZone = runway?.let { runwayZone(it, profile) }
        HeroNumber(runway?.let { "%.0f".format(it * 30) } ?: "—", "Аварийный запас хода, дней")
        if (crisisZone != null) QuietCard(runwayLabel(crisisZone, profile), runwayTone(crisisZone))
        val trancheRemainder = weeklyTrancheRemainder(emergencyValue)
        val trancheNote = if (trancheRemainder == 0L) "" else " Остаток ${formatRubles(trancheRemainder)} остаётся в запасе."
        MetaText("Недельный транш: ${formatRubles(weeklyTranche(emergencyValue))}.$trancheNote")
        CommitButton("Сохранить карту", onClick = {
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
        })
        OutlinedButton(onClick = {
            viewModel.saveCrisis((ui.crisis ?: CrisisMapEntity(cycleId)).copy(drillDone = true))
        }) { Text("Учебная тревога на 24 часа проведена") }
        if (quarantine != null) QuietCard("72 часа без необратимых решений. Карантин ещё идёт.", CardTone.Error)
        TonalButton("Начать карантин 72 часа") { viewModel.startQuarantine("H72", 72, "Карантин на резкие решения") }
        Field("Недельный транш, ₽", tranche, { tranche = it }, number = true)
        TonalButton("Записать транш") {
            val amount = parseRubles(tranche) ?: return@TonalButton
            viewModel.saveTranche(amount, runway ?: 0.0)
        }
        ui.tranches.forEach { item -> MetaText("${formatRubles(item.amount)} · запас ${"%.1f".format(item.runwayMonths)} мес.") }
    }
}

@Composable
fun FanScreen(viewModel: JournalViewModel) {
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id ?: return
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
        Field("Рабочие часы недели", weekHours, { weekHours = it }, number = true)
        val total = weekHours.toDoubleOrNull() ?: 0.0
        MetaText("Ядро ${formatHours(echelonHours(total, 0.60))} ч, рост ${formatHours(echelonHours(total, 0.30))} ч, поиск ${formatHours(echelonHours(total, 0.10))} ч.")
        TonalButton("Поставить часы веера в календарь") {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
            if (granted) insertFanHold(context, total) else calendarPermission.launch(Manifest.permission.WRITE_CALENDAR)
        }
        ChoiceGroup(
            options = listOf("CORE" to "Ядро", "GROWTH" to "Рост", "SEARCH" to "Поиск"),
            selected = echelon,
            onSelect = { echelon = it },
        )
        Field("Направление", name, { name = it })
        Field("Часы в неделю", hours, { hours = it }, number = true)
        Field("Горизонт, суток", horizon, { horizon = it }, number = true)
        Field("Три опережающих сигнала", signals, { signals = it }, singleLine = false)
        CommitButton("Добавить ветвь", onClick = {
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
        })
        val duplicated = ui.channels.groupBy { it.failurePoint.trim().lowercase() }.filter { it.key.isNotBlank() && it.value.size > 1 }.keys
        ui.branches.forEach { branch ->
            val locked = branch.reviewAt != null && branch.reviewAt > System.currentTimeMillis() && !branch.closed
            MetaText("${branch.echelon}: ${branch.name}, ${formatHours(branch.hoursPerWeek)} ч${if (branch.closed) ", закрыта" else ""}")
            if (!branch.closed) {
                OutlinedButton(onClick = { viewModel.closeBranch(branch.id) }, enabled = !locked) {
                    Text(if (locked) "Ревизия ещё не наступила" else "Закрыть направление")
                }
            }
        }
        Field("Точка отказа канала", failure, { failure = it })
        Field("Номер канала, 1–3", slot, { slot = it }, number = true)
        TonalButton("Добавить канал", onClick = {
            viewModel.saveChannel(
                FanChannelEntity(
                    cycleId = cycleId,
                    echelon = echelon,
                    slot = slot.toIntOrNull() ?: 1,
                    failurePoint = failure,
                ),
            )
        })
        ui.channels.forEach { channel ->
            val same = channel.failurePoint.trim().lowercase() in duplicated
            MetaText("${channel.echelon} ${channel.slot}: ${channel.failurePoint}${if (same) " — совпадает с другим каналом, это один канал" else ""}")
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
    val cycleId = ui.cycle?.id ?: return
    val fields = remember { mutableStateListOf("", "", "", "", "", "", "") }
    var balance by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var years by remember { mutableStateOf("") }
    var reinvest by remember { mutableStateOf("") }
    var support by remember { mutableStateOf("") }
    var crystal by remember { mutableStateOf("") }
    var drawdown by remember { mutableStateOf(false) }
    var coupon by remember { mutableStateOf("") }
    ScreenColumn {
        QuietCard(org.interferolog.mibt.content.CALC_DISCLAIMER, CardTone.Tertiary)
        QuietCard(JournalCanon.passportExample)
        MetaText("Поля ниже пустые. Пример из главы в них не подставляется и прогнозом не является.")
        JournalCanon.passportFields.forEachIndexed { index, label ->
            Field(label, fields[index], { fields[index] = it }, singleLine = false)
        }
        Field("Счёт, ₽", balance, { balance = it }, number = true)
        Field("Взнос в месяц, ₽", payment, { payment = it }, number = true)
        Field("Доходность в год, %", rate, { rate = it }, number = true)
        Field("Лет", years, { years = it }, number = true)
        val paymentKop = parseRubles(payment)
        val rateValue = rate.toDoubleOrNull()
        val yearValue = years.toIntOrNull()
        if (payment.isNotBlank() && rate.isNotBlank() && years.isNotBlank() &&
            paymentKop != null && rateValue != null && yearValue != null && yearValue > 0
        ) {
            val value = futureValue(paymentKop, rateValue / 100.0, yearValue * 12)
            HeroNumber(formatRubles(value.toLong()), "Иллюстрация счёта к концу срока по введённым полям")
            BodyText(org.interferolog.mibt.content.CALC_DISCLAIMER)
        } else {
            MetaText("Впишите взнос, доходность и срок — тогда появится иллюстрация по вашим полям. Пока они пустые, крупной цифры нет.")
        }
        val half = (parseRubles(balance) ?: 0) / 2
        if (balance.isNotBlank()) {
            BodyText("Просадка 50% оставляет на счёте ${formatRubles(half)}. Опора, рабочие инструменты и сон при этом остаются отдельной Массой покоя.")
        }
        SwitchRow("Переборка выдержала стресс-тест", drawdown) { drawdown = it }
        Field("Купонный приток, ₽", coupon, { coupon = it }, number = true)
        MetaText("Доли реинвеста, Опоры и Кристалла журнал числами не задаёт. Впишите своё правило.")
        Field("Реинвест, %", reinvest, { reinvest = it }, number = true)
        Field("В Опору, %", support, { support = it }, number = true)
        Field("В Кристалл, %", crystal, { crystal = it }, number = true)
        val couponKop = parseRubles(coupon)
        if (coupon.isNotBlank() && couponKop != null && listOf(reinvest, support, crystal).all { it.isNotBlank() }) {
            val split = splitCoupon(couponKop, reinvest.toDoubleOrNull() ?: 0.0, support.toDoubleOrNull() ?: 0.0, crystal.toDoubleOrNull() ?: 0.0)
            MetaText("Реинвест ${formatRubles(split.first)}, Опора ${formatRubles(split.second)}, Кристалл ${formatRubles(split.third)}.")
        }
        CommitButton("Сохранить паспорт", onClick = {
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
        })
    }
}

@Composable
fun ClusterScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id ?: return
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var transparency by remember { mutableStateOf("") }
    var word by remember { mutableStateOf("") }
    var gain by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("") }
    ScreenColumn {
        ui.people.forEach { person ->
            val index = cohesion(person.transparency, person.word, person.coherence)
            MetaText("${person.name}: Kᵢ ${"%.1f".format(index)}, ${circleLabel(circleOf(index, person.transparency, person.word, person.coherence))}. ${person.action}")
        }
        Field("Человек", name, { name = it })
        Field("Роль", role, { role = it })
        Field("Прозрачность, 1–10", transparency, { transparency = it }, number = true)
        Field("Слово, 1–10", word, { word = it }, number = true)
        Field("Прирост связности, 1–10", gain, { gain = it }, number = true)
        Field("Одно действие", action, { action = it })
        val axesReady = listOf(transparency, word, gain).all { (it.toIntOrNull() ?: 0) in 1..10 }
        if (!axesReady) MetaText("Оси 1–10 пустые, пока не вписаны свои оценки. Семёрка по умолчанию не ставится.")
        CommitButton("Добавить", enabled = axesReady, onClick = {
            viewModel.savePerson(
                ClusterPersonEntity(
                    cycleId = cycleId,
                    name = name.ifBlank { "Человек" },
                    role = role,
                    transparency = transparency.toIntOrNull() ?: return@CommitButton,
                    word = word.toIntOrNull() ?: return@CommitButton,
                    coherence = gain.toIntOrNull() ?: return@CommitButton,
                    action = action,
                ),
            )
        })
    }
}

@Composable
fun CrystalScreen(viewModel: JournalViewModel) {
    val context = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cycleId = ui.cycle?.id ?: return
    val spheres = remember { mutableStateListOf("", "", "", "", "") }
    var answer by remember { mutableStateOf("") }
    var what by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var stone by remember { mutableStateOf("") }
    var ring by remember { mutableIntStateOf(1) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    ScreenColumn {
        JournalCanon.positionSpheres.forEachIndexed { index, label ->
            Field(label, spheres[index], { spheres[index] = it })
        }
        Field("Ответ в двух-трёх предложениях", answer, { answer = it }, singleLine = false)
        CommitButton("Сохранить сверку позиции", onClick = {
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
        })
        ChoiceGroup(
            options = (1..3).map { index -> index.toString() to "Кольцо $index" },
            selected = ring.toString(),
            onSelect = { ring = it.toInt() },
        )
        BodyText(JournalCanon.rings[ring - 1])
        Field("Что создаётся", what, { what = it }, singleLine = false)
        Field("Доля узла Рост, %", share, { share = it }, number = true)
        Field("Часы в неделю", hours, { hours = it }, number = true)
        Field("Первый камень за 30 дней", stone, { stone = it })
        TonalButton("Записать кольцо", onClick = {
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
        })
        ui.rings.forEach { item -> MetaText("Кольцо ${item.ring}: ${item.what}. ${formatHours(item.hoursPerWeek)} ч.") }
        OutlinedButton(onClick = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
            if (!granted) permission.launch(Manifest.permission.WRITE_CALENDAR) else insertCrystal(context, hours.toDoubleOrNull() ?: 2.0)
        }) { Text("Поставить часы Кристалла в календарь") }
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
