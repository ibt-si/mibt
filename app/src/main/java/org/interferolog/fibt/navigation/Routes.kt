package org.interferolog.fibt.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute : NavKey
@Serializable data object MapRoute : NavKey
@Serializable data class WeekRoute(val number: Int) : NavKey
@Serializable data class SilenceGateRoute(val target: String) : NavKey
@Serializable data object HolterRoute : NavKey
@Serializable data object QuickSpendRoute : NavKey
@Serializable data object PauseRoute : NavKey
@Serializable data object PointCRoute : NavKey
@Serializable data object SourcesRoute : NavKey
@Serializable data object PriceRoute : NavKey
@Serializable data object WorkRoute : NavKey
@Serializable data object FamilyRoute : NavKey
@Serializable data object RestRoute : NavKey
@Serializable data object CrisisRoute : NavKey
@Serializable data object FanRoute : NavKey
@Serializable data object CapitalRoute : NavKey
@Serializable data object ClusterRoute : NavKey
@Serializable data object CrystalRoute : NavKey
@Serializable data object MaintenanceRoute : NavKey
@Serializable data object ScriptsRoute : NavKey
@Serializable data object ExportRoute : NavKey
@Serializable data object IntakeRoute : NavKey

fun NavBackStack<NavKey>.reveal(route: NavKey) {
    val existing = indexOfLast { it == route }
    if (existing < 0) {
        add(route)
        return
    }
    while (size - 1 > existing) removeAt(size - 1)
}

fun NavBackStack<NavKey>.open(route: NavKey) {
    val gate = when (route) {
        HolterRoute -> "holter"
        SourcesRoute -> "sources"
        RestRoute -> "rest"
        CrisisRoute -> "crisis"
        CapitalRoute -> "capital"
        else -> null
    }
    add(if (gate == null) route else SilenceGateRoute(gate))
}

fun decodeGate(target: String): NavKey = when (target) {
    "holter" -> HolterRoute
    "sources" -> SourcesRoute
    "rest" -> RestRoute
    "crisis" -> CrisisRoute
    "capital" -> CapitalRoute
    else -> HomeRoute
}
