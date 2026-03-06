package br.com.tec.tecmotors.data.remote

import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.FuelUsageType
import br.com.tec.tecmotors.domain.model.LocalStateSnapshot
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType

object RemoteSnapshotMapper {
    private const val FIELD_UPDATED_AT = "updatedAtMillis"
    private const val FIELD_VEHICLES = "vehicles"
    private const val FIELD_ODOMETER = "odometerRecords"
    private const val FIELD_FUEL = "fuelRecords"
    private const val FIELD_MAINTENANCE = "maintenanceRecords"
    private const val FIELD_SCHEMA = "schemaVersion"
    private const val SCHEMA_VERSION = 3L

    fun toRemoteMap(snapshot: LocalStateSnapshot): Map<String, Any> {
        return mapOf(
            FIELD_SCHEMA to SCHEMA_VERSION,
            FIELD_UPDATED_AT to snapshot.updatedAtMillis,
            FIELD_VEHICLES to snapshot.vehicles.map {
                mapOf(
                    "id" to it.id,
                    "name" to it.name,
                    "type" to it.type.name
                )
            },
            FIELD_ODOMETER to snapshot.odometerRecords.map {
                mapOf(
                    "id" to it.id,
                    "vehicleId" to it.vehicleId,
                    "dateEpochDay" to it.dateEpochDay,
                    "odometerKm" to it.odometerKm
                )
            },
            FIELD_FUEL to snapshot.fuelRecords.map {
                mapOf(
                    "id" to it.id,
                    "vehicleId" to it.vehicleId,
                    "dateEpochDay" to it.dateEpochDay,
                    "odometerKm" to it.odometerKm,
                    "liters" to it.liters,
                    "pricePerLiter" to it.pricePerLiter,
                    "stationName" to it.stationName,
                    "usageType" to it.usageType.name,
                    "receiptImageUri" to it.receiptImageUri
                )
            },
            FIELD_MAINTENANCE to snapshot.maintenanceRecords.map {
                mapOf(
                    "id" to it.id,
                    "vehicleId" to it.vehicleId,
                    "type" to it.type.name,
                    "title" to it.title,
                    "notes" to it.notes,
                    "createdAtEpochDay" to it.createdAtEpochDay,
                    "dueDateEpochDay" to it.dueDateEpochDay,
                    "dueOdometerKm" to it.dueOdometerKm,
                    "estimatedCost" to it.estimatedCost,
                    "done" to it.done,
                    "receiptImageUri" to it.receiptImageUri
                )
            }
        )
    }

    fun fromRemoteMap(data: Map<String, Any?>): LocalStateSnapshot {
        val updatedAtMillis = asLong(data[FIELD_UPDATED_AT]) ?: 0L
        val vehicles = asMapList(data[FIELD_VEHICLES]).mapNotNull(::mapToVehicle)
        val odometerRecords = asMapList(data[FIELD_ODOMETER]).mapNotNull(::mapToOdometer)
        val fuelRecords = asMapList(data[FIELD_FUEL]).mapNotNull(::mapToFuel)
        val maintenanceRecords = asMapList(data[FIELD_MAINTENANCE]).mapNotNull(::mapToMaintenance)

        return LocalStateSnapshot(
            vehicles = vehicles,
            odometerRecords = odometerRecords,
            fuelRecords = fuelRecords,
            maintenanceRecords = maintenanceRecords,
            updatedAtMillis = updatedAtMillis
        )
    }

    private fun asMapList(value: Any?): List<Map<String, Any?>> {
        val raw = value as? List<*> ?: return emptyList()
        return raw.mapNotNull { it as? Map<String, Any?> }
    }

    private fun mapToVehicle(map: Map<String, Any?>): Vehicle? {
        val id = asLong(map["id"]) ?: return null
        val name = map["name"] as? String ?: return null
        val type = runCatching {
            VehicleType.valueOf(map["type"] as? String ?: VehicleType.CAR.name)
        }.getOrDefault(VehicleType.CAR)
        return Vehicle(id = id, name = name, type = type)
    }

    private fun mapToOdometer(map: Map<String, Any?>): OdometerRecord? {
        return OdometerRecord(
            id = asLong(map["id"]) ?: return null,
            vehicleId = asLong(map["vehicleId"]) ?: return null,
            dateEpochDay = asLong(map["dateEpochDay"]) ?: return null,
            odometerKm = asDouble(map["odometerKm"]) ?: return null
        )
    }

    private fun mapToFuel(map: Map<String, Any?>): FuelRecord? {
        return FuelRecord(
            id = asLong(map["id"]) ?: return null,
            vehicleId = asLong(map["vehicleId"]) ?: return null,
            dateEpochDay = asLong(map["dateEpochDay"]) ?: return null,
            odometerKm = asDouble(map["odometerKm"]) ?: return null,
            liters = asDouble(map["liters"]) ?: return null,
            pricePerLiter = asDouble(map["pricePerLiter"]) ?: return null,
            stationName = (map["stationName"] as? String).orEmpty(),
            usageType = runCatching {
                FuelUsageType.valueOf((map["usageType"] as? String) ?: FuelUsageType.MIXED.name)
            }.getOrDefault(FuelUsageType.MIXED),
            receiptImageUri = map["receiptImageUri"] as? String
        )
    }

    private fun mapToMaintenance(map: Map<String, Any?>): MaintenanceRecord? {
        val id = asLong(map["id"]) ?: return null
        val vehicleId = asLong(map["vehicleId"]) ?: return null
        val typeRaw = map["type"] as? String ?: MaintenanceType.OTHER.name
        val type = runCatching { MaintenanceType.valueOf(typeRaw) }.getOrDefault(MaintenanceType.OTHER)

        return MaintenanceRecord(
            id = id,
            vehicleId = vehicleId,
            type = type,
            title = map["title"] as? String ?: type.label,
            notes = map["notes"] as? String ?: "",
            createdAtEpochDay = asLong(map["createdAtEpochDay"]) ?: 0L,
            dueDateEpochDay = asLong(map["dueDateEpochDay"]),
            dueOdometerKm = asDouble(map["dueOdometerKm"]),
            estimatedCost = asDouble(map["estimatedCost"]),
            done = map["done"] as? Boolean ?: false,
            receiptImageUri = map["receiptImageUri"] as? String
        )
    }

    private fun asLong(value: Any?): Long? {
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is Float -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    private fun asDouble(value: Any?): Double? {
        return when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Long -> value.toDouble()
            is Int -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }
}
