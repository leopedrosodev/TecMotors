package br.com.tec.tecmotors

import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Settings
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.domain.repository.MaintenanceRepository
import br.com.tec.tecmotors.domain.repository.OdometerRepository
import br.com.tec.tecmotors.domain.repository.RefuelRepository
import br.com.tec.tecmotors.domain.repository.SettingsRepository
import br.com.tec.tecmotors.domain.repository.VehicleRepository
import br.com.tec.tecmotors.domain.usecase.CalculateCostPerKmMetricsUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateMonthlyMetricsUseCase
import br.com.tec.tecmotors.domain.usecase.CalculatePeriodReportUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateVehicleSummaryUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveMaintenanceUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveOdometersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveRefuelsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveSettingsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
import br.com.tec.tecmotors.domain.usecase.SetMonthlyBudgetUseCase
import br.com.tec.tecmotors.presentation.common.UiFeedback
import br.com.tec.tecmotors.presentation.reports.ReportsUiEvent
import br.com.tec.tecmotors.presentation.reports.ReportsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun applyCustomPeriod_withInvalidRange_setsTypedErrorFeedback() = runTest {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = createViewModel(settingsRepository)
        val collectJob: Job = launch { viewModel.uiState.collect { } }

        advanceUntilIdle()
        viewModel.onEvent(ReportsUiEvent.ChangeCustomStartDate("20/04/2026"))
        viewModel.onEvent(ReportsUiEvent.ChangeCustomEndDate("10/04/2026"))
        advanceUntilIdle()

        viewModel.onEvent(ReportsUiEvent.ApplyCustomPeriod)
        advanceUntilIdle()

        assertEquals(
            UiFeedback.Error("Periodo personalizado invalido"),
            viewModel.uiState.value.exportFeedback
        )
        collectJob.cancel()
    }

    @Test
    fun saveBudget_withValidValue_setsTypedSuccessFeedbackAndPersistsBudget() = runTest {
        val settingsRepository = FakeSettingsRepository()
        val viewModel = createViewModel(settingsRepository)
        val collectJob: Job = launch { viewModel.uiState.collect { } }

        advanceUntilIdle()
        viewModel.onEvent(ReportsUiEvent.ChangeBudgetInput("500"))
        advanceUntilIdle()

        viewModel.onEvent(ReportsUiEvent.SaveBudget)
        advanceUntilIdle()

        assertEquals(UiFeedback.Success("Orcamento salvo"), viewModel.uiState.value.exportFeedback)
        assertEquals(500.0, settingsRepository.current.monthlyBudgetCar, 0.001)
        assertEquals("500.00", viewModel.uiState.value.budgetInputText)
        collectJob.cancel()
    }

    @Test
    fun otherVehicle_usesBudgetFromVehicleBudgetMap() = runTest {
        val settingsRepository = FakeSettingsRepository(
            initialSettings = Settings(
                darkThemeEnabled = true,
                legacyImportDone = false,
                dataUpdatedAtMillis = 0L,
                monthlyBudgetCar = 0.0,
                monthlyBudgetMotorcycle = 0.0,
                vehicleBudgets = mapOf(VehicleType.OTHER to 320.0)
            )
        )
        val viewModel = createViewModel(
            settingsRepository = settingsRepository,
            vehicles = listOf(Vehicle(7L, "Van", VehicleType.OTHER))
        )
        val collectJob: Job = launch { viewModel.uiState.collect { } }

        advanceUntilIdle()

        assertEquals(320.0, viewModel.uiState.value.budgetValue, 0.001)
        assertEquals("320.00", viewModel.uiState.value.budgetInputText)
        collectJob.cancel()
    }

    private fun createViewModel(
        settingsRepository: FakeSettingsRepository,
        vehicles: List<Vehicle> = listOf(Vehicle(1L, "Meu Carro", VehicleType.CAR))
    ): ReportsViewModel {
        val vehicleRepository = FakeVehicleRepository(vehicles)
        val refuelRepository = FakeRefuelRepository()
        val odometerRepository = FakeOdometerRepository()
        val maintenanceRepository = FakeMaintenanceRepository()
        val periodUseCase = CalculatePeriodReportUseCase()

        return ReportsViewModel(
            observeVehiclesUseCase = ObserveVehiclesUseCase(vehicleRepository),
            observeRefuelsUseCase = ObserveRefuelsUseCase(refuelRepository),
            observeOdometersUseCase = ObserveOdometersUseCase(odometerRepository),
            observeMaintenanceUseCase = ObserveMaintenanceUseCase(maintenanceRepository),
            observeSettingsUseCase = ObserveSettingsUseCase(settingsRepository),
            setMonthlyBudgetUseCase = SetMonthlyBudgetUseCase(settingsRepository),
            calculatePeriodReportUseCase = periodUseCase,
            calculateMonthlyMetricsUseCase = CalculateMonthlyMetricsUseCase(periodUseCase),
            calculateCostPerKmMetricsUseCase = CalculateCostPerKmMetricsUseCase(),
            calculateVehicleSummaryUseCase = CalculateVehicleSummaryUseCase()
        )
    }

    private class FakeVehicleRepository(
        vehicles: List<Vehicle>
    ) : VehicleRepository {
        private val vehicles = MutableStateFlow(vehicles)

        override fun observeVehicles(): Flow<List<Vehicle>> = vehicles.asStateFlow()

        override suspend fun getVehicles(): List<Vehicle> = vehicles.value

        override suspend fun ensureDefaultVehiclesIfEmpty() = Unit

        override suspend fun addVehicle(name: String, type: VehicleType) = Unit

        override suspend fun renameVehicle(vehicleId: Long, name: String) = Unit
    }

    private class FakeRefuelRepository : RefuelRepository {
        private val records = MutableStateFlow<List<FuelRecord>>(emptyList())

        override fun observeRefuels(): Flow<List<FuelRecord>> = records.asStateFlow()

        override suspend fun getRefuels(): List<FuelRecord> = records.value

        override suspend fun addRefuel(
            vehicleId: Long,
            dateEpochDay: Long,
            odometerKm: Double,
            liters: Double,
            pricePerLiter: Double,
            stationName: String,
            usageType: br.com.tec.tecmotors.domain.model.FuelUsageType,
            receiptImageUri: String?
        ) = Unit
    }

    private class FakeOdometerRepository : OdometerRepository {
        private val records = MutableStateFlow<List<OdometerRecord>>(emptyList())

        override fun observeOdometerRecords(): Flow<List<OdometerRecord>> = records.asStateFlow()

        override suspend fun getOdometerRecords(): List<OdometerRecord> = records.value

        override suspend fun addOdometer(vehicleId: Long, dateEpochDay: Long, odometerKm: Double) = Unit
    }

    private class FakeMaintenanceRepository : MaintenanceRepository {
        private val records = MutableStateFlow<List<MaintenanceRecord>>(emptyList())

        override fun observeMaintenance(): Flow<List<MaintenanceRecord>> = records.asStateFlow()

        override fun observeMaintenance(vehicleId: Long): Flow<List<MaintenanceRecord>> = records.asStateFlow()

        override suspend fun getMaintenance(): List<MaintenanceRecord> = records.value

        override suspend fun addMaintenance(
            vehicleId: Long,
            type: br.com.tec.tecmotors.domain.model.MaintenanceType,
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

    private class FakeSettingsRepository(
        initialSettings: Settings = Settings(
            darkThemeEnabled = true,
            legacyImportDone = false,
            dataUpdatedAtMillis = 0L,
            monthlyBudgetCar = 0.0,
            monthlyBudgetMotorcycle = 0.0
        )
    ) : SettingsRepository {
        val settingsFlow = MutableStateFlow(initialSettings)

        val current: Settings
            get() = settingsFlow.value

        override fun observeSettings(): Flow<Settings> = settingsFlow.asStateFlow()

        override fun observeDarkTheme(): Flow<Boolean> =
            MutableStateFlow(settingsFlow.value.darkThemeEnabled).asStateFlow()

        override suspend fun getSettings(): Settings = settingsFlow.value

        override suspend fun setDarkTheme(enabled: Boolean) {
            settingsFlow.value = settingsFlow.value.copy(darkThemeEnabled = enabled)
        }

        override suspend fun setMonthlyBudget(vehicleType: VehicleType, amount: Double) {
            settingsFlow.value = when (vehicleType) {
                VehicleType.CAR -> settingsFlow.value.copy(
                    monthlyBudgetCar = amount,
                    vehicleBudgets = settingsFlow.value.vehicleBudgets + (VehicleType.CAR to amount)
                )
                VehicleType.MOTORCYCLE -> settingsFlow.value.copy(
                    monthlyBudgetMotorcycle = amount,
                    vehicleBudgets = settingsFlow.value.vehicleBudgets + (VehicleType.MOTORCYCLE to amount)
                )
                VehicleType.OTHER -> settingsFlow.value.copy(
                    vehicleBudgets = settingsFlow.value.vehicleBudgets + (VehicleType.OTHER to amount)
                )
            }
        }

        override suspend fun markLegacyImportDone() = Unit

        override suspend fun touchDataUpdatedAt(timestampMillis: Long) = Unit
    }
}
