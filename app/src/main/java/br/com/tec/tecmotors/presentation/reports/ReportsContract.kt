package br.com.tec.tecmotors.presentation.reports

import br.com.tec.tecmotors.domain.model.CostPerKmMetric
import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MonthlyMetric
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.PeriodReport
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleSummary

sealed interface ReportsUiEvent {
    data class SelectVehicle(val vehicleId: Long) : ReportsUiEvent
    data class ChangeCustomStartDate(val value: String) : ReportsUiEvent
    data class ChangeCustomEndDate(val value: String) : ReportsUiEvent
    data object ApplyCustomPeriod : ReportsUiEvent
    data class ChangeBudgetInput(val value: String) : ReportsUiEvent
    data object SaveBudget : ReportsUiEvent
    data class SetExportFeedback(val message: String?) : ReportsUiEvent
}

data class ReportsUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val fuelRecords: List<FuelRecord> = emptyList(),
    val odometerRecords: List<OdometerRecord> = emptyList(),
    val maintenanceRecords: List<MaintenanceRecord> = emptyList(),
    val selectedVehicleId: Long = -1L,
    val weeklyReport: PeriodReport = PeriodReport(0.0, 0.0, 0.0, 0.0, 0, 0.0),
    val monthlyReport: PeriodReport = PeriodReport(0.0, 0.0, 0.0, 0.0, 0, 0.0),
    val customReport: PeriodReport = PeriodReport(0.0, 0.0, 0.0, 0.0, 0, 0.0),
    val vehicleSummary: VehicleSummary = VehicleSummary(0.0, 0.0, 0.0, 0.0, 0.0),
    val monthlyMetrics: List<MonthlyMetric> = emptyList(),
    val costPerKmMetrics: List<CostPerKmMetric> = emptyList(),
    val dashboardCurrentMonthTotal: Double = 0.0,
    val dashboardCurrentMonthDistance: Double = 0.0,
    val dashboardCurrentMonthRefuels: Int = 0,
    val dashboardCurrentMonthMaintenance: Double = 0.0,
    val customStartDateText: String = "",
    val customEndDateText: String = "",
    val budgetInputText: String = "",
    val budgetValue: Double = 0.0,
    val budgetExceeded: Boolean = false,
    val budgetRemaining: Double = 0.0,
    val exportFeedback: String? = null
)
