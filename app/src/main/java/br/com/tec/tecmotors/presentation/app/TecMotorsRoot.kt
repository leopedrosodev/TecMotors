package br.com.tec.tecmotors.presentation.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.tec.tecmotors.AppTopBarTitle
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.core.common.simpleViewModelFactory
import br.com.tec.tecmotors.core.di.AppContainer
import br.com.tec.tecmotors.presentation.account.AccountSyncDialog
import br.com.tec.tecmotors.presentation.account.AccountSyncUiEvent
import br.com.tec.tecmotors.presentation.account.AccountSyncViewModel
import br.com.tec.tecmotors.presentation.common.UiFeedback
import br.com.tec.tecmotors.presentation.home.HomeScreen
import br.com.tec.tecmotors.presentation.home.HomeViewModel
import br.com.tec.tecmotors.presentation.maintenance.MaintenanceScreen
import br.com.tec.tecmotors.presentation.maintenance.MaintenanceViewModel
import br.com.tec.tecmotors.presentation.refuels.FuelCalculatorScreen
import br.com.tec.tecmotors.presentation.refuels.FuelCalculatorViewModel
import br.com.tec.tecmotors.presentation.refuels.QuickRefuelSheet
import br.com.tec.tecmotors.presentation.refuels.RefuelsScreen
import br.com.tec.tecmotors.presentation.refuels.RefuelsUiEvent
import br.com.tec.tecmotors.presentation.refuels.RefuelsViewModel
import br.com.tec.tecmotors.presentation.reports.ReportsScreen
import br.com.tec.tecmotors.presentation.reports.ReportsViewModel
import br.com.tec.tecmotors.presentation.vehicles.VehiclesScreen
import br.com.tec.tecmotors.presentation.vehicles.VehiclesViewModel
import br.com.tec.tecmotors.resolveGoogleClientId
import br.com.tec.tecmotors.ui.theme.FeedbackSuccess
import br.com.tec.tecmotors.ui.theme.TecMotorsTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TecMotorsRoot(
    appContainer: AppContainer,
    openQuickRefuelOnStart: Boolean = false,
    openOdometerOnStart: Boolean = false
) {
    val appViewModel: AppViewModel = viewModel(
        factory = remember(appContainer) {
            simpleViewModelFactory {
                AppViewModel(
                    observeDarkThemeUseCase = appContainer.observeDarkThemeUseCase,
                    setDarkThemeUseCase = appContainer.setDarkThemeUseCase
                )
            }
        }
    )
    val homeViewModel: HomeViewModel = viewModel(
        factory = remember(appContainer) {
            simpleViewModelFactory {
                HomeViewModel(
                    observeVehiclesUseCase = appContainer.observeVehiclesUseCase,
                    observeRefuelsUseCase = appContainer.observeRefuelsUseCase,
                    observeOdometersUseCase = appContainer.observeOdometersUseCase,
                    observeMaintenanceUseCase = appContainer.observeMaintenanceUseCase,
                    observeSettingsUseCase = appContainer.observeSettingsUseCase,
                    calculatePeriodReportUseCase = appContainer.calculatePeriodReportUseCase,
                    decideRemindersUseCase = appContainer.decideRemindersUseCase
                )
            }
        }
    )
    val vehiclesViewModel: VehiclesViewModel = viewModel(
        factory = remember(appContainer) {
            simpleViewModelFactory {
                VehiclesViewModel(
                    observeVehiclesUseCase = appContainer.observeVehiclesUseCase,
                    observeOdometersUseCase = appContainer.observeOdometersUseCase,
                    renameVehicleUseCase = appContainer.renameVehicleUseCase,
                    addOdometerUseCase = appContainer.addOdometerUseCase,
                    addVehicleUseCase = appContainer.addVehicleUseCase
                )
            }
        }
    )
    val refuelsViewModel: RefuelsViewModel = viewModel(
        factory = remember(appContainer) {
            simpleViewModelFactory {
                RefuelsViewModel(
                    observeVehiclesUseCase = appContainer.observeVehiclesUseCase,
                    observeRefuelsUseCase = appContainer.observeRefuelsUseCase,
                    observeOdometersUseCase = appContainer.observeOdometersUseCase,
                    addRefuelUseCase = appContainer.addRefuelUseCase,
                    updateRefuelUseCase = appContainer.updateRefuelUseCase,
                    deleteRefuelUseCase = appContainer.deleteRefuelUseCase
                )
            }
        }
    )
    val maintenanceViewModel: MaintenanceViewModel = viewModel(
        factory = remember(appContainer) {
            simpleViewModelFactory {
                MaintenanceViewModel(
                    observeVehiclesUseCase = appContainer.observeVehiclesUseCase,
                    observeOdometersUseCase = appContainer.observeOdometersUseCase,
                    observeMaintenanceUseCase = appContainer.observeMaintenanceUseCase,
                    addMaintenanceUseCase = appContainer.addMaintenanceUseCase,
                    setMaintenanceDoneUseCase = appContainer.setMaintenanceDoneUseCase,
                    calculateMaintenanceStatusUseCase = appContainer.calculateMaintenanceStatusUseCase,
                    calculateComponentHealthUseCase = appContainer.calculateComponentHealthUseCase
                )
            }
        }
    )
    val reportsViewModel: ReportsViewModel = viewModel(
        factory = remember(appContainer) {
            simpleViewModelFactory {
                ReportsViewModel(
                    observeVehiclesUseCase = appContainer.observeVehiclesUseCase,
                    observeRefuelsUseCase = appContainer.observeRefuelsUseCase,
                    observeOdometersUseCase = appContainer.observeOdometersUseCase,
                    observeMaintenanceUseCase = appContainer.observeMaintenanceUseCase,
                    observeSettingsUseCase = appContainer.observeSettingsUseCase,
                    setMonthlyBudgetUseCase = appContainer.setMonthlyBudgetUseCase,
                    calculatePeriodReportUseCase = appContainer.calculatePeriodReportUseCase,
                    calculateMonthlyMetricsUseCase = appContainer.calculateMonthlyMetricsUseCase,
                    calculateCostPerKmMetricsUseCase = appContainer.calculateCostPerKmMetricsUseCase,
                    calculateVehicleSummaryUseCase = appContainer.calculateVehicleSummaryUseCase
                )
            }
        }
    )
    val calculatorViewModel: FuelCalculatorViewModel = viewModel(
        factory = remember(appContainer) {
            simpleViewModelFactory {
                FuelCalculatorViewModel(
                    observeVehiclesUseCase = appContainer.observeVehiclesUseCase,
                    observeRefuelsUseCase = appContainer.observeRefuelsUseCase,
                    observeOdometersUseCase = appContainer.observeOdometersUseCase,
                    calculateVehicleSummaryUseCase = appContainer.calculateVehicleSummaryUseCase
                )
            }
        }
    )
    val accountSyncViewModel: AccountSyncViewModel = viewModel(
        factory = remember(appContainer) {
            simpleViewModelFactory {
                AccountSyncViewModel(
                    signInWithGoogleUseCase = appContainer.signInWithGoogleUseCase,
                    signOutUseCase = appContainer.signOutUseCase,
                    uploadLocalStateUseCase = appContainer.uploadLocalStateUseCase,
                    downloadRemoteStateUseCase = appContainer.downloadRemoteStateUseCase,
                    syncNowUseCase = appContainer.syncNowUseCase,
                    currentSyncUserUseCase = appContainer.currentSyncUserUseCase,
                    getLocalSnapshotUseCase = appContainer.getLocalSnapshotUseCase,
                    restoreLocalSnapshotUseCase = appContainer.restoreLocalSnapshotUseCase
                )
            }
        }
    )

    val appState by appViewModel.uiState.collectAsStateWithLifecycle()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val vehiclesState by vehiclesViewModel.uiState.collectAsStateWithLifecycle()
    val refuelsState by refuelsViewModel.uiState.collectAsStateWithLifecycle()
    val maintenanceState by maintenanceViewModel.uiState.collectAsStateWithLifecycle()
    val reportsState by reportsViewModel.uiState.collectAsStateWithLifecycle()
    val calculatorState by calculatorViewModel.uiState.collectAsStateWithLifecycle()
    val accountState by accountSyncViewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode != Activity.RESULT_OK || activityResult.data == null) {
            accountSyncViewModel.onEvent(
                AccountSyncUiEvent.SetStatusMessage(context.getString(R.string.feedback_login_cancelled))
            )
            return@rememberLauncherForActivityResult
        }

        val account = runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)
                .getResult(ApiException::class.java)
        }.getOrElse {
            accountSyncViewModel.onEvent(
                AccountSyncUiEvent.SetStatusMessage(
                    context.getString(R.string.feedback_login_failed, it.message.orEmpty())
                )
            )
            return@rememberLauncherForActivityResult
        }

        val idToken = account.idToken
        if (idToken.isNullOrBlank()) {
            accountSyncViewModel.onEvent(
                AccountSyncUiEvent.SetStatusMessage(context.getString(R.string.feedback_missing_id_token))
            )
            return@rememberLauncherForActivityResult
        }

        accountSyncViewModel.onEvent(AccountSyncUiEvent.SubmitGoogleIdToken(idToken))
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Atalhos vindos das acoes de notificacao.
    LaunchedEffect(openQuickRefuelOnStart, openOdometerOnStart) {
        when {
            openQuickRefuelOnStart ->
                appViewModel.onEvent(AppUiEvent.SetQuickRefuelVisible(true))

            openOdometerOnStart ->
                appViewModel.onEvent(AppUiEvent.Navigate(AppDestination.VEHICLES))
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var currentSnackbarFeedback by remember { mutableStateOf<UiFeedback?>(null) }

    LaunchedEffect(vehiclesViewModel) {
        vehiclesViewModel.events.collect { feedback ->
            currentSnackbarFeedback = feedback
            snackbarHostState.showSnackbar(feedback.message)
        }
    }
    LaunchedEffect(refuelsViewModel) {
        refuelsViewModel.events.collect { feedback ->
            currentSnackbarFeedback = feedback
            snackbarHostState.showSnackbar(feedback.message)
        }
    }
    LaunchedEffect(maintenanceViewModel) {
        maintenanceViewModel.events.collect { feedback ->
            currentSnackbarFeedback = feedback
            snackbarHostState.showSnackbar(feedback.message)
        }
    }

    TecMotorsTheme(darkTheme = appState.darkThemeEnabled) {
        val destination = appState.destination

        BackHandler(enabled = destination != AppDestination.HOME) {
            appViewModel.onEvent(AppUiEvent.NavigateBack)
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = currentSnackbarFeedback.containerColor(),
                        contentColor = currentSnackbarFeedback.contentColor()
                    )
                }
            },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        if (!destination.isPrimary) {
                            IconButton(onClick = { appViewModel.onEvent(AppUiEvent.NavigateBack) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        }
                    },
                    title = {
                        if (destination == AppDestination.HOME) {
                            AppTopBarTitle()
                        } else {
                            Text(stringResource(destination.titleRes))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            appViewModel.onEvent(AppUiEvent.SetAccountDialogVisible(true))
                        }) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = stringResource(R.string.content_desc_account_sync),
                                tint = if (accountState.userEmail != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        IconButton(onClick = { appViewModel.onEvent(AppUiEvent.ToggleTheme) }) {
                            Icon(
                                imageVector = if (appState.darkThemeEnabled) {
                                    Icons.Filled.LightMode
                                } else {
                                    Icons.Filled.DarkMode
                                },
                                contentDescription = if (appState.darkThemeEnabled) {
                                    stringResource(R.string.content_desc_theme_light)
                                } else {
                                    stringResource(R.string.content_desc_theme_dark)
                                },
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (destination.isPrimary) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        AppDestination.primaryDestinations.forEach { item ->
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = { appViewModel.onEvent(AppUiEvent.Navigate(item)) },
                                icon = {
                                    Icon(
                                        imageVector = destinationIcon(item),
                                        contentDescription = null
                                    )
                                },
                                label = { Text(stringResource(item.titleRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (destination.showsQuickRefuel) {
                    ExtendedFloatingActionButton(
                        onClick = { appViewModel.onEvent(AppUiEvent.SetQuickRefuelVisible(true)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Text(
                            text = stringResource(R.string.action_quick_refuel),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (destination) {
                    AppDestination.HOME -> HomeScreen(
                        state = homeState,
                        onEvent = homeViewModel::onEvent,
                        onOpenMaintenance = {
                            appViewModel.onEvent(AppUiEvent.Navigate(AppDestination.MAINTENANCE))
                        },
                        onOpenVehicles = {
                            appViewModel.onEvent(AppUiEvent.Navigate(AppDestination.VEHICLES))
                        },
                        onOpenCalculator = {
                            appViewModel.onEvent(AppUiEvent.Navigate(AppDestination.CALCULATOR))
                        },
                        onOpenRefuelHistory = {
                            appViewModel.onEvent(AppUiEvent.Navigate(AppDestination.REFUEL_HISTORY))
                        }
                    )

                    AppDestination.MAINTENANCE -> MaintenanceScreen(
                        state = maintenanceState,
                        onEvent = maintenanceViewModel::onEvent
                    )

                    AppDestination.REPORTS -> ReportsScreen(
                        state = reportsState,
                        onEvent = reportsViewModel::onEvent
                    )

                    AppDestination.VEHICLES -> VehiclesScreen(
                        state = vehiclesState,
                        onEvent = vehiclesViewModel::onEvent
                    )

                    AppDestination.CALCULATOR -> FuelCalculatorScreen(
                        state = calculatorState,
                        onEvent = calculatorViewModel::onEvent
                    )

                    AppDestination.REFUEL_HISTORY -> RefuelsScreen(
                        state = refuelsState,
                        onEvent = refuelsViewModel::onEvent,
                        onEditRefuel = { recordId ->
                            refuelsViewModel.onEvent(RefuelsUiEvent.StartEditing(recordId))
                            appViewModel.onEvent(AppUiEvent.SetQuickRefuelVisible(true))
                        }
                    )
                }

                if (appState.showAccountDialog) {
                    AccountSyncDialog(
                        state = accountState,
                        onDismiss = {
                            appViewModel.onEvent(AppUiEvent.SetAccountDialogVisible(false))
                        },
                        onLoginGoogle = {
                            val clientId = resolveGoogleClientId(context)
                            if (clientId == null) {
                                accountSyncViewModel.onEvent(
                                    AccountSyncUiEvent.SetStatusMessage(
                                        context.getString(R.string.feedback_missing_google_config)
                                    )
                                )
                                return@AccountSyncDialog
                            }

                            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestIdToken(clientId)
                                .build()
                            val signInClient = GoogleSignIn.getClient(context, options)
                            googleSignInLauncher.launch(signInClient.signInIntent)
                        },
                        onEvent = accountSyncViewModel::onEvent,
                        onBuildBackupJson = { accountSyncViewModel.buildBackupJson() },
                        onImportBackupJson = { raw -> accountSyncViewModel.importBackupJson(raw) },
                        runBusyAction = { block -> accountSyncViewModel.withBusy(block) }
                    )
                }
            }
        }

        if (appState.showQuickRefuel) {
            QuickRefuelSheet(
                state = refuelsState,
                onEvent = refuelsViewModel::onEvent,
                onDismiss = {
                    refuelsViewModel.onEvent(RefuelsUiEvent.DiscardQuickRefuel)
                    appViewModel.onEvent(AppUiEvent.SetQuickRefuelVisible(false))
                }
            )
        }
    }
}

private fun destinationIcon(destination: AppDestination): ImageVector = when (destination) {
    AppDestination.HOME -> Icons.Filled.Home
    AppDestination.MAINTENANCE -> Icons.Filled.Build
    AppDestination.REPORTS -> Icons.Filled.BarChart
    AppDestination.VEHICLES -> Icons.Filled.DirectionsCar
    AppDestination.CALCULATOR -> Icons.Filled.Calculate
    AppDestination.REFUEL_HISTORY -> Icons.Filled.LocalGasStation
}

@Composable
private fun UiFeedback?.containerColor(): Color {
    return when (this) {
        is UiFeedback.Error -> MaterialTheme.colorScheme.errorContainer
        is UiFeedback.Success -> FeedbackSuccess
        is UiFeedback.Info, null -> MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun UiFeedback?.contentColor(): Color {
    return when (this) {
        is UiFeedback.Error -> MaterialTheme.colorScheme.onErrorContainer
        is UiFeedback.Success -> MaterialTheme.colorScheme.onPrimary
        is UiFeedback.Info, null -> MaterialTheme.colorScheme.onPrimary
    }
}
