package br.com.tec.tecmotors.presentation.refuels

import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.FuelUsageType
import br.com.tec.tecmotors.domain.model.Vehicle

sealed interface RefuelsUiEvent {
    data class SelectVehicle(val vehicleId: Long) : RefuelsUiEvent
    data class ChangeDate(val value: String) : RefuelsUiEvent
    data class ChangeOdometer(val value: String) : RefuelsUiEvent
    data class ChangeLiters(val value: String) : RefuelsUiEvent
    data class ChangePrice(val value: String) : RefuelsUiEvent
    data class ChangeStation(val value: String) : RefuelsUiEvent
    data class SelectUsageType(val value: FuelUsageType) : RefuelsUiEvent
    data class SetReceiptImageUri(val value: String?) : RefuelsUiEvent
    data object SaveRefuel : RefuelsUiEvent
}

data class StationInsight(
    val stationName: String,
    val averagePrice: Double,
    val timesUsed: Int
)

data class RefuelsUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val fuelRecords: List<FuelRecord> = emptyList(),
    val selectedVehicleId: Long = -1L,
    val dateText: String = "",
    val odometerText: String = "",
    val litersText: String = "",
    val priceText: String = "",
    val stationText: String = "",
    val selectedUsageType: FuelUsageType = FuelUsageType.MIXED,
    val receiptImageUri: String? = null,
    val stationInsights: List<StationInsight> = emptyList(),
    val suggestedStationName: String? = null
)
