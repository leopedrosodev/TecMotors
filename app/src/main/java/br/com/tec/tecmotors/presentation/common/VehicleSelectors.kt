package br.com.tec.tecmotors.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType

@Composable
fun VehicleChipSelector(
    vehicles: List<Vehicle>,
    selectedVehicleId: Long,
    onSelect: (Long) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        vehicles.forEach { vehicle ->
            FilterChip(
                selected = selectedVehicleId == vehicle.id,
                onClick = { onSelect(vehicle.id) },
                label = { Text(vehicle.name) }
            )
        }
    }
}

@Composable
fun VehicleCardSelector(
    vehicles: List<Vehicle>,
    selectedVehicleId: Long,
    onSelect: (Long) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        vehicles.forEach { vehicle ->
            val selected = selectedVehicleId == vehicle.id
            val scale = animateFloatAsState(if (selected) 1f else 0.96f, label = "vehicle-card-scale")
            val selectedBlue = Color(0xFF2F81F7)

            Card(
                modifier = Modifier
                    .width(154.dp)
                    .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) selectedBlue else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(14.dp)
                    ),
                onClick = { onSelect(vehicle.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        selectedBlue.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(
                        imageVector = when (vehicle.type) {
                            VehicleType.CAR -> Icons.Filled.DirectionsCar
                            VehicleType.MOTORCYCLE -> Icons.Filled.TwoWheeler
                            VehicleType.OTHER -> Icons.Filled.LocalShipping
                        },
                        contentDescription = vehicle.type.label,
                        tint = if (selected) selectedBlue else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = vehicle.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = vehicle.type.label,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
