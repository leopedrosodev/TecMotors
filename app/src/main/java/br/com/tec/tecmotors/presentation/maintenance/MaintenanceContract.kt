package br.com.tec.tecmotors.presentation.maintenance

import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleHealthIndex
import br.com.tec.tecmotors.domain.usecase.MaintenanceDueStatus

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
    data class SetAddSheetVisible(val visible: Boolean) : MaintenanceUiEvent
}

/**
 * Um item de manutencao ja resolvido para a UI: status, quanto falta e o quanto
 * do intervalo ja foi consumido. A tela nao calcula nada disso.
 */
data class MaintenanceItem(
    val record: MaintenanceRecord,
    val status: MaintenanceDueStatus,
    val kmRemaining: Double?,
    val daysRemaining: Long?,
    /** 0 = acabou de ser feita, 1 = vencida. */
    val consumedFraction: Float
) {
    val needsAttention: Boolean
        get() = status == MaintenanceDueStatus.OVERDUE || status == MaintenanceDueStatus.DUE_SOON
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
    val showAddSheet: Boolean = false,
    val currentOdometerKm: Double? = null,
    val vehicleHealthIndex: VehicleHealthIndex? = null,
    val attentionItems: List<MaintenanceItem> = emptyList(),
    val onTrackItems: List<MaintenanceItem> = emptyList(),
    val doneItems: List<MaintenanceItem> = emptyList()
) {
    val healthPercent: Int
        get() = 100 - (vehicleHealthIndex?.attentionPercent ?: 0)

    val hasPlanning: Boolean
        get() = attentionItems.isNotEmpty() || onTrackItems.isNotEmpty() || doneItems.isNotEmpty()
}
