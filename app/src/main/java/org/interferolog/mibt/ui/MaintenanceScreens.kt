// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.interferolog.mibt.JournalViewModel
import org.interferolog.mibt.content.JournalCanon
import org.interferolog.mibt.data.ReminderPlanEntity
import org.interferolog.mibt.domain.repairWeekForMarker
import org.interferolog.mibt.navigation.ExportRoute
import org.interferolog.mibt.navigation.IntakeRoute
import org.interferolog.mibt.navigation.WeekRoute
import org.interferolog.mibt.navigation.reveal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

@Composable
fun MaintenanceScreen(viewModel: JournalViewModel, backStack: NavBackStack<NavKey>) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var note by remember { mutableStateOf("") }
    val anchor = ui.cycle?.completedAt ?: ui.cycle?.startedAt ?: System.currentTimeMillis()
    val scheme = MaterialTheme.colorScheme

    ScreenColumn {
        SectionHeader("Четыре ритма")
        Field("Заметка к отметке ритма", note, { note = it }, singleLine = false)
        SegmentedGroup(count = JournalCanon.rhythms.size) { index, shape ->
            val (rhythm, label) = JournalCanon.rhythms[index]
            val last = ui.maintenance.filter { it.rhythm == rhythm }.maxByOrNull { it.atMillis }
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape),
                supportingContent = {
                    Text(
                        if (last == null) "Записей пока нет" else "Последняя отметка: ${formatDate(last.atMillis)}",
                    )
                },
                trailingContent = {
                    TonalButton(
                        text = "Отметить",
                        enabled = ui.cycle != null,
                    ) {
                        viewModel.logMaintenance(rhythm, note)
                        note = ""
                    }
                },
                colors = ListItemDefaults.colors(containerColor = scheme.surfaceContainerLow),
                content = { Text(label, style = MaterialTheme.typography.bodyMedium) },
            )
        }

        SectionHeader("Пять маркеров")
        SegmentedGroup(count = JournalCanon.markers.size) { index, shape ->
            val label = JournalCanon.markers[index]
            val marker = index + 1
            val last = ui.markers.filter { it.marker == marker && it.marked }.maxByOrNull { it.atMillis }
            val stale = System.currentTimeMillis() - (last?.atMillis ?: anchor) > 14L * 24 * 60 * 60 * 1000
            val needsRepair = stale && ui.cycle?.status == "COMPLETED"
            val repairWeek = repairWeekForMarker(marker)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape),
            ) {
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    leadingContent = {
                        Icon(
                            if (last != null) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (needsRepair) scheme.error else if (last != null) scheme.primary else scheme.outline,
                        )
                    },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (last == null) "Не отмечен" else "Отмечен ${formatDate(last.atMillis)}",
                            )
                            if (needsRepair) {
                                Text(
                                    "Не отмечен дольше двух недель. Адрес ремонта — неделя $repairWeek.",
                                    color = scheme.error,
                                )
                                TonalButton("Открыть неделю $repairWeek") {
                                    backStack.reveal(WeekRoute(repairWeek))
                                }
                            }
                        }
                    },
                    trailingContent = {
                        TonalButton(
                            text = if (last == null) "Отметить" else "Обновить",
                            enabled = ui.cycle != null,
                        ) {
                            viewModel.setMarker(marker, true)
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (needsRepair) scheme.errorContainer else scheme.surfaceContainerLow,
                    ),
                    content = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                )
            }
        }

        SectionHeader("Три бесшумных напоминания")
        SectionCard {
            ReminderEditor(viewModel, showHeader = false)
        }

        SectionHeader("Инструменты журнала")
        SectionCard {
            TonalButton("Экспорт в Markdown", modifier = Modifier.fillMaxWidth()) {
                backStack.reveal(ExportRoute)
            }
            OutlinedButton(
                onClick = { backStack.reveal(IntakeRoute) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Пройти тест заново")
            }
        }
    }
}

@Composable
fun ScriptsScreen() {
    ScreenColumn {
        MetaText("Реплики приведены дословно по главам книги.")
        JournalCanon.scripts.forEach { card ->
            SectionCard(tone = if (card.quoted) CardTone.Primary else CardTone.Surface) {
                Text(card.title, style = MaterialTheme.typography.titleMedium)
                card.intro?.let { MetaText(it) }
                BodyText(card.body)
                card.marker?.let { marker ->
                    SectionCard(tone = CardTone.Tertiary) {
                        BodyText(marker)
                    }
                }
                MetaText(card.where)
            }
        }
    }
}

@Composable
fun ExportScreen(viewModel: JournalViewModel) {
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) viewModel.exportTo(uri)
    }
    ScreenColumn {
        SectionCard {
            BodyText("Файл Markdown содержит суммы открытым текстом. Его можно сохранить или отправить через системное меню.")
            CommitButton("Создать файл") {
                create.launch("mibt-telemetry-${LocalDate.now()}.md")
            }
        }
    }
}

@Composable
fun ReminderEditor(viewModel: JournalViewModel, showHeader: Boolean = true) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val reminders = ui.reminders
    var enabled by remember(reminders) { mutableStateOf(reminders?.enabled ?: true) }
    var first by remember(reminders) { mutableIntStateOf(reminders?.minute1 ?: 9 * 60) }
    var second by remember(reminders) { mutableIntStateOf(reminders?.minute2 ?: 14 * 60) }
    var third by remember(reminders) { mutableIntStateOf(reminders?.minute3 ?: 20 * 60) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    if (showHeader) {
        SectionTitle("Три бесшумных напоминания")
    }
    MetaText("По сигналу — вес в стопах, выдох и вопрос Точки C. Канал тихий, без значка вины.")
    SwitchRow("Напоминания включены", enabled) { enabled = it }
    TimePickerField("Первое напоминание", first) { first = it }
    TimePickerField("Второе напоминание", second) { second = it }
    TimePickerField("Третье напоминание", third) { third = it }
    TonalButton("Сохранить напоминания", modifier = Modifier.fillMaxWidth()) {
        if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        viewModel.saveReminders(
            ReminderPlanEntity(
                enabled = enabled,
                minute1 = first,
                minute2 = second,
                minute3 = third,
            ),
        )
    }
}
