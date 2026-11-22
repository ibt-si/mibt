// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.interferolog.mibt.JournalViewModel
import org.interferolog.mibt.content.BlankCanon
import org.interferolog.mibt.content.JournalCanon
import org.interferolog.mibt.content.POINT_C
import org.interferolog.mibt.data.JournalUi
import org.interferolog.mibt.data.PhaseDiaryEntryEntity
import org.interferolog.mibt.data.WellEntryEntity
import org.interferolog.mibt.domain.etaBand
import org.interferolog.mibt.domain.etaBandLabel
import org.interferolog.mibt.domain.formatRatio
import org.interferolog.mibt.domain.formatRubles
import org.interferolog.mibt.domain.parseRubles
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CaptureScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    if (ui.cycle == null) {
        NoCycleNotice()
        return
    }
    val blank = BlankCanon.phaseDiary
    var amount by remember { mutableStateOf("") }
    var context by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var fact by remember { mutableStateOf("") }

    fun submit(impulse: String) {
        viewModel.saveDiary(parseRubles(amount), context, body, impulse, fact)
        amount = ""
        context = ""
        body = ""
        fact = ""
    }

    ScreenColumn {
        BlankHeader(blank)
        QuietCard("Время запишется в момент выбора. Минута тишины здесь не нужна.", CardTone.Secondary)
        SectionCard {
            Field("Сумма", amount, { amount = it }, number = true, suffix = "₽")
            Field("Контекст", context, { context = it })
            Field("Телесный маркер", body, { body = it })
            Field("Факт без оценки", fact, { fact = it }, singleLine = false)
            ActionGroup(
                options = listOf(
                    "Из пустоты" to { submit("EMPTY") },
                    "Из наполненности" to { submit("FULL") },
                ),
            )
        }
        blank.referenceTables.forEach { ReferenceTable(it) }
        ExampleRows(blank.columns, blank.examples)
        impulseShareLine(ui.diary.size, ui.diary.count { it.impulse == "EMPTY" })?.let { MetaText(it) }
        EntryList(
            columns = blank.columns,
            entries = ui.diary.sortedByDescending { it.atMillis }.map { EntryLine(it.id, diaryCells(it)) },
            onDelete = viewModel::deleteDiary,
        )
    }
}

@Composable
fun PointCScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    if (ui.cycle == null) {
        NoCycleNotice()
        return
    }
    val blank = BlankCanon.pointC
    var situation by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("POINT_C") }
    var spasm by remember { mutableStateOf(JournalCanon.spasms.first()) }
    var effect by remember { mutableStateOf("") }

    ScreenColumn {
        BlankHeader(blank)
        QuietCard(POINT_C, CardTone.Primary)
        SectionCard {
            Field("Ситуация", situation, { situation = it })
            Field("Сумма", amount, { amount = it }, number = true, suffix = "₽")
            ChoiceGroup(
                options = listOf("WELL" to "Колодец", "POINT_C" to "Точка C"),
                selected = question,
                onSelect = { question = it },
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Спазм колодца", style = MaterialTheme.typography.labelLarge)
                ChoiceChips(
                    options = JournalCanon.spasms.map { it to it },
                    selected = spasm,
                    onSelect = { spasm = it },
                )
            }
            Field("Что изменила пауза", effect, { effect = it }, singleLine = false)
            CommitButton("Записать") {
                viewModel.saveWell(situation, parseRubles(amount), question, spasm, effect)
                situation = ""
                amount = ""
                effect = ""
            }
        }
        blank.referenceTables.forEach { ReferenceTable(it) }
        ExampleRows(blank.columns, blank.examples)
        EntryList(
            columns = blank.columns,
            entries = ui.wells.sortedByDescending { it.atMillis }.map { EntryLine(it.id, wellCells(it)) },
            onDelete = viewModel::deleteWell,
        )
    }
}

@Composable
fun QuickSpendScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    if (ui.cycle == null) {
        NoCycleNotice()
        return
    }
    ScreenColumn {
        SectionCard {
            SpendForm(viewModel, cleanSheet = false, title = null)
        }
    }
}

@Composable
fun HolterScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    if (ui.cycle == null) {
        NoCycleNotice()
        return
    }
    var left by remember { mutableIntStateOf(240) }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000)
            left -= 1
        }
    }
    val missed = missedDays(ui)
    val ratio = currentEta(ui)
    val obz = ui.spends.filter { it.node == "OBZ" }.sumOf { it.amountKopecks }

    ScreenColumn {
        MetricCard(
            value = ratio?.let(::formatRatio) ?: "—",
            caption = "Индекс η",
            status = if (ratio == null) {
                "Появится после первой траты вне обязательств."
            } else {
                "${etaBandLabel(etaBand(ratio))}. Обязательства ${formatRubles(obz)} в формулу не входят."
            },
            tone = ratio?.let { etaTone(etaBand(it)) } ?: CardTone.Surface,
        )
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Разметка дня (4 минуты)", style = MaterialTheme.typography.titleMedium)
                ToneBadge("${left / 60}:${"%02d".format(left % 60)}", CardTone.Secondary)
            }
            LinearProgressIndicator(
                progress = { (240 - left) / 240f },
                modifier = Modifier.fillMaxWidth(),
            )
            MetaText("Таймер не закрывает запись.")
        }
        if (missed > 0) {
            SectionCard(tone = CardTone.Tertiary) {
                BodyText("Пропущенные дни можно закрыть одной строкой в Шум и продолжить с сегодняшнего вечера. Пропущено дней: $missed.")
                CleanSheetForm(viewModel)
            }
        }
        SectionCard {
            SpendForm(viewModel, cleanSheet = false, title = "Трата дня")
        }
        if (ui.bills.isNotEmpty()) {
            SectionHeader("Зеркальные счета")
            SegmentedGroup(count = ui.bills.size) { index, shape ->
                val bill = ui.bills[index]
                val settled = bill.settledAt != null
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape),
                    leadingContent = if (settled) {
                        {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else null,
                    supportingContent = {
                        Text(if (settled) "Перевод сделан" else "К переводу в Рост")
                    },
                    trailingContent = if (!settled) {
                        {
                            TonalButton("Сделан") { viewModel.settleBill(bill) }
                        }
                    } else null,
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    content = { Text(formatRubles(bill.amountKopecks), style = MaterialTheme.typography.titleMedium) },
                )
            }
        }
    }
}

@Composable
private fun CleanSheetForm(viewModel: JournalViewModel) {
    var amount by remember { mutableStateOf("") }
    Field("Забытая разница", amount, { amount = it }, number = true, suffix = "₽")
    TonalButton(
        text = "Закрыть пропуск одной строкой",
        enabled = parseRubles(amount) != null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val value = parseRubles(amount) ?: return@TonalButton
        viewModel.saveSpend("U4", value, "Чистый лист", cleanSheet = true, canFundMirror = false)
        amount = ""
    }
}

@Composable
private fun SpendForm(viewModel: JournalViewModel, cleanSheet: Boolean, title: String?) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var node by remember { mutableStateOf("U1") }
    var askMirror by remember { mutableStateOf(false) }

    if (title != null) {
        Text(title, style = MaterialTheme.typography.titleLargeEmphasized)
    }
    Field("Сумма", amount, { amount = it }, number = true, suffix = "₽")
    Field("Заметка", note, { note = it })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            listOf("U1" to "Опора", "U2" to "Инструмент"),
            listOf("U3" to "Рост", "U4" to "Шум"),
            listOf("OBZ" to "Обязательства", "INCOME" to "Доход"),
        ).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (id, label) ->
                    FilterChip(
                        selected = node == id,
                        onClick = { node = id },
                        modifier = Modifier.weight(1f),
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
    }
    CommitButton(
        text = "Записать",
        enabled = parseRubles(amount) != null,
    ) {
        val value = parseRubles(amount) ?: return@CommitButton
        if (node == "U4" && !cleanSheet) {
            askMirror = true
        } else {
            viewModel.saveSpend(node, value, note, cleanSheet, canFundMirror = false)
            amount = ""
            note = ""
        }
    }
    if (askMirror) {
        SectionCard(tone = CardTone.Tertiary) {
            BodyText("Трата в Шум открывает зеркальный счёт на ту же сумму в Рост. Если двойной цены сейчас нет, покупка не записывается.")
            CommitButton("Перевести столько же") {
                val value = parseRubles(amount) ?: return@CommitButton
                viewModel.saveSpend(node, value, note, false, canFundMirror = true)
                askMirror = false
                amount = ""
                note = ""
            }
            OutlinedButton(
                onClick = {
                    val value = parseRubles(amount) ?: return@OutlinedButton
                    viewModel.saveSpend(node, value, note, false, canFundMirror = false)
                    askMirror = false
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Покупки не будет")
            }
        }
    }
}

private val entryTime: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

private fun formatEntryTime(millis: Long): String =
    entryTime.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

private fun withAmount(text: String, amountKopecks: Long?): String {
    val money = amountKopecks?.let(::formatRubles)
    return listOf(text.trim(), money)
        .filter { !it.isNullOrBlank() }
        .joinToString(", ")
        .ifBlank { "—" }
}

private fun diaryCells(entry: PhaseDiaryEntryEntity): List<String> = listOf(
    formatEntryTime(entry.atMillis),
    withAmount(entry.context, entry.amountKopecks),
    entry.bodyMarker.ifBlank { "—" },
    when (entry.impulse) {
        "EMPTY" -> "П"
        "FULL" -> "Н"
        else -> entry.impulse
    },
    entry.fact.ifBlank { "—" },
)

private fun wellCells(entry: WellEntryEntity): List<String> = listOf(
    formatEntryTime(entry.atMillis),
    withAmount(entry.situation, entry.amountKopecks),
    when (entry.question) {
        "WELL" -> "Колодец"
        "POINT_C" -> "Точка C"
        else -> entry.question
    },
    entry.spasm.ifBlank { "—" },
    entry.pauseEffect.ifBlank { "—" },
)

private fun missedDays(ui: JournalUi): Int {
    if (ui.spends.isEmpty()) return 0
    val zone = ZoneId.systemDefault()
    val days = ui.spends.map { Instant.ofEpochMilli(it.atMillis).atZone(zone).toLocalDate() }.toSet()
    val today = LocalDate.now(zone)
    var cursor = days.min()
    var missed = 0
    while (cursor.isBefore(today)) {
        if (cursor !in days) missed += 1
        cursor = cursor.plusDays(1)
    }
    return missed
}
