package br.com.tec.tecmotors.shared.domain.model

enum class SharedMaintenanceDueStatus {
    DONE,
    OVERDUE,
    DUE_SOON,
    ON_TRACK
}

data class SharedFuelRecord(
    val vehicleId: Long,
    val dateEpochDay: Long,
    val odometerKm: Double,
    val liters: Double,
    val pricePerLiter: Double,
    val maintenanceCostPortion: Double = 0.0
) {
    val totalCost: Double = liters * pricePerLiter
}

data class SharedOdometerRecord(
    val vehicleId: Long,
    val dateEpochDay: Long,
    val odometerKm: Double
)

data class SharedMaintenanceRecord(
    val vehicleId: Long,
    val createdAtEpochDay: Long,
    val dueDateEpochDay: Long?,
    val dueOdometerKm: Double?,
    val estimatedCost: Double?,
    val done: Boolean
)

data class SharedPeriodReport(
    val distanceKm: Double,
    val liters: Double,
    val averageKmPerLiter: Double,
    val fuelCost: Double,
    val maintenanceCost: Double,
    val totalCost: Double,
    val refuelCount: Int
)

data class SharedMonthlyMetric(
    val monthKey: String,
    val totalCost: Double,
    val distanceKm: Double,
    val kmPerLiter: Double
)

data class SharedCostPerKmMetric(
    val monthKey: String,
    val costPerKm: Double
)

data class SharedVehicleSummary(
    val distanceKm: Double,
    val liters: Double,
    val fuelCost: Double,
    val kmPerLiter: Double,
    val costPerKm: Double
)
