package br.com.tec.tecmotors.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.AppVersionBadge
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.presentation.common.formatCurrency
import br.com.tec.tecmotors.presentation.common.formatDate
import br.com.tec.tecmotors.presentation.common.formatInteger
import br.com.tec.tecmotors.presentation.common.formatNumber
import kotlin.math.abs

/**
 * Porta de entrada do app: responde o que o usuario quer saber ao abrir,
 * sem formulario nenhum na frente.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    onOpenMaintenance: () -> Unit,
    onOpenVehicles: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenRefuelHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VehicleRow(
            vehicles = state.vehicles,
            selectedVehicleId = state.selectedVehicleId,
            onSelect = { onEvent(HomeUiEvent.SelectVehicle(it)) },
            onAdd = onOpenVehicles
        )

        MonthSpendCard(state = state)

        state.maintenanceAlert?.let { alert ->
            AttentionCard(
                title = if (alert.kmRemaining < 0) {
                    stringResource(
                        R.string.home_alert_maintenance_overdue,
                        alert.title,
                        formatKm(abs(alert.kmRemaining))
                    )
                } else {
                    stringResource(
                        R.string.home_alert_maintenance_due,
                        alert.title,
                        formatKm(alert.kmRemaining)
                    )
                },
                detail = stringResource(
                    R.string.home_alert_maintenance_detail,
                    formatKm(alert.dueOdometerKm),
                    formatKm(alert.currentOdometerKm)
                ),
                onClick = onOpenMaintenance
            )
        }

        OdometerCard(
            odometerKm = state.lastOdometerKm,
            daysAgo = state.lastOdometerDaysAgo,
            onUpdate = onOpenVehicles
        )

        MonthStatsRow(state = state)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onOpenCalculator,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Calculate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.tab_calculator))
            }
            OutlinedButton(
                onClick = onOpenMaintenance,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Build,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.tab_maintenance))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_recent_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onOpenRefuelHistory) {
                Text(stringResource(R.string.home_see_all))
            }
        }

        if (state.recentEntries.isEmpty()) {
            EmptyRecentCard()
        } else {
            state.recentEntries.forEach { entry ->
                RecentEntryRow(entry = entry)
            }
        }

        AppVersionBadge(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
        )
    }
}

@Composable
private fun VehicleRow(
    vehicles: List<Vehicle>,
    selectedVehicleId: Long,
    onSelect: (Long) -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        vehicles.forEach { vehicle ->
            FilterChip(
                selected = vehicle.id == selectedVehicleId,
                onClick = { onSelect(vehicle.id) },
                label = {
                    Text(
                        text = vehicle.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = vehicleIcon(vehicle.type),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        TextButton(onClick = onAdd) {
            Text(stringResource(R.string.home_manage_vehicles))
        }
    }
}

@Composable
private fun MonthSpendCard(state: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.home_month_spend, state.monthLabel),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatCurrency(state.monthCost),
                style = MaterialTheme.typography.displaySmall,
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
                    trackColor = MaterialTheme.colorScheme.background,
                    drawStopIndicator = {}
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(
                            R.string.home_budget_total,
                            formatCurrency(state.monthBudget)
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
            } else {
                Text(
                    text = stringResource(R.string.home_budget_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AttentionCard(
    title: String,
    detail: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun OdometerCard(
    odometerKm: Double?,
    daysAgo: Int?,
    onUpdate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (odometerKm == null) {
                        stringResource(R.string.text_no_odometer)
                    } else {
                        stringResource(R.string.home_odometer_value, formatKm(odometerKm))
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        daysAgo == null -> stringResource(R.string.home_odometer_never)
                        daysAgo == 0 -> stringResource(R.string.home_odometer_today)
                        daysAgo == 1 -> stringResource(R.string.home_odometer_yesterday)
                        else -> stringResource(R.string.home_odometer_days_ago, daysAgo)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onUpdate, shape = CircleShape) {
                Text(stringResource(R.string.home_update_odometer))
            }
        }
    }
}

@Composable
private fun MonthStatsRow(state: HomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatTile(
            value = if (state.monthKmPerLiter > 0) formatNumber(state.monthKmPerLiter) else "—",
            label = stringResource(R.string.home_stat_km_per_liter),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = formatKm(state.monthDistanceKm),
            label = stringResource(R.string.home_stat_distance),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = if (state.monthCostPerKm > 0) formatCurrency(state.monthCostPerKm) else "—",
            label = stringResource(R.string.home_stat_cost_per_km),
            modifier = Modifier.weight(1f)
        )
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun RecentEntryRow(entry: HomeEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (entry.kind) {
                        HomeEntryKind.REFUEL -> Icons.Filled.LocalGasStation
                        HomeEntryKind.MAINTENANCE -> Icons.Filled.Build
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatDate(entry.dateEpochDay)} · ${entry.subtitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.amount > 0.0) {
                Text(
                    text = formatCurrency(entry.amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyRecentCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.home_empty_detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun vehicleIcon(type: VehicleType): ImageVector = when (type) {
    VehicleType.CAR -> Icons.Filled.DirectionsCar
    VehicleType.MOTORCYCLE -> Icons.Filled.TwoWheeler
    VehicleType.OTHER -> Icons.Filled.LocalShipping
}

private fun formatKm(value: Double): String = formatInteger(value)
