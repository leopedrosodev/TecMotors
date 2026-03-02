package br.com.tec.tecmotors.domain.usecase

import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MonthlyMetric
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.PeriodReport
import br.com.tec.tecmotors.domain.model.VehicleSummary
import br.com.tec.tecmotors.domain.repository.OdometerRepository
import br.com.tec.tecmotors.domain.repository.RefuelRepository
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class ObserveReportsDataUseCase(
    private val refuelRepository: RefuelRepository,
    private val odometerRepository: OdometerRepository
) {
    operator fun invoke() = combine(
        refuelRepository.observeRefuels(),
        odometerRepository.observeOdometerRecords()
    ) { fuels, odometers ->
        ReportsData(fuels, odometers)
    }
}

data class ReportsData(
    val fuelRecords: List<FuelRecord>,
    val odometerRecords: List<OdometerRecord>
)

class CalculatePeriodReportUseCase {
    operator fun invoke(
        vehicleId: Long,
        start: LocalDate,
        end: LocalDate,
        fuelRecords: List<FuelRecord>,
        odometerRecords: List<OdometerRecord>
    ): PeriodReport {
        val startEpoch = start.toEpochDay()
        val endEpoch = end.toEpochDay()

        val periodFuels = fuelRecords.filter {
            it.vehicleId == vehicleId && it.dateEpochDay in startEpoch..endEpoch
        }
        val periodOdometers = odometerRecords.filter {
            it.vehicleId == vehicleId && it.dateEpochDay in startEpoch..endEpoch
        }

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
        val totalCost = periodFuels.sumOf { it.totalCost }
        val averageKmPerLiter = if (liters > 0.0) distance / liters else 0.0
        val averageMonthlyCost = calculateAverageMonthlyCost(vehicleId, fuelRecords)

        return PeriodReport(
            distanceKm = distance,
            liters = liters,
            averageKmPerLiter = averageKmPerLiter,
            totalCost = totalCost,
            refuelCount = periodFuels.size,
            averageMonthlyCost = averageMonthlyCost
        )
    }

    private fun calculateAverageMonthlyCost(vehicleId: Long, fuelRecords: List<FuelRecord>): Double {
        val monthTotals = fuelRecords
            .asSequence()
            .filter { it.vehicleId == vehicleId }
            .groupBy {
                val date = LocalDate.ofEpochDay(it.dateEpochDay)
                YearMonth.of(date.year, date.month)
            }
            .mapValues { (_, records) -> records.sumOf { it.totalCost } }
            .values

        return if (monthTotals.isNotEmpty()) monthTotals.average() else 0.0
    }
}

class CalculateMonthlyMetricsUseCase(
    private val calculatePeriodReportUseCase: CalculatePeriodReportUseCase
) {
    private val monthFormatter = DateTimeFormatter.ofPattern("MMM/yy", Locale.forLanguageTag("pt-BR"))

    operator fun invoke(
        vehicleId: Long,
        fuelRecords: List<FuelRecord>,
        odometerRecords: List<OdometerRecord>,
        maintenanceRecords: List<MaintenanceRecord> = emptyList(),
        monthsBackInclusive: Int = 5
    ): List<MonthlyMetric> {
        val nowMonth = YearMonth.now()
        return (monthsBackInclusive downTo 0).map { offset ->
            val month = nowMonth.minusMonths(offset.toLong())
            val monthStartEpoch = month.atDay(1).toEpochDay()
            val monthEndEpoch = month.atEndOfMonth().toEpochDay()
            val report = calculatePeriodReportUseCase(
                vehicleId = vehicleId,
                start = month.atDay(1),
                end = month.atEndOfMonth(),
                fuelRecords = fuelRecords,
                odometerRecords = odometerRecords
            )
            val maintenanceCost = maintenanceRecords
                .asSequence()
                .filter { it.vehicleId == vehicleId }
                .filter { it.createdAtEpochDay in monthStartEpoch..monthEndEpoch }
                .sumOf { it.estimatedCost ?: 0.0 }
            MonthlyMetric(
                monthYear = month.format(monthFormatter),
                totalCost = report.totalCost + maintenanceCost,
                kmPerLiter = report.averageKmPerLiter,
                distanceKm = report.distanceKm
            )
        }
    }
}

class CalculateVehicleSummaryUseCase {
    operator fun invoke(
        vehicleId: Long,
        fuelRecords: List<FuelRecord>,
        odometerRecords: List<OdometerRecord>
    ): VehicleSummary {
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
        val totalCost = filteredFuel.sumOf { it.totalCost }
        val kmPerLiter = if (liters > 0.0) distance / liters else 0.0
        val costPerKm = if (distance > 0.0) totalCost / distance else 0.0

        return VehicleSummary(
            distanceKm = distance,
            liters = liters,
            totalCost = totalCost,
            kmPerLiter = kmPerLiter,
            costPerKm = costPerKm
        )
    }
}
