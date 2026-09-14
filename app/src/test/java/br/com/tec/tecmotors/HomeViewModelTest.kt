package br.com.tec.tecmotors

import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.FuelUsageType
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Settings
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.domain.repository.MaintenanceRepository
import br.com.tec.tecmotors.domain.repository.OdometerRepository
import br.com.tec.tecmotors.domain.repository.RefuelRepository
import br.com.tec.tecmotors.domain.repository.SettingsRepository
import br.com.tec.tecmotors.domain.repository.VehicleRepository
import br.com.tec.tecmotors.domain.usecase.CalculatePeriodReportUseCase
import br.com.tec.tecmotors.domain.usecase.DecideRemindersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveMaintenanceUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveOdometersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveRefuelsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveSettingsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
import br.com.tec.tecmotors.presentation.home.HomeEntryKind
import br.com.tec.tecmotors.presentation.home.HomeUiEvent
import br.com.tec.tecmotors.presentation.home.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 3, 20)
    private val car = Vehicle(1L, "Gol", VehicleType.CAR)
    private val moto = Vehicle(2L, "Biz", VehicleType.MOTORCYCLE)

    @Test
    fun monthSpendAndBudget_areDerivedFromCurrentMonth() = runTest {
        val viewModel = buildViewModel(
            refuels = listOf(
                refuel(id = 1L, dateEpochDay = today.withDayOfMonth(3).toEpochDay(), liters = 30.0, pricePerLiter = 6.0),
                refuel(id = 2L, dateEpochDay = today.withDayOfMonth(15).toEpochDay(), liters = 20.0, pricePerLiter = 6.0),
                // mes anterior: nao pode entrar na conta
                refuel(id = 3L, dateEpochDay = LocalDate.of(2026, 2, 10).toEpochDay(), liters = 50.0, pricePerLiter = 6.0)
            ),
            settings = settings(budgetCar = 600.0)
        )

        val collectJob: Job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(300.0, state.monthCost, 0.001)
        assertEquals(600.0, state.monthBudget, 0.001)
        assertEquals(300.0, state.budgetRemaining, 0.001)
        assertEquals(0.5f, state.budgetProgress, 0.001f)
        assertEquals(false, state.budgetExceeded)
        assertEquals(true, state.hasBudget)

        collectJob.cancel()
    }

    @Test
    fun budgetOverspent_isFlagged() = runTest {
        val viewModel = buildViewModel(
            refuels = listOf(
                refuel(id = 1L, dateEpochDay = today.withDayOfMonth(3).toEpochDay(), liters = 100.0, pricePerLiter = 7.0)
            ),
            settings = settings(budgetCar = 600.0)
        )

        val collectJob: Job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(true, state.budgetExceeded)
        assertEquals(1f, state.budgetProgress, 0.001f)
        assertTrue(state.budgetRemaining < 0.0)

        collectJob.cancel()
    }

    @Test
    fun withoutBudget_progressStaysZeroAndFlagIsOff() = runTest {
        val viewModel = buildViewModel(
            refuels = listOf(
                refuel(id = 1L, dateEpochDay = today.withDayOfMonth(3).toEpochDay())
            ),
            settings = settings(budgetCar = 0.0)
        )

        val collectJob: Job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.hasBudget)
        assertEquals(0f, state.budgetProgress, 0.001f)
        assertEquals(false, state.budgetExceeded)

        collectJob.cancel()
    }

    @Test
    fun lastReading_usesMostRecentBetweenOdometerAndRefuel() = runTest {
        val viewModel = buildViewModel(
            refuels = listOf(
                refuel(id = 1L, dateEpochDay = today.minusDays(4).toEpochDay(), odometerKm = 45_320.0)
            ),
            odometers = listOf(
                odometer(id = 1L, dateEpochDay = today.minusDays(12).toEpochDay(), odometerKm = 44_900.0)
            )
        )

        val collectJob: Job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(45_320.0, state.lastOdometerKm!!, 0.001)
        assertEquals(4, state.lastOdometerDaysAgo)

        collectJob.cancel()
    }

    @Test
    fun maintenanceAlert_isExposedForSelectedVehicleOnly() = runTest {
        val viewModel = buildViewModel(
            odometers = listOf(
                odometer(id = 1L, dateEpochDay = today.toEpochDay(), odometerKm = 45_320.0),
                odometer(id = 2L, vehicleId = 2L, dateEpochDay = today.toEpochDay(), odometerKm = 10_000.0)
            ),
            maintenance = listOf(
                maintenance(id = 5L, vehicleId = 2L, dueOdometerKm = 10_200.0, title = "Corrente")
            )
        )

        val collectJob: Job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.maintenanceAlert)

        viewModel.onEvent(HomeUiEvent.SelectVehicle(2L))
        advanceUntilIdle()

        val alert = viewModel.uiState.value.maintenanceAlert
        assertNotNull(alert)
        assertEquals("Corrente", alert!!.title)
        assertEquals(200.0, alert.kmRemaining, 0.001)

        collectJob.cancel()
    }

    @Test
    fun recentEntries_mixRefuelsAndMaintenanceNewestFirst() = runTest {
        val viewModel = buildViewModel(
            refuels = listOf(
                refuel(id = 1L, dateEpochDay = today.minusDays(9).toEpochDay()),
                refuel(id = 2L, dateEpochDay = today.minusDays(1).toEpochDay())
            ),
            maintenance = listOf(
                maintenance(id = 5L, dueOdometerKm = 99_000.0, title = "Filtro de ar")
                    .copy(createdAtEpochDay = today.minusDays(4).toEpochDay())
            )
        )

        val collectJob: Job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val entries = viewModel.uiState.value.recentEntries
        assertEquals(3, entries.size)
        assertEquals(HomeEntryKind.REFUEL, entries[0].kind)
        assertEquals(HomeEntryKind.MAINTENANCE, entries[1].kind)
        assertEquals("Filtro de ar", entries[1].title)
        assertEquals(HomeEntryKind.REFUEL, entries[2].kind)

        collectJob.cancel()
    }

    @Test
    fun emptyState_reportsNoData() = runTest {
        val viewModel = buildViewModel()

        val collectJob: Job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.hasAnyData)
        assertEquals(1L, state.selectedVehicleId)
        assertEquals("Março", state.monthLabel)

        collectJob.cancel()
    }

    private fun buildViewModel(
        vehicles: List<Vehicle> = listOf(car, moto),
        refuels: List<FuelRecord> = emptyList(),
        odometers: List<OdometerRecord> = emptyList(),
        maintenance: List<MaintenanceRecord> = emptyList(),
        settings: Settings = settings()
    ): HomeViewModel {
        val vehicleRepository = FakeVehicleRepository(vehicles)
        val refuelRepository = FakeRefuelRepository(refuels)
        val odometerRepository = FakeOdometerRepository(odometers)
        val maintenanceRepository = FakeMaintenanceRepository(maintenance)
        val settingsRepository = FakeSettingsRepository(settings)

        return HomeViewModel(
            observeVehiclesUseCase = ObserveVehiclesUseCase(vehicleRepository),
            observeRefuelsUseCase = ObserveRefuelsUseCase(refuelRepository),
            observeOdometersUseCase = ObserveOdometersUseCase(odometerRepository),
            observeMaintenanceUseCase = ObserveMaintenanceUseCase(maintenanceRepository),
            observeSettingsUseCase = ObserveSettingsUseCase(settingsRepository),
            calculatePeriodReportUseCase = CalculatePeriodReportUseCase(),
            decideRemindersUseCase = DecideRemindersUseCase(),
            today = { today }
        )
    }

    private fun settings(budgetCar: Double = 0.0) = Settings(
        darkThemeEnabled = true,
        legacyImportDone = true,
        dataUpdatedAtMillis = 0L,
        monthlyBudgetCar = budgetCar,
        monthlyBudgetMotorcycle = 0.0
    )

    private fun refuel(
        id: Long,
        dateEpochDay: Long,
        vehicleId: Long = 1L,
        odometerKm: Double = 45_000.0,
        liters: Double = 30.0,
        pricePerLiter: Double = 6.0
    ) = FuelRecord(
        id = id,
        vehicleId = vehicleId,
        dateEpochDay = dateEpochDay,
        odometerKm = odometerKm,
        liters = liters,
        pricePerLiter = pricePerLiter,
        usageType = FuelUsageType.MIXED
    )

    private fun odometer(
        id: Long,
        dateEpochDay: Long,
        odometerKm: Double,
        vehicleId: Long = 1L
    ) = OdometerRecord(id, vehicleId, dateEpochDay, odometerKm)

    private fun maintenance(
        id: Long,
        dueOdometerKm: Double,
        title: String,
        vehicleId: Long = 1L
    ) = MaintenanceRecord(
        id = id,
        vehicleId = vehicleId,
        type = MaintenanceType.OIL_CHANGE,
        title = title,
        notes = "",
        createdAtEpochDay = 0L,
        dueDateEpochDay = null,
        dueOdometerKm = dueOdometerKm,
        estimatedCost = null,
        done = false
    )

    private class FakeVehicleRepository(vehicles: List<Vehicle>) : VehicleRepository {
        private val state = MutableStateFlow(vehicles)
        override fun observeVehicles(): Flow<List<Vehicle>> = state.asStateFlow()
        override suspend fun getVehicles(): List<Vehicle> = state.value
        override suspend fun ensureDefaultVehiclesIfEmpty() = Unit
        override suspend fun addVehicle(name: String, type: VehicleType) = Unit
        override suspend fun renameVehicle(vehicleId: Long, name: String) = Unit
    }

    private class FakeRefuelRepository(records: List<FuelRecord>) : RefuelRepository {
        private val state = MutableStateFlow(records)
        override fun observeRefuels(): Flow<List<FuelRecord>> = state.asStateFlow()
        override suspend fun getRefuels(): List<FuelRecord> = state.value
        override suspend fun addRefuel(
            vehicleId: Long,
            dateEpochDay: Long,
            odometerKm: Double,
            liters: Double,
            pricePerLiter: Double,
            stationName: String,
            usageType: FuelUsageType,
            receiptImageUri: String?
        ) = Unit
    }

    private class FakeOdometerRepository(records: List<OdometerRecord>) : OdometerRepository {
        private val state = MutableStateFlow(records)
        override fun observeOdometerRecords(): Flow<List<OdometerRecord>> = state.asStateFlow()
        override suspend fun getOdometerRecords(): List<OdometerRecord> = state.value
        override suspend fun addOdometer(vehicleId: Long, dateEpochDay: Long, odometerKm: Double) = Unit
    }

    private class FakeMaintenanceRepository(records: List<MaintenanceRecord>) : MaintenanceRepository {
        private val state = MutableStateFlow(records)
        override fun observeMaintenance(): Flow<List<MaintenanceRecord>> = state.asStateFlow()
        override fun observeMaintenance(vehicleId: Long): Flow<List<MaintenanceRecord>> = state.asStateFlow()
        override suspend fun getMaintenance(): List<MaintenanceRecord> = state.value
        override suspend fun addMaintenance(
            vehicleId: Long,
            type: MaintenanceType,
            title: String,
            notes: String,
            createdAtEpochDay: Long,
            dueDateEpochDay: Long?,
            dueOdometerKm: Double?,
            estimatedCost: Double?,
            receiptImageUri: String?
        ) = Unit

        override suspend fun setDone(recordId: Long, done: Boolean) = Unit
    }

    private class FakeSettingsRepository(settings: Settings) : SettingsRepository {
        private val state = MutableStateFlow(settings)
        override fun observeSettings(): Flow<Settings> = state.asStateFlow()
        override fun observeDarkTheme(): Flow<Boolean> = MutableStateFlow(true).asStateFlow()
        override suspend fun getSettings(): Settings = state.value
        override suspend fun setDarkTheme(enabled: Boolean) = Unit
        override suspend fun setMonthlyBudget(vehicleType: VehicleType, amount: Double) = Unit
        override suspend fun markLegacyImportDone() = Unit
        override suspend fun touchDataUpdatedAt(timestampMillis: Long) = Unit
    }
}
