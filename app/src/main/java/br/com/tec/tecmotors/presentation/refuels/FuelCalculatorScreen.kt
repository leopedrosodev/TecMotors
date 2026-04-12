package br.com.tec.tecmotors.presentation.refuels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.presentation.common.DecimalField
import br.com.tec.tecmotors.presentation.common.MoneyField
import br.com.tec.tecmotors.presentation.common.formatCurrency
import br.com.tec.tecmotors.presentation.common.formatNumber
import br.com.tec.tecmotors.presentation.common.parseDecimal

private enum class MissingField(val label: String) {
    DISTANCE(""),
    CONSUMPTION(""),
    LITERS("")
}

@Composable
fun FuelCalculatorScreen() {
    var missingField by remember { mutableStateOf(MissingField.LITERS) }
    var distanceText by remember { mutableStateOf("") }
    var consumptionText by remember { mutableStateOf("") }
    var litersText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }

    val distance = parseDecimal(distanceText)
    val consumption = parseDecimal(consumptionText)
    val liters = parseDecimal(litersText)
    val price = parseDecimal(priceText)

    val solvedDistance = when (missingField) {
        MissingField.DISTANCE -> if (consumption != null && liters != null) consumption * liters else null
        MissingField.CONSUMPTION -> distance
        MissingField.LITERS -> distance
    }

    val solvedConsumption = when (missingField) {
        MissingField.DISTANCE -> consumption
        MissingField.CONSUMPTION -> if (distance != null && liters != null && liters > 0.0) distance / liters else null
        MissingField.LITERS -> consumption
    }

    val solvedLiters = when (missingField) {
        MissingField.DISTANCE -> liters
        MissingField.CONSUMPTION -> liters
        MissingField.LITERS -> if (distance != null && consumption != null && consumption > 0.0) distance / consumption else null
    }

    val totalCost = if (solvedLiters != null && price != null) solvedLiters * price else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.title_calculator), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.desc_calculator_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MissingField.entries.forEach { option ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        RadioButton(
                            selected = missingField == option,
                            onClick = { missingField = option }
                        )
                        Text(
                            text = when (option) {
                                MissingField.DISTANCE -> stringResource(R.string.calc_missing_distance)
                                MissingField.CONSUMPTION -> stringResource(R.string.calc_missing_consumption)
                                MissingField.LITERS -> stringResource(R.string.calc_missing_liters)
                            },
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }

        DecimalField(
            value = distanceText,
            onValueChange = { distanceText = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.calc_distance),
            enabled = missingField != MissingField.DISTANCE
        )

        DecimalField(
            value = consumptionText,
            onValueChange = { consumptionText = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.calc_consumption),
            enabled = missingField != MissingField.CONSUMPTION
        )

        DecimalField(
            value = litersText,
            onValueChange = { litersText = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.calc_liters),
            enabled = missingField != MissingField.LITERS
        )

        MoneyField(
            value = priceText,
            onValueChange = { priceText = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.calc_price)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(stringResource(R.string.calc_result), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.calc_result_distance, solvedDistance?.let(::formatNumber) ?: "-"))
                Text(stringResource(R.string.calc_result_consumption, solvedConsumption?.let(::formatNumber) ?: "-"))
                Text(stringResource(R.string.calc_result_liters, solvedLiters?.let(::formatNumber) ?: "-"))
                Text(stringResource(R.string.calc_result_cost, totalCost?.let(::formatCurrency) ?: "-"))
            }
        }

        TextButton(
            onClick = {
                distanceText = ""
                consumptionText = ""
                litersText = ""
                priceText = ""
                missingField = MissingField.LITERS
            }
        ) {
            Text(stringResource(R.string.action_clear_calculator))
        }
    }
}
