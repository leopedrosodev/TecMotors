package br.com.tec.tecmotors

import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.domain.usecase.DecideRemindersUseCase
import br.com.tec.tecmotors.domain.usecase.ReminderAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DecideRemindersUseCaseTest {
    private val useCase = DecideRemindersUseCase()
    private val car = Vehicle(id = 1L, name = "Gol", type = VehicleType.CAR)

    @Test
    fun noHistoryAtAll_producesNoAlerts() {
        val alerts = useCase(
            today = LocalDate.of(2026, 3, 10),
            vehicles = listOf(car),
            fuelRecords = emptyList(),
            odometerRecords = emptyList(),
            maintenanceRecords = emptyList()
        )

        assertEquals(emptyList<ReminderAlert>(), alerts)
    }

    @Test
    fun refuelWithinGap_producesNoAlert() {
        val today = LocalDate.of(2026, 3, 10)

        val alerts = useCase(
            today = today,
            vehicles = listOf(car),
            fuelRecords = listOf(refuel(dateEpochDay = today.minusDays(5).toEpochDay())),
            odometerRecords = emptyList(),
            maintenanceRecords = emptyList(),
            refuelGapDays = 14
        )

        assertEquals(emptyList<ReminderAlert>(), alerts)
    }

    @Test
    fun refuelOlderThanGap_producesNoRecentRefuelAlert() {
        val today = LocalDate.of(2026, 3, 20)

        val alerts = useCase(
            today = today,
            vehicles = listOf(car),
            fuelRecords = listOf(
                refuel(dateEpochDay = today.minusDays(18).toEpochDay(), stationName = "Ipiranga")
            ),
            odometerRecords = emptyList(),
            maintenanceRecords = emptyList(),
            refuelGapDays = 14
        )

        assertEquals(
            listOf(
                ReminderAlert.NoRecentRefuel(
                    vehicleId = 1L,
                    vehicleName = "Gol",
                    daysSinceLastRefuel = 18,
                    lastStationName = "Ipiranga"
                )
            ),
            alerts
        )
    }

    @Test
    fun maintenanceInsideThreshold_producesAlertWithRemainingKm() {
        val today = LocalDate.of(2026, 3, 10)

        val alerts = useCase(
            today = today,
            vehicles = listOf(car),
            fuelRecords = emptyList(),
            odometerRecords = listOf(odometer(today.toEpochDay(), 45_320.0)),
            maintenanceRecords = listOf(maintenance(id = 7L, dueOdometerKm = 45_800.0)),
            maintenanceThresholdKm = 1_000.0
        )

        assertEquals(
            listOf(
                ReminderAlert.MaintenanceDueSoon(
                    vehicleId = 1L,
                    vehicleName = "Gol",
                    recordId = 7L,
                    title = "Troca de oleo",
                    kmRemaining = 480.0,
                    dueOdometerKm = 45_800.0,
                    currentOdometerKm = 45_320.0
                )
            ),
            alerts
        )
    }

    @Test
    fun maintenanceBeyondThreshold_producesNoAlert() {
        val today = LocalDate.of(2026, 3, 10)

        val alerts = useCase(
            today = today,
            vehicles = listOf(car),
            fuelRecords = emptyList(),
            odometerRecords = listOf(odometer(today.toEpochDay(), 40_000.0)),
            maintenanceRecords = listOf(maintenance(id = 7L, dueOdometerKm = 45_800.0)),
            maintenanceThresholdKm = 1_000.0
        )

        assertEquals(emptyList<ReminderAlert>(), alerts)
    }

    @Test
    fun overdueMaintenance_isReportedWithNegativeRemaining() {
        val today = LocalDate.of(2026, 3, 10)

        val alerts = useCase(
            today = today,
            vehicles = listOf(car),
            fuelRecords = emptyList(),
            odometerRecords = listOf(odometer(today.toEpochDay(), 46_500.0)),
            maintenanceRecords = listOf(maintenance(id = 7L, dueOdometerKm = 45_800.0))
        )

        val alert = alerts.filterIsInstance<ReminderAlert.MaintenanceDueSoon>().single()
        assertEquals(-700.0, alert.kmRemaining, 0.001)
    }

    @Test
    fun doneMaintenance_isIgnored() {
        val today = LocalDate.of(2026, 3, 10)

        val alerts = useCase(
            today = today,
            vehicles = listOf(car),
            fuelRecords = emptyList(),
            odometerRecords = listOf(odometer(today.toEpochDay(), 45_320.0)),
            maintenanceRecords = listOf(maintenance(id = 7L, dueOdometerKm = 45_800.0, done = true))
        )

        assertEquals(emptyList<ReminderAlert>(), alerts)
    }

    @Test
    fun refuelOdometerCountsAsCurrentKm() {
        val today = LocalDate.of(2026, 3, 10)

        val alerts = useCase(
            today = today,
            vehicles = listOf(car),
            fuelRecords = listOf(
                refuel(dateEpochDay = today.toEpochDay(), odometerKm = 45_500.0)
            ),
            odometerRecords = listOf(odometer(today.minusDays(30).toEpochDay(), 44_000.0)),
            maintenanceRecords = listOf(maintenance(id = 7L, dueOdometerKm = 45_800.0)),
            maintenanceThresholdKm = 1_000.0
        )

        val alert = alerts.filterIsInstance<ReminderAlert.MaintenanceDueSoon>().single()
        assertEquals(300.0, alert.kmRemaining, 0.001)
    }

    @Test
    fun monthEndWithoutOdometerReading_producesAlert() {
        val today = LocalDate.of(2026, 3, 30)

        val alerts = useCase(
            today = today,
            vehicles = listOf(car),
            fuelRecords = listOf(refuel(dateEpochDay = today.minusDays(2).toEpochDay())),
            odometerRecords = listOf(odometer(LocalDate.of(2026, 2, 10).toEpochDay(), 44_000.0)),
            maintenanceRecords = emptyList(),
            monthEndWindowDays = 3
        )

        assertTrue(alerts.any { it is ReminderAlert.MonthEndWithoutOdometer })
    }

    @Test
    fun monthEndWithOdometerReadingInSameMonth_producesNoAlert() {
        val today = LocalDate.of(2026, 3, 30)

        val alerts = useCase(
            today = today,
            vehicles = listOf(car),
            fuelRecords = listOf(refuel(dateEpochDay = today.minusDays(2).toEpochDay())),
            odometerRecords = listOf(odometer(LocalDate.of(2026, 3, 12).toEpochDay(), 45_000.0)),
            maintenanceRecords = emptyList(),
            monthEndWindowDays = 3
        )

        assertTrue(alerts.none { it is ReminderAlert.MonthEndWithoutOdometer })
    }

    @Test
    fun outsideMonthEndWindow_producesNoMonthEndAlert() {
        val today = LocalDate.of(2026, 3, 10)

        val alerts = useCase(
            today = today,
            vehicles = listOf(car),
            fuelRecords = listOf(refuel(dateEpochDay = today.minusDays(2).toEpochDay())),
            odometerRecords = listOf(odometer(LocalDate.of(2026, 2, 10).toEpochDay(), 44_000.0)),
            maintenanceRecords = emptyList(),
            monthEndWindowDays = 3
        )

        assertTrue(alerts.none { it is ReminderAlert.MonthEndWithoutOdometer })
    }

    @Test
    fun alertsAreOrderedByUrgency() {
        val today = LocalDate.of(2026, 3, 30)

        val alerts = useCase(
            today = today,
            vehicles = listOf(car),
            fuelRecords = listOf(
                refuel(dateEpochDay = today.minusDays(40).toEpochDay(), odometerKm = 45_320.0)
            ),
            odometerRecords = listOf(odometer(LocalDate.of(2026, 2, 10).toEpochDay(), 44_000.0)),
            maintenanceRecords = listOf(maintenance(id = 7L, dueOdometerKm = 45_800.0)),
            refuelGapDays = 14,
            maintenanceThresholdKm = 1_000.0,
            monthEndWindowDays = 3
        )

        assertEquals(3, alerts.size)
        assertTrue(alerts[0] is ReminderAlert.MaintenanceDueSoon)
        assertTrue(alerts[1] is ReminderAlert.MonthEndWithoutOdometer)
        assertTrue(alerts[2] is ReminderAlert.NoRecentRefuel)
    }

    @Test
    fun alertsOnlyCoverKnownVehicles() {
        val today = LocalDate.of(2026, 3, 20)

        val alerts = useCase(
            today = today,
            vehicles = listOf(car),
            fuelRecords = listOf(
                refuel(vehicleId = 99L, dateEpochDay = today.minusDays(40).toEpochDay())
            ),
            odometerRecords = emptyList(),
            maintenanceRecords = listOf(maintenance(id = 7L, vehicleId = 99L, dueOdometerKm = 1.0))
        )

        assertEquals(emptyList<ReminderAlert>(), alerts)
    }

    private fun refuel(
        dateEpochDay: Long,
        vehicleId: Long = 1L,
        odometerKm: Double = 45_000.0,
        stationName: String = ""
    ) = FuelRecord(
        id = dateEpochDay,
        vehicleId = vehicleId,
        dateEpochDay = dateEpochDay,
        odometerKm = odometerKm,
        liters = 30.0,
        pricePerLiter = 6.0,
        stationName = stationName
    )

    private fun odometer(
        dateEpochDay: Long,
        odometerKm: Double,
        vehicleId: Long = 1L
    ) = OdometerRecord(
        id = dateEpochDay,
        vehicleId = vehicleId,
        dateEpochDay = dateEpochDay,
        odometerKm = odometerKm
    )

    private fun maintenance(
        id: Long,
        dueOdometerKm: Double,
        vehicleId: Long = 1L,
        done: Boolean = false
    ) = MaintenanceRecord(
        id = id,
        vehicleId = vehicleId,
        type = MaintenanceType.OIL_CHANGE,
        title = "Troca de oleo",
        notes = "",
        createdAtEpochDay = 0L,
        dueDateEpochDay = null,
        dueOdometerKm = dueOdometerKm,
        estimatedCost = null,
        done = done
    )
}
