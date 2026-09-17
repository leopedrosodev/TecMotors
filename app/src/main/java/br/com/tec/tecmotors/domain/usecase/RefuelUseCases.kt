package br.com.tec.tecmotors.domain.usecase

import br.com.tec.tecmotors.domain.model.FuelUsageType
import br.com.tec.tecmotors.domain.repository.RefuelRepository

class ObserveRefuelsUseCase(private val repository: RefuelRepository) {
    operator fun invoke() = repository.observeRefuels()
}

class AddRefuelUseCase(private val repository: RefuelRepository) {
    suspend operator fun invoke(
        vehicleId: Long,
        dateEpochDay: Long,
        odometerKm: Double,
        liters: Double,
        pricePerLiter: Double,
        stationName: String,
        usageType: FuelUsageType,
        receiptImageUri: String?
    ) {
        repository.addRefuel(
            vehicleId = vehicleId,
            dateEpochDay = dateEpochDay,
            odometerKm = odometerKm,
            liters = liters,
            pricePerLiter = pricePerLiter,
            stationName = stationName,
            usageType = usageType,
            receiptImageUri = receiptImageUri
        )
    }
}

class UpdateRefuelUseCase(private val repository: RefuelRepository) {
    suspend operator fun invoke(
        recordId: Long,
        vehicleId: Long,
        dateEpochDay: Long,
        odometerKm: Double,
        liters: Double,
        pricePerLiter: Double,
        stationName: String,
        usageType: FuelUsageType,
        receiptImageUri: String?
    ) = repository.updateRefuel(
        recordId = recordId,
        vehicleId = vehicleId,
        dateEpochDay = dateEpochDay,
        odometerKm = odometerKm,
        liters = liters,
        pricePerLiter = pricePerLiter,
        stationName = stationName,
        usageType = usageType,
        receiptImageUri = receiptImageUri
    )
}

class DeleteRefuelUseCase(private val repository: RefuelRepository) {
    suspend operator fun invoke(recordId: Long) = repository.deleteRefuel(recordId)
}
