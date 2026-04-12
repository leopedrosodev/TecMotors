package br.com.tec.tecmotors.domain.model

enum class VehicleType(val label: String) {
    CAR("Carro"),
    MOTORCYCLE("Moto"),
    OTHER("Outro")
}

enum class FuelUsageType(val label: String) {
    CITY("Cidade"),
    HIGHWAY("Estrada"),
    MIXED("Misto")
}

data class Vehicle(
    val id: Long,
    val name: String,
    val type: VehicleType
)

data class OdometerRecord(
    val id: Long,
    val vehicleId: Long,
    val dateEpochDay: Long,
    val odometerKm: Double
)

data class FuelRecord(
    val id: Long,
    val vehicleId: Long,
    val dateEpochDay: Long,
    val odometerKm: Double,
    val liters: Double,
    val pricePerLiter: Double,
    val stationName: String = "",
    val usageType: FuelUsageType = FuelUsageType.MIXED,
    val receiptImageUri: String? = null
) {
    val totalCost: Double = liters * pricePerLiter
}

enum class MaintenanceType(val label: String, val defaultIntervalKm: Double) {
    OIL_CHANGE("Troca de oleo", 5000.0),
    TIRE_ROTATION("Rodizio de pneus", 20000.0),
    BRAKE_SERVICE("Freios", 30000.0),
    GENERAL_REVIEW("Revisao geral", 10000.0),
    OTHER("Outro", 10000.0)
}

data class ComponentHealth(
    val type: MaintenanceType,
    val qualityPercent: Int,
    val kmRemaining: Double?,
    val daysSinceLastService: Int?,
    val hasData: Boolean
)

data class VehicleHealthIndex(
    val attentionPercent: Int,
    val components: List<ComponentHealth>
)

data class MaintenanceRecord(
    val id: Long,
    val vehicleId: Long,
    val type: MaintenanceType,
    val title: String,
    val notes: String,
    val createdAtEpochDay: Long,
    val dueDateEpochDay: Long?,
    val dueOdometerKm: Double?,
    val estimatedCost: Double?,
    val done: Boolean,
    val receiptImageUri: String? = null
)

data class PeriodReport(
    val distanceKm: Double,
    val liters: Double,
    val averageKmPerLiter: Double,
    val totalCost: Double,
    val refuelCount: Int,
    val averageMonthlyCost: Double,
    val maintenanceCost: Double = 0.0
) {
    val overallCost: Double = totalCost + maintenanceCost
}

data class MonthlyMetric(
    val monthYear: String,
    val totalCost: Double,
    val kmPerLiter: Double,
    val distanceKm: Double
)

data class CostPerKmMetric(
    val monthYear: String,
    val costPerKm: Double
)

data class VehicleSummary(
    val distanceKm: Double,
    val liters: Double,
    val totalCost: Double,
    val kmPerLiter: Double,
    val costPerKm: Double
)

data class LocalStateSnapshot(
    val vehicles: List<Vehicle>,
    val odometerRecords: List<OdometerRecord>,
    val fuelRecords: List<FuelRecord>,
    val maintenanceRecords: List<MaintenanceRecord>,
    val updatedAtMillis: Long
)

data class SyncResult(
    val message: String,
    val localUpdated: Boolean
)

data class Settings(
    val darkThemeEnabled: Boolean,
    val legacyImportDone: Boolean,
    val dataUpdatedAtMillis: Long,
    val monthlyBudgetCar: Double,
    val monthlyBudgetMotorcycle: Double,
    val vehicleBudgets: Map<VehicleType, Double> = emptyMap()
)

fun Settings.monthlyBudgetFor(vehicleType: VehicleType?): Double = when (vehicleType) {
    VehicleType.CAR -> vehicleBudgets[VehicleType.CAR] ?: monthlyBudgetCar
    VehicleType.MOTORCYCLE -> vehicleBudgets[VehicleType.MOTORCYCLE] ?: monthlyBudgetMotorcycle
    VehicleType.OTHER -> vehicleBudgets[VehicleType.OTHER] ?: 0.0
    null -> 0.0
}
