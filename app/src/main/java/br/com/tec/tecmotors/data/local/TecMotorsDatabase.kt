package br.com.tec.tecmotors.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.tec.tecmotors.data.local.dao.FuelDao
import br.com.tec.tecmotors.data.local.dao.MaintenanceDao
import br.com.tec.tecmotors.data.local.dao.OdometerDao
import br.com.tec.tecmotors.data.local.dao.SettingsDao
import br.com.tec.tecmotors.data.local.dao.VehicleBudgetDao
import br.com.tec.tecmotors.data.local.dao.VehicleDao
import br.com.tec.tecmotors.data.local.entity.FuelRecordEntity
import br.com.tec.tecmotors.data.local.entity.MaintenanceRecordEntity
import br.com.tec.tecmotors.data.local.entity.OdometerRecordEntity
import br.com.tec.tecmotors.data.local.entity.SettingsEntity
import br.com.tec.tecmotors.data.local.entity.VehicleBudgetEntity
import br.com.tec.tecmotors.data.local.entity.VehicleEntity

@Database(
    entities = [
        VehicleEntity::class,
        OdometerRecordEntity::class,
        FuelRecordEntity::class,
        MaintenanceRecordEntity::class,
        SettingsEntity::class,
        VehicleBudgetEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class TecMotorsDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun odometerDao(): OdometerDao
    abstract fun fuelDao(): FuelDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun settingsDao(): SettingsDao
    abstract fun vehicleBudgetDao(): VehicleBudgetDao
}
