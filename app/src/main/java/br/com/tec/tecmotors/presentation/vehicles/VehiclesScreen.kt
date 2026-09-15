package br.com.tec.tecmotors.presentation.vehicles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.presentation.common.formatDate
import br.com.tec.tecmotors.presentation.common.formatInteger
import br.com.tec.tecmotors.presentation.common.vehicleTypeIcon

@Composable
fun VehiclesScreen(
    state: VehiclesUiState,
    onEvent: (VehiclesUiEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.vehicles.forEach { vehicle ->
            VehicleCard(
                vehicle = vehicle,
                draftName = state.nameDrafts[vehicle.id] ?: vehicle.name,
                lastOdometerKm = state.lastOdometerOf(vehicle.id),
                lastOdometerDay = state.lastOdometerDayOf(vehicle.id),
                onNameChange = { onEvent(VehiclesUiEvent.ChangeVehicleName(vehicle.id, it)) },
                onNameSave = { onEvent(VehiclesUiEvent.SaveVehicleName(vehicle.id)) },
                onRegisterOdometer = {
                    onEvent(VehiclesUiEvent.SelectVehicle(vehicle.id))
                    onEvent(VehiclesUiEvent.SetOdometerSheetVisible(true))
                }
            )
        }

        OutlinedButton(
            onClick = { onEvent(VehiclesUiEvent.SetAddVehicleSheetVisible(true)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.title_add_vehicle))
        }

        Text(
            text = stringResource(R.string.hint_odometer_month),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (state.showOdometerSheet) {
        OdometerSheet(
            state = state,
            onEvent = onEvent,
            onDismiss = { onEvent(VehiclesUiEvent.SetOdometerSheetVisible(false)) }
        )
    }

    if (state.showAddVehicleSheet) {
        AddVehicleSheet(
            state = state,
            onEvent = onEvent,
            onDismiss = { onEvent(VehiclesUiEvent.SetAddVehicleSheetVisible(false)) }
        )
    }
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    draftName: String,
    lastOdometerKm: Double?,
    lastOdometerDay: Long?,
    onNameChange: (String) -> Unit,
    onNameSave: () -> Unit,
    onRegisterOdometer: () -> Unit
) {
    val renamed = draftName.trim() != vehicle.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = vehicleTypeIcon(vehicle.type),
                        contentDescription = vehicle.type.label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (lastOdometerKm == null) {
                            stringResource(R.string.text_no_odometer)
                        } else {
                            stringResource(R.string.home_odometer_value, formatInteger(lastOdometerKm))
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = lastOdometerDay?.let {
                            stringResource(R.string.vehicles_last_reading, formatDate(it))
                        } ?: vehicle.type.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(onClick = onRegisterOdometer, shape = CircleShape) {
                    Text(stringResource(R.string.vehicles_register_km))
                }
            }

            OutlinedTextField(
                value = draftName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.label_vehicle_name)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (renamed) {
                        IconButton(onClick = onNameSave) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(R.string.action_save_name),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    }
}
