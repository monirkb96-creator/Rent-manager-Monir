package com.example.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.data.model.AppSettings
import com.example.data.model.BankAccount
import com.example.data.model.BankTransaction
import com.example.data.model.Building
import com.example.data.model.CashTransaction
import com.example.data.model.Expense
import com.example.data.model.Flat
import com.example.data.model.MonthlyBill
import com.example.data.model.PaymentRecord
import com.example.data.model.Receipt
import com.example.data.model.Tenant
import com.example.data.model.UserProfile
import com.example.data.remote.GatewayInitiationResult
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.LocalStrings
import com.example.ui.screens.AdminBuildingsScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminSettingsScreen
import com.example.ui.screens.AdminTenantsScreen
import com.example.ui.screens.AdminTransactionsScreen
import com.example.ui.screens.TenantBillsScreen
import com.example.ui.screens.TenantHomeScreen
import com.example.ui.screens.TenantMessagesScreen
import com.example.ui.screens.TenantPaymentsScreen
import com.example.ui.screens.TenantProfileScreen
import com.example.ui.viewmodel.AdminDataSnapshot
import com.example.ui.viewmodel.TenantDataSnapshot

object AppRoutes {
    const val ROUTE_ADMIN_DASHBOARD = "admin_dashboard"
    const val ROUTE_ADMIN_BUILDINGS = "admin_buildings"
    const val ROUTE_ADMIN_TENANTS = "admin_tenants"
    const val ROUTE_ADMIN_TRANSACTIONS = "admin_transactions"
    const val ROUTE_ADMIN_SETTINGS = "admin_settings"

    const val ROUTE_TENANT_HOME = "tenant_home"
    const val ROUTE_TENANT_MY_BILLS = "tenant_my_bills"
    const val ROUTE_TENANT_PAYMENTS = "tenant_payments"
    const val ROUTE_TENANT_MESSAGES = "tenant_messages"
    const val ROUTE_TENANT_PROFILE = "tenant_profile"
}

enum class AdminDestination(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val labelOf: (AppStrings) -> String
) {
    Dashboard(
        route = AppRoutes.ROUTE_ADMIN_DASHBOARD,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "admin_nav_dashboard",
        labelOf = { it.navDashboard }
    ),
    Buildings(
        route = AppRoutes.ROUTE_ADMIN_BUILDINGS,
        selectedIcon = Icons.Filled.Apartment,
        unselectedIcon = Icons.Outlined.Apartment,
        testTag = "admin_nav_buildings",
        labelOf = { it.navBuildings }
    ),
    Tenants(
        route = AppRoutes.ROUTE_ADMIN_TENANTS,
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People,
        testTag = "admin_nav_tenants",
        labelOf = { it.navTenants }
    ),
    Transactions(
        route = AppRoutes.ROUTE_ADMIN_TRANSACTIONS,
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
        testTag = "admin_nav_transactions",
        labelOf = { it.navTransactions }
    ),
    Settings(
        route = AppRoutes.ROUTE_ADMIN_SETTINGS,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "admin_nav_settings",
        labelOf = { it.navSettings }
    )
}

enum class TenantDestination(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val labelOf: (AppStrings) -> String
) {
    Home(
        route = AppRoutes.ROUTE_TENANT_HOME,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "tenant_nav_home",
        labelOf = { it.navHome }
    ),
    MyBills(
        route = AppRoutes.ROUTE_TENANT_MY_BILLS,
        selectedIcon = Icons.Filled.Receipt,
        unselectedIcon = Icons.Outlined.Receipt,
        testTag = "tenant_nav_my_bills",
        labelOf = { it.navMyBills }
    ),
    Payments(
        route = AppRoutes.ROUTE_TENANT_PAYMENTS,
        selectedIcon = Icons.Filled.Payments,
        unselectedIcon = Icons.Outlined.Payments,
        testTag = "tenant_nav_payments",
        labelOf = { it.navPayments }
    ),
    Messages(
        route = AppRoutes.ROUTE_TENANT_MESSAGES,
        selectedIcon = Icons.Filled.Email,
        unselectedIcon = Icons.Outlined.Email,
        testTag = "tenant_nav_messages",
        labelOf = { it.navMessages }
    ),
    Profile(
        route = AppRoutes.ROUTE_TENANT_PROFILE,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "tenant_nav_profile",
        labelOf = { it.navProfile }
    )
}

fun NavHostController.navigateSingleTopTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AdminBottomNavigationBar(
    currentRoute: String?,
    pendingVerificationCount: Int = 0,
    onSelectDestination: (AdminDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    NavigationBar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("admin_bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        AdminDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            val label = destination.labelOf(strings)
            val showBadge = destination == AdminDestination.Transactions && pendingVerificationCount > 0
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectDestination(destination) },
                icon = {
                    NavigationIconWithOptionalBadge(
                        icon = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = label,
                        badgeCount = if (showBadge) pendingVerificationCount else 0
                    )
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(destination.testTag)
            )
        }
    }
}

@Composable
fun TenantBottomNavigationBar(
    currentRoute: String?,
    unpaidBillsCount: Int = 0,
    unreadMessagesCount: Int = 0,
    onSelectDestination: (TenantDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    NavigationBar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("tenant_bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        TenantDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            val label = destination.labelOf(strings)
            val badgeCount = when (destination) {
                TenantDestination.MyBills -> unpaidBillsCount
                TenantDestination.Messages -> unreadMessagesCount
                else -> 0
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectDestination(destination) },
                icon = {
                    NavigationIconWithOptionalBadge(
                        icon = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = label,
                        badgeCount = badgeCount
                    )
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(destination.testTag)
            )
        }
    }
}

@Composable
fun AdminNavigationRail(
    currentRoute: String?,
    pendingVerificationCount: Int = 0,
    onSelectDestination: (AdminDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    NavigationRail(
        modifier = modifier.testTag("admin_navigation_rail"),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        AdminDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            val label = destination.labelOf(strings)
            val badgeCount = if (destination == AdminDestination.Transactions) pendingVerificationCount else 0
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectDestination(destination) },
                icon = {
                    NavigationIconWithOptionalBadge(
                        icon = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = label,
                        badgeCount = badgeCount
                    )
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("${destination.testTag}_rail")
            )
        }
    }
}

@Composable
fun TenantNavigationRail(
    currentRoute: String?,
    unpaidBillsCount: Int = 0,
    unreadMessagesCount: Int = 0,
    onSelectDestination: (TenantDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    NavigationRail(
        modifier = modifier.testTag("tenant_navigation_rail"),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        TenantDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            val label = destination.labelOf(strings)
            val badgeCount = when (destination) {
                TenantDestination.MyBills -> unpaidBillsCount
                TenantDestination.Messages -> unreadMessagesCount
                else -> 0
            }
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectDestination(destination) },
                icon = {
                    NavigationIconWithOptionalBadge(
                        icon = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = label,
                        badgeCount = badgeCount
                    )
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("${destination.testTag}_rail")
            )
        }
    }
}

@Composable
private fun NavigationIconWithOptionalBadge(
    icon: ImageVector,
    contentDescription: String,
    badgeCount: Int
) {
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    } else {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

fun NavDestinationMatchesRoute(
    navBackStackEntry: androidx.navigation.NavBackStackEntry?,
    route: String
): Boolean {
    return navBackStackEntry?.destination?.hierarchy?.any { it.route == route } == true
}

@Composable
fun AdminNavHost(
    navController: NavHostController,
    profile: UserProfile,
    state: AdminDataSnapshot,
    onLanguageChange: (AppLanguage) -> Unit,
    onGenerateMonthlyBills: (Boolean) -> Unit,
    onVerifyPayment: (PaymentRecord, BankAccount?) -> Unit,
    onRejectPayment: (PaymentRecord, String) -> Unit,
    onSaveBuilding: (Building) -> Unit,
    onDeleteBuilding: (Building) -> Unit,
    onSaveFlat: (Flat) -> Unit,
    onDeleteFlat: (Flat) -> Unit = {},
    onSaveAndAssignTenant: (Tenant, Flat) -> Unit,
    onToggleTenantStatus: (Tenant) -> Unit,
    onVacateTenant: (Tenant) -> Unit,
    onDeleteTenant: (Tenant) -> Unit = {},
    onSendPersonalMessage: (Tenant, String, String, String) -> Unit,
    onSendAnnouncement: (String, String, String, Building?) -> Unit,
    onRecordDirectPayment: (MonthlyBill, Double, String, String, BankAccount?, String) -> Unit,
    onAddBankAccount: (BankAccount) -> Unit,
    onRecordBankTx: (BankAccount, Double, String, String, String) -> Unit,
    onRecordBankTxWithFlat: (BankAccount, Double, String, String, String, String, String) -> Unit = { acc, amt, type, desc, ref, _, _ ->
        onRecordBankTx(acc, amt, type, desc, ref)
    },
    onRecordCashTx: (Double, String, String, String) -> Unit,
    onAddExpense: (Expense, BankAccount?) -> Unit,
    onDeletePayment: (PaymentRecord) -> Unit = {},
    onDeleteBankAccount: (BankAccount) -> Unit = {},
    onDeleteBankTx: (BankTransaction) -> Unit = {},
    onDeleteCashTx: (CashTransaction) -> Unit = {},
    onDeleteExpense: (Expense) -> Unit = {},
    onDeleteReceipt: (Receipt) -> Unit = {},
    onSaveSettings: (AppSettings) -> Unit,
    onBuildBackupJson: () -> String,
    onSignOut: () -> Unit,
    onMarkNotificationRead: (String) -> Unit = {},
    onMarkAllNotificationsRead: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AdminDestination.Dashboard.route,
        modifier = modifier.fillMaxSize(),
        enterTransition = { fadeIn(animationSpec = tween(180)) },
        exitTransition = { fadeOut(animationSpec = tween(180)) }
    ) {
        composable(route = AdminDestination.Dashboard.route) {
            AdminDashboardScreen(
                state = state,
                onLanguageChange = onLanguageChange,
                onGenerateMonthlyBills = onGenerateMonthlyBills,
                onVerifyPayment = onVerifyPayment,
                onRejectPayment = onRejectPayment,
                onNavigateToBuildings = {
                    navController.navigateSingleTopTab(AdminDestination.Buildings.route)
                },
                onNavigateToTransactions = {
                    navController.navigateSingleTopTab(AdminDestination.Transactions.route)
                },
                onMarkNotificationRead = onMarkNotificationRead,
                onMarkAllNotificationsRead = onMarkAllNotificationsRead
            )
        }
        composable(route = AdminDestination.Buildings.route) {
            AdminBuildingsScreen(
                state = state,
                onSaveBuilding = onSaveBuilding,
                onDeleteBuilding = onDeleteBuilding,
                onSaveFlat = onSaveFlat,
                onDeleteFlat = onDeleteFlat,
                onAssignTenantToFlat = onSaveAndAssignTenant
            )
        }
        composable(route = AdminDestination.Tenants.route) {
            AdminTenantsScreen(
                state = state,
                onSaveAndAssignTenant = onSaveAndAssignTenant,
                onToggleStatus = onToggleTenantStatus,
                onVacateTenant = onVacateTenant,
                onDeleteTenant = onDeleteTenant,
                onSendPersonalMessage = onSendPersonalMessage,
                onSendAnnouncement = onSendAnnouncement
            )
        }
        composable(route = AdminDestination.Transactions.route) {
            AdminTransactionsScreen(
                state = state,
                onVerifyPayment = onVerifyPayment,
                onRejectPayment = onRejectPayment,
                onRecordDirectPayment = onRecordDirectPayment,
                onAddBankAccount = onAddBankAccount,
                onRecordBankTx = onRecordBankTx,
                onRecordBankTxWithFlat = onRecordBankTxWithFlat,
                onRecordCashTx = onRecordCashTx,
                onAddExpense = onAddExpense,
                onDeletePayment = onDeletePayment,
                onDeleteBankAccount = onDeleteBankAccount,
                onDeleteBankTx = onDeleteBankTx,
                onDeleteCashTx = onDeleteCashTx,
                onDeleteExpense = onDeleteExpense,
                onDeleteReceipt = onDeleteReceipt
            )
        }
        composable(route = AdminDestination.Settings.route) {
            AdminSettingsScreen(
                profile = profile,
                state = state,
                onLanguageChange = onLanguageChange,
                onSaveSettings = onSaveSettings,
                onBuildBackupJson = onBuildBackupJson,
                onSignOut = onSignOut
            )
        }
    }
}

@Composable
fun TenantNavHost(
    navController: NavHostController,
    profile: UserProfile,
    state: TenantDataSnapshot,
    gatewayResult: GatewayInitiationResult?,
    onDismissGatewayResult: () -> Unit,
    onSubmitPayment: (MonthlyBill, Double, String, String, String) -> Unit,
    onSubmitPaymentWithReceipt: (MonthlyBill, Double, String, String, String, String, String, String) -> Unit = { bill, amt, method, ref, notes, _, _, _ ->
        onSubmitPayment(bill, amt, method, ref, notes)
    },
    onMarkMessageRead: (String) -> Unit,
    onReplyToOwner: (String, String) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onSwitchToOwner: (() -> Unit)? = null,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = TenantDestination.Home.route,
        modifier = modifier.fillMaxSize(),
        enterTransition = { fadeIn(animationSpec = tween(180)) },
        exitTransition = { fadeOut(animationSpec = tween(180)) }
    ) {
        composable(route = TenantDestination.Home.route) {
            TenantHomeScreen(
                profile = profile,
                state = state,
                gatewayResult = gatewayResult,
                onDismissGatewayResult = onDismissGatewayResult,
                onSubmitPayment = onSubmitPaymentWithReceipt,
                onLanguageChange = onLanguageChange
            )
        }
        composable(route = TenantDestination.MyBills.route) {
            TenantBillsScreen(
                state = state,
                onSubmitPayment = onSubmitPaymentWithReceipt
            )
        }
        composable(route = TenantDestination.Payments.route) {
            TenantPaymentsScreen(
                state = state,
                onSubmitPayment = onSubmitPaymentWithReceipt
            )
        }
        composable(route = TenantDestination.Messages.route) {
            TenantMessagesScreen(
                state = state,
                onMarkRead = onMarkMessageRead,
                onReplyToOwner = onReplyToOwner
            )
        }
        composable(route = TenantDestination.Profile.route) {
            TenantProfileScreen(
                profile = profile,
                state = state,
                onLanguageChange = onLanguageChange,
                onSwitchToOwner = onSwitchToOwner,
                onSignOut = onSignOut
            )
        }
    }
}
