// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ScrollState

@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    clearFab: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val margin = if (LocalConfiguration.current.screenWidthDp >= 600) 24.dp else 16.dp
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = margin)
            .padding(top = 16.dp, bottom = if (clearFab) 96.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLargeEmphasized,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
fun Field(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    number: Boolean = false,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text,
        ),
    )
}

@Composable
fun BodyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
}

@Composable
fun MetaText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun HeroNumber(value: String, caption: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.displaySmallEmphasized,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            caption,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

enum class CardTone { Surface, Primary, Tertiary, Error }

@Composable
fun QuietCard(text: String, tone: CardTone = CardTone.Surface) {
    val scheme = MaterialTheme.colorScheme
    val (background, foreground) = when (tone) {
        CardTone.Surface -> scheme.surfaceContainer to scheme.onSurface
        CardTone.Primary -> scheme.primaryContainer to scheme.onPrimaryContainer
        CardTone.Tertiary -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        CardTone.Error -> scheme.errorContainer to scheme.onErrorContainer
    }
    Surface(color = background, contentColor = foreground, shape = MaterialTheme.shapes.large) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun ChoiceGroup(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    ButtonGroup(
        overflowIndicator = { menuState -> ButtonGroupDefaults.OverflowIndicator(menuState) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEach { (id, label) ->
            toggleableItem(
                checked = selected == id,
                label = label,
                onCheckedChange = { onSelect(id) },
                weight = 1f,
            )
        }
    }
}

@Composable
fun ActionGroup(options: List<Pair<String, () -> Unit>>, enabled: Boolean = true) {
    ButtonGroup(
        overflowIndicator = { menuState -> ButtonGroupDefaults.OverflowIndicator(menuState) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEach { (label, action) ->
            clickableItem(onClick = action, label = label, enabled = enabled, weight = 1f)
        }
    }
}

@Composable
fun ChoiceChips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (id, label) ->
            FilterChip(
                selected = selected == id,
                onClick = { onSelect(id) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f).padding(end = 16.dp), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun CommitButton(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelLargeEmphasized)
    }
}

@Composable
fun TonalButton(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(text)
    }
}

@Composable
fun RemoveButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val error = MaterialTheme.colorScheme.error
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = error),
        border = BorderStroke(1.dp, error),
    ) {
        Text(text)
    }
}
