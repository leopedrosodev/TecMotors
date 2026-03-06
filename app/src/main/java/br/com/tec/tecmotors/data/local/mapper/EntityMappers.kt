package br.com.tec.tecmotors.data.local.mapper

import br.com.tec.tecmotors.data.local.entity.FuelRecordEntity
import br.com.tec.tecmotors.data.local.entity.MaintenanceRecordEntity
import br.com.tec.tecmotors.data.local.entity.OdometerRecordEntity
import br.com.tec.tecmotors.data.local.entity.SettingsEntity
import br.com.tec.tecmotors.data.local.entity.VehicleEntity
import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.FuelUsageType
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Settings
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType

fun VehicleEntity.toDomain(): Vehicle = Vehicle(
    id = id,
    name = name,
    type = runCatching { VehicleType.valueOf(type) }.getOrDefault(VehicleType.CAR)
)

fun Vehicle.toEntity(): VehicleEntity = VehicleEntity(
    id = id,
    name = name,
    type = type.name
)

fun OdometerRecordEntity.toDomain(): OdometerRecord = OdometerRecord(
    id = id,
    vehicleId = vehicleId,
    dateEpochDay = dateEpochDay,
    odometerKm = odometerKm
)

fun OdometerRecord.toEntity(): OdometerRecordEntity = OdometerRecordEntity(
    id = id,
    vehicleId = vehicleId,
    dateEpochDay = dateEpochDay,
    odometerKm = odometerKm
)

fun FuelRecordEntity.toDomain(): FuelRecord = FuelRecord(
    id = id,
    vehicleId = vehicleId,
    dateEpochDay = dateEpochDay,
    odometerKm = odometerKm,
    liters = liters,
    pricePerLiter = pricePerLiter,
    stationName = stationName,
    usageType = runCatching { FuelUsageType.valueOf(usageType) }.getOrDefault(FuelUsageType.MIXED),
    receiptImageUri = receiptImageUri
)

fun FuelRecord.toEntity(): FuelRecordEntity = FuelRecordEntity(
    id = id,
    vehicleId = vehicleId,
    dateEpochDay = dateEpochDay,
    odometerKm = odometerKm,
    liters = liters,
    pricePerLiter = pricePerLiter,
    stationName = stationName,
    usageType = usageType.name,
    receiptImageUri = receiptImageUri
)

fun MaintenanceRecordEntity.toDomain(): MaintenanceRecord = MaintenanceRecord(
    id = id,
    vehicleId = vehicleId,
    type = runCatching { MaintenanceType.valueOf(type) }.getOrDefault(MaintenanceType.OTHER),
    title = title,
    notes = notes,
    createdAtEpochDay = createdAtEpochDay,
    dueDateEpochDay = dueDateEpochDay,
    dueOdometerKm = dueOdometerKm,
    estimatedCost = estimatedCost,
    done = done,
    receiptImageUri = receiptImageUri
)

fun MaintenanceRecord.toEntity(): MaintenanceRecordEntity = MaintenanceRecordEntity(
    id = id,
    vehicleId = vehicleId,
    type = type.name,
    title = title,
    notes = notes,
    createdAtEpochDay = createdAtEpochDay,
    dueDateEpochDay = dueDateEpochDay,
    dueOdometerKm = dueOdometerKm,
    estimatedCost = estimatedCost,
    done = done,
    receiptImageUri = receiptImageUri
)

fun SettingsEntity.toDomain(): Settings = Settings(
    darkThemeEnabled = darkThemeEnabled,
    legacyImportDone = legacyImportDone,
    dataUpdatedAtMillis = dataUpdatedAtMillis,
    monthlyBudgetCar = monthlyBudgetCar,
    monthlyBudgetMotorcycle = monthlyBudgetMotorcycle
)

fun Settings.toEntity(): SettingsEntity = SettingsEntity(
    id = 1,
    darkThemeEnabled = darkThemeEnabled,
    legacyImportDone = legacyImportDone,
    dataUpdatedAtMillis = dataUpdatedAtMillis,
    monthlyBudgetCar = monthlyBudgetCar,
    monthlyBudgetMotorcycle = monthlyBudgetMotorcycle
)
