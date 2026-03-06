package br.com.tec.tecmotors.domain.usecase

import br.com.tec.tecmotors.domain.model.CostPerKmMetric
import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MonthlyMetric
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.PeriodReport
import br.com.tec.tecmotors.domain.model.VehicleSummary
import br.com.tec.tecmotors.domain.repository.OdometerRepository
import br.com.tec.tecmotors.domain.repository.RefuelRepository
import br.com.tec.tecmotors.shared.domain.model.SharedFuelRecord
import br.com.tec.tecmotors.shared.domain.model.SharedMaintenanceRecord
import br.com.tec.tecmotors.shared.domain.model.SharedMonthlyMetric
import br.com.tec.tecmotors.shared.domain.model.SharedOdometerRecord
import br.com.tec.tecmotors.shared.domain.usecase.CalculateSharedCostPerKmMetricsUseCase
import br.com.tec.tecmotors.shared.domain.usecase.CalculateSharedPeriodReportUseCase
import br.com.tec.tecmotors.shared.domain.usecase.CalculateSharedVehicleSummaryUseCase
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

class CalculatePeriodReportUseCase(
    private val sharedUseCase: CalculateSharedPeriodReportUseCase = CalculateSharedPeriodReportUseCase()
) {
    operator fun invoke(
        vehicleId: Long,
        start: LocalDate,
        end: LocalDate,
        fuelRecords: List<FuelRecord>,
        odometerRecords: List<OdometerRecord>,
        maintenanceRecords: List<MaintenanceRecord> = emptyList()
    ): PeriodReport {
        val sharedReport = sharedUseCase(
            vehicleId = vehicleId,
            startEpochDay = start.toEpochDay(),
            endEpochDay = end.toEpochDay(),
            fuelRecords = fuelRecords.map { it.toShared() },
            odometerRecords = odometerRecords.map { it.toShared() },
            maintenanceRecords = maintenanceRecords.map { it.toShared() }
        )

        return PeriodReport(
            distanceKm = sharedReport.distanceKm,
            liters = sharedReport.liters,
            averageKmPerLiter = sharedReport.averageKmPerLiter,
            totalCost = sharedReport.fuelCost,
            refuelCount = sharedReport.refuelCount,
            averageMonthlyCost = calculateAverageMonthlyCost(vehicleId, fuelRecords),
            maintenanceCost = sharedReport.maintenanceCost
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
            val report = calculatePeriodReportUseCase(
                vehicleId = vehicleId,
                start = month.atDay(1),
                end = month.atEndOfMonth(),
                fuelRecords = fuelRecords,
                odometerRecords = odometerRecords,
                maintenanceRecords = maintenanceRecords
            )
            MonthlyMetric(
                monthYear = month.format(monthFormatter),
                totalCost = report.overallCost,
                kmPerLiter = report.averageKmPerLiter,
                distanceKm = report.distanceKm
            )
        }
    }
}

class CalculateCostPerKmMetricsUseCase(
    private val sharedUseCase: CalculateSharedCostPerKmMetricsUseCase = CalculateSharedCostPerKmMetricsUseCase()
) {
    operator fun invoke(monthlyMetrics: List<MonthlyMetric>): List<CostPerKmMetric> {
        val shared = sharedUseCase(
            monthlyMetrics.map {
                SharedMonthlyMetric(
                    monthKey = it.monthYear,
                    totalCost = it.totalCost,
                    distanceKm = it.distanceKm,
                    kmPerLiter = it.kmPerLiter
                )
            }
        )
        return shared.map { CostPerKmMetric(monthYear = it.monthKey, costPerKm = it.costPerKm) }
    }
}

class CalculateVehicleSummaryUseCase(
    private val sharedUseCase: CalculateSharedVehicleSummaryUseCase = CalculateSharedVehicleSummaryUseCase()
) {
    operator fun invoke(
        vehicleId: Long,
        fuelRecords: List<FuelRecord>,
        odometerRecords: List<OdometerRecord>
    ): VehicleSummary {
        val shared = sharedUseCase(
            vehicleId = vehicleId,
            fuelRecords = fuelRecords.map { it.toShared() },
            odometerRecords = odometerRecords.map { it.toShared() }
        )

        return VehicleSummary(
            distanceKm = shared.distanceKm,
            liters = shared.liters,
            totalCost = shared.fuelCost,
            kmPerLiter = shared.kmPerLiter,
            costPerKm = shared.costPerKm
        )
    }
}

private fun FuelRecord.toShared(): SharedFuelRecord = SharedFuelRecord(
    vehicleId = vehicleId,
    dateEpochDay = dateEpochDay,
    odometerKm = odometerKm,
    liters = liters,
    pricePerLiter = pricePerLiter
)

private fun OdometerRecord.toShared(): SharedOdometerRecord = SharedOdometerRecord(
    vehicleId = vehicleId,
    dateEpochDay = dateEpochDay,
    odometerKm = odometerKm
)

private fun MaintenanceRecord.toShared(): SharedMaintenanceRecord = SharedMaintenanceRecord(
    vehicleId = vehicleId,
    createdAtEpochDay = createdAtEpochDay,
    dueDateEpochDay = dueDateEpochDay,
    dueOdometerKm = dueOdometerKm,
    estimatedCost = estimatedCost,
    done = done
)
