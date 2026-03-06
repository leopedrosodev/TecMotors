package br.com.tec.tecmotors.presentation.maintenance

import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Vehicle

sealed interface MaintenanceUiEvent {
    data class SelectVehicle(val vehicleId: Long) : MaintenanceUiEvent
    data class SelectType(val type: MaintenanceType) : MaintenanceUiEvent
    data class ChangeTitle(val value: String) : MaintenanceUiEvent
    data class ChangeDueDate(val value: String) : MaintenanceUiEvent
    data class ChangeDueKm(val value: String) : MaintenanceUiEvent
    data class ChangeEstimatedCost(val value: String) : MaintenanceUiEvent
    data class ChangeNotes(val value: String) : MaintenanceUiEvent
    data class SetReceiptImageUri(val value: String?) : MaintenanceUiEvent
    data object SaveMaintenance : MaintenanceUiEvent
    data class ToggleDone(val recordId: Long, val done: Boolean) : MaintenanceUiEvent
    data object ClearFeedback : MaintenanceUiEvent
}

data class MaintenanceUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val odometerRecords: List<OdometerRecord> = emptyList(),
    val maintenanceRecords: List<MaintenanceRecord> = emptyList(),
    val selectedVehicleId: Long = -1L,
    val selectedType: MaintenanceType = MaintenanceType.OIL_CHANGE,
    val titleText: String = MaintenanceType.OIL_CHANGE.label,
    val dueDateText: String = "",
    val dueKmText: String = "",
    val estimatedCostText: String = "",
    val notesText: String = "",
    val receiptImageUri: String? = null,
    val kmAlerts: List<MaintenanceRecord> = emptyList(),
    val feedback: String? = null
)
