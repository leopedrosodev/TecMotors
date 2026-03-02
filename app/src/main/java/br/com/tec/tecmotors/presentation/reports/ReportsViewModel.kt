package br.com.tec.tecmotors.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tec.tecmotors.domain.usecase.CalculateMonthlyMetricsUseCase
import br.com.tec.tecmotors.domain.usecase.CalculatePeriodReportUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateVehicleSummaryUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveMaintenanceUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveOdometersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveRefuelsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
import br.com.tec.tecmotors.domain.model.PeriodReport
import br.com.tec.tecmotors.domain.model.VehicleSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class ReportsViewModel(
    private val observeVehiclesUseCase: ObserveVehiclesUseCase,
    private val observeRefuelsUseCase: ObserveRefuelsUseCase,
    private val observeOdometersUseCase: ObserveOdometersUseCase,
    private val observeMaintenanceUseCase: ObserveMaintenanceUseCase,
    private val calculatePeriodReportUseCase: CalculatePeriodReportUseCase,
    private val calculateMonthlyMetricsUseCase: CalculateMonthlyMetricsUseCase,
    private val calculateVehicleSummaryUseCase: CalculateVehicleSummaryUseCase
) : ViewModel() {
    private val localState = MutableStateFlow(ReportsUiState())

    val uiState: StateFlow<ReportsUiState> = combine(
        observeVehiclesUseCase(),
        observeRefuelsUseCase(),
        observeOdometersUseCase(),
        observeMaintenanceUseCase(),
        localState
    ) { vehicles, refuels, odometers, maintenance, state ->
        val selected = state.selectedVehicleId.takeIf { id -> vehicles.any { it.id == id } }
            ?: vehicles.firstOrNull()?.id
            ?: -1L

        val today = LocalDate.now()
        val weekStart = today.with(DayOfWeek.MONDAY)
        val monthStart = YearMonth.of(today.year, today.month).atDay(1)

        val weeklyReport = if (selected > 0L) {
            calculatePeriodReportUseCase(
                vehicleId = selected,
                start = weekStart,
                end = today,
                fuelRecords = refuels,
                odometerRecords = odometers
            )
        } else emptyReport()

        val monthlyReport = if (selected > 0L) {
            calculatePeriodReportUseCase(
                vehicleId = selected,
                start = monthStart,
                end = today,
                fuelRecords = refuels,
                odometerRecords = odometers
            )
        } else emptyReport()

        val summary = if (selected > 0L) {
            calculateVehicleSummaryUseCase(
                vehicleId = selected,
                fuelRecords = refuels,
                odometerRecords = odometers
            )
        } else emptySummary()

        val monthlyMetrics = if (selected > 0L) {
            calculateMonthlyMetricsUseCase(
                vehicleId = selected,
                fuelRecords = refuels,
                odometerRecords = odometers,
                maintenanceRecords = maintenance,
                monthsBackInclusive = 5
            )
        } else emptyList()

        state.copy(
            vehicles = vehicles,
            fuelRecords = refuels,
            odometerRecords = odometers,
            maintenanceRecords = maintenance,
            selectedVehicleId = selected,
            weeklyReport = weeklyReport,
            monthlyReport = monthlyReport,
            vehicleSummary = summary,
            monthlyMetrics = monthlyMetrics
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = localState.value
    )

    fun onEvent(event: ReportsUiEvent) {
        when (event) {
            is ReportsUiEvent.SelectVehicle -> {
                localState.update { it.copy(selectedVehicleId = event.vehicleId) }
            }

            is ReportsUiEvent.SetExportFeedback -> {
                localState.update { it.copy(exportFeedback = event.message) }
            }
        }
    }

    private fun emptyReport(): PeriodReport {
        return PeriodReport(
            distanceKm = 0.0,
            liters = 0.0,
            averageKmPerLiter = 0.0,
            totalCost = 0.0,
            refuelCount = 0,
            averageMonthlyCost = 0.0
        )
    }

    private fun emptySummary(): VehicleSummary {
        return VehicleSummary(
            distanceKm = 0.0,
            liters = 0.0,
            totalCost = 0.0,
            kmPerLiter = 0.0,
            costPerKm = 0.0
        )
    }
}
