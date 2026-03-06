package br.com.tec.tecmotors.domain.usecase

import br.com.tec.tecmotors.domain.model.LocalStateSnapshot
import br.com.tec.tecmotors.domain.model.VehicleType
import br.com.tec.tecmotors.domain.repository.SettingsRepository
import br.com.tec.tecmotors.domain.repository.SnapshotRepository
import br.com.tec.tecmotors.domain.repository.SyncRepository

class ObserveDarkThemeUseCase(private val repository: SettingsRepository) {
    operator fun invoke() = repository.observeDarkTheme()
}

class SetDarkThemeUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setDarkTheme(enabled)
}

class ObserveSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke() = repository.observeSettings()
}

class SetMonthlyBudgetUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(vehicleType: VehicleType, amount: Double) {
        repository.setMonthlyBudget(vehicleType, amount)
    }
}

class GetLocalSnapshotUseCase(private val repository: SnapshotRepository) {
    suspend operator fun invoke(): LocalStateSnapshot = repository.getSnapshot()
}

class RestoreLocalSnapshotUseCase(private val repository: SnapshotRepository) {
    suspend operator fun invoke(snapshot: LocalStateSnapshot) = repository.restoreSnapshot(snapshot)
}

class SignInWithGoogleUseCase(private val repository: SyncRepository) {
    suspend operator fun invoke(idToken: String) = repository.signInWithGoogleIdToken(idToken)
}

class SignOutUseCase(private val repository: SyncRepository) {
    operator fun invoke() = repository.signOut()
}

class UploadLocalStateUseCase(private val repository: SyncRepository) {
    suspend operator fun invoke() = repository.uploadLocalState()
}

class DownloadRemoteStateUseCase(private val repository: SyncRepository) {
    suspend operator fun invoke() = repository.downloadRemoteState()
}

class SyncNowUseCase(private val repository: SyncRepository) {
    suspend operator fun invoke() = repository.syncNow()
}

class CurrentSyncUserUseCase(private val repository: SyncRepository) {
    operator fun invoke() = repository.currentUserEmail()
}
