package br.com.tec.tecmotors.presentation.vehicles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.presentation.common.DateBrPickerField
import br.com.tec.tecmotors.presentation.common.DecimalField
import br.com.tec.tecmotors.presentation.common.VehicleFilterRow
import br.com.tec.tecmotors.presentation.common.formatInteger

/** Registro de odometro. Era uma secao no fim da tela; virou folha. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdometerSheet(
    state: VehiclesUiState,
    onEvent: (VehiclesUiEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val last = state.lastOdometerOf(state.selectedVehicleId)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.label_register_odometer),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (state.vehicles.size > 1) {
                VehicleFilterRow(
                    vehicles = state.vehicles,
                    selectedVehicleId = state.selectedVehicleId,
                    onSelect = { onEvent(VehiclesUiEvent.SelectVehicle(it)) }
                )
            }

            DateBrPickerField(
                value = state.dateText,
                onValueChange = { onEvent(VehiclesUiEvent.ChangeDate(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.label_date_br)
            )

            DecimalField(
                value = state.odometerText,
                onValueChange = { onEvent(VehiclesUiEvent.ChangeOdometer(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.label_odometer_km)
            )

            last?.let {
                Text(
                    text = stringResource(R.string.quick_refuel_previous_odometer, formatInteger(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { onEvent(VehiclesUiEvent.SaveOdometer) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_save_odometer),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

/** Cadastro de veiculo: nome e, no mesmo toque, o tipo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleSheet(
    state: VehiclesUiState,
    onEvent: (VehiclesUiEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.title_add_vehicle),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = state.newVehicleName,
                onValueChange = { onEvent(VehiclesUiEvent.ChangeNewVehicleName(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.label_new_vehicle_name)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                text = stringResource(R.string.vehicles_pick_type),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypeButton(
                    label = stringResource(R.string.action_add_car),
                    icon = Icons.Filled.DirectionsCar,
                    onClick = { onEvent(VehiclesUiEvent.AddVehicle(VehicleType.CAR)) },
                    modifier = Modifier.weight(1f)
                )
                TypeButton(
                    label = stringResource(R.string.action_add_moto),
                    icon = Icons.Filled.TwoWheeler,
                    onClick = { onEvent(VehiclesUiEvent.AddVehicle(VehicleType.MOTORCYCLE)) },
                    modifier = Modifier.weight(1f)
                )
                TypeButton(
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
        }
    }
}

@Composable
private fun TypeButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
