package br.com.tec.tecmotors.shared.domain.usecase

import br.com.tec.tecmotors.shared.domain.model.SharedFuelRecord
import br.com.tec.tecmotors.shared.domain.model.SharedMaintenanceDueStatus
import br.com.tec.tecmotors.shared.domain.model.SharedMaintenanceRecord
import br.com.tec.tecmotors.shared.domain.model.SharedMonthlyMetric
import br.com.tec.tecmotors.shared.domain.model.SharedOdometerRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedReportUseCasesTest {
    @Test
    fun calculatePeriodReport_includesMaintenanceCost() {
        val useCase = CalculateSharedPeriodReportUseCase()

        val report = useCase(
            vehicleId = 1L,
            startEpochDay = 100L,
            endEpochDay = 130L,
            fuelRecords = listOf(
                SharedFuelRecord(1L, 100L, 1000.0, 20.0, 5.0),
                SharedFuelRecord(1L, 120L, 1300.0, 15.0, 5.2)
            ),
            odometerRecords = listOf(
                SharedOdometerRecord(1L, 130L, 1500.0)
            ),
            maintenanceRecords = listOf(
                SharedMaintenanceRecord(
                    vehicleId = 1L,
                    createdAtEpochDay = 125L,
                    dueDateEpochDay = null,
                    dueOdometerKm = null,
                    estimatedCost = 200.0,
                    done = false
                )
            )
        )

        assertEquals(500.0, report.distanceKm, 0.001)
        assertEquals(178.0, report.fuelCost, 0.001)
        assertEquals(200.0, report.maintenanceCost, 0.001)
        assertEquals(378.0, report.totalCost, 0.001)
    }

    @Test
    fun calculateVehicleSummary_returnsExpectedValues() {
        val useCase = CalculateSharedVehicleSummaryUseCase()
        val summary = useCase(
            vehicleId = 1L,
            fuelRecords = listOf(
                SharedFuelRecord(1L, 100L, 1000.0, 20.0, 5.0),
                SharedFuelRecord(1L, 120L, 1300.0, 15.0, 5.2)
            ),
            odometerRecords = listOf(
                SharedOdometerRecord(1L, 130L, 1500.0)
            )
        )

        assertEquals(500.0, summary.distanceKm, 0.001)
        assertEquals(35.0, summary.liters, 0.001)
        assertEquals(178.0, summary.fuelCost, 0.001)
        assertEquals(14.2857, summary.kmPerLiter, 0.01)
        assertEquals(0.356, summary.costPerKm, 0.001)
    }

    @Test
    fun calculateCostPerKm_returnsExpectedValues() {
        val useCase = CalculateSharedCostPerKmMetricsUseCase()
        val result = useCase(
            listOf(
                SharedMonthlyMetric("2026-01", totalCost = 400.0, distanceKm = 200.0, kmPerLiter = 10.0),
                SharedMonthlyMetric("2026-02", totalCost = 0.0, distanceKm = 0.0, kmPerLiter = 0.0)
            )
        )

        assertEquals(2, result.size)
        assertEquals(2.0, result[0].costPerKm, 0.001)
        assertEquals(0.0, result[1].costPerKm, 0.001)
    }

    @Test
    fun maintenanceStatus_dueSoonByKm() {
        val useCase = CalculateSharedMaintenanceStatusUseCase()
        val status = useCase(
            record = SharedMaintenanceRecord(
                vehicleId = 1L,
                createdAtEpochDay = 100L,
                dueDateEpochDay = null,
                dueOdometerKm = 20500.0,
                estimatedCost = 100.0,
                done = false
            ),
            todayEpochDay = 200L,
            currentOdometerKm = 20100.0
        )

        assertEquals(SharedMaintenanceDueStatus.DUE_SOON, status)
    }
}
