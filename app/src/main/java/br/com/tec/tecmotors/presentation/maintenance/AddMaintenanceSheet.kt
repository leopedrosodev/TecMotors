package br.com.tec.tecmotors.presentation.maintenance

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.presentation.common.DateBrPickerField
import br.com.tec.tecmotors.presentation.common.DecimalField
import br.com.tec.tecmotors.presentation.common.MoneyField
import br.com.tec.tecmotors.presentation.common.formatInteger

/**
 * Lancamento de manutencao. Era um formulario no meio da tela, empurrando o
 * planejamento para baixo da dobra; virou folha, como o registro rapido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceSheet(
    state: MaintenanceUiState,
    onEvent: (MaintenanceUiEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMore by remember { mutableStateOf(false) }

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
                text = stringResource(R.string.maintenance_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MaintenanceType.entries.forEach { type ->
                    FilterChip(
                        selected = state.selectedType == type,
                        onClick = { onEvent(MaintenanceUiEvent.SelectType(type)) },
                        label = { Text(type.label) },
                        shape = CircleShape
                    )
                }
            }

            OutlinedTextField(
                value = state.titleText,
                onValueChange = { onEvent(MaintenanceUiEvent.ChangeTitle(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.label_maintenance_title)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            DecimalField(
                value = state.dueKmText,
                onValueChange = { onEvent(MaintenanceUiEvent.ChangeDueKm(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.label_due_km_optional)
            )

            state.currentOdometerKm?.let { current ->
                Text(
                    text = stringResource(
                        R.string.maintenance_due_helper,
                        formatInteger(current),
                        formatInteger(state.selectedType.defaultIntervalKm)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { onEvent(MaintenanceUiEvent.SaveMaintenance) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_save_maintenance),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            TextButton(
                onClick = { showMore = !showMore },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (showMore) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.maintenance_more_options),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            AnimatedVisibility(visible = showMore) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateBrPickerField(
                        value = state.dueDateText,
                        onValueChange = { onEvent(MaintenanceUiEvent.ChangeDueDate(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.label_due_date_optional)
                    )

                    MoneyField(
                        value = state.estimatedCostText,
                        onValueChange = { onEvent(MaintenanceUiEvent.ChangeEstimatedCost(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.label_estimated_cost_optional)
                    )

                    OutlinedTextField(
                        value = state.notesText,
                        onValueChange = { onEvent(MaintenanceUiEvent.ChangeNotes(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.label_notes)) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { receiptPicker.launch(arrayOf("image/*")) }) {
                            Text(stringResource(R.string.action_attach_receipt))
                        }
                        if (state.receiptImageUri != null) {
                            OutlinedButton(
                                onClick = { onEvent(MaintenanceUiEvent.SetReceiptImageUri(null)) }
                            ) {
                                Text(stringResource(R.string.action_remove_attachment))
                            }
                        }
                    }
                }
            }
        }
    }
}
