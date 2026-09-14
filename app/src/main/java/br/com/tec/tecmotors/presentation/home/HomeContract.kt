package br.com.tec.tecmotors.presentation.home

import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.usecase.ReminderAlert

sealed interface HomeUiEvent {
    data class SelectVehicle(val vehicleId: Long) : HomeUiEvent
}

enum class HomeEntryKind { REFUEL, MAINTENANCE }

data class HomeEntry(
    val id: Long,
    val kind: HomeEntryKind,
    val title: String,
    val subtitle: String,
    val dateEpochDay: Long,
    val amount: Double
)

data class HomeUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: Long = -1L,
    val monthLabel: String = "",
    val monthCost: Double = 0.0,
    val monthBudget: Double = 0.0,
    val budgetRemaining: Double = 0.0,
    val budgetProgress: Float = 0f,
    val budgetExceeded: Boolean = false,
    val hasBudget: Boolean = false,
    val monthDistanceKm: Double = 0.0,
    val monthKmPerLiter: Double = 0.0,
    val monthCostPerKm: Double = 0.0,
    val monthRefuelCount: Int = 0,
    val lastOdometerKm: Double? = null,
    val lastOdometerDaysAgo: Int? = null,
    val maintenanceAlert: ReminderAlert.MaintenanceDueSoon? = null,
    val recentEntries: List<HomeEntry> = emptyList()
) {
    val selectedVehicle: Vehicle?
        get() = vehicles.firstOrNull { it.id == selectedVehicleId }

    val hasAnyData: Boolean
        get() = recentEntries.isNotEmpty() || lastOdometerKm != null
}
