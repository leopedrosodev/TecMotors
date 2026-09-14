package br.com.tec.tecmotors

import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.FuelUsageType
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.domain.repository.OdometerRepository
import br.com.tec.tecmotors.domain.repository.RefuelRepository
import br.com.tec.tecmotors.domain.repository.VehicleRepository
import br.com.tec.tecmotors.domain.usecase.AddRefuelUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveOdometersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveRefuelsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
import br.com.tec.tecmotors.presentation.common.UiFeedback
import br.com.tec.tecmotors.presentation.refuels.RefuelsUiEvent
import br.com.tec.tecmotors.presentation.refuels.RefuelsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RefuelsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveRefuel_withInvalidFields_emitsRepeatedErrorFeedback() = runTest {
        val vehicleRepository = FakeVehicleRepository()
        val refuelRepository = FakeRefuelRepository()

        val viewModel = RefuelsViewModel(
            observeVehiclesUseCase = ObserveVehiclesUseCase(vehicleRepository),
            observeRefuelsUseCase = ObserveRefuelsUseCase(refuelRepository),
            observeOdometersUseCase = ObserveOdometersUseCase(FakeOdometerRepository()),
            addRefuelUseCase = AddRefuelUseCase(refuelRepository)
        )
        val collectJob: Job = launch { viewModel.uiState.collect { } }
        val feedbacksDeferred = async { viewModel.events.take(2).toList() }

        advanceUntilIdle()
        viewModel.onEvent(RefuelsUiEvent.SaveRefuel)
        advanceUntilIdle()
        viewModel.onEvent(RefuelsUiEvent.SaveRefuel)
        advanceUntilIdle()

        assertEquals(
            listOf(
                UiFeedback.Error("Preencha todos os campos com valores validos"),
                UiFeedback.Error("Preencha todos os campos com valores validos")
            ),
            feedbacksDeferred.await()
        )
        assertEquals(0, refuelRepository.addCalls)
        collectJob.cancel()
    }

    @Test
    fun saveQuickRefuel_derivesPricePerLiterFromTotalPaid() = runTest {
        val vehicleRepository = FakeVehicleRepository()
        val refuelRepository = FakeRefuelRepository()

        val viewModel = RefuelsViewModel(
            observeVehiclesUseCase = ObserveVehiclesUseCase(vehicleRepository),
            observeRefuelsUseCase = ObserveRefuelsUseCase(refuelRepository),
            observeOdometersUseCase = ObserveOdometersUseCase(FakeOdometerRepository()),
            addRefuelUseCase = AddRefuelUseCase(refuelRepository)
        )
        val collectJob: Job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.onEvent(RefuelsUiEvent.ChangeTotalPaid("180"))
        viewModel.onEvent(RefuelsUiEvent.ChangeLiters("32,1"))
        viewModel.onEvent(RefuelsUiEvent.ChangeOdometer("45320"))
        advanceUntilIdle()

        viewModel.onEvent(RefuelsUiEvent.SaveQuickRefuel)
        advanceUntilIdle()

        val saved = refuelRepository.lastAdded!!
        assertEquals(1, refuelRepository.addCalls)
        assertEquals(32.1, saved.liters, 0.001)
        assertEquals(45320.0, saved.odometerKm, 0.001)
        assertEquals(180.0 / 32.1, saved.pricePerLiter, 0.0001)
        assertEquals(180.0, saved.totalCost, 0.0001)
        collectJob.cancel()
    }

    @Test
    fun saveQuickRefuel_withoutTotalPaid_emitsErrorAndSavesNothing() = runTest {
        val vehicleRepository = FakeVehicleRepository()
        val refuelRepository = FakeRefuelRepository()

        val viewModel = RefuelsViewModel(
            observeVehiclesUseCase = ObserveVehiclesUseCase(vehicleRepository),
            observeRefuelsUseCase = ObserveRefuelsUseCase(refuelRepository),
            observeOdometersUseCase = ObserveOdometersUseCase(FakeOdometerRepository()),
            addRefuelUseCase = AddRefuelUseCase(refuelRepository)
        )
        val collectJob: Job = launch { viewModel.uiState.collect { } }
        val feedbackDeferred = async { viewModel.events.take(1).toList() }
        advanceUntilIdle()

        viewModel.onEvent(RefuelsUiEvent.ChangeLiters("32,1"))
        viewModel.onEvent(RefuelsUiEvent.ChangeOdometer("45320"))
        advanceUntilIdle()
        viewModel.onEvent(RefuelsUiEvent.SaveQuickRefuel)
        advanceUntilIdle()

        assertEquals(
            listOf(UiFeedback.Error("Informe valor pago, litros e odometro")),
            feedbackDeferred.await()
        )
        assertEquals(0, refuelRepository.addCalls)
        collectJob.cancel()
    }

    @Test
    fun quickEntry_exposesLiveConsumptionPreview() = runTest {
        val vehicleRepository = FakeVehicleRepository()
        val refuelRepository = FakeRefuelRepository()

        val viewModel = RefuelsViewModel(
            observeVehiclesUseCase = ObserveVehiclesUseCase(vehicleRepository),
            observeRefuelsUseCase = ObserveRefuelsUseCase(refuelRepository),
            observeOdometersUseCase = ObserveOdometersUseCase(FakeOdometerRepository()),
            addRefuelUseCase = AddRefuelUseCase(refuelRepository)
        )
        val collectJob: Job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        // primeiro abastecimento define o odometro anterior
        viewModel.onEvent(RefuelsUiEvent.ChangeTotalPaid("100"))
        viewModel.onEvent(RefuelsUiEvent.ChangeLiters("20"))
        viewModel.onEvent(RefuelsUiEvent.ChangeOdometer("44900"))
        advanceUntilIdle()
        viewModel.onEvent(RefuelsUiEvent.SaveQuickRefuel)
        advanceUntilIdle()

        viewModel.onEvent(RefuelsUiEvent.ChangeTotalPaid("180"))
        viewModel.onEvent(RefuelsUiEvent.ChangeLiters("30"))
        viewModel.onEvent(RefuelsUiEvent.ChangeOdometer("45320"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(44900.0, state.lastOdometerKm!!, 0.001)
        assertEquals(6.0, state.computedPricePerLiter!!, 0.001)
        assertEquals(420.0, state.distanceSinceLastKm!!, 0.001)
        assertEquals(14.0, state.estimatedKmPerLiter!!, 0.001)
        collectJob.cancel()
    }

    private class FakeVehicleRepository : VehicleRepository {
        private val vehicles = MutableStateFlow(
            listOf(Vehicle(1L, "Meu Carro", VehicleType.CAR))
        )

        override fun observeVehicles(): Flow<List<Vehicle>> = vehicles.asStateFlow()

        override suspend fun getVehicles(): List<Vehicle> = vehicles.value

        override suspend fun ensureDefaultVehiclesIfEmpty() = Unit

        override suspend fun addVehicle(name: String, type: VehicleType) = Unit

        override suspend fun renameVehicle(vehicleId: Long, name: String) = Unit
    }

    private class FakeOdometerRepository : OdometerRepository {
        private val records = MutableStateFlow<List<OdometerRecord>>(emptyList())

        override fun observeOdometerRecords(): Flow<List<OdometerRecord>> = records.asStateFlow()

        override suspend fun getOdometerRecords(): List<OdometerRecord> = records.value

        override suspend fun addOdometer(vehicleId: Long, dateEpochDay: Long, odometerKm: Double) = Unit
    }

    private class FakeRefuelRepository : RefuelRepository {
        private val records = MutableStateFlow<List<FuelRecord>>(emptyList())
        var addCalls: Int = 0
        var lastAdded: FuelRecord? = null

        override fun observeRefuels(): Flow<List<FuelRecord>> = records.asStateFlow()

        override suspend fun getRefuels(): List<FuelRecord> = records.value

        override suspend fun addRefuel(
            vehicleId: Long,
            dateEpochDay: Long,
            odometerKm: Double,
            liters: Double,
            pricePerLiter: Double,
            stationName: String,
            usageType: FuelUsageType,
            receiptImageUri: String?
        ) {
            addCalls += 1
            val nextId = (records.value.maxOfOrNull { it.id } ?: 0L) + 1L
            val added = FuelRecord(
                id = nextId,
                vehicleId = vehicleId,
                dateEpochDay = dateEpochDay,
                odometerKm = odometerKm,
                liters = liters,
                pricePerLiter = pricePerLiter,
                stationName = stationName,
                usageType = usageType,
                receiptImageUri = receiptImageUri
            )
            lastAdded = added
            records.value = records.value + added
        }
    }
}
