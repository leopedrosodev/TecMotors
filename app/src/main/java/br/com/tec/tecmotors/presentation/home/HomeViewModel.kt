package br.com.tec.tecmotors.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Settings
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.monthlyBudgetFor
import br.com.tec.tecmotors.domain.usecase.CalculatePeriodReportUseCase
import br.com.tec.tecmotors.domain.usecase.DecideRemindersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveMaintenanceUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveOdometersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveRefuelsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveSettingsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
import br.com.tec.tecmotors.domain.usecase.ReminderAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tela inicial: responde "quanto gastei, o que esta vencendo e o que falta
 * registrar" sem o usuario precisar procurar. Nao cria regra de negocio
 * propria - so compoe casos de uso que ja existem.
 */
class HomeViewModel(
    observeVehiclesUseCase: ObserveVehiclesUseCase,
    observeRefuelsUseCase: ObserveRefuelsUseCase,
    observeOdometersUseCase: ObserveOdometersUseCase,
    observeMaintenanceUseCase: ObserveMaintenanceUseCase,
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val calculatePeriodReportUseCase: CalculatePeriodReportUseCase,
    private val decideRemindersUseCase: DecideRemindersUseCase,
    private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val localState = MutableStateFlow(HomeUiState())

    private val data = combine(
        observeVehiclesUseCase(),
        observeRefuelsUseCase(),
        observeOdometersUseCase(),
        observeMaintenanceUseCase(),
        observeSettingsUseCase()
    ) { vehicles, refuels, odometers, maintenance, settings ->
        HomeData(vehicles, refuels, odometers, maintenance, settings)
    }

    val uiState: StateFlow<HomeUiState> = combine(data, localState) { data, state ->
        buildState(data, state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.SelectVehicle ->
                localState.update { it.copy(selectedVehicleId = event.vehicleId) }
        }
    }

    private fun buildState(data: HomeData, state: HomeUiState): HomeUiState {
        val vehicles = data.vehicles
        val selectedId = state.selectedVehicleId
            .takeIf { id -> vehicles.any { it.id == id } }
            ?: vehicles.firstOrNull()?.id
            ?: -1L

        val selectedVehicle = vehicles.firstOrNull { it.id == selectedId }
        val now = today()
        val month = YearMonth.from(now)

        val report = calculatePeriodReportUseCase(
            vehicleId = selectedId,
            start = month.atDay(1),
            end = month.atEndOfMonth(),
            fuelRecords = data.refuels,
            odometerRecords = data.odometers,
            maintenanceRecords = data.maintenance
        )

        val budget = data.settings.monthlyBudgetFor(selectedVehicle?.type)
        val monthCost = report.overallCost
        val hasBudget = budget > 0.0

        val vehicleRefuels = data.refuels.filter { it.vehicleId == selectedId }
        val vehicleOdometers = data.odometers.filter { it.vehicleId == selectedId }
        val vehicleMaintenance = data.maintenance.filter { it.vehicleId == selectedId }

        val lastReading = latestReading(vehicleRefuels, vehicleOdometers)

        val maintenanceAlert = decideRemindersUseCase(
            today = now,
            vehicles = vehicles,
            fuelRecords = data.refuels,
            odometerRecords = data.odometers,
            maintenanceRecords = data.maintenance,
            maintenanceThresholdKm = DecideRemindersUseCase.HOME_MAINTENANCE_THRESHOLD_KM
        ).filterIsInstance<ReminderAlert.MaintenanceDueSoon>()
            .firstOrNull { it.vehicleId == selectedId }

        return HomeUiState(
            vehicles = vehicles,
            selectedVehicleId = selectedId,
            monthLabel = monthLabel(now),
            monthCost = monthCost,
            monthBudget = budget,
            budgetRemaining = budget - monthCost,
            budgetProgress = if (hasBudget) {
                (monthCost / budget).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            },
            budgetExceeded = hasBudget && monthCost > budget,
            hasBudget = hasBudget,
            monthDistanceKm = report.distanceKm,
            monthKmPerLiter = report.averageKmPerLiter,
            monthCostPerKm = if (report.distanceKm > 0.0) monthCost / report.distanceKm else 0.0,
            monthRefuelCount = report.refuelCount,
            lastOdometerKm = lastReading?.odometerKm,
            lastOdometerDaysAgo = lastReading?.let {
                (now.toEpochDay() - it.dateEpochDay).toInt().coerceAtLeast(0)
            },
            maintenanceAlert = maintenanceAlert,
            recentEntries = recentEntries(vehicleRefuels, vehicleMaintenance)
        )
    }

    /** Leitura de km mais recente, venha de odometro avulso ou de abastecimento. */
    private fun latestReading(
        refuels: List<FuelRecord>,
        odometers: List<OdometerRecord>
    ): Reading? {
        val candidates = refuels.map { Reading(it.dateEpochDay, it.odometerKm) } +
            odometers.map { Reading(it.dateEpochDay, it.odometerKm) }

        return candidates.maxWithOrNull(
            compareBy<Reading> { it.dateEpochDay }.thenBy { it.odometerKm }
        )
    }

    private fun recentEntries(
        refuels: List<FuelRecord>,
        maintenance: List<MaintenanceRecord>
    ): List<HomeEntry> {
        val fromRefuels = refuels.map { record ->
            HomeEntry(
                id = record.id,
                kind = HomeEntryKind.REFUEL,
                title = record.stationName.trim().ifBlank { "Abastecimento" },
                subtitle = buildString {
                    append(formatLiters(record.liters))
                    append(" L")
                    append(" · ")
                    append(formatKm(record.odometerKm))
                    append(" km")
                },
                dateEpochDay = record.dateEpochDay,
                amount = record.totalCost
            )
        }

        val fromMaintenance = maintenance.map { record ->
            HomeEntry(
                id = record.id,
                kind = HomeEntryKind.MAINTENANCE,
                title = record.title.trim().ifBlank { record.type.label },
                subtitle = record.type.label,
                dateEpochDay = record.createdAtEpochDay,
                amount = record.estimatedCost ?: 0.0
            )
        }

        return (fromRefuels + fromMaintenance)
            .sortedByDescending { it.dateEpochDay }
            .take(MAX_RECENT_ENTRIES)
    }

    private fun monthLabel(date: LocalDate): String =
        date.format(monthFormatter).replaceFirstChar { it.titlecase(ptBr) }

    private data class Reading(val dateEpochDay: Long, val odometerKm: Double)

    private data class HomeData(
        val vehicles: List<Vehicle>,
        val refuels: List<FuelRecord>,
        val odometers: List<OdometerRecord>,
        val maintenance: List<MaintenanceRecord>,
        val settings: Settings
    )

    companion object {
        private const val MAX_RECENT_ENTRIES = 4
        private val ptBr: Locale = Locale.forLanguageTag("pt-BR")
        private val monthFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMMM", ptBr)

        private fun formatLiters(value: Double): String = String.format(ptBr, "%.1f", value)
        private fun formatKm(value: Double): String = String.format(ptBr, "%,.0f", value)
    }
}
