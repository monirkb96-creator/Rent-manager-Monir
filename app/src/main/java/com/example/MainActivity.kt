package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.NotificationItem
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.RentRepository
import com.example.data.repository.SessionResolution
import com.example.i18n.AppLanguage
import com.example.i18n.BanglaStrings
import com.example.i18n.EnglishStrings
import com.example.i18n.LanguagePreferences
import com.example.i18n.LocalLanguage
import com.example.i18n.LocalStrings
import com.example.ui.navigation.AdminBottomNavigationBar
import com.example.ui.navigation.AdminDestination
import com.example.ui.navigation.AdminNavHost
import com.example.ui.navigation.AdminNavigationRail
import com.example.ui.navigation.TenantBottomNavigationBar
import com.example.ui.navigation.TenantDestination
import com.example.ui.navigation.TenantNavHost
import com.example.ui.navigation.TenantNavigationRail
import com.example.ui.navigation.navigateSingleTopTab
import com.example.ui.screens.InactiveAccountScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.WorkspaceSetupScreen
import com.example.ui.theme.SmartRentTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.TenantViewModel
import com.example.util.NotificationAlertHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartRentRootApp()
        }
    }
}

@Composable
fun SmartRentRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val languagePrefs = remember { LanguagePreferences(context) }
    val language by languagePrefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.BN)
    val strings = if (language == AppLanguage.BN) BanglaStrings else EnglishStrings

    val databaseId = remember { context.getString(R.string.firestore_database_id) }
    val firestore = remember(databaseId) { FirebaseFirestore.getInstance(databaseId) }
    val authRepository = remember(firestore) { AuthRepository(firestore, Firebase.auth) }
    val credentialManager = remember { CredentialManager.create(context) }

    val authViewModel: AuthViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AuthViewModel(authRepository) }
        }
    )

    var firebaseUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var selectedLoginRole by rememberSaveable { mutableStateOf("tenant") }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            firebaseUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        delay(350L)
        if (Firebase.auth.currentUser == null) {
            authRepository.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { firebaseUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    LaunchedEffect(firebaseUser?.uid) {
        authViewModel.resolveForUser(firebaseUser)
    }

    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalLanguage provides language,
        LocalStrings provides strings
    ) {
        SmartRentTheme {
            val onLangChange: (AppLanguage) -> Unit = { nextLang ->
                scope.launch { languagePrefs.setLanguage(nextLang) }
            }
            val onSignOut: () -> Unit = {
                authRepository.signOut(credentialManager, scope)
            }

            when (val session = sessionState) {
                is SessionResolution.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SessionResolution.Unauthenticated -> {
                    SignInScreen(
                        authRepository = authRepository,
                        credentialManager = credentialManager,
                        selectedRole = selectedLoginRole,
                        onRoleSelected = { role ->
                            selectedLoginRole = role
                            authViewModel.setPreferredRole(role)
                        },
                        onLanguageChange = onLangChange,
                        onAuthSuccess = {
                            authViewModel.setPreferredRole(selectedLoginRole)
                            firebaseUser = Firebase.auth.currentUser
                        }
                    )
                }
                is SessionResolution.NeedsWorkspaceSetup -> {
                    WorkspaceSetupScreen(
                        userEmail = session.user.email.orEmpty(),
                        onSelectOwner = { authViewModel.initializeAsOwner(session.user) },
                        onSelectTenant = { authViewModel.initializeAsTenant(session.user) },
                        onSignOut = onSignOut
                    )
                }
                is SessionResolution.InactiveAccount -> {
                    InactiveAccountScreen(
                        email = session.profile.email,
                        onSignOut = onSignOut
                    )
                }
                is SessionResolution.Error -> {
                    SignInScreen(
                        authRepository = authRepository,
                        credentialManager = credentialManager,
                        selectedRole = selectedLoginRole,
                        onRoleSelected = { role ->
                            selectedLoginRole = role
                            authViewModel.setPreferredRole(role)
                        },
                        onLanguageChange = onLangChange,
                        onAuthSuccess = {
                            authViewModel.setPreferredRole(selectedLoginRole)
                            firebaseUser = Firebase.auth.currentUser
                        }
                    )
                }
                is SessionResolution.Authenticated -> {
                    androidx.compose.runtime.key(session.user.uid, session.isAdmin) {
                        if (session.isAdmin) {
                            AdminShell(
                                currentUserId = session.user.uid,
                                profile = session.profile,
                                onLanguageChange = onLangChange,
                                onSignOut = onSignOut
                            )
                        } else {
                            TenantShell(
                                currentUserId = session.user.uid,
                                profile = session.profile,
                                onLanguageChange = onLangChange,
                                onSwitchToOwner = {
                                    authViewModel.upgradeCurrentTenantToOwner(session.user)
                                },
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminShell(
    currentUserId: String,
    profile: UserProfile,
    onLanguageChange: (AppLanguage) -> Unit,
    onSignOut: () -> Unit
) {
    val viewModel: AdminViewModel = viewModel(
        key = "admin_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                AdminViewModel(RentRepository(db, Firebase.auth), currentUserId)
            }
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    NotificationPermissionRequester()
    NotificationWatcherEffect(
        isLoading = state.isLoading,
        notifications = state.notifications
    )

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AdminDestination.Dashboard.route
    val pendingPaymentsCount = state.pendingPayments.size

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    AdminBottomNavigationBar(
                        currentRoute = currentRoute,
                        pendingVerificationCount = pendingPaymentsCount,
                        onSelectDestination = { destination ->
                            navController.navigateSingleTopTab(destination.route)
                        }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    AdminNavigationRail(
                        currentRoute = currentRoute,
                        pendingVerificationCount = pendingPaymentsCount,
                        onSelectDestination = { destination ->
                            navController.navigateSingleTopTab(destination.route)
                        }
                    )
                }
                AdminNavHost(
                    navController = navController,
                    profile = profile,
                    state = state,
                    onLanguageChange = onLanguageChange,
                    onGenerateMonthlyBills = viewModel::generateMonthlyBills,
                    onVerifyPayment = viewModel::verifyPayment,
                    onRejectPayment = viewModel::rejectPayment,
                    onSaveBuilding = viewModel::saveBuilding,
                    onDeleteBuilding = viewModel::deleteBuilding,
                    onSaveFlat = viewModel::saveFlat,
                    onDeleteFlat = viewModel::deleteFlat,
                    onSaveAndAssignTenant = viewModel::saveAndAssignTenant,
                    onToggleTenantStatus = viewModel::toggleTenantStatus,
                    onVacateTenant = viewModel::vacateTenant,
                    onDeleteTenant = viewModel::deleteTenant,
                    onSendPersonalMessage = viewModel::sendPersonalMessage,
                    onSendAnnouncement = viewModel::sendAnnouncement,
                    onRecordDirectPayment = viewModel::recordDirectPayment,
                    onAddBankAccount = viewModel::addBankAccount,
                    onRecordBankTx = viewModel::recordBankTx,
                    onRecordBankTxWithFlat = viewModel::recordBankTxWithFlat,
                    onRecordCashTx = viewModel::recordCashTx,
                    onAddExpense = viewModel::addExpense,
                    onDeletePayment = viewModel::deletePayment,
                    onDeleteBankAccount = viewModel::deleteBankAccount,
                    onDeleteBankTx = viewModel::deleteBankTx,
                    onDeleteCashTx = viewModel::deleteCashTx,
                    onDeleteExpense = viewModel::deleteExpense,
                    onDeleteReceipt = viewModel::deleteReceipt,
                    onSaveSettings = viewModel::updateSettings,
                    onBuildBackupJson = viewModel::buildBackupJson,
                    onSignOut = onSignOut,
                    onMarkNotificationRead = viewModel::markNotificationRead,
                    onMarkAllNotificationsRead = viewModel::markAllNotificationsRead,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TenantShell(
    currentUserId: String,
    profile: UserProfile,
    onLanguageChange: (AppLanguage) -> Unit,
    onSwitchToOwner: () -> Unit,
    onSignOut: () -> Unit
) {
    val viewModel: TenantViewModel = viewModel(
        key = "tenant_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                TenantViewModel(RentRepository(db, Firebase.auth), currentUserId)
            }
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val gatewayResult by viewModel.gatewayResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    NotificationPermissionRequester()
    NotificationWatcherEffect(
        isLoading = state.isLoading,
        notifications = state.notifications
    )

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: TenantDestination.Home.route
    val unpaidBillsCount = state.displayBills.count { it.remainingDue > 0.0 }
    val unreadMessagesCount = state.messages.count { !it.isRead }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    TenantBottomNavigationBar(
                        currentRoute = currentRoute,
                        unpaidBillsCount = unpaidBillsCount,
                        unreadMessagesCount = unreadMessagesCount,
                        onSelectDestination = { destination ->
                            navController.navigateSingleTopTab(destination.route)
                        }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    TenantNavigationRail(
                        currentRoute = currentRoute,
                        unpaidBillsCount = unpaidBillsCount,
                        unreadMessagesCount = unreadMessagesCount,
                        onSelectDestination = { destination ->
                            navController.navigateSingleTopTab(destination.route)
                        }
                    )
                }
                TenantNavHost(
                    navController = navController,
                    profile = profile,
                    state = state,
                    gatewayResult = gatewayResult,
                    onDismissGatewayResult = viewModel::clearGatewayResult,
                    onSubmitPayment = { bill, amt, method, ref, notes ->
                        viewModel.submitPayment(bill, amt, method, ref, notes)
                    },
                    onSubmitPaymentWithReceipt = viewModel::submitPayment,
                    onMarkMessageRead = viewModel::markMessageRead,
                    onReplyToOwner = viewModel::replyToOwner,
                    onLanguageChange = onLanguageChange,
                    onSwitchToOwner = onSwitchToOwner,
                    onSignOut = onSignOut,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NotificationPermissionRequester() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        NotificationAlertHelper.ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationAlertHelper.hasNotificationPermission(context)
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun NotificationWatcherEffect(
    isLoading: Boolean,
    notifications: List<NotificationItem>
) {
    val context = LocalContext.current
    val seenIds = remember { mutableStateListOf<String>() }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading, notifications) {
        if (isLoading) return@LaunchedEffect
        if (!initialized) {
            seenIds.clear()
            seenIds.addAll(notifications.map { it.id })
            initialized = true
            return@LaunchedEffect
        }
        val newlyArrived = notifications.filter { !it.isRead && it.id !in seenIds }
        newlyArrived.forEach { n ->
            seenIds.add(n.id)
            NotificationAlertHelper.showPaymentNotification(
                context = context,
                notificationIdKey = n.id,
                title = n.title,
                body = n.body
            )
        }
    }
}
