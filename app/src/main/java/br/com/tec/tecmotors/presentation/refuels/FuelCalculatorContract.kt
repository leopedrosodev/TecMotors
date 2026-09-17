package br.com.tec.tecmotors.presentation.refuels

import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.domain.model.Vehicle

/** O que a calculadora resolve; os outros dois campos sao a entrada. */
enum class CalculatorTarget(val titleRes: Int) {
    LITERS(R.string.calc_target_liters),
    DISTANCE(R.string.calc_target_distance),
    CONSUMPTION(R.string.calc_target_consumption)
}

sealed interface CalculatorUiEvent {
    data class SelectTarget(val target: CalculatorTarget) : CalculatorUiEvent
    data class SelectVehicle(val vehicleId: Long) : CalculatorUiEvent
    data class ChangeDistance(val value: String) : CalculatorUiEvent
    data class ChangeConsumption(val value: String) : CalculatorUiEvent
    data class ChangeLiters(val value: String) : CalculatorUiEvent
    data class ChangePrice(val value: String) : CalculatorUiEvent

    /** Preenche consumo e preco com o historico real do veiculo. */
    data object UseVehicleData : CalculatorUiEvent
    data object Clear : CalculatorUiEvent
}

data class CalculatorUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: Long = -1L,
    val target: CalculatorTarget = CalculatorTarget.LITERS,
    val distanceText: String = "",
    val consumptionText: String = "",
    val litersText: String = "",
    val priceText: String = "",
    // --- o que o app ja sabe sobre este veiculo ---
    val vehicleKmPerLiter: Double? = null,
    val vehiclePricePerLiter: Double? = null,
    // --- resultados ---
    val distance: Double? = null,
    val consumption: Double? = null,
    val liters: Double? = null,
    val totalCost: Double? = null
) {
    val hasVehicleData: Boolean
        get() = vehicleKmPerLiter != null || vehiclePricePerLiter != null

    fun isTarget(field: CalculatorTarget): Boolean = target == field
}
