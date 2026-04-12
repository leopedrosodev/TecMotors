package br.com.tec.tecmotors

import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.domain.repository.OdometerRepository
import br.com.tec.tecmotors.domain.repository.VehicleRepository
import br.com.tec.tecmotors.domain.usecase.AddVehicleUseCase
import br.com.tec.tecmotors.domain.usecase.AddOdometerUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveOdometersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
import br.com.tec.tecmotors.domain.usecase.RenameVehicleUseCase
import br.com.tec.tecmotors.presentation.common.UiFeedback
import br.com.tec.tecmotors.presentation.vehicles.VehiclesUiEvent
import br.com.tec.tecmotors.presentation.vehicles.VehiclesViewModel
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
class VehiclesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveVehicleName_updatesStateAndRepository_andEmitsRepeatedFeedback() = runTest {
        val vehicleRepository = FakeVehicleRepository()
        val odometerRepository = FakeOdometerRepository()

        val viewModel = VehiclesViewModel(
            observeVehiclesUseCase = ObserveVehiclesUseCase(vehicleRepository),
            observeOdometersUseCase = ObserveOdometersUseCase(odometerRepository),
            renameVehicleUseCase = RenameVehicleUseCase(vehicleRepository),
            addOdometerUseCase = AddOdometerUseCase(odometerRepository),
            addVehicleUseCase = AddVehicleUseCase(vehicleRepository)
        )
        val collectJob: Job = launch { viewModel.uiState.collect { } }
        val feedbacksDeferred = async { viewModel.events.take(2).toList() }

        advanceUntilIdle()
        viewModel.onEvent(VehiclesUiEvent.ChangeVehicleName(1L, "Carro Novo"))
        advanceUntilIdle()
        viewModel.onEvent(VehiclesUiEvent.SaveVehicleName(1L))
        advanceUntilIdle()
        viewModel.onEvent(VehiclesUiEvent.SaveVehicleName(1L))

        advanceUntilIdle()

        assertEquals("Carro Novo", vehicleRepository.getVehicles().first().name)
        assertEquals(
            listOf(
                UiFeedback.Success("Nome atualizado"),
                UiFeedback.Success("Nome atualizado")
            ),
            feedbacksDeferred.await()
        )
        collectJob.cancel()
    }

    private class FakeVehicleRepository : VehicleRepository {
        private val vehicles = MutableStateFlow(
            listOf(Vehicle(1, "Meu Carro", VehicleType.CAR))
        )

        override fun observeVehicles(): Flow<List<Vehicle>> = vehicles.asStateFlow()

        override suspend fun getVehicles(): List<Vehicle> = vehicles.value

        override suspend fun ensureDefaultVehiclesIfEmpty() = Unit

        override suspend fun addVehicle(name: String, type: VehicleType) {
            val nextId = (vehicles.value.maxOfOrNull { it.id } ?: 0L) + 1L
            vehicles.value = vehicles.value + Vehicle(nextId, name, type)
        }

        override suspend fun renameVehicle(vehicleId: Long, name: String) {
            vehicles.value = vehicles.value.map { if (it.id == vehicleId) it.copy(name = name) else it }
        }
    }

    private class FakeOdometerRepository : OdometerRepository {
        val records = MutableStateFlow<List<OdometerRecord>>(emptyList())

        override fun observeOdometerRecords(): Flow<List<OdometerRecord>> = records.asStateFlow()

        override suspend fun getOdometerRecords(): List<OdometerRecord> = records.value

        override suspend fun addOdometer(vehicleId: Long, dateEpochDay: Long, odometerKm: Double) {
            val id = (records.value.maxOfOrNull { it.id } ?: 0L) + 1L
            records.value = records.value + OdometerRecord(
                id = id,
                vehicleId = vehicleId,
                dateEpochDay = dateEpochDay,
                odometerKm = odometerKm
            )
        }
    }
}
