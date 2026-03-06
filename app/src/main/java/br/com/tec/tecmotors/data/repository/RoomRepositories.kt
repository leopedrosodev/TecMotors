package br.com.tec.tecmotors.data.repository

import androidx.room.withTransaction
import br.com.tec.tecmotors.data.local.RoomSnapshotDataSource
import br.com.tec.tecmotors.data.local.TecMotorsDatabase
import br.com.tec.tecmotors.data.local.entity.FuelRecordEntity
import br.com.tec.tecmotors.data.local.entity.MaintenanceRecordEntity
import br.com.tec.tecmotors.data.local.entity.OdometerRecordEntity
import br.com.tec.tecmotors.data.local.entity.SettingsEntity
import br.com.tec.tecmotors.data.local.entity.VehicleEntity
import br.com.tec.tecmotors.data.local.mapper.toDomain
import br.com.tec.tecmotors.domain.model.FuelUsageType
import br.com.tec.tecmotors.domain.model.LocalStateSnapshot
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.Settings
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.domain.repository.MaintenanceRepository
import br.com.tec.tecmotors.domain.repository.OdometerRepository
import br.com.tec.tecmotors.domain.repository.RefuelRepository
import br.com.tec.tecmotors.domain.repository.SettingsRepository
import br.com.tec.tecmotors.domain.repository.SnapshotRepository
import br.com.tec.tecmotors.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VehicleRepositoryImpl(
    private val database: TecMotorsDatabase,
    private val settingsRepository: SettingsRepository
) : VehicleRepository {
    override fun observeVehicles(): Flow<List<Vehicle>> {
        return database.vehicleDao().observeAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getVehicles(): List<Vehicle> {
        return database.vehicleDao().getAll().map { it.toDomain() }
    }

    override suspend fun ensureDefaultVehiclesIfEmpty() {
        if (database.vehicleDao().count() > 0) return
        val defaults = listOf(
            VehicleEntity(id = 1L, name = "Meu Carro", type = VehicleType.CAR.name),
            VehicleEntity(id = 2L, name = "Minha Moto", type = VehicleType.MOTORCYCLE.name)
        )
        database.vehicleDao().upsertAll(defaults)
        settingsRepository.touchDataUpdatedAt()
    }

    override suspend fun renameVehicle(vehicleId: Long, name: String) {
        database.vehicleDao().updateName(vehicleId, name.trim())
        settingsRepository.touchDataUpdatedAt()
    }
}

class OdometerRepositoryImpl(
    private val database: TecMotorsDatabase,
    private val settingsRepository: SettingsRepository
) : OdometerRepository {
    override fun observeOdometerRecords() =
        database.odometerDao().observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getOdometerRecords() =
        database.odometerDao().getAll().map { it.toDomain() }

    override suspend fun addOdometer(vehicleId: Long, dateEpochDay: Long, odometerKm: Double) {
        val nextId = (database.odometerDao().maxId() ?: 0L) + 1L
        database.odometerDao().upsert(
            OdometerRecordEntity(
                id = nextId,
                vehicleId = vehicleId,
                dateEpochDay = dateEpochDay,
                odometerKm = odometerKm
            )
        )
        settingsRepository.touchDataUpdatedAt()
    }
}

class RefuelRepositoryImpl(
    private val database: TecMotorsDatabase,
    private val settingsRepository: SettingsRepository
) : RefuelRepository {
    override fun observeRefuels() =
        database.fuelDao().observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getRefuels() =
        database.fuelDao().getAll().map { it.toDomain() }

    override suspend fun addRefuel(
        vehicleId: Long,
        dateEpochDay: Long,
        odometerKm: Double,
        liters: Double,
        pricePerLiter: Double,
        stationName: String,
        usageType: FuelUsageType,
        receiptImageUri: String?
    ) {
        val nextId = (database.fuelDao().maxId() ?: 0L) + 1L
        database.fuelDao().upsert(
            FuelRecordEntity(
                id = nextId,
                vehicleId = vehicleId,
                dateEpochDay = dateEpochDay,
                odometerKm = odometerKm,
                liters = liters,
                pricePerLiter = pricePerLiter,
                stationName = stationName.trim(),
                usageType = usageType.name,
                receiptImageUri = receiptImageUri
            )
        )
        settingsRepository.touchDataUpdatedAt()
    }
}

class MaintenanceRepositoryImpl(
    private val database: TecMotorsDatabase,
    private val settingsRepository: SettingsRepository
) : MaintenanceRepository {
    override fun observeMaintenance(): Flow<List<MaintenanceRecord>> {
        return database.maintenanceDao().observeAll().map { list -> list.map { it.toDomain() } }
    }

    override fun observeMaintenance(vehicleId: Long): Flow<List<MaintenanceRecord>> {
        return database.maintenanceDao().observeByVehicle(vehicleId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getMaintenance(): List<MaintenanceRecord> {
        return database.maintenanceDao().getAll().map { it.toDomain() }
    }

    override suspend fun addMaintenance(
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
        val nextId = (database.maintenanceDao().maxId() ?: 0L) + 1L
        database.maintenanceDao().upsert(
            MaintenanceRecordEntity(
                id = nextId,
                vehicleId = vehicleId,
                type = type.name,
                title = title.trim(),
                notes = notes.trim(),
                createdAtEpochDay = createdAtEpochDay,
                dueDateEpochDay = dueDateEpochDay,
                dueOdometerKm = dueOdometerKm,
                estimatedCost = estimatedCost,
                done = false,
                receiptImageUri = receiptImageUri
            )
        )
        settingsRepository.touchDataUpdatedAt()
    }

    override suspend fun setDone(recordId: Long, done: Boolean) {
        database.maintenanceDao().updateDone(recordId, done)
        settingsRepository.touchDataUpdatedAt()
    }
}

class SettingsRepositoryImpl(
    private val database: TecMotorsDatabase
) : SettingsRepository {
    override fun observeSettings(): Flow<Settings> {
        return database.settingsDao().observe().map { entity ->
            (entity ?: defaultSettings()).toDomain()
        }
    }

    override fun observeDarkTheme(): Flow<Boolean> {
        return observeSettings().map { it.darkThemeEnabled }
    }

    override suspend fun getSettings(): Settings {
        return (database.settingsDao().get() ?: defaultSettings()).toDomain()
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        val current = database.settingsDao().get() ?: defaultSettings()
        database.settingsDao().upsert(current.copy(darkThemeEnabled = enabled))
    }

    override suspend fun setMonthlyBudget(vehicleType: VehicleType, amount: Double) {
        val normalized = amount.coerceAtLeast(0.0)
        val current = database.settingsDao().get() ?: defaultSettings()
        val updated = when (vehicleType) {
            VehicleType.CAR -> current.copy(monthlyBudgetCar = normalized)
            VehicleType.MOTORCYCLE -> current.copy(monthlyBudgetMotorcycle = normalized)
        }
        database.settingsDao().upsert(updated)
    }

    override suspend fun markLegacyImportDone() {
        val current = database.settingsDao().get() ?: defaultSettings()
        database.settingsDao().upsert(current.copy(legacyImportDone = true))
    }

    override suspend fun touchDataUpdatedAt(timestampMillis: Long) {
        val current = database.settingsDao().get() ?: defaultSettings()
        database.settingsDao().upsert(current.copy(dataUpdatedAtMillis = timestampMillis))
    }

    private fun defaultSettings(): SettingsEntity {
        return SettingsEntity(
            id = 1,
            darkThemeEnabled = true,
            legacyImportDone = false,
            dataUpdatedAtMillis = System.currentTimeMillis(),
            monthlyBudgetCar = 0.0,
            monthlyBudgetMotorcycle = 0.0
        )
    }
}

class SnapshotRepositoryImpl(
    private val database: TecMotorsDatabase,
    private val dataSource: RoomSnapshotDataSource
) : SnapshotRepository {
    override suspend fun getSnapshot(): LocalStateSnapshot {
        return dataSource.getSnapshot()
    }

    override suspend fun restoreSnapshot(snapshot: LocalStateSnapshot) {
        database.withTransaction {
            dataSource.restoreSnapshot(snapshot)
        }
    }
}

private fun SettingsEntity.toDomain(): Settings = Settings(
    darkThemeEnabled = darkThemeEnabled,
    legacyImportDone = legacyImportDone,
    dataUpdatedAtMillis = dataUpdatedAtMillis,
    monthlyBudgetCar = monthlyBudgetCar,
    monthlyBudgetMotorcycle = monthlyBudgetMotorcycle
)
