package br.com.tec.tecmotors.presentation.maintenance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.domain.usecase.MaintenanceDueStatus
import br.com.tec.tecmotors.presentation.common.VehicleFilterRow
import br.com.tec.tecmotors.presentation.common.formatCurrency
import br.com.tec.tecmotors.presentation.common.formatDate
import br.com.tec.tecmotors.presentation.common.formatInteger
import kotlin.math.abs

@Composable
fun MaintenanceScreen(
    state: MaintenanceUiState,
    onEvent: (MaintenanceUiEvent) -> Unit
) {
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
            onSelect = { onEvent(MaintenanceUiEvent.SelectVehicle(it)) }
        )

        HealthCard(state = state)

        Button(
            onClick = { onEvent(MaintenanceUiEvent.SetAddSheetVisible(true)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.maintenance_add))
        }

        if (state.attentionItems.isNotEmpty()) {
            SectionTitle(stringResource(R.string.maintenance_section_attention))
            state.attentionItems.forEach { item ->
                MaintenanceItemCard(
                    item = item,
                    onToggleDone = {
                        onEvent(MaintenanceUiEvent.ToggleDone(item.record.id, true))
                    }
                )
            }
        }

        if (state.onTrackItems.isNotEmpty()) {
            SectionTitle(stringResource(R.string.maintenance_section_ok))
            state.onTrackItems.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { item ->
                        OnTrackTile(item = item, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        if (state.doneItems.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(
                    text = stringResource(R.string.maintenance_section_done, state.doneItems.size),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        if (expanded) {
                            stringResource(R.string.action_collapse)
                        } else {
                            stringResource(R.string.action_expand)
                        }
                    )
                }
            }

            if (expanded) {
                state.doneItems.forEach { item ->
                    MaintenanceItemCard(
                        item = item,
                        onToggleDone = {
                            onEvent(MaintenanceUiEvent.ToggleDone(item.record.id, false))
                        }
                    )
                }
            }
        }

        if (!state.hasPlanning) {
            EmptyCard()
        }
    }

    if (state.showAddSheet) {
        AddMaintenanceSheet(
            state = state,
            onEvent = onEvent,
            onDismiss = { onEvent(MaintenanceUiEvent.SetAddSheetVisible(false)) }
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun HealthCard(state: MaintenanceUiState) {
    val attention = state.attentionItems.size
    val onTrack = state.onTrackItems.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HealthRing(
                percent = state.healthPercent,
                hasData = state.hasPlanning,
                modifier = Modifier.size(112.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (attention > 0) {
                        stringResource(R.string.maintenance_health_attention, attention)
                    } else if (state.hasPlanning) {
                        stringResource(R.string.maintenance_health_all_good)
                    } else {
                        stringResource(R.string.maintenance_health_no_data)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                if (state.hasPlanning) {
                    LegendRow(
                        color = MaterialTheme.colorScheme.tertiary,
                        text = stringResource(R.string.maintenance_legend_attention, attention)
                    )
                    LegendRow(
                        color = MaterialTheme.colorScheme.primary,
                        text = stringResource(R.string.maintenance_legend_ok, onTrack)
                    )
                }

                state.currentOdometerKm?.let {
                    Text(
                        text = stringResource(R.string.maintenance_current_km, formatInteger(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthRing(percent: Int, hasData: Boolean, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.background
    val arc = if (percent >= 60) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 11.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)

            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )

            if (hasData) {
                drawArc(
                    color = arc,
                    startAngle = -90f,
                    sweepAngle = 360f * (percent.coerceIn(0, 100) / 100f),
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (hasData) "$percent%" else "—",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.maintenance_health_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendRow(color: Color, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MaintenanceItemCard(
    item: MaintenanceItem,
    onToggleDone: () -> Unit
) {
    val accent = when (item.status) {
        MaintenanceDueStatus.OVERDUE -> MaterialTheme.colorScheme.error
        MaintenanceDueStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiary
        MaintenanceDueStatus.DONE -> MaterialTheme.colorScheme.onSurfaceVariant
        MaintenanceDueStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.record.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.record.type.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = remainingLabel(item),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }

            if (item.kmRemaining != null) {
                LinearProgressIndicator(
                    progress = { item.consumedFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = {}
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    item.record.dueOdometerKm?.let {
                        Text(
                            text = stringResource(R.string.maintenance_due_km, formatInteger(it)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item.record.dueDateEpochDay?.let {
                        Text(
                            text = stringResource(R.string.maintenance_due_date, formatDate(it)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item.record.estimatedCost?.takeIf { it > 0.0 }?.let {
                        Text(
                            text = stringResource(R.string.maintenance_estimated, formatCurrency(it)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onToggleDone) {
                    Text(
                        if (item.record.done) {
                            stringResource(R.string.action_mark_pending)
                        } else {
                            stringResource(R.string.action_mark_done)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnTrackTile(item: MaintenanceItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = item.record.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = remainingLabel(item),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = stringResource(R.string.maintenance_empty_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.maintenance_empty_detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun remainingLabel(item: MaintenanceItem): String {
    item.kmRemaining?.let { km ->
        return if (km < 0) {
            stringResource(R.string.maintenance_overdue_km, formatInteger(abs(km)))
        } else {
            stringResource(R.string.maintenance_remaining_km, formatInteger(km))
        }
    }
    item.daysRemaining?.let { days ->
        return if (days < 0) {
            stringResource(R.string.maintenance_overdue_days, abs(days))
        } else {
            stringResource(R.string.maintenance_remaining_days, days)
        }
    }
    return stringResource(R.string.maintenance_no_due)
}
