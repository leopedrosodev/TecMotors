package br.com.tec.tecmotors.core.di

import android.content.Context
import androidx.room.Room
import br.com.tec.tecmotors.data.local.RoomSnapshotDataSource
import br.com.tec.tecmotors.data.local.TecMotorsDatabase
import br.com.tec.tecmotors.data.local.migration.LegacyImportManager
import br.com.tec.tecmotors.data.local.migration.LegacyPreferencesReader
import br.com.tec.tecmotors.data.local.migration.RoomMigrations
import br.com.tec.tecmotors.data.repository.MaintenanceRepositoryImpl
import br.com.tec.tecmotors.data.repository.OdometerRepositoryImpl
import br.com.tec.tecmotors.data.repository.RefuelRepositoryImpl
import br.com.tec.tecmotors.data.repository.SettingsRepositoryImpl
import br.com.tec.tecmotors.data.repository.SnapshotRepositoryImpl
import br.com.tec.tecmotors.data.repository.SyncRepositoryImpl
import br.com.tec.tecmotors.data.repository.VehicleRepositoryImpl
import br.com.tec.tecmotors.domain.repository.MaintenanceRepository
import br.com.tec.tecmotors.domain.repository.OdometerRepository
import br.com.tec.tecmotors.domain.repository.RefuelRepository
import br.com.tec.tecmotors.domain.repository.SettingsRepository
import br.com.tec.tecmotors.domain.repository.SnapshotRepository
import br.com.tec.tecmotors.domain.repository.SyncRepository
import br.com.tec.tecmotors.domain.repository.VehicleRepository
import br.com.tec.tecmotors.domain.usecase.AddMaintenanceUseCase
import br.com.tec.tecmotors.domain.usecase.AddOdometerUseCase
import br.com.tec.tecmotors.domain.usecase.AddRefuelUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateCostPerKmMetricsUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateMaintenanceStatusUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateMonthlyMetricsUseCase
import br.com.tec.tecmotors.domain.usecase.CalculatePeriodReportUseCase
import br.com.tec.tecmotors.domain.usecase.CalculateVehicleSummaryUseCase
import br.com.tec.tecmotors.domain.usecase.CurrentSyncUserUseCase
import br.com.tec.tecmotors.domain.usecase.DownloadRemoteStateUseCase
import br.com.tec.tecmotors.domain.usecase.EnsureDefaultVehiclesUseCase
import br.com.tec.tecmotors.domain.usecase.GetLocalSnapshotUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveDarkThemeUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveMaintenanceUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveOdometersUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveRefuelsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveReportsDataUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveSettingsUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveVehiclesUseCase
import br.com.tec.tecmotors.domain.usecase.RenameVehicleUseCase
import br.com.tec.tecmotors.domain.usecase.RestoreLocalSnapshotUseCase
import br.com.tec.tecmotors.domain.usecase.SetDarkThemeUseCase
import br.com.tec.tecmotors.domain.usecase.SetMaintenanceDoneUseCase
import br.com.tec.tecmotors.domain.usecase.SetMonthlyBudgetUseCase
import br.com.tec.tecmotors.domain.usecase.SignInWithGoogleUseCase
import br.com.tec.tecmotors.domain.usecase.SignOutUseCase
import br.com.tec.tecmotors.domain.usecase.SyncNowUseCase
import br.com.tec.tecmotors.domain.usecase.UploadLocalStateUseCase

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: TecMotorsDatabase = Room.databaseBuilder(
        appContext,
        TecMotorsDatabase::class.java,
        "tec_motors.db"
    )
        .addMigrations(
            RoomMigrations.MIGRATION_1_2,
            RoomMigrations.MIGRATION_2_3
        )
        .build()

    private val settingsRepository: SettingsRepository = SettingsRepositoryImpl(database)

    private val vehicleRepository: VehicleRepository = VehicleRepositoryImpl(
        database = database,
        settingsRepository = settingsRepository
    )

    private val odometerRepository: OdometerRepository = OdometerRepositoryImpl(
        database = database,
        settingsRepository = settingsRepository
    )

    private val refuelRepository: RefuelRepository = RefuelRepositoryImpl(
        database = database,
        settingsRepository = settingsRepository
    )

    private val maintenanceRepository: MaintenanceRepository = MaintenanceRepositoryImpl(
        database = database,
        settingsRepository = settingsRepository
    )

    private val snapshotDataSource = RoomSnapshotDataSource(database)

    private val snapshotRepository: SnapshotRepository = SnapshotRepositoryImpl(
        database = database,
        dataSource = snapshotDataSource
    )

    private val syncRepository: SyncRepository = SyncRepositoryImpl(
        context = appContext,
        snapshotRepository = snapshotRepository
    )

    val legacyImportManager = LegacyImportManager(
        database = database,
        reader = LegacyPreferencesReader(appContext)
    )

    val observeDarkThemeUseCase = ObserveDarkThemeUseCase(settingsRepository)
    val setDarkThemeUseCase = SetDarkThemeUseCase(settingsRepository)
    val observeSettingsUseCase = ObserveSettingsUseCase(settingsRepository)
    val setMonthlyBudgetUseCase = SetMonthlyBudgetUseCase(settingsRepository)

    val observeVehiclesUseCase = ObserveVehiclesUseCase(vehicleRepository)
    val ensureDefaultVehiclesUseCase = EnsureDefaultVehiclesUseCase(vehicleRepository)
    val renameVehicleUseCase = RenameVehicleUseCase(vehicleRepository)
    val observeOdometersUseCase = ObserveOdometersUseCase(odometerRepository)
    val addOdometerUseCase = AddOdometerUseCase(odometerRepository)

    val observeRefuelsUseCase = ObserveRefuelsUseCase(refuelRepository)
    val addRefuelUseCase = AddRefuelUseCase(refuelRepository)

    val observeMaintenanceUseCase = ObserveMaintenanceUseCase(maintenanceRepository)
    val addMaintenanceUseCase = AddMaintenanceUseCase(maintenanceRepository)
    val setMaintenanceDoneUseCase = SetMaintenanceDoneUseCase(maintenanceRepository)
    val calculateMaintenanceStatusUseCase = CalculateMaintenanceStatusUseCase()

    val observeReportsDataUseCase = ObserveReportsDataUseCase(refuelRepository, odometerRepository)
    val calculatePeriodReportUseCase = CalculatePeriodReportUseCase()
    val calculateMonthlyMetricsUseCase = CalculateMonthlyMetricsUseCase(calculatePeriodReportUseCase)
    val calculateCostPerKmMetricsUseCase = CalculateCostPerKmMetricsUseCase()
    val calculateVehicleSummaryUseCase = CalculateVehicleSummaryUseCase()

    val getLocalSnapshotUseCase = GetLocalSnapshotUseCase(snapshotRepository)
    val restoreLocalSnapshotUseCase = RestoreLocalSnapshotUseCase(snapshotRepository)

    val signInWithGoogleUseCase = SignInWithGoogleUseCase(syncRepository)
    val signOutUseCase = SignOutUseCase(syncRepository)
    val uploadLocalStateUseCase = UploadLocalStateUseCase(syncRepository)
    val downloadRemoteStateUseCase = DownloadRemoteStateUseCase(syncRepository)
    val syncNowUseCase = SyncNowUseCase(syncRepository)
    val currentSyncUserUseCase = CurrentSyncUserUseCase(syncRepository)
}
