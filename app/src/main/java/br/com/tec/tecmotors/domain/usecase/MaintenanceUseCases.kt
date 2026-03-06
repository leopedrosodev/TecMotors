package br.com.tec.tecmotors.domain.usecase

import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.repository.MaintenanceRepository
import br.com.tec.tecmotors.shared.domain.model.SharedMaintenanceDueStatus
import br.com.tec.tecmotors.shared.domain.model.SharedMaintenanceRecord
import br.com.tec.tecmotors.shared.domain.usecase.CalculateSharedMaintenanceStatusUseCase
import java.time.LocalDate

enum class MaintenanceDueStatus {
    DONE,
    OVERDUE,
    DUE_SOON,
    ON_TRACK
}

class ObserveMaintenanceUseCase(private val repository: MaintenanceRepository) {
    operator fun invoke() = repository.observeMaintenance()
}

class ObserveMaintenanceByVehicleUseCase(private val repository: MaintenanceRepository) {
    operator fun invoke(vehicleId: Long) = repository.observeMaintenance(vehicleId)
}

class AddMaintenanceUseCase(private val repository: MaintenanceRepository) {
    suspend operator fun invoke(
        vehicleId: Long,
        type: MaintenanceType,
        title: String,
        notes: String,
        createdAtEpochDay: Long,
        dueDateEpochDay: Long?,
        dueOdometerKm: Double?,
        estimatedCost: Double?,
        receiptImageUri: String?
    ) {
        repository.addMaintenance(
            vehicleId = vehicleId,
            type = type,
            title = title,
            notes = notes,
            createdAtEpochDay = createdAtEpochDay,
            dueDateEpochDay = dueDateEpochDay,
            dueOdometerKm = dueOdometerKm,
            estimatedCost = estimatedCost,
            receiptImageUri = receiptImageUri
        )
    }
}

class SetMaintenanceDoneUseCase(private val repository: MaintenanceRepository) {
    suspend operator fun invoke(recordId: Long, done: Boolean) {
        repository.setDone(recordId, done)
    }
}

class CalculateMaintenanceStatusUseCase(
    private val sharedUseCase: CalculateSharedMaintenanceStatusUseCase = CalculateSharedMaintenanceStatusUseCase()
) {
    operator fun invoke(
        record: MaintenanceRecord,
        currentOdometerKm: Double?,
        today: LocalDate
    ): MaintenanceDueStatus {
        val sharedStatus = sharedUseCase(
            record = record.toShared(),
            todayEpochDay = today.toEpochDay(),
            currentOdometerKm = currentOdometerKm
        )
        return sharedStatus.toDomain()
    }
}

private fun MaintenanceRecord.toShared(): SharedMaintenanceRecord = SharedMaintenanceRecord(
    vehicleId = vehicleId,
    createdAtEpochDay = createdAtEpochDay,
    dueDateEpochDay = dueDateEpochDay,
    dueOdometerKm = dueOdometerKm,
    estimatedCost = estimatedCost,
    done = done
)

private val SharedMaintenanceDueStatus.toDomain: MaintenanceDueStatus
    get() = when (this) {
        SharedMaintenanceDueStatus.DONE -> MaintenanceDueStatus.DONE
        SharedMaintenanceDueStatus.OVERDUE -> MaintenanceDueStatus.OVERDUE
        SharedMaintenanceDueStatus.DUE_SOON -> MaintenanceDueStatus.DUE_SOON
        SharedMaintenanceDueStatus.ON_TRACK -> MaintenanceDueStatus.ON_TRACK
    }
