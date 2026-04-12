package br.com.tec.tecmotors.presentation.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tec.tecmotors.domain.usecase.AddOdometerUseCase
import br.com.tec.tecmotors.domain.usecase.AddVehicleUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveOdometersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
import br.com.tec.tecmotors.domain.usecase.RenameVehicleUseCase
import br.com.tec.tecmotors.presentation.common.UiFeedback
import br.com.tec.tecmotors.presentation.common.parseDateBrOrIso
import br.com.tec.tecmotors.presentation.common.parseDecimal
import br.com.tec.tecmotors.presentation.common.todayBr
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VehiclesViewModel(
    private val observeVehiclesUseCase: ObserveVehiclesUseCase,
    private val observeOdometersUseCase: ObserveOdometersUseCase,
    private val renameVehicleUseCase: RenameVehicleUseCase,
    private val addOdometerUseCase: AddOdometerUseCase,
    private val addVehicleUseCase: AddVehicleUseCase
) : ViewModel() {
    private val localState = MutableStateFlow(
        VehiclesUiState(
            dateText = todayBr()
        )
    )
    private val _events = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    val uiState: StateFlow<VehiclesUiState> = combine(
        observeVehiclesUseCase(),
        observeOdometersUseCase(),
        localState
    ) { vehicles, odometers, state ->
        val selected = state.selectedVehicleId.takeIf { id -> vehicles.any { it.id == id } }
            ?: vehicles.firstOrNull()?.id
            ?: -1L

        val drafts = if (state.nameDrafts.isEmpty()) {
            vehicles.associate { it.id to it.name }
        } else {
            state.nameDrafts
        }

        state.copy(
            vehicles = vehicles,
            odometerRecords = odometers,
            selectedVehicleId = selected,
            nameDrafts = drafts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = localState.value
    )

    fun onEvent(event: VehiclesUiEvent) {
        when (event) {
            is VehiclesUiEvent.SelectVehicle -> {
                localState.update { it.copy(selectedVehicleId = event.vehicleId) }
            }

            is VehiclesUiEvent.ChangeDate -> {
                localState.update { it.copy(dateText = event.value) }
            }

            is VehiclesUiEvent.ChangeOdometer -> {
                localState.update { it.copy(odometerText = event.value) }
            }

            is VehiclesUiEvent.ChangeVehicleName -> {
                localState.update {
                    it.copy(nameDrafts = it.nameDrafts + (event.vehicleId to event.value))
                }
            }

            is VehiclesUiEvent.SaveVehicleName -> {
                val name = uiState.value.nameDrafts[event.vehicleId]?.trim().orEmpty()
                if (name.isBlank()) {
                    emitFeedback(UiFeedback.Error("Nome invalido"))
                    return
                }

                viewModelScope.launch {
                    renameVehicleUseCase(event.vehicleId, name)
                    emitFeedback(UiFeedback.Success("Nome atualizado"))
                }
            }

            VehiclesUiEvent.SaveOdometer -> {
                val state = uiState.value
                val date = parseDateBrOrIso(state.dateText)
                val odometer = parseDecimal(state.odometerText)
                if (state.selectedVehicleId <= 0L || date == null || odometer == null) {
                    emitFeedback(UiFeedback.Error("Preencha data e odometro corretamente"))
                    return
                }

                viewModelScope.launch {
                    addOdometerUseCase(
                        vehicleId = state.selectedVehicleId,
                        dateEpochDay = date.toEpochDay(),
                        odometerKm = odometer
                    )
                    localState.update {
                        it.copy(
                            odometerText = ""
                        )
                    }
                    emitFeedback(UiFeedback.Success("Odometro registrado"))
                }
            }

            is VehiclesUiEvent.ChangeNewVehicleName -> {
                localState.update { it.copy(newVehicleName = event.value) }
            }

            is VehiclesUiEvent.AddVehicle -> {
                val name = uiState.value.newVehicleName.trim()
                if (name.isBlank()) {
                    emitFeedback(UiFeedback.Error("Informe o nome do veiculo"))
                    return
                }
                viewModelScope.launch {
                    addVehicleUseCase(name, event.type)
                    localState.update {
                        it.copy(
                            newVehicleName = ""
                        )
                    }
                    emitFeedback(UiFeedback.Success("Veiculo adicionado"))
                }
            }
        }
    }

    private fun emitFeedback(feedback: UiFeedback) {
        if (!_events.tryEmit(feedback)) {
            viewModelScope.launch { _events.emit(feedback) }
        }
    }
}
