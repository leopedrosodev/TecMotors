package br.com.tec.tecmotors.presentation.refuels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tec.tecmotors.domain.usecase.CalculateVehicleSummaryUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveOdometersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveRefuelsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
import br.com.tec.tecmotors.presentation.common.parseDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.Locale

/**
 * Calculadora de combustivel.
 *
 * O ganho sobre a versao anterior nao e visual: ela agora conhece o veiculo.
 * Consumo e preco por litro deixam de ser chute do usuario e passam a poder vir
 * do historico real - que o app ja tinha e nao usava aqui.
 */
class FuelCalculatorViewModel(
    observeVehiclesUseCase: ObserveVehiclesUseCase,
    observeRefuelsUseCase: ObserveRefuelsUseCase,
    observeOdometersUseCase: ObserveOdometersUseCase,
    private val calculateVehicleSummaryUseCase: CalculateVehicleSummaryUseCase
) : ViewModel() {
    private val localState = MutableStateFlow(CalculatorUiState())

    val uiState: StateFlow<CalculatorUiState> = combine(
        observeVehiclesUseCase(),
        observeRefuelsUseCase(),
        observeOdometersUseCase(),
        localState
    ) { vehicles, refuels, odometers, state ->
        val selected = state.selectedVehicleId.takeIf { id -> vehicles.any { it.id == id } }
            ?: vehicles.firstOrNull()?.id
            ?: -1L

        val summary = if (selected > 0L) {
            calculateVehicleSummaryUseCase(
                vehicleId = selected,
                fuelRecords = refuels,
                odometerRecords = odometers
            )
        } else {
            null
        }

        val lastPrice = refuels
            .filter { it.vehicleId == selected }
            .maxByOrNull { it.dateEpochDay }
            ?.pricePerLiter

        solve(
            state.copy(
                vehicles = vehicles,
                selectedVehicleId = selected,
                vehicleKmPerLiter = summary?.kmPerLiter?.takeIf { it > 0.0 },
                vehiclePricePerLiter = lastPrice?.takeIf { it > 0.0 }
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalculatorUiState()
    )

    fun onEvent(event: CalculatorUiEvent) {
        when (event) {
            is CalculatorUiEvent.SelectTarget -> localState.update { it.copy(target = event.target) }
            is CalculatorUiEvent.SelectVehicle -> localState.update { it.copy(selectedVehicleId = event.vehicleId) }
            is CalculatorUiEvent.ChangeDistance -> localState.update { it.copy(distanceText = event.value) }
            is CalculatorUiEvent.ChangeConsumption -> localState.update { it.copy(consumptionText = event.value) }
            is CalculatorUiEvent.ChangeLiters -> localState.update { it.copy(litersText = event.value) }
            is CalculatorUiEvent.ChangePrice -> localState.update { it.copy(priceText = event.value) }

            CalculatorUiEvent.UseVehicleData -> {
                val current = uiState.value
                localState.update {
                    it.copy(
                        consumptionText = current.vehicleKmPerLiter?.let(::decimalInput) ?: it.consumptionText,
                        priceText = current.vehiclePricePerLiter?.let(::decimalInput) ?: it.priceText
                    )
                }
            }

            CalculatorUiEvent.Clear -> localState.update {
                it.copy(
                    distanceText = "",
                    consumptionText = "",
                    litersText = "",
                    priceText = ""
                )
            }
        }
    }

    /** Resolve o campo escolhido a partir dos outros dois. */
    private fun solve(state: CalculatorUiState): CalculatorUiState {
        val distanceInput = parseDecimal(state.distanceText)
        val consumptionInput = parseDecimal(state.consumptionText)
        val litersInput = parseDecimal(state.litersText)
        val price = parseDecimal(state.priceText)

        val distance = when (state.target) {
            CalculatorTarget.DISTANCE ->
                if (consumptionInput != null && litersInput != null) consumptionInput * litersInput else null
            else -> distanceInput
        }

        val consumption = when (state.target) {
            CalculatorTarget.CONSUMPTION ->
                if (distanceInput != null && litersInput != null && litersInput > 0.0) {
                    distanceInput / litersInput
                } else {
                    null
                }
            else -> consumptionInput
        }

        val liters = when (state.target) {
            CalculatorTarget.LITERS ->
                if (distanceInput != null && consumptionInput != null && consumptionInput > 0.0) {
                    distanceInput / consumptionInput
                } else {
                    null
                }
            else -> litersInput
        }

        return state.copy(
            distance = distance,
            consumption = consumption,
            liters = liters,
            totalCost = if (liters != null && price != null) liters * price else null
        )
    }

    /** Sem separador de milhar: o texto volta por parseDecimal. */
    private fun decimalInput(value: Double): String =
        String.format(Locale.forLanguageTag("pt-BR"), "%.2f", value)
}
