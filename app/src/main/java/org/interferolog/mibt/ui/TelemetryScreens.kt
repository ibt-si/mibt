package org.interferolog.mibt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import org.interferolog.mibt.JournalViewModel
import org.interferolog.mibt.content.JournalCanon
import org.interferolog.mibt.content.POINT_C
import org.interferolog.mibt.domain.formatRubles
import org.interferolog.mibt.domain.parseRubles
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun CaptureScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var amount by remember { mutableStateOf("") }
    var context by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var fact by remember { mutableStateOf("") }
    ScreenColumn {
        BodyText("Время запишется в момент выбора. Минута тишины здесь не нужна.")
        Field("Сумма, ₽", amount, { amount = it }, number = true)
        Field("Контекст", context, { context = it })
        Field("Телесный маркер", body, { body = it })
        Field("Факт без оценки", fact, { fact = it }, singleLine = false)
        ActionGroup(
            options = listOf(
                "Из пустоты" to { viewModel.saveDiary(parseRubles(amount), context, body, "EMPTY", fact) },
                "Из наполненности" to { viewModel.saveDiary(parseRubles(amount), context, body, "FULL", fact) },
            ),
            enabled = ui.cycle != null,
        )
        ui.diary.take(5).forEach { entry ->
            MetaText("${if (entry.impulse == "EMPTY") "П" else "Н"} · ${entry.amountKopecks?.let(::formatRubles) ?: "без суммы"} · ${entry.context}")
        }
    }
}

@Composable
fun PointCScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var situation by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("POINT_C") }
    var spasm by remember { mutableStateOf(JournalCanon.spasms.first()) }
    var effect by remember { mutableStateOf("") }
    ScreenColumn {
        Text(POINT_C, style = MaterialTheme.typography.titleLargeEmphasized)
        Field("Ситуация", situation, { situation = it })
        Field("Сумма, ₽", amount, { amount = it }, number = true)
        ChoiceGroup(
            options = listOf("WELL" to "Колодец", "POINT_C" to "Точка C"),
            selected = question,
            onSelect = { question = it },
        )
        ChoiceChips(
            options = JournalCanon.spasms.map { it to it },
            selected = spasm,
            onSelect = { spasm = it },
        )
        Field("Что изменила пауза", effect, { effect = it }, singleLine = false)
        CommitButton(
            text = "Записать",
            enabled = ui.cycle != null,
            onClick = { viewModel.saveWell(situation, parseRubles(amount), question, spasm, effect) },
        )
    }
}

@Composable
fun QuickSpendScreen(viewModel: JournalViewModel) {
    ScreenColumn {
        SpendForm(viewModel, cleanSheet = false, title = null)
    }
}

@Composable
fun HolterScreen(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var left by remember { mutableIntStateOf(240) }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000)
            left -= 1
        }
    }
    val missed = missedDays(ui)
    ScreenColumn {
        BodyText("Четыре минуты на разметку дня. Осталось ${left / 60}:${"%02d".format(left % 60)}. Таймер не закрывает запись.")
        BodyText(etaLine(ui))
        if (missed > 0) {
            QuietCard(
                "Пропущенные дни можно закрыть одной строкой в Шум и продолжить с сегодняшнего вечера. Пропущено дней: $missed.",
                CardTone.Tertiary,
            )
            CleanSheetForm(viewModel)
        }
        SpendForm(viewModel, cleanSheet = false, title = "Трата дня")
        SectionTitle("Зеркальные счета")
        ui.bills.forEach { bill ->
            val settled = bill.settledAt != null
            MetaText("${formatRubles(bill.amountKopecks)} · ${if (settled) "перевод сделан" else "к переводу в Рост"}")
            if (!settled) OutlinedButton(onClick = { viewModel.settleBill(bill) }) { Text("Перевод сделан") }
        }
    }
}

@Composable
private fun CleanSheetForm(viewModel: JournalViewModel) {
    var amount by remember { mutableStateOf("") }
    Field("Забытая разница, ₽", amount, { amount = it }, number = true)
    TonalButton("Закрыть пропуск одной строкой") {
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
    if (title != null) SectionTitle(title)
    Field("Сумма, ₽", amount, { amount = it }, number = true)
    Field("Заметка", note, { note = it })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            listOf("U1" to "Опора", "U2" to "Инструмент"),
            listOf("U3" to "Рост", "U4" to "Шум"),
            listOf("OBZ" to "Обз", "INCOME" to "Доход"),
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (id, label) ->
                    FilterChip(
                        selected = node == id,
                        onClick = { node = id },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
    CommitButton("Записать") {
        val value = parseRubles(amount) ?: return@CommitButton
        if (node == "U4" && !cleanSheet) askMirror = true else {
            viewModel.saveSpend(node, value, note, cleanSheet, canFundMirror = false)
            amount = ""
        }
    }
    if (askMirror) {
        QuietCard(
            "Трата в Шум открывает зеркальный счёт на ту же сумму в Рост. Если двойной цены сейчас нет, покупка не записывается.",
            CardTone.Tertiary,
        )
        CommitButton("Перевести столько же") {
            val value = parseRubles(amount) ?: return@CommitButton
            viewModel.saveSpend(node, value, note, false, canFundMirror = true)
            askMirror = false
            amount = ""
        }
        OutlinedButton(onClick = {
            val value = parseRubles(amount) ?: return@OutlinedButton
            viewModel.saveSpend(node, value, note, false, canFundMirror = false)
            askMirror = false
        }) { Text("Покупки не будет") }
    }
}

private fun missedDays(ui: org.interferolog.mibt.data.JournalUi): Int {
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
