package br.com.tec.tecmotors.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.PeriodReport
import br.com.tec.tecmotors.domain.model.VehicleSummary
import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.domain.usecase.CalculateCostPerKmMetricsUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateMonthlyMetricsUseCase
import br.com.tec.tecmotors.domain.usecase.CalculatePeriodReportUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateVehicleSummaryUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveMaintenanceUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveOdometersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveRefuelsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveSettingsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
import br.com.tec.tecmotors.domain.usecase.SetMonthlyBudgetUseCase
import br.com.tec.tecmotors.presentation.common.parseDateBrOrIso
import br.com.tec.tecmotors.presentation.common.parseDecimal
import br.com.tec.tecmotors.presentation.common.todayBr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class ReportsViewModel(
    private val observeVehiclesUseCase: ObserveVehiclesUseCase,
    private val observeRefuelsUseCase: ObserveRefuelsUseCase,
    private val observeOdometersUseCase: ObserveOdometersUseCase,
    private val observeMaintenanceUseCase: ObserveMaintenanceUseCase,
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase,
    private val calculatePeriodReportUseCase: CalculatePeriodReportUseCase,
    private val calculateMonthlyMetricsUseCase: CalculateMonthlyMetricsUseCase,
    private val calculateCostPerKmMetricsUseCase: CalculateCostPerKmMetricsUseCase,
    private val calculateVehicleSummaryUseCase: CalculateVehicleSummaryUseCase
) : ViewModel() {
    private val localState = MutableStateFlow(
        ReportsUiState(
            customStartDateText = todayBr(),
            customEndDateText = todayBr()
        )
    )

    private val settingsAndStateFlow = combine(
        observeSettingsUseCase(),
        localState
    ) { settings, state ->
        settings to state
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        observeVehiclesUseCase(),
        observeRefuelsUseCase(),
        observeOdometersUseCase(),
        observeMaintenanceUseCase(),
        settingsAndStateFlow
    ) { vehicles, refuels, odometers, maintenance, settingsAndState ->
        val (settings, state) = settingsAndState
        val previousSelected = state.selectedVehicleId
        val selected = previousSelected.takeIf { id -> vehicles.any { it.id == id } }
            ?: vehicles.firstOrNull()?.id
            ?: -1L
        val selectedVehicle = vehicles.firstOrNull { it.id == selected }

        val today = LocalDate.now()
        val weekStart = today.with(DayOfWeek.MONDAY)
        val monthStart = YearMonth.of(today.year, today.month).atDay(1)

        val weeklyReport = if (selected > 0L) {
            reportWithMaintenance(
                vehicleId = selected,
                start = weekStart,
                end = today,
                refuels = refuels,
                odometers = odometers,
                maintenance = maintenance
            )
        } else {
            emptyReport()
        }

        val monthlyReport = if (selected > 0L) {
            reportWithMaintenance(
                vehicleId = selected,
                start = monthStart,
                end = today,
                refuels = refuels,
                odometers = odometers,
                maintenance = maintenance
            )
        } else {
            emptyReport()
        }

        val summary = if (selected > 0L) {
            calculateVehicleSummaryUseCase(
                vehicleId = selected,
                fuelRecords = refuels,
                odometerRecords = odometers
            )
        } else {
            emptySummary()
        }

        val monthlyMetrics = if (selected > 0L) {
            calculateMonthlyMetricsUseCase(
                vehicleId = selected,
                fuelRecords = refuels,
                odometerRecords = odometers,
                maintenanceRecords = maintenance,
                monthsBackInclusive = 5
            )
        } else {
            emptyList()
        }

        val costPerKmMetrics = calculateCostPerKmMetricsUseCase(monthlyMetrics)

        val customStart = parseDateBrOrIso(state.customStartDateText)
        val customEnd = parseDateBrOrIso(state.customEndDateText)
        val customReport = if (
            selected > 0L &&
            customStart != null &&
            customEnd != null &&
            !customStart.isAfter(customEnd)
        ) {
            reportWithMaintenance(
                vehicleId = selected,
                start = customStart,
                end = customEnd,
                refuels = refuels,
                odometers = odometers,
                maintenance = maintenance
            )
        } else {
            emptyReport()
        }

        val budgetValue = when (selectedVehicle?.type) {
            VehicleType.CAR -> settings.monthlyBudgetCar
            VehicleType.MOTORCYCLE -> settings.monthlyBudgetMotorcycle
            null -> 0.0
        }

        val shouldHydrateBudgetField = state.budgetInputText.isBlank() || selected != previousSelected
        val budgetInputText = if (shouldHydrateBudgetField) {
            if (budgetValue > 0.0) {
                String.format(Locale.US, "%.2f", budgetValue)
            } else {
                ""
            }
        } else {
            state.budgetInputText
        }

        val budgetExceeded = budgetValue > 0.0 && monthlyReport.overallCost > budgetValue
        val budgetRemaining = if (budgetValue > 0.0) budgetValue - monthlyReport.overallCost else 0.0

        val monthMaintenanceCost = maintenance
            .filter { it.vehicleId == selected }
            .filter { it.createdAtEpochDay in monthStart.toEpochDay()..today.toEpochDay() }
            .sumOf { it.estimatedCost ?: 0.0 }

        state.copy(
            vehicles = vehicles,
            fuelRecords = refuels,
            odometerRecords = odometers,
            maintenanceRecords = maintenance,
            selectedVehicleId = selected,
            weeklyReport = weeklyReport,
            monthlyReport = monthlyReport,
            customReport = customReport,
            vehicleSummary = summary,
            monthlyMetrics = monthlyMetrics,
            costPerKmMetrics = costPerKmMetrics,
            dashboardCurrentMonthTotal = monthlyReport.overallCost,
            dashboardCurrentMonthDistance = monthlyReport.distanceKm,
            dashboardCurrentMonthRefuels = monthlyReport.refuelCount,
            dashboardCurrentMonthMaintenance = monthMaintenanceCost,
            budgetValue = budgetValue,
            budgetInputText = budgetInputText,
            budgetExceeded = budgetExceeded,
            budgetRemaining = budgetRemaining
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = localState.value
    )

    fun onEvent(event: ReportsUiEvent) {
        when (event) {
            is ReportsUiEvent.SelectVehicle -> {
                localState.update { it.copy(selectedVehicleId = event.vehicleId, budgetInputText = "") }
            }

            is ReportsUiEvent.ChangeCustomStartDate -> {
                localState.update { it.copy(customStartDateText = event.value) }
            }

            is ReportsUiEvent.ChangeCustomEndDate -> {
                localState.update { it.copy(customEndDateText = event.value) }
            }

            ReportsUiEvent.ApplyCustomPeriod -> {
                val start = parseDateBrOrIso(uiState.value.customStartDateText)
                val end = parseDateBrOrIso(uiState.value.customEndDateText)
                if (start == null || end == null || start.isAfter(end)) {
                    localState.update { it.copy(exportFeedback = "Periodo personalizado invalido") }
                } else {
                    localState.update { it.copy(exportFeedback = null) }
                }
            }

            is ReportsUiEvent.ChangeBudgetInput -> {
                localState.update { it.copy(budgetInputText = event.value) }
            }

            ReportsUiEvent.SaveBudget -> {
                val current = uiState.value
                val selectedVehicle = current.vehicles.firstOrNull { it.id == current.selectedVehicleId } ?: return
                val parsed = parseDecimal(current.budgetInputText)
                if (parsed == null || parsed < 0.0) {
                    localState.update { it.copy(exportFeedback = "Orcamento invalido") }
                    return
                }

                viewModelScope.launch {
                    setMonthlyBudgetUseCase(selectedVehicle.type, parsed)
                    localState.update {
                        it.copy(
                            exportFeedback = "Orcamento salvo",
                            budgetInputText = String.format(Locale.US, "%.2f", parsed)
                        )
                    }
                }
            }

            is ReportsUiEvent.SetExportFeedback -> {
                localState.update { it.copy(exportFeedback = event.message) }
            }
        }
    }

    private fun reportWithMaintenance(
        vehicleId: Long,
        start: LocalDate,
        end: LocalDate,
        refuels: List<FuelRecord>,
        odometers: List<OdometerRecord>,
        maintenance: List<MaintenanceRecord>
    ): PeriodReport {
        return calculatePeriodReportUseCase(
            vehicleId = vehicleId,
            start = start,
            end = end,
            fuelRecords = refuels,
            odometerRecords = odometers,
            maintenanceRecords = maintenance
        )
    }

    private fun emptyReport(): PeriodReport {
        return PeriodReport(
            distanceKm = 0.0,
            liters = 0.0,
            averageKmPerLiter = 0.0,
            totalCost = 0.0,
            refuelCount = 0,
            averageMonthlyCost = 0.0,
            maintenanceCost = 0.0
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
