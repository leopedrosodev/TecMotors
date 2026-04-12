package br.com.tec.tecmotors.presentation.refuels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tec.tecmotors.domain.usecase.AddRefuelUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveRefuelsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
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

class RefuelsViewModel(
    private val observeVehiclesUseCase: ObserveVehiclesUseCase,
    private val observeRefuelsUseCase: ObserveRefuelsUseCase,
    private val addRefuelUseCase: AddRefuelUseCase
) : ViewModel() {
    private val localState = MutableStateFlow(RefuelsUiState(dateText = todayBr()))
    private val _events = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    val uiState: StateFlow<RefuelsUiState> = combine(
        observeVehiclesUseCase(),
        observeRefuelsUseCase(),
        localState
    ) { vehicles, refuels, state ->
        val selected = state.selectedVehicleId.takeIf { id -> vehicles.any { it.id == id } }
            ?: vehicles.firstOrNull()?.id
            ?: -1L

        val selectedVehicleRefuels = refuels.filter { it.vehicleId == selected }
        val stationInsights = selectedVehicleRefuels
            .asSequence()
            .map { it.stationName.trim() to it.pricePerLiter }
            .filter { it.first.isNotBlank() }
            .groupBy({ it.first }, { it.second })
            .map { (station, prices) ->
                StationInsight(
                    stationName = station,
                    averagePrice = prices.average(),
                    timesUsed = prices.size
                )
            }
            .sortedBy { it.averagePrice }
            .toList()

        val suggested = stationInsights.firstOrNull()?.stationName

        state.copy(
            vehicles = vehicles,
            fuelRecords = refuels,
            selectedVehicleId = selected,
            stationInsights = stationInsights,
            suggestedStationName = suggested
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = localState.value
    )

    fun onEvent(event: RefuelsUiEvent) {
        when (event) {
            is RefuelsUiEvent.SelectVehicle -> localState.update { it.copy(selectedVehicleId = event.vehicleId) }
            is RefuelsUiEvent.ChangeDate -> localState.update { it.copy(dateText = event.value) }
            is RefuelsUiEvent.ChangeOdometer -> localState.update { it.copy(odometerText = event.value) }
            is RefuelsUiEvent.ChangeLiters -> localState.update { it.copy(litersText = event.value) }
            is RefuelsUiEvent.ChangePrice -> localState.update { it.copy(priceText = event.value) }
            is RefuelsUiEvent.ChangeStation -> localState.update { it.copy(stationText = event.value) }
            is RefuelsUiEvent.SelectUsageType -> localState.update { it.copy(selectedUsageType = event.value) }
            is RefuelsUiEvent.SetReceiptImageUri -> localState.update { it.copy(receiptImageUri = event.value) }

            RefuelsUiEvent.SaveRefuel -> {
                val state = uiState.value
                val date = parseDateBrOrIso(state.dateText)
                val odometer = parseDecimal(state.odometerText)
                val liters = parseDecimal(state.litersText)
                val price = parseDecimal(state.priceText)

                if (state.selectedVehicleId <= 0L || date == null || odometer == null || liters == null || price == null) {
                    emitFeedback(UiFeedback.Error("Preencha todos os campos com valores validos"))
                    return
                }

                viewModelScope.launch {
                    addRefuelUseCase(
                        vehicleId = state.selectedVehicleId,
                        dateEpochDay = date.toEpochDay(),
                        odometerKm = odometer,
                        liters = liters,
                        pricePerLiter = price,
                        stationName = state.stationText,
                        usageType = state.selectedUsageType,
                        receiptImageUri = state.receiptImageUri
                    )
                    localState.update {
                        it.copy(
                            odometerText = "",
                            litersText = "",
                            priceText = "",
                            receiptImageUri = null
                        )
                    }
                    emitFeedback(UiFeedback.Success("Abastecimento salvo"))
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
