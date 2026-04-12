package br.com.tec.tecmotors.domain.repository

import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.FuelUsageType
import br.com.tec.tecmotors.domain.model.LocalStateSnapshot
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Settings
import br.com.tec.tecmotors.domain.model.SyncResult
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun observeVehicles(): Flow<List<Vehicle>>
    suspend fun getVehicles(): List<Vehicle>
    suspend fun ensureDefaultVehiclesIfEmpty()
    suspend fun addVehicle(name: String, type: VehicleType)
    suspend fun renameVehicle(vehicleId: Long, name: String)
}

interface RefuelRepository {
    fun observeRefuels(): Flow<List<FuelRecord>>
    suspend fun getRefuels(): List<FuelRecord>
    suspend fun addRefuel(
        vehicleId: Long,
        dateEpochDay: Long,
        odometerKm: Double,
        liters: Double,
        pricePerLiter: Double,
        stationName: String,
        usageType: FuelUsageType,
        receiptImageUri: String?
    )
}

interface OdometerRepository {
    fun observeOdometerRecords(): Flow<List<OdometerRecord>>
    suspend fun getOdometerRecords(): List<OdometerRecord>
    suspend fun addOdometer(vehicleId: Long, dateEpochDay: Long, odometerKm: Double)
}

interface MaintenanceRepository {
    fun observeMaintenance(): Flow<List<MaintenanceRecord>>
    fun observeMaintenance(vehicleId: Long): Flow<List<MaintenanceRecord>>
    suspend fun getMaintenance(): List<MaintenanceRecord>
    suspend fun addMaintenance(
        vehicleId: Long,
        type: MaintenanceType,
        title: String,
        notes: String,
        createdAtEpochDay: Long,
        dueDateEpochDay: Long?,
        dueOdometerKm: Double?,
        estimatedCost: Double?,
        receiptImageUri: String?
    )

    suspend fun setDone(recordId: Long, done: Boolean)
}

interface SettingsRepository {
    fun observeSettings(): Flow<Settings>
    fun observeDarkTheme(): Flow<Boolean>
    suspend fun getSettings(): Settings
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setMonthlyBudget(vehicleType: VehicleType, amount: Double)
    suspend fun markLegacyImportDone()
    suspend fun touchDataUpdatedAt(timestampMillis: Long = System.currentTimeMillis())
}

interface SnapshotRepository {
    suspend fun getSnapshot(): LocalStateSnapshot
    suspend fun restoreSnapshot(snapshot: LocalStateSnapshot)
}

interface SyncRepository {
    fun currentUserEmail(): String?
    fun isSignedIn(): Boolean
    suspend fun signInWithGoogleIdToken(idToken: String): Result<String>
    fun signOut()
    suspend fun uploadLocalState(): Result<SyncResult>
    suspend fun downloadRemoteState(): Result<SyncResult>
    suspend fun syncNow(): Result<SyncResult>
}
