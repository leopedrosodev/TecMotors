package br.com.tec.tecmotors.presentation.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.domain.model.ComponentHealth
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.VehicleHealthIndex
import br.com.tec.tecmotors.presentation.common.formatNumber

@Composable
fun ComponentsDashboard(
    healthIndex: VehicleHealthIndex,
    modifier: Modifier = Modifier
) {
    val componentsWithData = healthIndex.components.filter { it.hasData }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.title_components_health),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        AttentionIndexCard(attentionPercent = healthIndex.attentionPercent)

        val goodComponents = componentsWithData.filter { it.qualityPercent >= 80 }
        if (goodComponents.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    goodComponents.forEach { component ->
                        Text(
                            text = stringResource(
                                R.string.text_component_ok,
                                component.type.dashboardLabel()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (componentsWithData.isNotEmpty()) {
            componentsWithData.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { component ->
                        ComponentHealthCard(
                            component = component,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        val componentsWithKm = componentsWithData.filter { it.kmRemaining != null }
        if (componentsWithKm.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            componentsWithKm.forEach { component ->
                HorizontalDivider()
                ComponentListRow(component = component)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun AttentionIndexCard(attentionPercent: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.title_attention_index),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$attentionPercent%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = attentionColor(attentionPercent)
                )
            }
            LinearProgressIndicator(
                progress = { attentionPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = attentionColor(attentionPercent),
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
private fun ComponentHealthCard(component: ComponentHealth, modifier: Modifier = Modifier) {
    val qualityColor = qualityColor(component.qualityPercent)
    val qualityLabel = qualityLabel(component.qualityPercent)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                Box(modifier = Modifier.size(20.dp)) {
                    Text(
                        text = component.type.icon,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = component.type.dashboardLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${component.qualityPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = qualityColor
                )
                Text(
                    text = qualityLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = qualityColor
                )
            }
            LinearProgressIndicator(
                progress = { component.qualityPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = qualityColor,
                trackColor = MaterialTheme.colorScheme.surface
            )
            component.daysSinceLastService?.let { days ->
                Text(
                    text = stringResource(R.string.text_last_service_days, days),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ComponentListRow(component: ComponentHealth) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = component.type.icon, style = MaterialTheme.typography.titleMedium)
            Text(
                text = component.type.dashboardLabel().uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            component.kmRemaining?.let { km ->
                Text(
                    text = stringResource(R.string.text_km_remaining, formatNumber(km)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = ">",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun qualityColor(qualityPercent: Int): Color = when {
    qualityPercent >= 70 -> MaterialTheme.colorScheme.primary
    qualityPercent >= 40 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

@Composable
private fun qualityLabel(qualityPercent: Int): String = when {
    qualityPercent >= 70 -> stringResource(R.string.text_quality_good)
    qualityPercent >= 40 -> stringResource(R.string.text_quality_fair)
    else -> stringResource(R.string.text_quality_poor)
}

@Composable
private fun attentionColor(attentionPercent: Int): Color = when {
    attentionPercent <= 20 -> MaterialTheme.colorScheme.primary
    attentionPercent <= 50 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

@Composable
private fun MaintenanceType.dashboardLabel(): String = when (this) {
    MaintenanceType.OIL_CHANGE -> stringResource(R.string.component_oil_quality)
    MaintenanceType.TIRE_ROTATION -> stringResource(R.string.component_tire_condition)
    MaintenanceType.BRAKE_SERVICE -> stringResource(R.string.component_brake_condition)
    MaintenanceType.GENERAL_REVIEW -> stringResource(R.string.component_general_review)
    MaintenanceType.OTHER -> label
}

private val MaintenanceType.icon: String
    get() = when (this) {
        MaintenanceType.OIL_CHANGE -> "🛢"
        MaintenanceType.TIRE_ROTATION -> "🔄"
        MaintenanceType.BRAKE_SERVICE -> "🔧"
        MaintenanceType.GENERAL_REVIEW -> "🔍"
        MaintenanceType.OTHER -> "⚙"
    }
