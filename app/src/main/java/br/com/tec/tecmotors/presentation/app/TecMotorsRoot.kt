package br.com.tec.tecmotors.presentation.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.tec.tecmotors.AppTopBarTitle
import br.com.tec.tecmotors.AppVersionBadge
import br.com.tec.tecmotors.IntroPresentationScreen
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.core.common.simpleViewModelFactory
import br.com.tec.tecmotors.core.di.AppContainer
import br.com.tec.tecmotors.presentation.account.AccountSyncDialog
import br.com.tec.tecmotors.presentation.account.AccountSyncUiEvent
import br.com.tec.tecmotors.presentation.account.AccountSyncViewModel
import br.com.tec.tecmotors.presentation.maintenance.MaintenanceScreen
import br.com.tec.tecmotors.presentation.maintenance.MaintenanceViewModel
import br.com.tec.tecmotors.presentation.refuels.FuelCalculatorScreen
import br.com.tec.tecmotors.presentation.refuels.RefuelsScreen
import br.com.tec.tecmotors.presentation.refuels.RefuelsViewModel
import br.com.tec.tecmotors.presentation.reports.ReportsScreen
import br.com.tec.tecmotors.presentation.reports.ReportsViewModel
import br.com.tec.tecmotors.presentation.vehicles.VehiclesScreen
import br.com.tec.tecmotors.presentation.vehicles.VehiclesViewModel
import br.com.tec.tecmotors.resolveGoogleClientId
import br.com.tec.tecmotors.ui.theme.TecMotorsTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private enum class AppTab(val titleRes: Int) {
    REFUELS(R.string.tab_refuels),
    MAINTENANCE(R.string.tab_maintenance),
    REPORTS(R.string.tab_reports),
    CALCULATOR(R.string.tab_calculator),
    VEHICLES(R.string.tab_vehicles)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TecMotorsRoot(appContainer: AppContainer) {
    val appViewModel: AppViewModel = viewModel(
        factory = remember(appContainer) {
            simpleViewModelFactory {
                AppViewModel(
                    observeDarkThemeUseCase = appContainer.observeDarkThemeUseCase,
                    setDarkThemeUseCase = appContainer.setDarkThemeUseCase,
                    ensureDefaultVehiclesUseCase = appContainer.ensureDefaultVehiclesUseCase,
                    legacyImportManager = appContainer.legacyImportManager
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
                    addRefuelUseCase = appContainer.addRefuelUseCase
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
    val vehiclesState by vehiclesViewModel.uiState.collectAsStateWithLifecycle()
    val refuelsState by refuelsViewModel.uiState.collectAsStateWithLifecycle()
    val maintenanceState by maintenanceViewModel.uiState.collectAsStateWithLifecycle()
    val reportsState by reportsViewModel.uiState.collectAsStateWithLifecycle()
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

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vehiclesState.feedback) {
        vehiclesState.feedback?.let {
            snackbarHostState.showSnackbar(it)
            vehiclesViewModel.onEvent(
                br.com.tec.tecmotors.presentation.vehicles.VehiclesUiEvent.ClearFeedback
            )
        }
    }
    LaunchedEffect(refuelsState.feedback) {
        refuelsState.feedback?.let {
            snackbarHostState.showSnackbar(it)
            refuelsViewModel.onEvent(
                br.com.tec.tecmotors.presentation.refuels.RefuelsUiEvent.ClearFeedback
            )
        }
    }
    LaunchedEffect(maintenanceState.feedback) {
        maintenanceState.feedback?.let {
            snackbarHostState.showSnackbar(it)
            maintenanceViewModel.onEvent(
                br.com.tec.tecmotors.presentation.maintenance.MaintenanceUiEvent.ClearFeedback
            )
        }
    }

    TecMotorsTheme(darkTheme = appState.darkThemeEnabled) {
        if (appState.showIntro) {
            IntroPresentationScreen(isDarkTheme = appState.darkThemeEnabled)
            return@TecMotorsTheme
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        AppTopBarTitle(isDarkTheme = appState.darkThemeEnabled)
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
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    val tabs = AppTab.entries
                    val selectedTab = appState.selectedTabIndex.coerceIn(0, tabs.lastIndex)

                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 8.dp
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { appViewModel.onEvent(AppUiEvent.SelectTab(index)) },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = {
                                    Text(
                                        text = stringResource(tab.titleRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }

                    when (tabs[selectedTab]) {
                        AppTab.VEHICLES -> VehiclesScreen(
                            state = vehiclesState,
                            onEvent = vehiclesViewModel::onEvent
                        )

                        AppTab.REFUELS -> RefuelsScreen(
                            state = refuelsState,
                            onEvent = refuelsViewModel::onEvent
                        )

                        AppTab.MAINTENANCE -> MaintenanceScreen(
                            state = maintenanceState,
                            viewModel = maintenanceViewModel,
                            onEvent = maintenanceViewModel::onEvent
                        )

                        AppTab.REPORTS -> ReportsScreen(
                            state = reportsState,
                            onEvent = reportsViewModel::onEvent
                        )

                        AppTab.CALCULATOR -> FuelCalculatorScreen()
                    }
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

                AppVersionBadge(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp)
                )
            }
        }
    }
}
