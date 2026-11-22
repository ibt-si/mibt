// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.interferolog.mibt.content.BlankCanon
import org.interferolog.mibt.content.ReferenceTable as CanonReferenceTable

data class EntryLine(val id: Long, val cells: List<String>)

@Composable
fun BlankHeader(blank: BlankCanon) {
    SectionHeader(blank.title)
}

@Composable
fun ReferenceTable(table: CanonReferenceTable, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(true) }
    val section = table.title ?: table.header.joinToString(" · ")
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = expanded, role = Role.Button, onValueChange = { expanded = it }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                section,
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
            )
        }
        if (expanded) {
            table.rows.forEach { row ->
                LabeledFields(table.header, row, secondary = false)
            }
        }
    }
}

@Composable
fun ExampleRows(
    columns: List<String>,
    examples: List<List<String>>,
    modifier: Modifier = Modifier,
) {
    if (examples.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        examples.forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToneBadge("Пример", CardTone.Secondary)
                LabeledFields(columns, row, secondary = true)
            }
        }
    }
}

@Composable
fun EntryList(
    columns: List<String>,
    entries: List<EntryLine>,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onEdit: ((Long) -> Unit)? = null,
) {
    var pendingId by remember { mutableStateOf<Long?>(null) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (entries.isEmpty()) {
            MetaText("Записей пока нет.")
        }
        entries.forEach { entry ->
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        columns.zip(entry.cells).forEach { (label, cell) ->
                            FieldLine(label, cell, secondary = false)
                        }
                    }
                    if (onEdit != null) {
                        EditIconButton("Править") { onEdit(entry.id) }
                    }
                    DeleteIconButton("Убрать запись") { pendingId = entry.id }
                }
            }
        }
    }
    val id = pendingId
    if (id != null) {
        AlertDialog(
            onDismissRequest = { pendingId = null },
            title = { Text("Убрать запись?") },
            text = { Text("Строка исчезнет из журнала.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingId = null
                        onDelete(id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Убрать") }
            },
            dismissButton = {
                TextButton(onClick = { pendingId = null }) { Text("Оставить") }
            },
        )
    }
}

@Composable
fun ColumnCard(
    title: String,
    columns: List<String>,
    cells: List<String>,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        columns.zip(cells).forEach { (label, cell) ->
            FieldLine(label, cell, secondary = false)
        }
    }
}

@Composable
private fun LabeledFields(labels: List<String>, cells: List<String>, secondary: Boolean) {
    SectionCard {
        labels.zip(cells).forEach { (label, cell) ->
            FieldLine(label, cell, secondary)
        }
    }
}

@Composable
private fun FieldLine(label: String, cell: String, secondary: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            cell,
            style = if (secondary) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            color = if (secondary) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
