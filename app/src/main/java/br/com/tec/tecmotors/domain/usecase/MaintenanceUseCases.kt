package br.com.tec.tecmotors.domain.usecase

import br.com.tec.tecmotors.domain.model.ComponentHealth
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.VehicleHealthIndex
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

class CalculateComponentHealthUseCase {
    operator fun invoke(
        vehicleId: Long,
        maintenanceRecords: List<MaintenanceRecord>,
        currentOdometerKm: Double?,
        today: LocalDate
    ): VehicleHealthIndex {
        val vehicleRecords = maintenanceRecords.filter { it.vehicleId == vehicleId }
        val todayEpoch = today.toEpochDay()

        val relevantTypes = MaintenanceType.entries.filter { it != MaintenanceType.OTHER }

        val components = relevantTypes.map { type ->
            val typeRecords = vehicleRecords.filter { it.type == type }
            val lastDone = typeRecords.filter { it.done }.maxByOrNull { it.createdAtEpochDay }
            val nextPending = typeRecords.filter { !it.done && it.dueOdometerKm != null }
                .minByOrNull { it.dueOdometerKm!! }

            val daysSinceLastService = lastDone?.let {
                (todayEpoch - it.createdAtEpochDay).toInt().coerceAtLeast(0)
            }

            val kmRemaining = if (currentOdometerKm != null && nextPending?.dueOdometerKm != null) {
                (nextPending.dueOdometerKm - currentOdometerKm).coerceAtLeast(0.0)
            } else null

            val qualityPercent = when {
                kmRemaining != null ->
                    ((kmRemaining / type.defaultIntervalKm) * 100).toInt().coerceIn(0, 100)
                lastDone != null -> 100
                else -> 0
            }

            ComponentHealth(
                type = type,
                qualityPercent = qualityPercent,
                kmRemaining = kmRemaining,
                daysSinceLastService = daysSinceLastService,
                hasData = lastDone != null || nextPending != null
            )
        }

        val withData = components.filter { it.hasData }
        val attentionPercent = if (withData.isEmpty()) 0
        else (100 - withData.map { it.qualityPercent }.average()).toInt().coerceIn(0, 100)

        return VehicleHealthIndex(attentionPercent = attentionPercent, components = components)
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

private fun SharedMaintenanceDueStatus.toDomain(): MaintenanceDueStatus = when (this) {
    SharedMaintenanceDueStatus.DONE -> MaintenanceDueStatus.DONE
    SharedMaintenanceDueStatus.OVERDUE -> MaintenanceDueStatus.OVERDUE
    SharedMaintenanceDueStatus.DUE_SOON -> MaintenanceDueStatus.DUE_SOON
    SharedMaintenanceDueStatus.ON_TRACK -> MaintenanceDueStatus.ON_TRACK
}
