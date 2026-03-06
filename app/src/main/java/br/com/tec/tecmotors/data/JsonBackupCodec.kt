package br.com.tec.tecmotors.data

import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.FuelUsageType
import br.com.tec.tecmotors.domain.model.LocalStateSnapshot
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType
import org.json.JSONArray
import org.json.JSONObject

object JsonBackupCodec {
    private const val KEY_SCHEMA_VERSION = "schemaVersion"
    private const val KEY_UPDATED_AT = "updatedAtMillis"
    private const val KEY_VEHICLES = "vehicles"
    private const val KEY_ODOMETERS = "odometerRecords"
    private const val KEY_REFUELS = "fuelRecords"
    private const val KEY_MAINTENANCE = "maintenanceRecords"

    fun encode(snapshot: LocalStateSnapshot): String {
        val root = JSONObject()
        root.put(KEY_SCHEMA_VERSION, 3)
        root.put(KEY_UPDATED_AT, snapshot.updatedAtMillis)
        root.put(
            KEY_VEHICLES,
            JSONArray().apply {
                snapshot.vehicles.forEach { vehicle ->
                    put(
                        JSONObject()
                            .put("id", vehicle.id)
                            .put("name", vehicle.name)
                            .put("type", vehicle.type.name)
                    )
                }
            }
        )
        root.put(
            KEY_ODOMETERS,
            JSONArray().apply {
                snapshot.odometerRecords.forEach { record ->
                    put(
                        JSONObject()
                            .put("id", record.id)
                            .put("vehicleId", record.vehicleId)
                            .put("dateEpochDay", record.dateEpochDay)
                            .put("odometerKm", record.odometerKm)
                    )
                }
            }
        )
        root.put(
            KEY_REFUELS,
            JSONArray().apply {
                snapshot.fuelRecords.forEach { record ->
                    put(
                        JSONObject()
                            .put("id", record.id)
                            .put("vehicleId", record.vehicleId)
                            .put("dateEpochDay", record.dateEpochDay)
                            .put("odometerKm", record.odometerKm)
                            .put("liters", record.liters)
                            .put("pricePerLiter", record.pricePerLiter)
                            .put("stationName", record.stationName)
                            .put("usageType", record.usageType.name)
                            .put("receiptImageUri", record.receiptImageUri)
                    )
                }
            }
        )
        root.put(
            KEY_MAINTENANCE,
            JSONArray().apply {
                snapshot.maintenanceRecords.forEach { record ->
                    put(
                        JSONObject()
                            .put("id", record.id)
                            .put("vehicleId", record.vehicleId)
                            .put("type", record.type.name)
                            .put("title", record.title)
                            .put("notes", record.notes)
                            .put("createdAtEpochDay", record.createdAtEpochDay)
                            .put("dueDateEpochDay", record.dueDateEpochDay)
                            .put("dueOdometerKm", record.dueOdometerKm)
                            .put("estimatedCost", record.estimatedCost)
                            .put("done", record.done)
                            .put("receiptImageUri", record.receiptImageUri)
                    )
                }
            }
        )
        return root.toString()
    }

    fun decode(raw: String): LocalStateSnapshot {
        val root = JSONObject(raw)

        return LocalStateSnapshot(
            vehicles = root.optJSONArray(KEY_VEHICLES).toObjectList(::parseVehicle),
            odometerRecords = root.optJSONArray(KEY_ODOMETERS).toObjectList(::parseOdometer),
            fuelRecords = root.optJSONArray(KEY_REFUELS).toObjectList(::parseRefuel),
            maintenanceRecords = root.optJSONArray(KEY_MAINTENANCE).toObjectList(::parseMaintenance),
            updatedAtMillis = root.optLong(KEY_UPDATED_AT, System.currentTimeMillis())
        )
    }

    private fun parseVehicle(json: JSONObject): Vehicle? {
        val id = json.optLong("id", -1L)
        if (id <= 0L) return null
        return Vehicle(
            id = id,
            name = json.optString("name", "Veiculo"),
            type = runCatching { VehicleType.valueOf(json.optString("type", VehicleType.CAR.name)) }
                .getOrDefault(VehicleType.CAR)
        )
    }

    private fun parseOdometer(json: JSONObject): OdometerRecord? {
        val id = json.optLong("id", -1L)
        val vehicleId = json.optLong("vehicleId", -1L)
        if (id <= 0L || vehicleId <= 0L) return null
        return OdometerRecord(
            id = id,
            vehicleId = vehicleId,
            dateEpochDay = json.optLong("dateEpochDay", 0L),
            odometerKm = json.optDouble("odometerKm", 0.0)
        )
    }

    private fun parseRefuel(json: JSONObject): FuelRecord? {
        val id = json.optLong("id", -1L)
        val vehicleId = json.optLong("vehicleId", -1L)
        if (id <= 0L || vehicleId <= 0L) return null
        return FuelRecord(
            id = id,
            vehicleId = vehicleId,
            dateEpochDay = json.optLong("dateEpochDay", 0L),
            odometerKm = json.optDouble("odometerKm", 0.0),
            liters = json.optDouble("liters", 0.0),
            pricePerLiter = json.optDouble("pricePerLiter", 0.0),
            stationName = json.optString("stationName", ""),
            usageType = runCatching {
                FuelUsageType.valueOf(json.optString("usageType", FuelUsageType.MIXED.name))
            }.getOrDefault(FuelUsageType.MIXED),
            receiptImageUri = json.optNullableString("receiptImageUri")
        )
    }

    private fun parseMaintenance(json: JSONObject): MaintenanceRecord? {
        val id = json.optLong("id", -1L)
        val vehicleId = json.optLong("vehicleId", -1L)
        if (id <= 0L || vehicleId <= 0L) return null
        return MaintenanceRecord(
            id = id,
            vehicleId = vehicleId,
            type = runCatching {
                MaintenanceType.valueOf(json.optString("type", MaintenanceType.OTHER.name))
            }.getOrDefault(MaintenanceType.OTHER),
            title = json.optString("title", "Manutencao"),
            notes = json.optString("notes", ""),
            createdAtEpochDay = json.optLong("createdAtEpochDay", 0L),
            dueDateEpochDay = json.optNullableLong("dueDateEpochDay"),
            dueOdometerKm = json.optNullableDouble("dueOdometerKm"),
            estimatedCost = json.optNullableDouble("estimatedCost"),
            done = json.optBoolean("done", false),
            receiptImageUri = json.optNullableString("receiptImageUri")
        )
    }

    private fun <T> JSONArray?.toObjectList(mapper: (JSONObject) -> T?): List<T> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                mapper(item)?.let(::add)
            }
        }
    }
}

private fun JSONObject.optNullableLong(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return optLong(key)
}

private fun JSONObject.optNullableDouble(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key)
}

private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key)
}
