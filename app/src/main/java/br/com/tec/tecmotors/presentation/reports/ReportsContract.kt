package br.com.tec.tecmotors.presentation.reports

import br.com.tec.tecmotors.domain.model.CostPerKmMetric
import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MonthlyMetric
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.PeriodReport
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleSummary
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.presentation.common.UiFeedback

/** Recortes de periodo do relatorio. Antes os tres apareciam empilhados na tela. */
enum class ReportPeriod(val titleRes: Int) {
    WEEK(R.string.reports_period_week),
    MONTH(R.string.reports_period_month),
    CUSTOM(R.string.reports_period_custom)
}

sealed interface ReportsUiEvent {
    data class SelectPeriod(val period: ReportPeriod) : ReportsUiEvent
    data class SelectVehicle(val vehicleId: Long) : ReportsUiEvent
    data class ChangeCustomStartDate(val value: String) : ReportsUiEvent
    data class ChangeCustomEndDate(val value: String) : ReportsUiEvent
    data object ApplyCustomPeriod : ReportsUiEvent
    data class ChangeBudgetInput(val value: String) : ReportsUiEvent
    data object SaveBudget : ReportsUiEvent
    data class SetExportFeedback(val feedback: UiFeedback?) : ReportsUiEvent
}

data class ReportsUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val fuelRecords: List<FuelRecord> = emptyList(),
    val odometerRecords: List<OdometerRecord> = emptyList(),
    val maintenanceRecords: List<MaintenanceRecord> = emptyList(),
    val selectedVehicleId: Long = -1L,
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTH,
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
    val exportFeedback: UiFeedback? = null
) {
    /** O relatorio do recorte ativo - a tela mostra um, nao tres. */
    val activeReport: PeriodReport
        get() = when (selectedPeriod) {
            ReportPeriod.WEEK -> weeklyReport
            ReportPeriod.MONTH -> monthlyReport
            ReportPeriod.CUSTOM -> customReport
        }

    val hasBudget: Boolean
        get() = budgetValue > 0.0

    val budgetProgress: Float
        get() = if (budgetValue > 0.0) {
            (dashboardCurrentMonthTotal / budgetValue).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
}
