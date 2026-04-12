package br.com.tec.tecmotors.domain.usecase

import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.domain.repository.OdometerRepository
import br.com.tec.tecmotors.domain.repository.VehicleRepository

class ObserveVehiclesUseCase(private val repository: VehicleRepository) {
    operator fun invoke() = repository.observeVehicles()
}

class EnsureDefaultVehiclesUseCase(private val repository: VehicleRepository) {
    suspend operator fun invoke() = repository.ensureDefaultVehiclesIfEmpty()
}

class AddVehicleUseCase(private val repository: VehicleRepository) {
    suspend operator fun invoke(name: String, type: VehicleType) = repository.addVehicle(name, type)
}

class RenameVehicleUseCase(private val repository: VehicleRepository) {
    suspend operator fun invoke(vehicleId: Long, name: String) = repository.renameVehicle(vehicleId, name)
}

class ObserveOdometersUseCase(private val repository: OdometerRepository) {
    operator fun invoke() = repository.observeOdometerRecords()
}

class AddOdometerUseCase(private val repository: OdometerRepository) {
    suspend operator fun invoke(vehicleId: Long, dateEpochDay: Long, odometerKm: Double) {
        repository.addOdometer(vehicleId, dateEpochDay, odometerKm)
    }
}
