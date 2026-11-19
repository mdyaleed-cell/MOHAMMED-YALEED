package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.firebase.FinanceRepository
import com.example.data.local.LocalGuestRepository
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.preferences.SecurityPreferences
import com.example.data.preferences.SecuritySettings
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ManageCategoriesAndWalletsScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.PlanScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionEntrySheet
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.HisabKhataTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object AppRoutes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_TRANSACTIONS = "transactions"
    const val ROUTE_PLAN = "plan"
    const val ROUTE_REPORTS = "reports"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_MANAGE_CATEGORIES = "manage_categories_wallets"
}

private data class NavTabItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val primaryTabs = listOf(
    NavTabItem(AppRoutes.ROUTE_DASHBOARD, R.string.nav_dashboard, Icons.Rounded.Home, Icons.Outlined.Home),
    NavTabItem(AppRoutes.ROUTE_TRANSACTIONS, R.string.nav_transactions, Icons.AutoMirrored.Rounded.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    NavTabItem(AppRoutes.ROUTE_PLAN, R.string.nav_plan, Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    NavTabItem(AppRoutes.ROUTE_REPORTS, R.string.nav_reports, Icons.Rounded.BarChart, Icons.Outlined.BarChart),
    NavTabItem(AppRoutes.ROUTE_SETTINGS, R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings)
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val securityPreferences = SecurityPreferences(applicationContext)

        setContent {
            val securitySettings by securityPreferences.settingsFlow
                .collectAsStateWithLifecycle(initialValue = SecuritySettings())
            val darkTheme = securitySettings.darkThemeOverride ?: isSystemInDarkTheme()

            HisabKhataTheme(darkTheme = darkTheme) {
                AppRoot(
                    securitySettings = securitySettings,
                    securityPreferences = securityPreferences
                )
            }
        }
    }
}

@Composable
private fun AppRoot(
    securitySettings: SecuritySettings,
    securityPreferences: SecurityPreferences
) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var sessionUnlocked by rememberSaveable { mutableStateOf(false) }
    var showAuthScreen by rememberSaveable { mutableStateOf(false) }

    val user = currentUser
    LaunchedEffect(user) {
        if (user != null) {
            showAuthScreen = false
        }
    }

    if (user == null && showAuthScreen) {
        AuthScreen(
            onAuthSuccess = { showAuthScreen = false },
            onContinueAsGuest = { showAuthScreen = false }
        )
    } else if (securitySettings.pinLockEnabled && securitySettings.pinHash.isNotEmpty() && !sessionUnlocked) {
        PinLockScreen(
            storedPinHash = securitySettings.pinHash,
            biometricEnabled = securitySettings.biometricEnabled,
            verifyPin = securityPreferences::verifyPin,
            onUnlocked = { sessionUnlocked = true }
        )
    } else {
        val activeUserId = user?.uid ?: "guest_local"
        key(activeUserId) {
            AuthenticatedAppShell(
                user = user,
                currentUserId = activeUserId,
                isGuestMode = user == null,
                onSwitchToGoogleSignIn = { showAuthScreen = true },
                securitySettings = securitySettings,
                securityPreferences = securityPreferences
            )
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    user: FirebaseUser?,
    currentUserId: String,
    isGuestMode: Boolean,
    onSwitchToGoogleSignIn: () -> Unit,
    securitySettings: SecuritySettings,
    securityPreferences: SecurityPreferences
) {
    val viewModel: FinanceViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val repository = if (isGuestMode) {
                    LocalGuestRepository(app)
                } else {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    FinanceRepository(db)
                }
                FinanceViewModel(repository, currentUserId)
            }
        }
    )

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val selectedBudgetMonth by viewModel.selectedBudgetMonth.collectAsStateWithLifecycle()
    val isOnline = rememberIsOnline()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoutes.ROUTE_DASHBOARD
    val showPrimaryNav = currentRoute in primaryTabs.map { it.route }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var showTransactionSheet by remember { mutableStateOf(false) }
    var sheetInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingTransaction by remember { mutableStateOf<TransactionRecord?>(null) }
    var planInitialTab by remember { mutableIntStateOf(0) }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (showTransactionSheet) {
        TransactionEntrySheet(
            initialType = sheetInitialType,
            editingTransaction = editingTransaction,
            categories = dashboardState.categories,
            paymentMethods = dashboardState.paymentMethods,
            onDismiss = {
                showTransactionSheet = false
                editingTransaction = null
            },
            onSave = { existingId, type, amount, cat, pm, note, dateMillis ->
                viewModel.saveTransaction(existingId, type, amount, cat, pm, note, dateMillis)
                showTransactionSheet = false
                editingTransaction = null
            },
            onDelete = { id ->
                viewModel.deleteTransaction(id)
                showTransactionSheet = false
                editingTransaction = null
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showPrimaryNav && !useNavRail) {
                    NavigationBar {
                        primaryTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(id = tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute == AppRoutes.ROUTE_DASHBOARD || currentRoute == AppRoutes.ROUTE_TRANSACTIONS) {
                    FloatingActionButton(
                        onClick = {
                            editingTransaction = null
                            sheetInitialType = TransactionType.EXPENSE
                            showTransactionSheet = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_transaction")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(id = R.string.cd_add_transaction)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showPrimaryNav && useNavRail) {
                    NavigationRail {
                        primaryTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(id = tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.ROUTE_DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AppRoutes.ROUTE_DASHBOARD) {
                        DashboardScreen(
                            state = dashboardState,
                            isOnline = isOnline,
                            isGuestMode = isGuestMode,
                            onSwitchToGoogleSignIn = onSwitchToGoogleSignIn,
                            onQuickAdd = { type ->
                                editingTransaction = null
                                sheetInitialType = type
                                showTransactionSheet = true
                            },
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                sheetInitialType = tx.parsedType
                                showTransactionSheet = true
                            },
                            onOpenTransactions = { navigateToTab(AppRoutes.ROUTE_TRANSACTIONS) },
                            onOpenPlan = { tabIdx ->
                                planInitialTab = tabIdx
                                navigateToTab(AppRoutes.ROUTE_PLAN)
                            },
                            onOpenManageCategoriesAndWallets = {
                                navController.navigate(AppRoutes.ROUTE_MANAGE_CATEGORIES)
                            }
                        )
                    }

                    composable(AppRoutes.ROUTE_TRANSACTIONS) {
                        TransactionsScreen(
                            state = dashboardState,
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                sheetInitialType = tx.parsedType
                                showTransactionSheet = true
                            }
                        )
                    }

                    composable(AppRoutes.ROUTE_PLAN) {
                        PlanScreen(
                            state = dashboardState,
                            selectedBudgetMonth = selectedBudgetMonth,
                            initialTab = planInitialTab,
                            viewModel = viewModel
                        )
                    }

                    composable(AppRoutes.ROUTE_REPORTS) {
                        ReportsScreen(
                            state = dashboardState,
                            onFeedback = { msg -> viewModel.notifyMessage(msg) }
                        )
                    }

                    composable(AppRoutes.ROUTE_SETTINGS) {
                        SettingsScreen(
                            user = user,
                            isOnline = isOnline,
                            hasPendingSync = dashboardState.hasPendingSync,
                            isGuestMode = isGuestMode,
                            onSwitchToGoogleSignIn = onSwitchToGoogleSignIn,
                            securitySettings = securitySettings,
                            securityPreferences = securityPreferences,
                            onOpenManageCategoriesAndWallets = {
                                navController.navigate(AppRoutes.ROUTE_MANAGE_CATEGORIES)
                            },
                            onFeedback = { msg -> viewModel.notifyMessage(msg) }
                        )
                    }

                    composable(AppRoutes.ROUTE_MANAGE_CATEGORIES) {
                        ManageCategoriesAndWalletsScreen(
                            state = dashboardState,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    val cm = remember(context) {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    fun checkConnected(): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    var online by remember { mutableStateOf(checkConnected()) }

    DisposableEffect(cm) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                online = true
            }

            override fun onLost(network: Network) {
                online = checkConnected()
            }
        }
        runCatching { cm.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }

    return online
}
