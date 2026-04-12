package br.com.tec.tecmotors

import br.com.tec.tecmotors.domain.model.MaintenanceRecord
import br.com.tec.tecmotors.domain.model.MaintenanceType
import br.com.tec.tecmotors.domain.model.OdometerRecord
import br.com.tec.tecmotors.domain.model.Vehicle
import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.domain.repository.MaintenanceRepository
import br.com.tec.tecmotors.domain.repository.OdometerRepository
import br.com.tec.tecmotors.domain.repository.VehicleRepository
import br.com.tec.tecmotors.domain.usecase.AddMaintenanceUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateComponentHealthUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateMaintenanceStatusUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveMaintenanceUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveOdometersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
import br.com.tec.tecmotors.domain.usecase.SetMaintenanceDoneUseCase
import br.com.tec.tecmotors.presentation.common.UiFeedback
import br.com.tec.tecmotors.presentation.maintenance.MaintenanceUiEvent
import br.com.tec.tecmotors.presentation.maintenance.MaintenanceViewModel
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
class MaintenanceViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveMaintenance_withoutDueInfo_emitsRepeatedErrorFeedback() = runTest {
        val vehicleRepository = FakeVehicleRepository()
        val odometerRepository = FakeOdometerRepository()
        val maintenanceRepository = FakeMaintenanceRepository()

        val viewModel = MaintenanceViewModel(
            observeVehiclesUseCase = ObserveVehiclesUseCase(vehicleRepository),
            observeOdometersUseCase = ObserveOdometersUseCase(odometerRepository),
            observeMaintenanceUseCase = ObserveMaintenanceUseCase(maintenanceRepository),
            addMaintenanceUseCase = AddMaintenanceUseCase(maintenanceRepository),
            setMaintenanceDoneUseCase = SetMaintenanceDoneUseCase(maintenanceRepository),
            calculateMaintenanceStatusUseCase = CalculateMaintenanceStatusUseCase(),
            calculateComponentHealthUseCase = CalculateComponentHealthUseCase()
        )
        val collectJob: Job = launch { viewModel.uiState.collect { } }
        val feedbacksDeferred = async { viewModel.events.take(2).toList() }

        advanceUntilIdle()
        viewModel.onEvent(MaintenanceUiEvent.SaveMaintenance)
        advanceUntilIdle()
        viewModel.onEvent(MaintenanceUiEvent.SaveMaintenance)
        advanceUntilIdle()

        assertEquals(
            listOf(
                UiFeedback.Error("Informe data ou odometro para vencimento"),
                UiFeedback.Error("Informe data ou odometro para vencimento")
            ),
            feedbacksDeferred.await()
        )
        assertEquals(0, maintenanceRepository.addCalls)
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

    private class FakeMaintenanceRepository : MaintenanceRepository {
        private val records = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
        var addCalls: Int = 0

        override fun observeMaintenance(): Flow<List<MaintenanceRecord>> = records.asStateFlow()

        override fun observeMaintenance(vehicleId: Long): Flow<List<MaintenanceRecord>> = records.asStateFlow()

        override suspend fun getMaintenance(): List<MaintenanceRecord> = records.value

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
        ) {
            addCalls += 1
            val nextId = (records.value.maxOfOrNull { it.id } ?: 0L) + 1L
            records.value = records.value + MaintenanceRecord(
                id = nextId,
                vehicleId = vehicleId,
                type = type,
                title = title,
                notes = notes,
                createdAtEpochDay = createdAtEpochDay,
                dueDateEpochDay = dueDateEpochDay,
                dueOdometerKm = dueOdometerKm,
                estimatedCost = estimatedCost,
                done = false,
                receiptImageUri = receiptImageUri
            )
        }

        override suspend fun setDone(recordId: Long, done: Boolean) = Unit
    }
}
