package org.interferolog.mibt.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.interferolog.mibt.JournalViewModel
import org.interferolog.mibt.content.JournalCanon
import org.interferolog.mibt.data.ReminderPlanEntity
import org.interferolog.mibt.domain.repairWeekForMarker
import org.interferolog.mibt.navigation.GuideRoute
import org.interferolog.mibt.navigation.IntakeRoute
import org.interferolog.mibt.navigation.WeekRoute
import java.time.LocalDate

@Composable
fun MaintenanceScreen(viewModel: JournalViewModel, backStack: NavBackStack<NavKey>) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var note by remember { mutableStateOf("") }
    val anchor = ui.cycle?.completedAt ?: ui.cycle?.startedAt ?: System.currentTimeMillis()
    ScreenColumn {
        JournalCanon.rhythms.forEach { (rhythm, label) ->
            val last = ui.maintenance.firstOrNull { it.rhythm == rhythm }
            BodyText(label)
            MetaText(if (last == null) "Записи этого ритма ещё нет." else "Последняя запись есть.")
            TonalButton("Отметить ритм", enabled = ui.cycle != null) { viewModel.logMaintenance(rhythm, note) }
        }
        Field("Заметка к ритму", note, { note = it }, singleLine = false)
        SectionTitle("Пять маркеров")
        JournalCanon.markers.forEachIndexed { index, label ->
            val marker = index + 1
            val last = ui.markers.filter { it.marker == marker && it.marked }.maxByOrNull { it.atMillis }
            val stale = System.currentTimeMillis() - (last?.atMillis ?: anchor) > 14L * 24 * 60 * 60 * 1000
            BodyText(label)
            TonalButton(
                text = if (last == null) "Отметить" else "Отмечено",
                enabled = ui.cycle != null,
                onClick = { viewModel.setMarker(marker, true) },
            )
            if (stale && ui.cycle?.status == "COMPLETED") {
                val week = repairWeekForMarker(marker)
                QuietCard("Этот маркер не отмечен дольше двух недель. Адрес ремонта — неделя $week.", CardTone.Error)
                TonalButton("Открыть неделю $week") { backStack.add(WeekRoute(week)) }
            }
        }
        ReminderEditor(viewModel)
        TonalButton("Как пользоваться журналом") { backStack.add(GuideRoute) }
        OutlinedButton(onClick = { backStack.add(IntakeRoute) }) { Text("Пройти тест заново") }
        TonalButton("Экспорт в Markdown") { backStack.add(org.interferolog.mibt.navigation.ExportRoute) }
    }
}

@Composable
fun ScriptsScreen() {
    ScreenColumn {
        BodyText("Где в журнале есть готовая фраза, она приведена дословно. Где журнал только отсылает к главе, ниже подсказка, что записать своими словами по шаблону журнала.")
        JournalCanon.scripts.forEach { card ->
            SectionTitle(card.title)
            if (card.quoted) {
                QuietCard(card.body)
            } else {
                BodyText(card.body)
            }
            MetaText(card.where)
        }
    }
}

@Composable
fun ExportScreen(viewModel: JournalViewModel) {
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) viewModel.exportTo(uri)
    }
    ScreenColumn {
        BodyText("Файл Markdown содержит суммы открытым текстом. Его можно сохранить или отправить через системное меню.")
        CommitButton("Создать файл") { create.launch("mibt-telemetry-${LocalDate.now()}.md") }
    }
}

@Composable
fun ReminderEditor(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val reminders = ui.reminders
    var enabled by remember(reminders) { mutableStateOf(reminders?.enabled ?: true) }
    var first by remember(reminders) { mutableStateOf(minutesLabel(reminders?.minute1 ?: 9 * 60)) }
    var second by remember(reminders) { mutableStateOf(minutesLabel(reminders?.minute2 ?: 14 * 60)) }
    var third by remember(reminders) { mutableStateOf(minutesLabel(reminders?.minute3 ?: 20 * 60)) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    SectionTitle("Три бесшумных напоминания")
    BodyText("По сигналу — вес в стопах, выдох и вопрос Точки C. Канал тихий, без значка вины.")
    SwitchRow("Напоминания включены", enabled) { enabled = it }
    Field("Первое, чч:мм", first, { first = it })
    Field("Второе, чч:мм", second, { second = it })
    Field("Третье, чч:мм", third, { third = it })
    TonalButton("Сохранить напоминания") {
        if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        viewModel.saveReminders(
            ReminderPlanEntity(
                enabled = enabled,
                minute1 = parseMinute(first),
                minute2 = parseMinute(second),
                minute3 = parseMinute(third),
            ),
        )
    }
}

private fun minutesLabel(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

private fun parseMinute(text: String): Int {
    val parts = text.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return (hour.coerceIn(0, 23) * 60) + minute.coerceIn(0, 59)
}
