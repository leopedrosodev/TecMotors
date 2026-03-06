package br.com.tec.tecmotors.data.local.migration

import android.content.Context
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

interface LegacySnapshotSource {
    fun readSnapshot(): LocalStateSnapshot
    fun readDarkThemeEnabled(defaultValue: Boolean = true): Boolean
}

class LegacyPreferencesReader(context: Context) : LegacySnapshotSource {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun readSnapshot(): LocalStateSnapshot {
        return LocalStateSnapshot(
            vehicles = readVehicles(),
            odometerRecords = readOdometerRecords(),
            fuelRecords = readFuelRecords(),
            maintenanceRecords = readMaintenanceRecords(),
            updatedAtMillis = prefs.getLong(KEY_DATA_UPDATED_AT, 0L)
        )
    }

    override fun readDarkThemeEnabled(defaultValue: Boolean): Boolean {
        return prefs.getBoolean(KEY_DARK_THEME, defaultValue)
    }

    private fun readVehicles(): List<Vehicle> {
        return readArray(KEY_VEHICLES).mapNotNull { json ->
            val id = json.optLong("id", 0L)
            if (id <= 0L) return@mapNotNull null
            Vehicle(
                id = id,
                name = json.optString("name", "").ifBlank { "Veiculo" },
                type = runCatching {
                    VehicleType.valueOf(json.optString("type", VehicleType.CAR.name))
                }.getOrDefault(VehicleType.CAR)
            )
        }
    }

    private fun readOdometerRecords(): List<OdometerRecord> {
        return readArray(KEY_ODOMETER_RECORDS).mapNotNull { json ->
            val id = json.optLong("id", 0L)
            val vehicleId = json.optLong("vehicleId", 0L)
            if (id <= 0L || vehicleId <= 0L) return@mapNotNull null
            OdometerRecord(
                id = id,
                vehicleId = vehicleId,
                dateEpochDay = json.optLong("dateEpochDay", 0L),
                odometerKm = json.optDouble("odometerKm", 0.0)
            )
        }
    }

    private fun readFuelRecords(): List<FuelRecord> {
        return readArray(KEY_FUEL_RECORDS).mapNotNull { json ->
            val id = json.optLong("id", 0L)
            val vehicleId = json.optLong("vehicleId", 0L)
            if (id <= 0L || vehicleId <= 0L) return@mapNotNull null
            FuelRecord(
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
                receiptImageUri = json.optStringOrNull("receiptImageUri")
            )
        }
    }

    private fun readMaintenanceRecords(): List<MaintenanceRecord> {
        return readArray(KEY_MAINTENANCE_RECORDS).mapNotNull { json ->
            val id = json.optLong("id", 0L)
            val vehicleId = json.optLong("vehicleId", 0L)
            if (id <= 0L || vehicleId <= 0L) return@mapNotNull null
            MaintenanceRecord(
                id = id,
                vehicleId = vehicleId,
                type = runCatching {
                    MaintenanceType.valueOf(json.optString("type", MaintenanceType.OTHER.name))
                }.getOrDefault(MaintenanceType.OTHER),
                title = json.optString("title", "").ifBlank { "Manutencao" },
                notes = json.optString("notes", ""),
                createdAtEpochDay = json.optLong("createdAtEpochDay", 0L),
                dueDateEpochDay = json.optLongOrNull("dueDateEpochDay"),
                dueOdometerKm = json.optDoubleOrNull("dueOdometerKm"),
                estimatedCost = json.optDoubleOrNull("estimatedCost"),
                done = json.optBoolean("done", false),
                receiptImageUri = json.optStringOrNull("receiptImageUri")
            )
        }
    }

    private fun readArray(key: String): List<JSONObject> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return List(array.length()) { index -> array.optJSONObject(index) }
            .filterNotNull()
    }

    companion object {
        private const val PREFS_NAME = "tec_motors_local_store"
        private const val KEY_VEHICLES = "vehicles"
        private const val KEY_ODOMETER_RECORDS = "odometer_records"
        private const val KEY_FUEL_RECORDS = "fuel_records"
        private const val KEY_MAINTENANCE_RECORDS = "maintenance_records"
        private const val KEY_DARK_THEME = "dark_theme_enabled"
        private const val KEY_DATA_UPDATED_AT = "data_updated_at"
    }
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return optLong(key)
}

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key)
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key)
}
