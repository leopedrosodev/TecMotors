package br.com.tec.tecmotors.presentation.app

import br.com.tec.tecmotors.R

/**
 * Destinos do app. Os quatro primeiros formam a barra inferior; os demais sao
 * secundarios, alcancados a partir do Resumo e com botao de voltar.
 */
enum class AppDestination(val titleRes: Int, val isPrimary: Boolean) {
    HOME(R.string.tab_home, isPrimary = true),
    MAINTENANCE(R.string.tab_maintenance, isPrimary = true),
    REPORTS(R.string.tab_reports, isPrimary = true),
    VEHICLES(R.string.tab_vehicles, isPrimary = true),
    CALCULATOR(R.string.tab_calculator, isPrimary = false),
    REFUEL_HISTORY(R.string.tab_refuels, isPrimary = false);

    /** Destinos onde faz sentido lancar um abastecimento pelo FAB. */
    val showsQuickRefuel: Boolean
        get() = isPrimary || this == REFUEL_HISTORY

    companion object {
        val primaryDestinations: List<AppDestination> = entries.filter { it.isPrimary }
    }
}

sealed interface AppUiEvent {
    data object ToggleTheme : AppUiEvent
    data class Navigate(val destination: AppDestination) : AppUiEvent
    data object NavigateBack : AppUiEvent
    data class SetAccountDialogVisible(val visible: Boolean) : AppUiEvent
    data class SetQuickRefuelVisible(val visible: Boolean) : AppUiEvent
}

data class AppUiState(
    val darkThemeEnabled: Boolean = true,
    val destination: AppDestination = AppDestination.HOME,
    val showAccountDialog: Boolean = false,
    val showQuickRefuel: Boolean = false
)
