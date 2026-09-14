package br.com.tec.tecmotors.core.startup

import br.com.tec.tecmotors.data.local.migration.LegacyImportManager
import br.com.tec.tecmotors.domain.usecase.EnsureDefaultVehiclesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Preparo de dados que precisa acontecer antes da primeira tela aparecer.
 *
 * A splash do sistema fica em pe enquanto [ready] for falso, entao a abertura
 * dura exatamente o tempo do trabalho real - sem delay fixo.
 */
class AppStartup(
    private val legacyImportManager: LegacyImportManager,
    private val ensureDefaultVehiclesUseCase: EnsureDefaultVehiclesUseCase
) {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val mutex = Mutex()

    /** Idempotente: chamadas depois da primeira retornam imediatamente. */
    suspend fun awaitReady() {
        if (_ready.value) return

        mutex.withLock {
            if (_ready.value) return

            // A leitura legada toca SharedPreferences: fora da main thread,
            // senao a splash trava o primeiro frame.
            withContext(Dispatchers.IO) {
                legacyImportManager.importIfNeeded()
                ensureDefaultVehiclesUseCase()
            }
            _ready.value = true
        }
    }
}
