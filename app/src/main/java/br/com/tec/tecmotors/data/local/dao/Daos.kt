package br.com.tec.tecmotors.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.tec.tecmotors.data.local.entity.FuelRecordEntity
import br.com.tec.tecmotors.data.local.entity.MaintenanceRecordEntity
import br.com.tec.tecmotors.data.local.entity.OdometerRecordEntity
import br.com.tec.tecmotors.data.local.entity.SettingsEntity
import br.com.tec.tecmotors.data.local.entity.VehicleBudgetEntity
import br.com.tec.tecmotors.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    suspend fun getAll(): List<VehicleEntity>

    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<VehicleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VehicleEntity)

    @Query("UPDATE vehicles SET name = :name WHERE id = :vehicleId")
    suspend fun updateName(vehicleId: Long, name: String)

    @Query("SELECT MAX(id) FROM vehicles")
    suspend fun maxId(): Long?
}

@Dao
interface OdometerDao {
    @Query("SELECT * FROM odometer_records ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<OdometerRecordEntity>>

    @Query("SELECT * FROM odometer_records ORDER BY dateEpochDay DESC, id DESC")
    suspend fun getAll(): List<OdometerRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<OdometerRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OdometerRecordEntity)

    @Query("SELECT MAX(id) FROM odometer_records")
    suspend fun maxId(): Long?

    @Query("SELECT MAX(odometerKm) FROM odometer_records WHERE vehicleId = :vehicleId")
    suspend fun latestOdometerKm(vehicleId: Long): Double?
}

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_records ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<FuelRecordEntity>>

    @Query("SELECT * FROM fuel_records ORDER BY dateEpochDay DESC, id DESC")
    suspend fun getAll(): List<FuelRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<FuelRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FuelRecordEntity)

    @Query("SELECT MAX(id) FROM fuel_records")
    suspend fun maxId(): Long?
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_records ORDER BY done ASC, COALESCE(dueDateEpochDay, 9223372036854775807) ASC, id ASC")
    fun observeAll(): Flow<List<MaintenanceRecordEntity>>

    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY done ASC, COALESCE(dueDateEpochDay, 9223372036854775807) ASC, id ASC")
    fun observeByVehicle(vehicleId: Long): Flow<List<MaintenanceRecordEntity>>

    @Query("SELECT * FROM maintenance_records ORDER BY done ASC, COALESCE(dueDateEpochDay, 9223372036854775807) ASC, id ASC")
    suspend fun getAll(): List<MaintenanceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MaintenanceRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MaintenanceRecordEntity)

    @Query("UPDATE maintenance_records SET done = :done WHERE id = :recordId")
    suspend fun updateDone(recordId: Long, done: Boolean)

    @Query("SELECT MAX(id) FROM maintenance_records")
    suspend fun maxId(): Long?
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)

    @Update
    suspend fun update(settings: SettingsEntity)
}

@Dao
interface VehicleBudgetDao {
    @Query("SELECT * FROM vehicle_budgets")
    fun observeAll(): Flow<List<VehicleBudgetEntity>>

    @Query("SELECT * FROM vehicle_budgets")
    suspend fun getAll(): List<VehicleBudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VehicleBudgetEntity)
}
