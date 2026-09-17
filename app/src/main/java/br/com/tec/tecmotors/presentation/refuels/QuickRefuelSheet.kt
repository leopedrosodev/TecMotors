package br.com.tec.tecmotors.presentation.refuels

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.domain.model.FuelUsageType
import br.com.tec.tecmotors.presentation.common.DateBrPickerField
import br.com.tec.tecmotors.presentation.common.formatCurrency
import br.com.tec.tecmotors.presentation.common.formatInteger
import br.com.tec.tecmotors.presentation.common.formatNumber

/**
 * Registro rapido: valor pago + litros + odometro. Tudo que da pra deduzir
 * (preco/L, posto, tipo de uso) fica fora do caminho.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickRefuelSheet(
    state: RefuelsUiState,
    onEvent: (RefuelsUiEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMoreFields by remember { mutableStateOf(false) }

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
            onEvent(RefuelsUiEvent.SetReceiptImageUri(uri.toString()))
        }
    }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.isEditing) {
                        stringResource(R.string.quick_refuel_title_edit)
                    } else {
                        stringResource(R.string.quick_refuel_title)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (state.isEditing) {
                    TextButton(
                        onClick = {
                            state.editingRecordId?.let {
                                onEvent(RefuelsUiEvent.DeleteRefuel(it))
                            }
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (state.vehicles.size > 1) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.vehicles.forEach { vehicle ->
                        FilterChip(
                            selected = vehicle.id == state.selectedVehicleId,
                            onClick = { onEvent(RefuelsUiEvent.SelectVehicle(vehicle.id)) },
                            label = {
                                Text(
                                    text = vehicle.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            shape = CircleShape
                        )
                    }
                }
            }

            DateBrPickerField(
                value = state.dateText,
                onValueChange = { onEvent(RefuelsUiEvent.ChangeDate(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.label_date_br)
            )

            BigNumberField(
                value = state.totalPaidText,
                onValueChange = { onEvent(RefuelsUiEvent.ChangeTotalPaid(it)) },
                label = stringResource(R.string.quick_refuel_total_paid),
                prefix = "R$"
            )

            BigNumberField(
                value = state.litersText,
                onValueChange = { onEvent(RefuelsUiEvent.ChangeLiters(it)) },
                label = stringResource(R.string.quick_refuel_liters),
                prefix = "L"
            )

            BigNumberField(
                value = state.odometerText,
                onValueChange = { onEvent(RefuelsUiEvent.ChangeOdometer(it)) },
                label = stringResource(R.string.quick_refuel_odometer),
                prefix = "KM",
                supportingText = state.lastOdometerKm?.let {
                    stringResource(R.string.quick_refuel_previous_odometer, formatInteger(it))
                }
            )

            state.computedPricePerLiter?.let { pricePerLiter ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = buildString {
                                append(
                                    context.getString(
                                        R.string.quick_refuel_price_per_liter,
                                        formatCurrency(pricePerLiter)
                                    )
                                )
                                state.distanceSinceLastKm?.let {
                                    append(" · ")
                                    append(
                                        context.getString(
                                            R.string.quick_refuel_distance_since,
                                            formatInteger(it)
                                        )
                                    )
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        state.estimatedKmPerLiter?.let {
                            Text(
                                text = stringResource(
                                    R.string.quick_refuel_km_per_liter,
                                    formatNumber(it)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = { showMoreFields = !showMoreFields },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (showMoreFields) {
                        Icons.Filled.ExpandLess
                    } else {
                        Icons.Filled.ExpandMore
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.quick_refuel_more_fields),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            AnimatedVisibility(visible = showMoreFields) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.stationText,
                        onValueChange = { onEvent(RefuelsUiEvent.ChangeStation(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.label_station_optional)) },
                        placeholder = {
                            if (state.lastStationName.isNotBlank()) {
                                Text(state.lastStationName)
                            }
                        },
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FuelUsageType.entries.forEach { usageType ->
                            FilterChip(
                                selected = state.selectedUsageType == usageType,
                                onClick = { onEvent(RefuelsUiEvent.SelectUsageType(usageType)) },
                                label = { Text(usageType.label) },
                                shape = CircleShape
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { receiptPicker.launch(arrayOf("image/*")) }) {
                            Text(stringResource(R.string.action_attach_receipt))
                        }
                        if (state.receiptImageUri != null) {
                            OutlinedButton(
                                onClick = { onEvent(RefuelsUiEvent.SetReceiptImageUri(null)) }
                            ) {
                                Text(stringResource(R.string.action_remove_attachment))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onEvent(RefuelsUiEvent.SaveQuickRefuel)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = state.quickEntryReady
            ) {
                Text(
                    text = if (state.isEditing) {
                        stringResource(R.string.action_save_changes)
                    } else {
                        stringResource(R.string.action_save_refuel)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Text(
                text = stringResource(R.string.quick_refuel_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BigNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    prefix: String,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        prefix = {
            Text(
                text = prefix,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        supportingText = supportingText?.let { { Text(it) } },
        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
    )
}
