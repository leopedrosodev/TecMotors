package br.com.tec.tecmotors.presentation.maintenance

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.usecase.MaintenanceDueStatus
import br.com.tec.tecmotors.presentation.common.VehicleCardSelector
import br.com.tec.tecmotors.presentation.common.formatCurrency
import br.com.tec.tecmotors.presentation.common.formatDate
import br.com.tec.tecmotors.presentation.common.formatNumber

@Composable
fun MaintenanceScreen(
    state: MaintenanceUiState,
    viewModel: MaintenanceViewModel,
    onEvent: (MaintenanceUiEvent) -> Unit
) {
    val context = LocalContext.current
    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onEvent(MaintenanceUiEvent.SetReceiptImageUri(uri.toString()))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.title_maintenance), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.desc_maintenance_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        VehicleCardSelector(
            vehicles = state.vehicles,
            selectedVehicleId = state.selectedVehicleId,
            onSelect = { onEvent(MaintenanceUiEvent.SelectVehicle(it)) }
        )

        state.vehicleHealthIndex?.let { healthIndex ->
            if (healthIndex.components.any { it.hasData }) {
                ComponentsDashboard(healthIndex = healthIndex, modifier = Modifier.fillMaxWidth())
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }

        if (state.kmAlerts.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(stringResource(R.string.title_km_alerts), fontWeight = FontWeight.Bold)
                    state.kmAlerts.take(3).forEach { alert ->
                        val dueKm = alert.dueOdometerKm ?: 0.0
                        Text(stringResource(R.string.text_km_alert_row, alert.title, formatNumber(dueKm)))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MaintenanceType.entries.forEach { type ->
                FilterChip(
                    selected = state.selectedType == type,
                    onClick = { onEvent(MaintenanceUiEvent.SelectType(type)) },
                    label = { Text(type.label) }
                )
            }
        }

        OutlinedTextField(
            value = state.titleText,
            onValueChange = { onEvent(MaintenanceUiEvent.ChangeTitle(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_maintenance_title)) },
            singleLine = true
        )
        OutlinedTextField(
            value = state.dueDateText,
            onValueChange = { onEvent(MaintenanceUiEvent.ChangeDueDate(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_due_date_optional)) },
            singleLine = true
        )
        OutlinedTextField(
            value = state.dueKmText,
            onValueChange = { onEvent(MaintenanceUiEvent.ChangeDueKm(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_due_km_optional)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        OutlinedTextField(
            value = state.estimatedCostText,
            onValueChange = { onEvent(MaintenanceUiEvent.ChangeEstimatedCost(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_estimated_cost_optional)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        OutlinedTextField(
            value = state.notesText,
            onValueChange = { onEvent(MaintenanceUiEvent.ChangeNotes(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_notes)) }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { receiptPicker.launch(arrayOf("image/*")) }) {
                Text(stringResource(R.string.action_attach_receipt))
            }
            if (state.receiptImageUri != null) {
                OutlinedButton(onClick = { onEvent(MaintenanceUiEvent.SetReceiptImageUri(null)) }) {
                    Text(stringResource(R.string.action_remove_attachment))
                }
            }
        }

        state.receiptImageUri?.let {
            Text(stringResource(R.string.text_attachment_added))
        }

        Button(
            onClick = { onEvent(MaintenanceUiEvent.SaveMaintenance) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_save_maintenance))
        }

        state.feedback?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        Text(stringResource(R.string.title_vehicle_planning), style = MaterialTheme.typography.titleMedium)

        val selectedMaintenance = state.maintenanceRecords
            .filter { it.vehicleId == state.selectedVehicleId }
            .sortedBy { it.dueDateEpochDay ?: Long.MAX_VALUE }

        if (selectedMaintenance.isEmpty()) {
            Text(stringResource(R.string.text_no_maintenance_for_vehicle))
        } else {
            selectedMaintenance.forEach { record ->
                MaintenanceCard(
                    record = record,
                    status = viewModel.statusOf(record),
                    onToggleDone = {
                        onEvent(MaintenanceUiEvent.ToggleDone(record.id, !record.done))
                    }
                )
            }
        }
    }
}

@Composable
private fun MaintenanceCard(
    record: MaintenanceRecord,
    status: MaintenanceDueStatus,
    onToggleDone: () -> Unit
) {
    val statusLabel = when (status) {
        MaintenanceDueStatus.DONE -> stringResource(R.string.status_done)
        MaintenanceDueStatus.OVERDUE -> stringResource(R.string.status_overdue)
        MaintenanceDueStatus.DUE_SOON -> stringResource(R.string.status_due_soon)
        MaintenanceDueStatus.ON_TRACK -> stringResource(R.string.status_on_track)
    }

    val statusColor = when (status) {
        MaintenanceDueStatus.DONE -> MaterialTheme.colorScheme.primary
        MaintenanceDueStatus.OVERDUE -> MaterialTheme.colorScheme.error
        MaintenanceDueStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiary
        MaintenanceDueStatus.ON_TRACK -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(record.title, fontWeight = FontWeight.Bold)
                AssistChip(onClick = {}, enabled = false, label = { Text(statusLabel) })
            }

            Text(stringResource(R.string.text_type, record.type.label))
            record.dueDateEpochDay?.let { Text(stringResource(R.string.text_due_date, formatDate(it))) }
            record.dueOdometerKm?.let { Text(stringResource(R.string.text_due_km, formatNumber(it))) }
            record.estimatedCost?.let { Text(stringResource(R.string.text_estimated_cost, formatCurrency(it))) }
            if (record.notes.isNotBlank()) {
                Text(stringResource(R.string.text_notes, record.notes))
            }
            if (record.receiptImageUri != null) {
                Text(stringResource(R.string.text_attachment_added))
            }

            Text(
                text = stringResource(R.string.text_status, statusLabel),
                color = statusColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )

            TextButton(onClick = onToggleDone) {
                Text(
                    if (record.done) {
                        stringResource(R.string.action_mark_pending)
                    } else {
                        stringResource(R.string.action_mark_done)
                    }
                )
            }
        }
    }
}
