package br.com.tec.tecmotors.shared.domain.usecase

import br.com.tec.tecmotors.shared.domain.model.SharedCostPerKmMetric
import br.com.tec.tecmotors.shared.domain.model.SharedFuelRecord
import br.com.tec.tecmotors.shared.domain.model.SharedMaintenanceDueStatus
import br.com.tec.tecmotors.shared.domain.model.SharedMaintenanceRecord
import br.com.tec.tecmotors.shared.domain.model.SharedMonthlyMetric
import br.com.tec.tecmotors.shared.domain.model.SharedOdometerRecord
import br.com.tec.tecmotors.shared.domain.model.SharedPeriodReport
import br.com.tec.tecmotors.shared.domain.model.SharedVehicleSummary

class CalculateSharedPeriodReportUseCase {
    operator fun invoke(
        vehicleId: Long,
        startEpochDay: Long,
        endEpochDay: Long,
        fuelRecords: List<SharedFuelRecord>,
        odometerRecords: List<SharedOdometerRecord>,
        maintenanceRecords: List<SharedMaintenanceRecord>
    ): SharedPeriodReport {
        val periodFuels = fuelRecords.filter {
            it.vehicleId == vehicleId && it.dateEpochDay in startEpochDay..endEpochDay
        }
        val periodOdometers = odometerRecords.filter {
            it.vehicleId == vehicleId && it.dateEpochDay in startEpochDay..endEpochDay
        }
        val periodMaintenanceCost = maintenanceRecords
            .asSequence()
            .filter { it.vehicleId == vehicleId }
            .filter { it.createdAtEpochDay in startEpochDay..endEpochDay }
            .sumOf { it.estimatedCost ?: 0.0 }

        val points = buildList {
            addAll(periodFuels.map { it.odometerKm })
            addAll(periodOdometers.map { it.odometerKm })
        }

        val distance = if (points.size >= 2) {
            (points.maxOrNull() ?: 0.0) - (points.minOrNull() ?: 0.0)
        } else {
            0.0
        }

        val liters = periodFuels.sumOf { it.liters }
        val fuelCost = periodFuels.sumOf { it.totalCost }
        val totalCost = fuelCost + periodMaintenanceCost
        val kmPerLiter = if (liters > 0.0) distance / liters else 0.0

        return SharedPeriodReport(
            distanceKm = distance,
            liters = liters,
            averageKmPerLiter = kmPerLiter,
            fuelCost = fuelCost,
            maintenanceCost = periodMaintenanceCost,
            totalCost = totalCost,
            refuelCount = periodFuels.size
        )
    }
}

class CalculateSharedVehicleSummaryUseCase {
    operator fun invoke(
        vehicleId: Long,
        fuelRecords: List<SharedFuelRecord>,
        odometerRecords: List<SharedOdometerRecord>
    ): SharedVehicleSummary {
        val filteredFuel = fuelRecords.filter { it.vehicleId == vehicleId }
        val points = buildList {
            addAll(filteredFuel.map { it.odometerKm })
            addAll(odometerRecords.filter { it.vehicleId == vehicleId }.map { it.odometerKm })
        }

        val distance = if (points.size >= 2) {
            (points.maxOrNull() ?: 0.0) - (points.minOrNull() ?: 0.0)
        } else {
            0.0
        }

        val liters = filteredFuel.sumOf { it.liters }
        val fuelCost = filteredFuel.sumOf { it.totalCost }
        val kmPerLiter = if (liters > 0.0) distance / liters else 0.0
        val costPerKm = if (distance > 0.0) fuelCost / distance else 0.0

        return SharedVehicleSummary(
            distanceKm = distance,
            liters = liters,
            fuelCost = fuelCost,
            kmPerLiter = kmPerLiter,
            costPerKm = costPerKm
        )
    }
}

class CalculateSharedCostPerKmMetricsUseCase {
    operator fun invoke(monthlyMetrics: List<SharedMonthlyMetric>): List<SharedCostPerKmMetric> {
        return monthlyMetrics.map {
            val costPerKm = if (it.distanceKm > 0.0) it.totalCost / it.distanceKm else 0.0
            SharedCostPerKmMetric(monthKey = it.monthKey, costPerKm = costPerKm)
        }
    }
}

class CalculateSharedMaintenanceStatusUseCase {
    operator fun invoke(
        record: SharedMaintenanceRecord,
        todayEpochDay: Long,
        currentOdometerKm: Double?
    ): SharedMaintenanceDueStatus {
        if (record.done) return SharedMaintenanceDueStatus.DONE

        val dateOverdue = record.dueDateEpochDay?.let { it < todayEpochDay } == true
        val kmOverdue = record.dueOdometerKm?.let { due -> currentOdometerKm?.let { it >= due } } == true
        if (dateOverdue || kmOverdue) return SharedMaintenanceDueStatus.OVERDUE

        val dueSoonByDate = record.dueDateEpochDay?.let { due ->
            val daysRemaining = due - todayEpochDay
            daysRemaining in 0..15
        } == true

        val dueSoonByKm = record.dueOdometerKm?.let { due ->
            val current = currentOdometerKm ?: return@let false
            val remaining = due - current
            remaining in 0.0..500.0
        } == true

        return if (dueSoonByDate || dueSoonByKm) {
            SharedMaintenanceDueStatus.DUE_SOON
        } else {
            SharedMaintenanceDueStatus.ON_TRACK
        }
    }
}
