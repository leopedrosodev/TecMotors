package br.com.tec.tecmotors.presentation.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.data.CsvExporter
import br.com.tec.tecmotors.domain.model.LocalStateSnapshot
import br.com.tec.tecmotors.presentation.common.ChartBar
import br.com.tec.tecmotors.presentation.common.DateBrPickerField
import br.com.tec.tecmotors.presentation.common.MetricBarChart
import br.com.tec.tecmotors.presentation.common.MoneyField
import br.com.tec.tecmotors.presentation.common.UiFeedback
import br.com.tec.tecmotors.presentation.common.VehicleFilterRow
import br.com.tec.tecmotors.presentation.common.formatCurrency
import br.com.tec.tecmotors.presentation.common.formatInteger
import br.com.tec.tecmotors.presentation.common.formatNumber
import java.time.LocalDate
import kotlin.math.abs

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
            onEvent(
                ReportsUiEvent.SetExportFeedback(
                    UiFeedback.Info(context.getString(R.string.feedback_export_cancelled))
                )
            )
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
                    onSuccess = { UiFeedback.Success(context.getString(R.string.feedback_export_success)) },
                    onFailure = {
                        UiFeedback.Error(
                            context.getString(R.string.feedback_export_failed, it.message.orEmpty())
                        )
                    }
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VehicleFilterRow(
            vehicles = state.vehicles,
            selectedVehicleId = state.selectedVehicleId,
            onSelect = { onEvent(ReportsUiEvent.SelectVehicle(it)) }
        )

        PeriodSelector(
            selected = state.selectedPeriod,
            onSelect = { onEvent(ReportsUiEvent.SelectPeriod(it)) }
        )

        if (state.selectedPeriod == ReportPeriod.CUSTOM) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateBrPickerField(
                            value = state.customStartDateText,
                            onValueChange = { onEvent(ReportsUiEvent.ChangeCustomStartDate(it)) },
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.label_period_start)
                        )
                        DateBrPickerField(
                            value = state.customEndDateText,
                            onValueChange = { onEvent(ReportsUiEvent.ChangeCustomEndDate(it)) },
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.label_period_end)
                        )
                    }
                    Button(
                        onClick = { onEvent(ReportsUiEvent.ApplyCustomPeriod) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.action_apply_period))
                    }
                }
            }
        }

        val report = state.activeReport

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile(
                value = formatCurrency(report.overallCost),
                label = stringResource(R.string.reports_stat_total),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = formatInteger(report.distanceKm),
                label = stringResource(R.string.reports_stat_distance),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile(
                value = if (report.averageKmPerLiter > 0) formatNumber(report.averageKmPerLiter) else "—",
                label = stringResource(R.string.reports_stat_km_per_liter),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = if (report.distanceKm > 0) {
                    formatCurrency(report.overallCost / report.distanceKm)
                } else {
                    "—"
                },
                label = stringResource(R.string.reports_stat_cost_per_km),
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.reports_breakdown_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                BreakdownRow(
                    label = stringResource(R.string.reports_breakdown_fuel),
                    value = formatCurrency(report.totalCost)
                )
                BreakdownRow(
                    label = stringResource(R.string.reports_breakdown_maintenance),
                    value = formatCurrency(report.maintenanceCost)
                )
                BreakdownRow(
                    label = stringResource(R.string.reports_breakdown_refuels),
                    value = report.refuelCount.toString()
                )
                BreakdownRow(
                    label = stringResource(R.string.reports_breakdown_liters),
                    value = formatNumber(report.liters)
                )
            }
        }

        BudgetCard(state = state, onEvent = onEvent)

        MetricBarChart(
            title = stringResource(R.string.title_monthly_cost_chart),
            subtitle = stringResource(R.string.reports_chart_months),
            bars = state.monthlyMetrics.mapIndexed { index, metric ->
                ChartBar(
                    label = metric.monthYear,
                    value = metric.totalCost,
                    valueText = compactCurrency(metric.totalCost),
                    partial = index == state.monthlyMetrics.lastIndex
                )
            }
        )

        MetricBarChart(
            title = stringResource(R.string.title_monthly_consumption_chart),
            subtitle = stringResource(R.string.reports_chart_km_per_liter),
            bars = state.monthlyMetrics.mapIndexed { index, metric ->
                ChartBar(
                    label = metric.monthYear,
                    value = metric.kmPerLiter,
                    valueText = formatNumber(metric.kmPerLiter),
                    partial = index == state.monthlyMetrics.lastIndex
                )
            }
        )

        MetricBarChart(
            title = stringResource(R.string.title_monthly_cost_per_km_chart),
            bars = state.costPerKmMetrics.mapIndexed { index, metric ->
                ChartBar(
                    label = metric.monthYear,
                    value = metric.costPerKm,
                    valueText = compactCurrency(metric.costPerKm),
                    partial = index == state.costPerKmMetrics.lastIndex
                )
            }
        )

        if (state.vehicles.size > 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.title_vehicle_compare),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    state.vehicles.forEachIndexed { index, vehicle ->
                        if (index > 0) HorizontalDivider()
                        val vehicleFuel = state.fuelRecords.filter { it.vehicleId == vehicle.id }
                        val totalFuel = vehicleFuel.sumOf { it.totalCost }
                        val maintenanceTotal = state.maintenanceRecords
                            .filter { it.vehicleId == vehicle.id }
                            .sumOf { it.estimatedCost ?: 0.0 }

                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = vehicle.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            BreakdownRow(
                                label = stringResource(R.string.reports_compare_fuel, vehicleFuel.size),
                                value = formatCurrency(totalFuel)
                            )
                            BreakdownRow(
                                label = stringResource(R.string.reports_compare_maintenance),
                                value = formatCurrency(maintenanceTotal)
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                val fileName = "tec-motors-${LocalDate.now()}.csv"
                exportCsvLauncher.launch(fileName)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FileDownload,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.action_export_csv))
        }

        state.exportFeedback?.let {
            Text(
                text = it.message,
                style = MaterialTheme.typography.bodySmall,
                color = when (it) {
                    is UiFeedback.Error -> MaterialTheme.colorScheme.error
                    is UiFeedback.Success -> MaterialTheme.colorScheme.primary
                    is UiFeedback.Info -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun PeriodSelector(
    selected: ReportPeriod,
    onSelect: (ReportPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        ReportPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(period) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(period.titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BudgetCard(
    state: ReportsUiState,
    onEvent: (ReportsUiEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.title_monthly_budget),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (state.hasBudget) {
                LinearProgressIndicator(
                    progress = { state.budgetProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (state.budgetExceeded) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = {}
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(
                            R.string.reports_budget_spent,
                            formatCurrency(state.dashboardCurrentMonthTotal),
                            formatCurrency(state.budgetValue)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (state.budgetExceeded) {
                            stringResource(
                                R.string.home_budget_exceeded,
                                formatCurrency(abs(state.budgetRemaining))
                            )
                        } else {
                            stringResource(
                                R.string.home_budget_remaining,
                                formatCurrency(state.budgetRemaining)
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (state.budgetExceeded) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoneyField(
                    value = state.budgetInputText,
                    onValueChange = { onEvent(ReportsUiEvent.ChangeBudgetInput(it)) },
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.label_monthly_budget)
                )
                Button(
                    onClick = { onEvent(ReportsUiEvent.SaveBudget) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

/** Moeda encurtada para caber em cima de uma barra estreita. */
private fun compactCurrency(value: Double): String = when {
    value >= 1000 -> "R$ ${formatInteger(value / 1000)}k"
    else -> "R$ ${formatInteger(value)}"
}
