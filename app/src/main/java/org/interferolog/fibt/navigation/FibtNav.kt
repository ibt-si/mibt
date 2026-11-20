package org.interferolog.fibt.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map as FilledMap
import androidx.compose.material.icons.automirrored.filled.MenuBook as FilledMenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.Map as OutlinedMap
import androidx.compose.material.icons.automirrored.outlined.MenuBook as OutlinedMenuBook
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.interferolog.fibt.JournalViewModel
import org.interferolog.fibt.content.JournalCanon
import org.interferolog.fibt.currentWeekNumber
import org.interferolog.fibt.ui.CaptureScreen
import org.interferolog.fibt.ui.CrisisScreen
import org.interferolog.fibt.ui.CrystalScreen
import org.interferolog.fibt.ui.ExportScreen
import org.interferolog.fibt.ui.FamilyScreen
import org.interferolog.fibt.ui.FanScreen
import org.interferolog.fibt.ui.HolterScreen
import org.interferolog.fibt.ui.IntakeScreen
import org.interferolog.fibt.ui.MaintenanceScreen
import org.interferolog.fibt.ui.MapScreen
import org.interferolog.fibt.ui.PassportScreen
import org.interferolog.fibt.ui.PointCScreen
import org.interferolog.fibt.ui.PriceScreen
import org.interferolog.fibt.ui.QuickSpendScreen
import org.interferolog.fibt.ui.RestScreen
import org.interferolog.fibt.ui.ScriptsScreen
import org.interferolog.fibt.ui.SilenceGateScreen
import org.interferolog.fibt.ui.SourcesScreen
import org.interferolog.fibt.ui.WeekScreen
import org.interferolog.fibt.ui.WorkScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.ImageVector

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
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FibtNav(viewModel: JournalViewModel, openToken: Int, openTarget: String?) {
    val backStack = rememberNavBackStack(navConfig, HomeRoute)
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    LaunchedEffect(notice) {
        val text = notice ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        viewModel.clearNotice()
    }
    LaunchedEffect(openToken) {
        when (openTarget) {
            "pause" -> if (backStack.lastOrNull() != PauseRoute) backStack.add(PauseRoute)
            "pointc" -> if (backStack.lastOrNull() != PointCRoute) backStack.add(PointCRoute)
        }
    }
    val top = backStack.lastOrNull()
    val week = ui.currentWeekNumber()
    val title = screenTitle(top, week, ui.cycle == null)
    val subtitle = screenSubtitle(top, week, ui.cycle == null, title)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                subtitle = { Text(subtitle) },
                navigationIcon = {
                    if (top != HomeRoute && top != null) {
                        IconButton(onClick = { backStack.removeLastOrNull() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    if (top !is SilenceGateRoute) {
                        DestinationAction(
                            selected = top is MapRoute,
                            label = "Маршрут",
                            filled = Icons.Filled.FilledMap,
                            outlined = Icons.Outlined.OutlinedMap,
                            onClick = { backStack.reveal(MapRoute) },
                        )
                        DestinationAction(
                            selected = top is ScriptsRoute,
                            label = "Скрипты",
                            filled = Icons.AutoMirrored.Filled.FilledMenuBook,
                            outlined = Icons.AutoMirrored.Outlined.OutlinedMenuBook,
                            onClick = { backStack.reveal(ScriptsRoute) },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (top !is SilenceGateRoute) {
                FloatingActionButtonMenu(
                    expanded = menu,
                    button = {
                        ToggleFloatingActionButton(
                            checked = menu,
                            onCheckedChange = { menu = it },
                        ) {
                            Icon(
                                if (menu) Icons.Filled.Close else Icons.Filled.Edit,
                                contentDescription = if (menu) "Закрыть" else "Запись",
                            )
                        }
                    },
                ) {
                    FloatingActionButtonMenuItem(
                        onClick = { menu = false; backStack.reveal(PauseRoute) },
                        text = { Text("Пауза") },
                        icon = { Icon(Icons.Filled.Pause, contentDescription = null) },
                    )
                    FloatingActionButtonMenuItem(
                        onClick = { menu = false; backStack.reveal(QuickSpendRoute) },
                        text = { Text("Трата") },
                        icon = { Icon(Icons.Filled.Payments, contentDescription = null) },
                    )
                    FloatingActionButtonMenuItem(
                        onClick = { menu = false; backStack.reveal(PointCRoute) },
                        text = { Text("Точка C") },
                        icon = { Icon(Icons.Filled.Adjust, contentDescription = null) },
                    )
                }
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            modifier = androidx.compose.ui.Modifier.padding(padding),
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<HomeRoute> {
                    when {
                        ui.cycle == null -> IntakeScreen(viewModel)
                        week == null -> MaintenanceScreen(viewModel, backStack)
                        else -> WeekScreen(viewModel, week, backStack)
                    }
                }
                entry<IntakeRoute> { IntakeScreen(viewModel) }
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
                entry<ClusterRoute> { org.interferolog.fibt.ui.ClusterScreen(viewModel) }
                entry<CrystalRoute> { CrystalScreen(viewModel) }
                entry<MaintenanceRoute> { MaintenanceScreen(viewModel, backStack) }
                entry<ScriptsRoute> { ScriptsScreen() }
                entry<ExportRoute> { ExportScreen(viewModel) }
            },
        )
    }
}

@Composable
private fun DestinationAction(
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

private fun screenTitle(top: NavKey?, week: Int?, intake: Boolean): String = when (top) {
    null, HomeRoute -> when {
        intake -> "Входной тест"
        week == null -> "Штатное обслуживание"
        else -> JournalCanon.week(week).title
    }
    MapRoute -> "Маршрут"
    is WeekRoute -> JournalCanon.week(top.number).title
    is SilenceGateRoute -> "Минута тишины"
    HolterRoute -> "Холтер четырёх узлов"
    QuickSpendRoute -> "Быстрая запись траты"
    PauseRoute -> "Пауза 3 секунды"
    PointCRoute -> "Точка C"
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
        intake -> "Входной тест"
        top is WeekRoute -> "Неделя ${top.number}"
        week == null -> "Штатное обслуживание"
        else -> "Неделя $week"
    }
    return if (context == title) "Бортовой журнал" else context
}
