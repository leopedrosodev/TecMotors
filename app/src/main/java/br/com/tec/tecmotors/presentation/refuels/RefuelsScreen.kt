package br.com.tec.tecmotors.presentation.refuels

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.domain.model.FuelUsageType
import br.com.tec.tecmotors.presentation.common.DateBrField
import br.com.tec.tecmotors.presentation.common.DecimalField
import br.com.tec.tecmotors.presentation.common.VehicleCardSelector
import br.com.tec.tecmotors.presentation.common.formatCurrency
import br.com.tec.tecmotors.presentation.common.formatDate
import br.com.tec.tecmotors.presentation.common.formatNumber

@Composable
fun RefuelsScreen(
    state: RefuelsUiState,
    onEvent: (RefuelsUiEvent) -> Unit
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
            onEvent(RefuelsUiEvent.SetReceiptImageUri(uri.toString()))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.title_refuels), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.desc_refuels_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        VehicleCardSelector(
            vehicles = state.vehicles,
            selectedVehicleId = state.selectedVehicleId,
            onSelect = { onEvent(RefuelsUiEvent.SelectVehicle(it)) }
        )

        DateBrField(
            value = state.dateText,
            onValueChange = { onEvent(RefuelsUiEvent.ChangeDate(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.label_date_br)
        )
        DecimalField(
            value = state.odometerText,
            onValueChange = { onEvent(RefuelsUiEvent.ChangeOdometer(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.label_odometer_km)
        )
        DecimalField(
            value = state.litersText,
            onValueChange = { onEvent(RefuelsUiEvent.ChangeLiters(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.label_liters)
        )
        DecimalField(
            value = state.priceText,
            onValueChange = { onEvent(RefuelsUiEvent.ChangePrice(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.label_price_per_liter)
        )
        OutlinedTextField(
            value = state.stationText,
            onValueChange = { onEvent(RefuelsUiEvent.ChangeStation(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_station_optional)) },
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
                    label = { Text(usageType.label) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { receiptPicker.launch(arrayOf("image/*")) }) {
                Text(stringResource(R.string.action_attach_receipt))
            }
            if (state.receiptImageUri != null) {
                OutlinedButton(onClick = { onEvent(RefuelsUiEvent.SetReceiptImageUri(null)) }) {
                    Text(stringResource(R.string.action_remove_attachment))
                }
            }
        }

        state.receiptImageUri?.let {
            Text(stringResource(R.string.text_attachment_added))
        }

        if (state.stationInsights.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(stringResource(R.string.title_station_history))
                    state.suggestedStationName?.let { suggested ->
                        Text(stringResource(R.string.text_station_suggestion, suggested))
                    }
                    state.stationInsights.take(4).forEach { insight ->
                        Text(
                            stringResource(
                                R.string.text_station_history_row,
                                insight.stationName,
                                formatNumber(insight.averagePrice),
                                insight.timesUsed
                            )
                        )
                    }
                }
            }
        }

        Button(
            onClick = { onEvent(RefuelsUiEvent.SaveRefuel) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_save_refuel))
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        Text(stringResource(R.string.title_latest_refuels), style = MaterialTheme.typography.titleMedium)

        if (state.fuelRecords.isEmpty()) {
            Text(stringResource(R.string.text_no_refuels))
        } else {
            state.fuelRecords.take(10).forEach { record ->
                val vehicleName = state.vehicles.firstOrNull { it.id == record.vehicleId }?.name ?: "Veiculo"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("$vehicleName - ${formatDate(record.dateEpochDay)}")
                        Text(stringResource(R.string.text_refuel_odometer, formatNumber(record.odometerKm)))
                        Text(
                            stringResource(
                                R.string.text_refuel_liters_price,
                                formatNumber(record.liters),
                                formatNumber(record.pricePerLiter)
                            )
                        )
                        if (record.stationName.isNotBlank()) {
                            Text(stringResource(R.string.text_station_label, record.stationName))
                        }
                        Text(stringResource(R.string.text_usage_label, record.usageType.label))
                        if (record.receiptImageUri != null) {
                            Text(stringResource(R.string.text_attachment_added))
                        }
                        Text(stringResource(R.string.text_refuel_total, formatCurrency(record.totalCost)))
                    }
                }
            }
        }
    }
}
