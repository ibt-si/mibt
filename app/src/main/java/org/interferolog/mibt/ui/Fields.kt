// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import org.interferolog.mibt.domain.CurrencyChoice

val LocalBottomClearance = compositionLocalOf { 16.dp }

/** Currency of the cycle currently on screen; new cycles use the value from settings. */
val LocalMoneyCurrency = compositionLocalOf<CurrencyChoice> { CurrencyChoice.RUB }

@Composable
fun moneyCurrency(): CurrencyChoice = LocalMoneyCurrency.current

@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    clearFab: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val margin = if (LocalConfiguration.current.screenWidthDp >= 600) 24.dp else 16.dp
    val bottomPadding = if (clearFab) LocalBottomClearance.current else 16.dp
    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(scrollState)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = margin)
                .padding(top = 12.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLargeEmphasized,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLargeEmphasized)
        if (!subtitle.isNullOrBlank()) {
            MetaText(subtitle)
        }
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    number: Boolean = false,
    integer: Boolean = false,
    singleLine: Boolean = true,
    suffix: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    imeAction: ImeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                integer -> KeyboardType.Number
                number -> KeyboardType.Decimal
                else -> KeyboardType.Text
            },
            imeAction = imeAction,
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

enum class CardTone { Surface, Primary, Secondary, Tertiary, Error }

@Composable
fun toneColors(tone: CardTone): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (tone) {
        CardTone.Surface -> scheme.surfaceContainerLow to scheme.onSurface
        CardTone.Primary -> scheme.primaryContainer to scheme.onPrimaryContainer
        CardTone.Secondary -> scheme.secondaryContainer to scheme.onSecondaryContainer
        CardTone.Tertiary -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        CardTone.Error -> scheme.errorContainer to scheme.onErrorContainer
    }
}

@Composable
fun QuietCard(text: String, tone: CardTone = CardTone.Surface) {
    val (background, foreground) = toneColors(tone)
    Surface(
        color = background,
        contentColor = foreground,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.Surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val (background, foreground) = toneColors(tone)
    Surface(
        color = background,
        contentColor = foreground,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun MetricCard(
    value: String,
    caption: String,
    status: String? = null,
    tone: CardTone = CardTone.Primary,
) {
    val (background, foreground) = toneColors(tone)
    Surface(
        color = background,
        contentColor = foreground,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(value, style = MaterialTheme.typography.displaySmallEmphasized)
            Text(caption, style = MaterialTheme.typography.titleMedium)
            if (!status.isNullOrBlank()) {
                Text(
                    status,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun ToneBadge(text: String, tone: CardTone) {
    val (background, foreground) = toneColors(tone)
    Surface(
        color = background,
        contentColor = foreground,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun SegmentedGroup(
    count: Int,
    modifier: Modifier = Modifier,
    itemContent: @Composable (index: Int, shape: Shape) -> Unit,
) {
    if (count <= 0) return
    val outer = 20.dp
    val inner = 4.dp
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(count) { index ->
            val shape = when {
                count == 1 -> RoundedCornerShape(outer)
                index == 0 -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
                index == count - 1 -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
                else -> RoundedCornerShape(inner)
            }
            itemContent(index, shape)
        }
    }
}

@Composable
fun ChoiceGroup(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (id, label) ->
            SegmentedButton(
                selected = selected == id,
                onClick = { onSelect(id) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}

@Composable
fun ActionGroup(
    options: List<Pair<String, () -> Unit>>,
    enabled: Boolean = true,
    stacked: Boolean = false,
) {
    if (stacked || options.any { it.first.length > 18 }) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { index, (label, action) ->
                if (index == 0) {
                    CommitButton(text = label, enabled = enabled, onClick = action)
                } else {
                    TonalButton(
                        text = label,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = action,
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { index, (label, action) ->
                if (index == 0) {
                    CommitButton(
                        text = label,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        onClick = action,
                    )
                } else {
                    TonalButton(
                        text = label,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        onClick = action,
                    )
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange = 1..10,
) {
    val sliderState = remember(range) {
        SliderState(
            value = value.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            trackRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
    LaunchedEffect(value) {
        if (sliderState.value.roundToInt() != value) {
            sliderState.value = value.toFloat()
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            ToneBadge("$value / ${range.last}", CardTone.Secondary)
        }
        Slider(
            state = sliderState,
            onValueChange = { raw ->
                sliderState.value = raw
                onValueChange(raw.roundToInt().coerceIn(range.first, range.last))
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun UnsettableScaleSelector(
    label: String,
    selected: Int?,
    range: IntRange = 1..10,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            MetaText(selected?.let { "$it / ${range.last}" } ?: "не выбрано")
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            range.forEach { number ->
                FilterChip(
                    selected = selected == number,
                    onClick = { onSelect(number) },
                    label = { Text(number.toString()) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    label: String,
    minuteOfDay: Int,
    onPick: (Int) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val formatted = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
    Surface(
        onClick = { open = true },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    formatted,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (open) {
        val state = rememberTimePickerState(
            initialHour = (minuteOfDay / 60).coerceIn(0, 23),
            initialMinute = (minuteOfDay % 60).coerceIn(0, 59),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPick(state.hour * 60 + state.minute)
                        open = false
                    },
                ) {
                    Text("Готово")
                }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Отмена") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    isoDate: String,
    onDateChange: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val parsed = runCatching { LocalDate.parse(isoDate) }.getOrElse { LocalDate.now() }
    val formatted = remember(parsed) {
        parsed.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }
    Surface(
        onClick = { open = true },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                MetaText(label)
                Text(formatted, style = MaterialTheme.typography.titleMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onDateChange(parsed.minusDays(1).toString()) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Предыдущий день")
                }
                IconButton(onClick = { open = true }) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = "Выбрать дату")
                }
                IconButton(onClick = { onDateChange(parsed.plusDays(1).toString()) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Следующий день")
                }
            }
        }
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = parsed.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onDateChange(picked.toString())
                        }
                        open = false
                    },
                ) {
                    Text("Выбрать")
                }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Отмена") }
            },
        ) {
            DatePicker(state = state)
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
        Text(
            label,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun CommitButton(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
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
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        Text(text)
    }
}

@Composable
fun RemoveButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val error = MaterialTheme.colorScheme.error
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = error),
        border = BorderStroke(1.dp, error),
    ) {
        Text(text)
    }
}

@Composable
fun EditIconButton(contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            Icons.Outlined.Edit,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun DeleteIconButton(contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            Icons.Outlined.Delete,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
fun NoCycleNotice() {
    ScreenColumn {
        QuietCard(
            "Сначала пройдите входной тест, чтобы открыть рабочий бланк цикла.",
            CardTone.Secondary,
        )
    }
}
