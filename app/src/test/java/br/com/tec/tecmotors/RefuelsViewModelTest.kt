package br.com.tec.tecmotors

import br.com.tec.tecmotors.domain.model.FuelRecord
import br.com.tec.tecmotors.domain.model.FuelUsageType
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.domain.repository.RefuelRepository
import br.com.tec.tecmotors.domain.repository.VehicleRepository
import br.com.tec.tecmotors.domain.usecase.AddRefuelUseCase
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

    private class FakeRefuelRepository : RefuelRepository {
        private val records = MutableStateFlow<List<FuelRecord>>(emptyList())
        var addCalls: Int = 0

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
            records.value = records.value + FuelRecord(
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
        }
    }
}
