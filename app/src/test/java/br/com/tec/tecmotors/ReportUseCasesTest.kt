package br.com.tec.tecmotors

import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.usecase.CalculateCostPerKmMetricsUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateMonthlyMetricsUseCase
import br.com.tec.tecmotors.domain.usecase.CalculatePeriodReportUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateVehicleSummaryUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReportUseCasesTest {
    private val periodUseCase = CalculatePeriodReportUseCase()

    @Test
    fun calculatePeriodReport_returnsExpectedValues() {
        val vehicleId = 1L
        val fuels = listOf(
            FuelRecord(1, vehicleId, LocalDate.of(2026, 2, 1).toEpochDay(), 1000.0, 20.0, 5.0),
            FuelRecord(2, vehicleId, LocalDate.of(2026, 2, 15).toEpochDay(), 1300.0, 15.0, 5.2)
        )
        val odometers = listOf(
            OdometerRecord(3, vehicleId, LocalDate.of(2026, 2, 28).toEpochDay(), 1500.0)
        )

        val report = periodUseCase(
            vehicleId = vehicleId,
            start = LocalDate.of(2026, 2, 1),
            end = LocalDate.of(2026, 2, 28),
            fuelRecords = fuels,
            odometerRecords = odometers
        )

        assertEquals(500.0, report.distanceKm, 0.001)
        assertEquals(35.0, report.liters, 0.001)
        assertEquals(14.2857, report.averageKmPerLiter, 0.01)
        assertEquals(178.0, report.totalCost, 0.001)
        assertEquals(2, report.refuelCount)
    }

    @Test
    fun monthlyMetrics_and_summary_haveConsistentValues() {
        val vehicleId = 1L
        val now = LocalDate.now()
        val currentMonth = now.withDayOfMonth(minOf(10, now.lengthOfMonth()))
        val previousMonth = currentMonth.minusMonths(1)
        val fuels = listOf(
            FuelRecord(1, vehicleId, previousMonth.toEpochDay(), 900.0, 20.0, 6.0),
            FuelRecord(2, vehicleId, currentMonth.toEpochDay(), 1200.0, 25.0, 6.2)
        )
        val odometers = listOf(
            OdometerRecord(3, vehicleId, now.toEpochDay(), 1400.0)
        )
        val maintenance = listOf(
            MaintenanceRecord(
                id = 10,
                vehicleId = vehicleId,
                type = MaintenanceType.OIL_CHANGE,
                title = "Troca",
                notes = "",
                createdAtEpochDay = currentMonth.toEpochDay(),
                dueDateEpochDay = null,
                dueOdometerKm = null,
                estimatedCost = 300.0,
                done = false
            )
        )

        val monthlyMetrics = CalculateMonthlyMetricsUseCase(periodUseCase)(
            vehicleId = vehicleId,
            fuelRecords = fuels,
            odometerRecords = odometers,
            maintenanceRecords = maintenance,
            monthsBackInclusive = 1
        )
        val summary = CalculateVehicleSummaryUseCase()(vehicleId, fuels, odometers)
        val costPerKm = CalculateCostPerKmMetricsUseCase()(monthlyMetrics)

        assertEquals(2, monthlyMetrics.size)
        assertTrue(monthlyMetrics.any { it.totalCost >= 455.0 })
        assertTrue(summary.totalCost > 0.0)
        assertTrue(summary.distanceKm >= 0.0)
        assertEquals(2, costPerKm.size)
    }
}
