package br.com.tec.tecmotors

import br.com.tec.tecmotors.domain.model.LocalStateSnapshot
import br.com.tec.tecmotors.domain.model.SyncResult
import br.com.tec.tecmotors.domain.repository.SnapshotRepository
import br.com.tec.tecmotors.domain.repository.SyncRepository
import br.com.tec.tecmotors.domain.usecase.CurrentSyncUserUseCase
import br.com.tec.tecmotors.domain.usecase.DownloadRemoteStateUseCase
import br.com.tec.tecmotors.domain.usecase.GetLocalSnapshotUseCase
import br.com.tec.tecmotors.domain.usecase.RestoreLocalSnapshotUseCase
import br.com.tec.tecmotors.domain.usecase.SignInWithGoogleUseCase
import br.com.tec.tecmotors.domain.usecase.SignOutUseCase
import br.com.tec.tecmotors.domain.usecase.SyncNowUseCase
import br.com.tec.tecmotors.domain.usecase.UploadLocalStateUseCase
import br.com.tec.tecmotors.presentation.account.AccountSyncUiEvent
import br.com.tec.tecmotors.presentation.account.AccountSyncViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSyncViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun submitGoogleIdToken_updatesUserAndBusyState() = runTest {
        val repository = FakeSyncRepository()
        val snapshotRepository = FakeSnapshotRepository()
        val viewModel = AccountSyncViewModel(
            signInWithGoogleUseCase = SignInWithGoogleUseCase(repository),
            signOutUseCase = SignOutUseCase(repository),
            uploadLocalStateUseCase = UploadLocalStateUseCase(repository),
            downloadRemoteStateUseCase = DownloadRemoteStateUseCase(repository),
            syncNowUseCase = SyncNowUseCase(repository),
            currentSyncUserUseCase = CurrentSyncUserUseCase(repository),
            getLocalSnapshotUseCase = GetLocalSnapshotUseCase(snapshotRepository),
            restoreLocalSnapshotUseCase = RestoreLocalSnapshotUseCase(snapshotRepository)
        )

        viewModel.onEvent(AccountSyncUiEvent.SubmitGoogleIdToken("token"))
        advanceUntilIdle()

        assertEquals("user@leo.com", viewModel.uiState.value.userEmail)
        assertEquals("Logado como user@leo.com", viewModel.uiState.value.statusMessage)
        assertEquals(false, viewModel.uiState.value.busy)
    }

    private class FakeSyncRepository : SyncRepository {
        private var email: String? = null

        override fun currentUserEmail(): String? = email

        override fun isSignedIn(): Boolean = email != null

        override suspend fun signInWithGoogleIdToken(idToken: String): Result<String> {
            delay(1)
            email = "user@leo.com"
            return Result.success(email!!)
        }

        override fun signOut() {
            email = null
        }

        override suspend fun uploadLocalState(): Result<SyncResult> {
            return Result.success(SyncResult("ok", false))
        }

        override suspend fun downloadRemoteState(): Result<SyncResult> {
            return Result.success(SyncResult("ok", true))
        }

        override suspend fun syncNow(): Result<SyncResult> {
            return Result.success(SyncResult("ok", false))
        }
    }

    private class FakeSnapshotRepository : SnapshotRepository {
        private var snapshot = LocalStateSnapshot(
            vehicles = emptyList(),
            odometerRecords = emptyList(),
            fuelRecords = emptyList(),
            maintenanceRecords = emptyList(),
            updatedAtMillis = 0L
        )

        override suspend fun getSnapshot(): LocalStateSnapshot = snapshot

        override suspend fun restoreSnapshot(snapshot: LocalStateSnapshot) {
            this.snapshot = snapshot
        }
    }
}
