package br.com.tec.tecmotors.presentation.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.data.CsvExporter
import br.com.tec.tecmotors.domain.model.LocalStateSnapshot
import br.com.tec.tecmotors.presentation.common.ChartBar
import br.com.tec.tecmotors.presentation.common.MetricBarChart
import br.com.tec.tecmotors.presentation.common.VehicleChipSelector
import br.com.tec.tecmotors.presentation.common.dateBrFormatter
import br.com.tec.tecmotors.presentation.common.formatCurrency
import br.com.tec.tecmotors.presentation.common.formatNumber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ReportsScreen(
    state: ReportsUiState,
    onEvent: (ReportsUiEvent) -> Unit
) {
    val context = LocalContext.current

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) {
            onEvent(ReportsUiEvent.SetExportFeedback(context.getString(R.string.feedback_export_cancelled)))
            return@rememberLauncherForActivityResult
        }

        val snapshot = LocalStateSnapshot(
            vehicles = state.vehicles,
            odometerRecords = state.odometerRecords,
            fuelRecords = state.fuelRecords,
            maintenanceRecords = state.maintenanceRecords,
            updatedAtMillis = System.currentTimeMillis()
        )

        val result = runCatching {
            val csvContent = CsvExporter.buildSnapshotCsv(snapshot)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(csvContent.toByteArray(Charsets.UTF_8))
            } ?: error("Falha ao abrir destino de exportacao")
        }

        onEvent(
            ReportsUiEvent.SetExportFeedback(
                result.fold(
                    onSuccess = { context.getString(R.string.feedback_export_success) },
                    onFailure = { context.getString(R.string.feedback_export_failed, it.message.orEmpty()) }
                )
            )
        )
    }

    val today = LocalDate.now()
    val weekStart = today.with(DayOfWeek.MONDAY)
    val monthStart = YearMonth.of(today.year, today.month).atDay(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.title_reports), style = MaterialTheme.typography.titleLarge)

        VehicleChipSelector(
            vehicles = state.vehicles,
            selectedVehicleId = state.selectedVehicleId,
            onSelect = { onEvent(ReportsUiEvent.SelectVehicle(it)) }
        )

        DashboardCard(
            monthlyTotal = state.dashboardCurrentMonthTotal,
            monthlyDistance = state.dashboardCurrentMonthDistance,
            monthlyRefuels = state.dashboardCurrentMonthRefuels,
            monthlyMaintenance = state.dashboardCurrentMonthMaintenance
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.title_monthly_budget), fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = state.budgetInputText,
                    onValueChange = { onEvent(ReportsUiEvent.ChangeBudgetInput(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(stringResource(R.string.label_monthly_budget)) }
                )
                Button(
                    onClick = { onEvent(ReportsUiEvent.SaveBudget) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_save_budget))
                }
                if (state.budgetValue > 0.0) {
                    val messageRes = if (state.budgetExceeded) {
                        R.string.text_budget_exceeded
                    } else {
                        R.string.text_budget_remaining
                    }
                    Text(
                        stringResource(messageRes, formatCurrency(kotlin.math.abs(state.budgetRemaining))),
                        color = if (state.budgetExceeded) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }

        PeriodReportCard(
            title = stringResource(R.string.title_weekly),
            period = stringResource(
                R.string.text_period_range,
                weekStart.format(dateBrFormatter),
                today.format(dateBrFormatter)
            ),
            report = state.weeklyReport
        )

        PeriodReportCard(
            title = stringResource(R.string.title_monthly),
            period = stringResource(
                R.string.text_period_range,
                monthStart.format(dateBrFormatter),
                today.format(dateBrFormatter)
            ),
            report = state.monthlyReport
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.title_custom_period), fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = state.customStartDateText,
                    onValueChange = { onEvent(ReportsUiEvent.ChangeCustomStartDate(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.label_period_start)) }
                )
                OutlinedTextField(
                    value = state.customEndDateText,
                    onValueChange = { onEvent(ReportsUiEvent.ChangeCustomEndDate(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.label_period_end)) }
                )
                Button(
                    onClick = { onEvent(ReportsUiEvent.ApplyCustomPeriod) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_apply_period))
                }
            }
        }

        PeriodReportCard(
            title = stringResource(R.string.title_custom_period_result),
            period = stringResource(
                R.string.text_period_range,
                state.customStartDateText,
                state.customEndDateText
            ),
            report = state.customReport
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(stringResource(R.string.title_vehicle_summary), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.text_total_distance, formatNumber(state.vehicleSummary.distanceKm)))
                Text(stringResource(R.string.text_average_consumption, formatNumber(state.vehicleSummary.kmPerLiter)))
                Text(stringResource(R.string.text_cost_per_km, formatCurrency(state.vehicleSummary.costPerKm)))
                Text(stringResource(R.string.text_total_fuel_cost, formatCurrency(state.vehicleSummary.totalCost)))
            }
        }

        MetricBarChart(
            title = stringResource(R.string.title_monthly_cost_chart),
            bars = state.monthlyMetrics.map {
                ChartBar(
                    label = it.monthYear,
                    value = it.totalCost,
                    valueText = shortValue(formatCurrency(it.totalCost))
                )
            }
        )

        MetricBarChart(
            title = stringResource(R.string.title_monthly_consumption_chart),
            bars = state.monthlyMetrics.map {
                ChartBar(
                    label = it.monthYear,
                    value = it.kmPerLiter,
                    valueText = "${formatNumber(it.kmPerLiter)}"
                )
            }
        )

        MetricBarChart(
            title = stringResource(R.string.title_monthly_cost_per_km_chart),
            bars = state.costPerKmMetrics.map {
                ChartBar(
                    label = it.monthYear,
                    value = it.costPerKm,
                    valueText = shortValue(formatCurrency(it.costPerKm))
                )
            }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.title_vehicle_compare), fontWeight = FontWeight.Bold)
                state.vehicles.forEach { vehicle ->
                    val vehicleFuel = state.fuelRecords.filter { it.vehicleId == vehicle.id }
                    val totalFuel = vehicleFuel.sumOf { it.totalCost }
                    val maintenanceTotal = state.maintenanceRecords
                        .filter { it.vehicleId == vehicle.id }
                        .sumOf { it.estimatedCost ?: 0.0 }
                    HorizontalDivider()
                    Text(vehicle.name, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.text_refuel_count, vehicleFuel.size))
                    Text(stringResource(R.string.text_fuel_total, formatCurrency(totalFuel)))
                    Text(stringResource(R.string.text_maintenance_total, formatCurrency(maintenanceTotal)))
                }
            }
        }

        Button(
            onClick = {
                val fileName = "tec-motors-${LocalDate.now()}.csv"
                exportCsvLauncher.launch(fileName)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_export_csv))
        }

        state.exportFeedback?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DashboardCard(
    monthlyTotal: Double,
    monthlyDistance: Double,
    monthlyRefuels: Int,
    monthlyMaintenance: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(stringResource(R.string.title_dashboard_month), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.text_dashboard_total, formatCurrency(monthlyTotal)))
            Text(stringResource(R.string.text_dashboard_distance, formatNumber(monthlyDistance)))
            Text(stringResource(R.string.text_dashboard_refuels, monthlyRefuels))
            Text(stringResource(R.string.text_dashboard_maintenance, formatCurrency(monthlyMaintenance)))
        }
    }
}

@Composable
private fun PeriodReportCard(
    title: String,
    period: String,
    report: br.com.tec.tecmotors.domain.model.PeriodReport
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(period, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(modifier = Modifier.padding(top = 2.dp, bottom = 2.dp))
            Text(stringResource(R.string.text_report_distance, formatNumber(report.distanceKm)))
            Text(stringResource(R.string.text_report_consumption, formatNumber(report.averageKmPerLiter)))
            Text(stringResource(R.string.text_report_cost, formatCurrency(report.totalCost)))
            Text(stringResource(R.string.text_report_maintenance_cost, formatCurrency(report.maintenanceCost)))
            Text(stringResource(R.string.text_report_total_cost, formatCurrency(report.overallCost)))
            Text(stringResource(R.string.text_report_refuels, report.refuelCount))
            Text(stringResource(R.string.text_report_liters, formatNumber(report.liters)))
        }
    }
}

private fun shortValue(value: String): String {
    return if (value.length > 11) value.take(11) else value
}
