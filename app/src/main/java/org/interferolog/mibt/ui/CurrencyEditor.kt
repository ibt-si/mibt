// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.interferolog.mibt.JournalViewModel
import org.interferolog.mibt.domain.CurrencyChoice

/** Currency choice shown in "Инструменты журнала". It applies to newly created cycles only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyEditor(viewModel: JournalViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val selected = CurrencyChoice.fromCode(ui.settings?.currencyCode ?: CurrencyChoice.systemDefault().code)
    val search = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val query = search.text.toString().trim()
    val choices = remember(query) {
        CurrencyChoice.allChoices().filter { choice ->
            query.isEmpty() || listOf(choice.code, choice.label, choice.symbol)
                .any { it.contains(query, ignoreCase = true) }
        }
    }
    val colors = MaterialTheme.colorScheme

    SectionTitle("Валюта новых циклов")
    MetaText("Суммы в текущем и старых циклах сохраняют валюту, в которой записаны.")
    ui.cycle?.let { cycle ->
        val current = CurrencyChoice.fromCode(cycle.currency)
        MetaText("Валюта текущего цикла: ${current.label} (${current.code})")
    }
    SearchBarDefaults.InputField(
        textFieldState = search,
        searchBarState = searchBarState,
        onSearch = {},
        placeholder = { Text("Поиск валюты") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
    )
    LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
        items(choices, key = { it.code }) { choice ->
            val active = choice.code == selected.code
            val textColor = if (active) colors.onPrimaryContainer else colors.onSurface
            val supportingColor = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { viewModel.setCurrency(choice.code) },
                supportingContent = {
                    Text("${choice.code} · ${choice.symbol}", color = supportingColor)
                },
                trailingContent = {
                    if (active) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Выбрано",
                            tint = colors.onPrimaryContainer,
                        )
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = if (active) colors.primaryContainer else colors.surfaceContainerLow,
                ),
                content = {
                    Text(
                        choice.label,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
            )
        }
    }
}
