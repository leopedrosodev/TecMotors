package br.com.tec.tecmotors.presentation.refuels

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.presentation.common.DecimalField
import br.com.tec.tecmotors.presentation.common.MoneyField
import br.com.tec.tecmotors.presentation.common.VehicleFilterRow
import br.com.tec.tecmotors.presentation.common.formatCurrency
import br.com.tec.tecmotors.presentation.common.formatNumber

@Composable
fun FuelCalculatorScreen(
    state: CalculatorUiState,
    onEvent: (CalculatorUiEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.vehicles.size > 1) {
            VehicleFilterRow(
                vehicles = state.vehicles,
                selectedVehicleId = state.selectedVehicleId,
                onSelect = { onEvent(CalculatorUiEvent.SelectVehicle(it)) }
            )
        }

        if (state.hasVehicleData) {
            OutlinedButton(
                onClick = { onEvent(CalculatorUiEvent.UseVehicleData) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(
                        R.string.calc_use_vehicle,
                        state.vehicleKmPerLiter?.let(::formatNumber) ?: "—",
                        state.vehiclePricePerLiter?.let(::formatCurrency) ?: "—"
                    )
                )
            }
        }

        Text(
            text = stringResource(R.string.calc_what_to_find),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            CalculatorTarget.entries.forEach { option ->
                val selected = state.target == option
                Surface(
                    modifier = Modifier.weight(1f),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEvent(CalculatorUiEvent.SelectTarget(option)) },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(option.titleRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) {
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

        CalculatorField(
            isResult = state.isTarget(CalculatorTarget.DISTANCE),
            value = state.distanceText,
            resultText = state.distance?.let { formatNumber(it) },
            onValueChange = { onEvent(CalculatorUiEvent.ChangeDistance(it)) },
            label = stringResource(R.string.calc_distance)
        )

        CalculatorField(
            isResult = state.isTarget(CalculatorTarget.CONSUMPTION),
            value = state.consumptionText,
            resultText = state.consumption?.let { formatNumber(it) },
            onValueChange = { onEvent(CalculatorUiEvent.ChangeConsumption(it)) },
            label = stringResource(R.string.calc_consumption)
        )

        CalculatorField(
            isResult = state.isTarget(CalculatorTarget.LITERS),
            value = state.litersText,
            resultText = state.liters?.let { formatNumber(it) },
            onValueChange = { onEvent(CalculatorUiEvent.ChangeLiters(it)) },
            label = stringResource(R.string.calc_liters)
        )

        MoneyField(
            value = state.priceText,
            onValueChange = { onEvent(CalculatorUiEvent.ChangePrice(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.calc_price)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.calc_total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = state.totalCost?.let(::formatCurrency) ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.liters?.let {
                        stringResource(R.string.calc_total_detail, formatNumber(it))
                    } ?: stringResource(R.string.calc_total_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        TextButton(
            onClick = { onEvent(CalculatorUiEvent.Clear) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_clear_calculator))
        }
    }
}

/**
 * O campo que esta sendo calculado mostra a resposta nele mesmo, destacada.
 * Antes ele ficava desabilitado e vazio, com o resultado num cartao separado -
 * o usuario tinha que procurar a resposta longe da pergunta.
 */
@Composable
private fun CalculatorField(
    isResult: Boolean,
    value: String,
    resultText: String?,
    onValueChange: (String) -> Unit,
    label: String
) {
    if (!isResult) {
        DecimalField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = label
        )
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = resultText ?: "—",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
