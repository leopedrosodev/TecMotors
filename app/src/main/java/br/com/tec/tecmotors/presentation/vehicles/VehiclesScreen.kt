package br.com.tec.tecmotors.presentation.vehicles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.presentation.common.DateBrField
import br.com.tec.tecmotors.presentation.common.DecimalField
import br.com.tec.tecmotors.presentation.common.VehicleChipSelector
import br.com.tec.tecmotors.presentation.common.formatNumber
import br.com.tec.tecmotors.ui.theme.accentHighlight

@Composable
fun VehiclesScreen(
    state: VehiclesUiState,
    onEvent: (VehiclesUiEvent) -> Unit
) {
    val accentHighlight = MaterialTheme.colorScheme.accentHighlight

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Header ---
        Text(stringResource(R.string.title_vehicles), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.desc_vehicles_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        // --- Existing vehicles ---
        state.vehicles.forEach { vehicle ->
            val draftName = state.nameDrafts[vehicle.id] ?: vehicle.name
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = vehicleIcon(vehicle.type),
                            contentDescription = vehicle.type.label,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = vehicle.type.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { onEvent(VehiclesUiEvent.ChangeVehicleName(vehicle.id, it)) },
                        label = { Text(stringResource(R.string.label_vehicle_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onEvent(VehiclesUiEvent.SaveVehicleName(vehicle.id)) }) {
                            Text(stringResource(R.string.action_save_name))
                        }
                        Text(
                            text = lastOdometerText(vehicle.id, state.odometerRecords),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))

        // --- Add vehicle section ---
        Text(
            stringResource(R.string.title_add_vehicle),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = state.newVehicleName,
            onValueChange = { onEvent(VehiclesUiEvent.ChangeNewVehicleName(it)) },
            label = { Text(stringResource(R.string.label_new_vehicle_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AddVehicleTypeButton(
                label = stringResource(R.string.action_add_car),
                icon = Icons.Filled.DirectionsCar,
                onClick = { onEvent(VehiclesUiEvent.AddVehicle(VehicleType.CAR)) },
                modifier = Modifier.weight(1f)
            )
            AddVehicleTypeButton(
                label = stringResource(R.string.action_add_moto),
                icon = Icons.Filled.TwoWheeler,
                onClick = { onEvent(VehiclesUiEvent.AddVehicle(VehicleType.MOTORCYCLE)) },
                modifier = Modifier.weight(1f)
            )
            AddVehicleTypeButton(
                label = stringResource(R.string.action_add_other),
                icon = Icons.Filled.LocalShipping,
                onClick = { onEvent(VehiclesUiEvent.AddVehicle(VehicleType.OTHER)) },
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = stringResource(R.string.hint_other_vehicle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))

        // --- Odometer section (highlighted) ---
        Text(
            stringResource(R.string.label_register_odometer),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        VehicleChipSelector(
            vehicles = state.vehicles,
            selectedVehicleId = state.selectedVehicleId,
            onSelect = { onEvent(VehiclesUiEvent.SelectVehicle(it)) }
        )

        DateBrField(
            value = state.dateText,
            onValueChange = { onEvent(VehiclesUiEvent.ChangeDate(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.label_date_br)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, accentHighlight),
            colors = CardDefaults.cardColors(
                containerColor = accentHighlight.copy(alpha = 0.08f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                DecimalField(
                    value = state.odometerText,
                    onValueChange = { onEvent(VehiclesUiEvent.ChangeOdometer(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.label_odometer_km),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentHighlight,
                        unfocusedBorderColor = accentHighlight.copy(alpha = 0.6f),
                        cursorColor = accentHighlight
                    )
                )
            }
        }

        Button(
            onClick = { onEvent(VehiclesUiEvent.SaveOdometer) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentHighlight)
        ) {
            Text(stringResource(R.string.action_save_odometer))
        }

        Text(
            text = stringResource(R.string.hint_odometer_month),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AddVehicleTypeButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun lastOdometerText(vehicleId: Long, records: List<OdometerRecord>): String {
    val latest = records
        .filter { it.vehicleId == vehicleId }
        .maxByOrNull { it.dateEpochDay }

    return if (latest == null) {
        stringResource(R.string.text_no_odometer)
    } else {
        stringResource(R.string.text_last_odometer, formatNumber(latest.odometerKm))
    }
}

private fun vehicleIcon(type: VehicleType): ImageVector = when (type) {
    VehicleType.CAR -> Icons.Filled.DirectionsCar
    VehicleType.MOTORCYCLE -> Icons.Filled.TwoWheeler
    VehicleType.OTHER -> Icons.Filled.LocalShipping
}
