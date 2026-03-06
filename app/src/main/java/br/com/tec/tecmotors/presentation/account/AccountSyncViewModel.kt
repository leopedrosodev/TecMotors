package br.com.tec.tecmotors.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tec.tecmotors.data.JsonBackupCodec
import br.com.tec.tecmotors.domain.model.SyncResult
import br.com.tec.tecmotors.domain.usecase.CurrentSyncUserUseCase
import br.com.tec.tecmotors.domain.usecase.DownloadRemoteStateUseCase
import br.com.tec.tecmotors.domain.usecase.GetLocalSnapshotUseCase
import br.com.tec.tecmotors.domain.usecase.RestoreLocalSnapshotUseCase
import br.com.tec.tecmotors.domain.usecase.SignInWithGoogleUseCase
import br.com.tec.tecmotors.domain.usecase.SignOutUseCase
import br.com.tec.tecmotors.domain.usecase.SyncNowUseCase
import br.com.tec.tecmotors.domain.usecase.UploadLocalStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountSyncViewModel(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val uploadLocalStateUseCase: UploadLocalStateUseCase,
    private val downloadRemoteStateUseCase: DownloadRemoteStateUseCase,
    private val syncNowUseCase: SyncNowUseCase,
    private val currentSyncUserUseCase: CurrentSyncUserUseCase,
    private val getLocalSnapshotUseCase: GetLocalSnapshotUseCase,
    private val restoreLocalSnapshotUseCase: RestoreLocalSnapshotUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AccountSyncUiState(
            userEmail = currentSyncUserUseCase()
        )
    )
    val uiState: StateFlow<AccountSyncUiState> = _uiState

    fun onEvent(event: AccountSyncUiEvent) {
        when (event) {
            is AccountSyncUiEvent.SubmitGoogleIdToken -> {
                runBusyAction {
                    val result = signInWithGoogleUseCase(event.idToken)
                    result.onSuccess { email ->
                        _uiState.update { state ->
                            state.copy(
                                userEmail = email,
                                statusMessage = "Logado como $email"
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { state ->
                            state.copy(statusMessage = error.message ?: "Falha no login Google.")
                        }
                    }
                }
            }

            AccountSyncUiEvent.SignOut -> {
                signOutUseCase()
                _uiState.update {
                    it.copy(
                        userEmail = null,
                        statusMessage = "Sessao encerrada."
                    )
                }
            }

            AccountSyncUiEvent.Upload -> runSyncAction { uploadLocalStateUseCase() }
            AccountSyncUiEvent.Download -> runSyncAction { downloadRemoteStateUseCase() }
            AccountSyncUiEvent.SyncNow -> runSyncAction { syncNowUseCase() }

            is AccountSyncUiEvent.SetStatusMessage -> {
                _uiState.update { it.copy(statusMessage = event.message) }
            }
        }
    }

    suspend fun buildBackupJson(): Result<String> {
        return runCatching {
            JsonBackupCodec.encode(getLocalSnapshotUseCase())
        }
    }

    suspend fun importBackupJson(raw: String): Result<Unit> {
        return runCatching {
            val snapshot = JsonBackupCodec.decode(raw)
            restoreLocalSnapshotUseCase(snapshot)
        }.onSuccess {
            _uiState.update { state ->
                state.copy(statusMessage = "Backup local restaurado com sucesso.")
            }
        }.onFailure { error ->
            _uiState.update { state ->
                state.copy(statusMessage = "Falha ao restaurar backup local: ${error.message.orEmpty()}")
            }
        }
    }

    fun withBusy(action: suspend () -> Unit) {
        runBusyAction(action)
    }

    private fun runSyncAction(action: suspend () -> Result<SyncResult>) {
        runBusyAction {
            val result = action()
            result.onSuccess { syncResult ->
                _uiState.update { state ->
                    state.copy(
                        statusMessage = syncResult.message,
                        userEmail = currentSyncUserUseCase()
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        statusMessage = error.message ?: "Falha na sincronizacao.",
                        userEmail = currentSyncUserUseCase()
                    )
                }
            }
        }
    }

    private fun runBusyAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            action()
            _uiState.update { it.copy(busy = false) }
        }
    }
}
