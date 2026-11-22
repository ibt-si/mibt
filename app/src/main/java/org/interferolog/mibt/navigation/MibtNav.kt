// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook as FilledMenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook as OutlinedMenuBook
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map as FilledMap
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map as OutlinedMap
import androidx.compose.material.icons.outlined.Today as OutlinedToday
import androidx.compose.material.icons.outlined.Tune as OutlinedTune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.interferolog.mibt.JournalViewModel
import org.interferolog.mibt.content.BlankCanon
import org.interferolog.mibt.content.JournalCanon
import org.interferolog.mibt.currentWeekNumber
import org.interferolog.mibt.ui.CaptureScreen
import org.interferolog.mibt.ui.ClusterScreen
import org.interferolog.mibt.ui.CrisisScreen
import org.interferolog.mibt.ui.CrystalScreen
import org.interferolog.mibt.ui.ExportScreen
import org.interferolog.mibt.ui.FamilyScreen
import org.interferolog.mibt.ui.FanScreen
import org.interferolog.mibt.ui.GuideScreen
import org.interferolog.mibt.ui.HolterScreen
import org.interferolog.mibt.ui.IntakeScreen
import org.interferolog.mibt.ui.LocalBottomClearance
import org.interferolog.mibt.ui.MaintenanceScreen
import org.interferolog.mibt.ui.MapScreen
import org.interferolog.mibt.ui.PassportScreen
import org.interferolog.mibt.ui.PointCScreen
import org.interferolog.mibt.ui.PriceScreen
import org.interferolog.mibt.ui.QuickSpendScreen
import org.interferolog.mibt.ui.RestScreen
import org.interferolog.mibt.ui.ScriptsScreen
import org.interferolog.mibt.ui.SegmentedGroup
import org.interferolog.mibt.ui.SilenceGateScreen
import org.interferolog.mibt.ui.SourcesScreen
import org.interferolog.mibt.ui.WeekScreen
import org.interferolog.mibt.ui.WorkScreen

private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(HomeRoute::class)
            subclass(MapRoute::class)
            subclass(WeekRoute::class)
            subclass(SilenceGateRoute::class)
            subclass(HolterRoute::class)
            subclass(QuickSpendRoute::class)
            subclass(PauseRoute::class)
            subclass(PointCRoute::class)
            subclass(SourcesRoute::class)
            subclass(PriceRoute::class)
            subclass(WorkRoute::class)
            subclass(FamilyRoute::class)
            subclass(RestRoute::class)
            subclass(CrisisRoute::class)
            subclass(FanRoute::class)
            subclass(CapitalRoute::class)
            subclass(ClusterRoute::class)
            subclass(CrystalRoute::class)
            subclass(MaintenanceRoute::class)
            subclass(ScriptsRoute::class)
            subclass(ExportRoute::class)
            subclass(IntakeRoute::class)
            subclass(GuideRoute::class)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MibtNav(viewModel: JournalViewModel, openToken: Int, openTarget: String?) {
    val backStack = rememberNavBackStack(navConfig, HomeRoute)
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var captureSheet by remember { mutableStateOf(false) }

    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val toolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = FloatingToolbarExitDirection.Bottom,
    )

    val top = backStack.lastOrNull()
    val week = ui.currentWeekNumber()
    val hasCycle = ui.cycle != null
    val isTopLevel = top == HomeRoute || top == MapRoute || top == ScriptsRoute || top == MaintenanceRoute
    val showFloatingToolbar = hasCycle && isTopLevel
    val showStandaloneFab = hasCycle && top is WeekRoute
    val showGuideAction = top != null &&
        top !is SilenceGateRoute &&
        top !is GuideRoute &&
        !(top == HomeRoute && !hasCycle) &&
        (isTopLevel || top is WeekRoute)

    LaunchedEffect(notice) {
        val text = notice ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        viewModel.clearNotice()
    }
    LaunchedEffect(openToken) {
        when (openTarget) {
            "pause" -> backStack.reveal(PauseRoute)
            "pointc" -> backStack.reveal(PointCRoute)
        }
    }
    LaunchedEffect(top) {
        captureSheet = false
        topBarScrollBehavior.state.heightOffset = 0f
        toolbarScrollBehavior.state.offset = 0f
    }

    val title = screenTitle(top, week, !hasCycle)
    val subtitle = screenSubtitle(top, week, !hasCycle, title)
    val bottomClearance = if (showFloatingToolbar || showStandaloneFab) 104.dp else 16.dp

    Scaffold(
        modifier = Modifier
            .nestedScroll(topBarScrollBehavior.nestedScrollConnection)
            .nestedScroll(toolbarScrollBehavior),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.padding(bottom = if (showFloatingToolbar) 84.dp else 0.dp),
            )
        },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                subtitle = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                scrollBehavior = topBarScrollBehavior,
                navigationIcon = {
                    if (!isTopLevel && top != null) {
                        IconButton(onClick = { backStack.removeLastOrNull() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    if (showGuideAction) {
                        IconButton(onClick = { backStack.reveal(GuideRoute) }) {
                            Icon(Icons.Outlined.Info, contentDescription = "Как пользоваться")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalBottomClearance provides bottomClearance) {
                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        entry<HomeRoute> {
                            when {
                                ui.cycle == null -> GuideScreen(onStartTest = { backStack.reveal(IntakeRoute) })
                                week == null -> MaintenanceScreen(viewModel, backStack)
                                else -> WeekScreen(viewModel, week, backStack)
                            }
                        }
                        entry<GuideRoute> {
                            GuideScreen(onStartTest = if (ui.cycle == null) ({ backStack.reveal(IntakeRoute) }) else null)
                        }
                        entry<IntakeRoute> {
                            IntakeScreen(viewModel, onRecorded = { backStack.switchTopLevel(HomeRoute) })
                        }
                        entry<MapRoute> { MapScreen(viewModel, backStack) }
                        entry<WeekRoute> { route -> WeekScreen(viewModel, route.number, backStack) }
                        entry<SilenceGateRoute> { route ->
                            SilenceGateScreen {
                                backStack.removeLastOrNull()
                                backStack.add(decodeGate(route.target))
                            }
                        }
                        entry<HolterRoute> { HolterScreen(viewModel) }
                        entry<QuickSpendRoute> { QuickSpendScreen(viewModel) }
                        entry<PauseRoute> { CaptureScreen(viewModel) }
                        entry<PointCRoute> { PointCScreen(viewModel) }
                        entry<SourcesRoute> { SourcesScreen(viewModel) }
                        entry<PriceRoute> { PriceScreen(viewModel) }
                        entry<WorkRoute> { WorkScreen(viewModel) }
                        entry<FamilyRoute> { FamilyScreen(viewModel) }
                        entry<RestRoute> { RestScreen(viewModel) }
                        entry<CrisisRoute> { CrisisScreen(viewModel) }
                        entry<FanRoute> { FanScreen(viewModel) }
                        entry<CapitalRoute> { PassportScreen(viewModel) }
                        entry<ClusterRoute> { ClusterScreen(viewModel) }
                        entry<CrystalRoute> { CrystalScreen(viewModel) }
                        entry<MaintenanceRoute> { MaintenanceScreen(viewModel, backStack) }
                        entry<ScriptsRoute> { ScriptsScreen() }
                        entry<ExportRoute> { ExportScreen(viewModel) }
                    },
                )
            }

            if (showFloatingToolbar) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    scrollBehavior = toolbarScrollBehavior,
                    colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
                    floatingActionButton = {
                        FloatingToolbarDefaults.StandardFloatingActionButton(
                            onClick = { captureSheet = true },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Быстрая запись")
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = FloatingToolbarDefaults.ScreenOffset),
                ) {
                    ToolbarDestination(
                        selected = top == HomeRoute,
                        label = if (week == null) "Сегодня" else "Неделя",
                        filled = Icons.Filled.Today,
                        outlined = Icons.Outlined.OutlinedToday,
                        onClick = { backStack.switchTopLevel(HomeRoute) },
                    )
                    ToolbarDestination(
                        selected = top == MapRoute,
                        label = "Маршрут",
                        filled = Icons.Filled.FilledMap,
                        outlined = Icons.Outlined.OutlinedMap,
                        onClick = { backStack.switchTopLevel(MapRoute) },
                    )
                    ToolbarDestination(
                        selected = top == ScriptsRoute,
                        label = "Скрипты",
                        filled = Icons.AutoMirrored.Filled.FilledMenuBook,
                        outlined = Icons.AutoMirrored.Outlined.OutlinedMenuBook,
                        onClick = { backStack.switchTopLevel(ScriptsRoute) },
                    )
                    ToolbarDestination(
                        selected = top == MaintenanceRoute,
                        label = "Обслуживание",
                        filled = Icons.Filled.Tune,
                        outlined = Icons.Outlined.OutlinedTune,
                        onClick = { backStack.switchTopLevel(MaintenanceRoute) },
                    )
                }
            } else if (showStandaloneFab) {
                FloatingActionButton(
                    onClick = { captureSheet = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(16.dp),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Быстрая запись")
                }
            }
        }
    }

    if (captureSheet) {
        QuickCaptureSheet(
            onDismiss = { captureSheet = false },
            onSelect = { route ->
                captureSheet = false
                backStack.reveal(route)
            },
        )
    }
}

@Composable
private fun ToolbarDestination(
    selected: Boolean,
    label: String,
    filled: ImageVector,
    outlined: ImageVector,
    onClick: () -> Unit,
) {
    if (selected) {
        FilledTonalIconButton(onClick = onClick) {
            Icon(filled, contentDescription = label)
        }
    } else {
        IconButton(onClick = onClick) {
            Icon(outlined, contentDescription = label)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCaptureSheet(
    onDismiss: () -> Unit,
    onSelect: (NavKey) -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val items = listOf(
        Triple(PauseRoute, "Пауза 3 секунды", Icons.Filled.Pause) to "Дневник фазы: из пустоты или наполненности",
        Triple(QuickSpendRoute, "Быстрая запись траты", Icons.Filled.Payments) to "Разметка по узлам и правило двойного тарифа",
        Triple(PointCRoute, "Точка C", Icons.Filled.Adjust) to "Колодец или Точка C, спазм и эффект паузы",
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Быстрая запись",
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            SegmentedGroup(count = items.size) { index, shape ->
                val (triple, description) = items[index]
                val (route, title, icon) = triple
                ListItem(
                    onClick = { onSelect(route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape),
                    leadingContent = {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    supportingContent = { Text(description) },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    content = { Text(title, style = MaterialTheme.typography.titleMedium) },
                )
            }
        }
    }
}

private fun screenTitle(top: NavKey?, week: Int?, intake: Boolean): String = when (top) {
    null, HomeRoute -> when {
        intake -> "Как пользоваться журналом"
        week == null -> "Штатное обслуживание"
        else -> JournalCanon.week(week).title
    }
    GuideRoute -> "Как пользоваться журналом"
    MapRoute -> "Маршрут"
    is WeekRoute -> JournalCanon.week(top.number).title
    is SilenceGateRoute -> "Минута тишины"
    HolterRoute -> "Холтер четырёх узлов"
    QuickSpendRoute -> "Быстрая запись траты"
    PauseRoute -> BlankCanon.phaseDiary.title
    PointCRoute -> BlankCanon.pointC.title
    SourcesRoute -> "Индекс сопротивления источников"
    PriceRoute -> "Тренажёр цены"
    WorkRoute -> "Дела участка"
    FamilyRoute -> "Два контура"
    RestRoute -> "Масса покоя"
    CrisisRoute -> "Карта кризиса"
    FanRoute -> "Карта веера"
    CapitalRoute -> "Инвестиционный паспорт"
    ClusterRoute -> "Карта кластера"
    CrystalRoute -> "Кристалл смысла"
    MaintenanceRoute -> "Штатное обслуживание"
    ScriptsRoute -> "Речевые скрипты"
    ExportRoute -> "Экспорт телеметрии"
    IntakeRoute -> "Входной тест"
    else -> "Бортовой журнал"
}

private fun screenSubtitle(top: NavKey?, week: Int?, intake: Boolean, title: String): String {
    val context = when {
        top is IntakeRoute -> "Входной тест"
        intake -> "Сначала порядок работы, затем тест"
        top is GuideRoute -> "Порядок работы с журналом"
        top is MapRoute -> "Маршрут цикла"
        top is ScriptsRoute -> "Фразы и шаблоны"
        top is ExportRoute -> "Markdown"
        top is WeekRoute -> "Неделя ${top.number}"
        top is HolterRoute || top is QuickSpendRoute -> "Неделя 3. Бланк"
        top is SourcesRoute -> "Неделя 4. Бланк"
        top is PriceRoute -> "Недели 4–5. Тренажёр цены"
        top is WorkRoute -> "Неделя 5. Бланк"
        top is FamilyRoute -> "Неделя 6. Бланк"
        top is RestRoute -> "Неделя 7. Бланк"
        top is CrisisRoute -> "Неделя 8. Бланк"
        top is FanRoute -> "Неделя 9. Бланк"
        top is CapitalRoute -> "Неделя 10. Бланк"
        top is ClusterRoute -> "Неделя 11. Бланк"
        top is CrystalRoute -> "Неделя 12. Бланк"
        top is SilenceGateRoute -> "Минута перед расчётом"
        week == null -> "Штатное обслуживание"
        else -> "Неделя $week"
    }
    return if (context == title) "Бортовой журнал" else context
}
