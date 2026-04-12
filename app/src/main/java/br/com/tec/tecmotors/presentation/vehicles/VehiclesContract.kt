package br.com.tec.tecmotors.presentation.vehicles

import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType

sealed interface VehiclesUiEvent {
    data class SelectVehicle(val vehicleId: Long) : VehiclesUiEvent
    data class ChangeDate(val value: String) : VehiclesUiEvent
    data class ChangeOdometer(val value: String) : VehiclesUiEvent
    data class ChangeVehicleName(val vehicleId: Long, val value: String) : VehiclesUiEvent
    data class SaveVehicleName(val vehicleId: Long) : VehiclesUiEvent
    data object SaveOdometer : VehiclesUiEvent
    data class ChangeNewVehicleName(val value: String) : VehiclesUiEvent
    data class AddVehicle(val type: VehicleType) : VehiclesUiEvent
}

data class VehiclesUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val odometerRecords: List<OdometerRecord> = emptyList(),
    val selectedVehicleId: Long = -1L,
    val dateText: String = "",
    val odometerText: String = "",
    val nameDrafts: Map<Long, String> = emptyMap(),
    val newVehicleName: String = ""
)
