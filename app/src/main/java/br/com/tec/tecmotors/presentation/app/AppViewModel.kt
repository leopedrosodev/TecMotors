package br.com.tec.tecmotors.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tec.tecmotors.domain.usecase.ObserveDarkThemeUseCase
import br.com.tec.tecmotors.domain.usecase.SetDarkThemeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado do shell: tema, destino atual e overlays.
 *
 * O preparo de dados da abertura vive em `core/startup/AppStartup` - a splash
 * do sistema e quem espera por ele, nao esta tela.
 */
class AppViewModel(
    private val observeDarkThemeUseCase: ObserveDarkThemeUseCase,
    private val setDarkThemeUseCase: SetDarkThemeUseCase
) : ViewModel() {
    private val localState = MutableStateFlow(AppUiState())

    val uiState: StateFlow<AppUiState> = combine(
        observeDarkThemeUseCase(),
        localState
    ) { darkTheme, state ->
        state.copy(darkThemeEnabled = darkTheme)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppUiState()
    )

    fun onEvent(event: AppUiEvent) {
        when (event) {
            is AppUiEvent.ToggleTheme -> {
                viewModelScope.launch {
                    setDarkThemeUseCase(!uiState.value.darkThemeEnabled)
                }
            }

            is AppUiEvent.Navigate -> {
                localState.update { it.copy(destination = event.destination) }
            }

            is AppUiEvent.NavigateBack -> {
                localState.update { it.copy(destination = AppDestination.HOME) }
            }

            is AppUiEvent.SetAccountDialogVisible -> {
                localState.update { it.copy(showAccountDialog = event.visible) }
            }

            is AppUiEvent.SetQuickRefuelVisible -> {
                localState.update { it.copy(showQuickRefuel = event.visible) }
            }
        }
    }
}
