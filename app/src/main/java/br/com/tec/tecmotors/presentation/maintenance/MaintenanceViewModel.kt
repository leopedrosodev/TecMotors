package br.com.tec.tecmotors.presentation.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.usecase.AddMaintenanceUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateComponentHealthUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateMaintenanceStatusUseCase
import br.com.tec.tecmotors.domain.usecase.MaintenanceDueStatus
import br.com.tec.tecmotors.domain.usecase.ObserveMaintenanceUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveOdometersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
import br.com.tec.tecmotors.domain.usecase.SetMaintenanceDoneUseCase
import br.com.tec.tecmotors.presentation.common.UiFeedback
import br.com.tec.tecmotors.presentation.common.parseDateBrOrIso
import br.com.tec.tecmotors.presentation.common.parseDecimal
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class MaintenanceViewModel(
    private val observeVehiclesUseCase: ObserveVehiclesUseCase,
    private val observeOdometersUseCase: ObserveOdometersUseCase,
    private val observeMaintenanceUseCase: ObserveMaintenanceUseCase,
    private val addMaintenanceUseCase: AddMaintenanceUseCase,
    private val setMaintenanceDoneUseCase: SetMaintenanceDoneUseCase,
    private val calculateMaintenanceStatusUseCase: CalculateMaintenanceStatusUseCase,
    private val calculateComponentHealthUseCase: CalculateComponentHealthUseCase
) : ViewModel() {
    private val localState = MutableStateFlow(MaintenanceUiState())
    private val _events = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    val uiState: StateFlow<MaintenanceUiState> = combine(
        observeVehiclesUseCase(),
        observeOdometersUseCase(),
        observeMaintenanceUseCase(),
        localState
    ) { vehicles, odometers, maintenance, state ->
        val selected = state.selectedVehicleId.takeIf { id -> vehicles.any { it.id == id } }
            ?: vehicles.firstOrNull()?.id
            ?: -1L

        val latestOdometer = odometers
            .filter { it.vehicleId == selected }
            .maxByOrNull { it.dateEpochDay }
            ?.odometerKm

        val kmAlerts = maintenance
            .filter { it.vehicleId == selected }
            .filter { !it.done }
            .filter { it.dueOdometerKm != null && latestOdometer != null }
            .filter {
                val remaining = (it.dueOdometerKm ?: 0.0) - (latestOdometer ?: 0.0)
                remaining <= 500.0
            }
            .sortedBy { it.dueOdometerKm ?: Double.MAX_VALUE }

        val healthIndex = calculateComponentHealthUseCase(
            vehicleId = selected,
            maintenanceRecords = maintenance,
            currentOdometerKm = latestOdometer,
            today = LocalDate.now()
        )

        state.copy(
            vehicles = vehicles,
            odometerRecords = odometers,
            maintenanceRecords = maintenance,
            selectedVehicleId = selected,
            kmAlerts = kmAlerts,
            vehicleHealthIndex = healthIndex
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = localState.value
    )

    fun onEvent(event: MaintenanceUiEvent) {
        when (event) {
            is MaintenanceUiEvent.SelectVehicle -> {
                localState.update { it.copy(selectedVehicleId = event.vehicleId) }
            }

            is MaintenanceUiEvent.SelectType -> {
                localState.update {
                    it.copy(
                        selectedType = event.type,
                        titleText = if (it.titleText.isBlank()) event.type.label else it.titleText
                    )
                }
            }

            is MaintenanceUiEvent.ChangeTitle -> localState.update { it.copy(titleText = event.value) }
            is MaintenanceUiEvent.ChangeDueDate -> localState.update { it.copy(dueDateText = event.value) }
            is MaintenanceUiEvent.ChangeDueKm -> localState.update { it.copy(dueKmText = event.value) }
            is MaintenanceUiEvent.ChangeEstimatedCost -> localState.update { it.copy(estimatedCostText = event.value) }
            is MaintenanceUiEvent.ChangeNotes -> localState.update { it.copy(notesText = event.value) }
            is MaintenanceUiEvent.SetReceiptImageUri -> localState.update { it.copy(receiptImageUri = event.value) }

            MaintenanceUiEvent.SaveMaintenance -> saveMaintenance()

            is MaintenanceUiEvent.ToggleDone -> {
                viewModelScope.launch {
                    setMaintenanceDoneUseCase(event.recordId, event.done)
                }
            }
        }
    }

    private fun saveMaintenance() {
        val state = uiState.value

        val dueDateText = state.dueDateText.trim()
        val dueKmText = state.dueKmText.trim()
        val estimatedCostText = state.estimatedCostText.trim()

        val dueDate = dueDateText.takeIf { it.isNotBlank() }?.let(::parseDateBrOrIso)
        val dueKm = dueKmText.takeIf { it.isNotBlank() }?.let(::parseDecimal)
        val estimatedCost = estimatedCostText.takeIf { it.isNotBlank() }?.let(::parseDecimal)

        if (state.selectedVehicleId <= 0L) {
            emitFeedback(UiFeedback.Error("Selecione um veiculo"))
            return
        }
        if (state.titleText.isBlank()) {
            emitFeedback(UiFeedback.Error("Informe um titulo para a manutencao"))
            return
        }
        if (dueDateText.isNotBlank() && dueDate == null) {
            emitFeedback(UiFeedback.Error("Data invalida"))
            return
        }
        if (dueKmText.isNotBlank() && (dueKm == null || dueKm <= 0.0)) {
            emitFeedback(UiFeedback.Error("Odometro invalido"))
            return
        }
        if (estimatedCostText.isNotBlank() && (estimatedCost == null || estimatedCost < 0.0)) {
            emitFeedback(UiFeedback.Error("Custo estimado invalido"))
            return
        }
        if (dueDate == null && dueKm == null) {
            emitFeedback(UiFeedback.Error("Informe data ou odometro para vencimento"))
            return
        }

        viewModelScope.launch {
            addMaintenanceUseCase(
                vehicleId = state.selectedVehicleId,
                type = state.selectedType,
                title = state.titleText,
                notes = state.notesText,
                createdAtEpochDay = LocalDate.now().toEpochDay(),
                dueDateEpochDay = dueDate?.toEpochDay(),
                dueOdometerKm = dueKm,
                estimatedCost = estimatedCost,
                receiptImageUri = state.receiptImageUri
            )
            localState.update {
                it.copy(
                    dueDateText = "",
                    dueKmText = "",
                    estimatedCostText = "",
                    notesText = "",
                    receiptImageUri = null
                )
            }
            emitFeedback(UiFeedback.Success("Manutencao cadastrada"))
        }
    }

    fun latestOdometer(vehicleId: Long): Double? {
        return uiState.value.odometerRecords
            .filter { it.vehicleId == vehicleId }
            .maxByOrNull { it.dateEpochDay }
            ?.odometerKm
    }

    fun maintenanceTypeLabel(type: MaintenanceType): String = type.label

    fun statusOf(record: MaintenanceRecord): MaintenanceDueStatus {
        val odometer = latestOdometer(record.vehicleId)
        return calculateMaintenanceStatusUseCase(record, odometer, LocalDate.now())
    }

    private fun emitFeedback(feedback: UiFeedback) {
        if (!_events.tryEmit(feedback)) {
            viewModelScope.launch { _events.emit(feedback) }
        }
    }
}
