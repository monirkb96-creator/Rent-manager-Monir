package com.example.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.UserProfile
import com.example.i18n.AppLanguage
import com.example.i18n.EnglishStrings
import com.example.i18n.LocalLanguage
import com.example.i18n.LocalStrings
import com.example.ui.theme.SmartRentTheme
import com.example.ui.viewmodel.AdminDataSnapshot
import com.example.ui.viewmodel.TenantDataSnapshot
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavigationStructureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun adminBottomNavigationBar_displaysAllFiveDestinationsAndNavigates() {
        var latestRoute: String? = null
        val adminProfile = UserProfile(
            id = "admin1",
            userId = "admin1",
            email = "owner@example.com",
            displayName = "Owner",
            role = "admin",
            status = "active"
        )

        composeRule.setContent {
            CompositionLocalProvider(
                LocalLanguage provides AppLanguage.EN,
                LocalStrings provides EnglishStrings
            ) {
                SmartRentTheme {
                    val navController = rememberNavController()
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route ?: AdminDestination.Dashboard.route
                    latestRoute = currentRoute

                    Scaffold(
                        bottomBar = {
                            AdminBottomNavigationBar(
                                currentRoute = currentRoute,
                                pendingVerificationCount = 2,
                                onSelectDestination = { dest ->
                                    navController.navigateSingleTopTab(dest.route)
                                }
                            )
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AdminNavHost(
                                navController = navController,
                                profile = adminProfile,
                                state = AdminDataSnapshot(),
                                onLanguageChange = {},
                                onGenerateMonthlyBills = {},
                                onVerifyPayment = { _, _ -> },
                                onRejectPayment = { _, _ -> },
                                onSaveBuilding = {},
                                onDeleteBuilding = {},
                                onSaveFlat = {},
                                onSaveAndAssignTenant = { _, _ -> },
                                onToggleTenantStatus = {},
                                onVacateTenant = {},
                                onSendPersonalMessage = { _, _, _, _ -> },
                                onSendAnnouncement = { _, _, _, _ -> },
                                onRecordDirectPayment = { _, _, _, _, _, _ -> },
                                onAddBankAccount = {},
                                onRecordBankTx = { _, _, _, _, _ -> },
                                onRecordCashTx = { _, _, _, _ -> },
                                onAddExpense = { _, _ -> },
                                onSaveSettings = {},
                                onBuildBackupJson = { "{}" },
                                onSignOut = {}
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag("admin_bottom_nav_bar").assertIsDisplayed()
        AdminDestination.entries.forEach { dest ->
            composeRule.onNodeWithTag(dest.testTag).assertIsDisplayed()
        }

        composeRule.onNodeWithTag(AdminDestination.Buildings.testTag).performClick()
        composeRule.waitForIdle()
        assertEquals(AdminDestination.Buildings.route, latestRoute)

        composeRule.onNodeWithTag(AdminDestination.Tenants.testTag).performClick()
        composeRule.waitForIdle()
        assertEquals(AdminDestination.Tenants.route, latestRoute)

        composeRule.onNodeWithTag(AdminDestination.Transactions.testTag).performClick()
        composeRule.waitForIdle()
        assertEquals(AdminDestination.Transactions.route, latestRoute)

        composeRule.onNodeWithTag(AdminDestination.Settings.testTag).performClick()
        composeRule.waitForIdle()
        assertEquals(AdminDestination.Settings.route, latestRoute)

        composeRule.onNodeWithTag(AdminDestination.Dashboard.testTag).performClick()
        composeRule.waitForIdle()
        assertEquals(AdminDestination.Dashboard.route, latestRoute)
    }

    @Test
    fun tenantBottomNavigationBar_displaysAllFiveDestinationsAndNavigates() {
        var latestRoute: String? = null
        val tenantProfile = UserProfile(
            id = "tenant1",
            userId = "tenant1",
            email = "tenant@example.com",
            displayName = "Tenant",
            role = "tenant",
            status = "active"
        )

        composeRule.setContent {
            CompositionLocalProvider(
                LocalLanguage provides AppLanguage.EN,
                LocalStrings provides EnglishStrings
            ) {
                SmartRentTheme {
                    val navController = rememberNavController()
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route ?: TenantDestination.Home.route
                    latestRoute = currentRoute

                    Scaffold(
                        bottomBar = {
                            TenantBottomNavigationBar(
                                currentRoute = currentRoute,
                                unpaidBillsCount = 1,
                                unreadMessagesCount = 3,
                                onSelectDestination = { dest ->
                                    navController.navigateSingleTopTab(dest.route)
                                }
                            )
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            TenantNavHost(
                                navController = navController,
                                profile = tenantProfile,
                                state = TenantDataSnapshot(),
                                gatewayResult = null,
                                onDismissGatewayResult = {},
                                onSubmitPayment = { _, _, _, _, _ -> },
                                onMarkMessageRead = {},
                                onReplyToOwner = { _, _ -> },
                                onLanguageChange = {},
                                onSignOut = {}
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag("tenant_bottom_nav_bar").assertIsDisplayed()
        TenantDestination.entries.forEach { dest ->
            composeRule.onNodeWithTag(dest.testTag).assertIsDisplayed()
        }

        composeRule.onNodeWithTag(TenantDestination.MyBills.testTag).performClick()
        composeRule.waitForIdle()
        assertEquals(TenantDestination.MyBills.route, latestRoute)

        composeRule.onNodeWithTag(TenantDestination.Payments.testTag).performClick()
        composeRule.waitForIdle()
        assertEquals(TenantDestination.Payments.route, latestRoute)

        composeRule.onNodeWithTag(TenantDestination.Messages.testTag).performClick()
        composeRule.waitForIdle()
        assertEquals(TenantDestination.Messages.route, latestRoute)

        composeRule.onNodeWithTag(TenantDestination.Profile.testTag).performClick()
        composeRule.waitForIdle()
        assertEquals(TenantDestination.Profile.route, latestRoute)

        composeRule.onNodeWithTag(TenantDestination.Home.testTag).performClick()
        composeRule.waitForIdle()
        assertEquals(TenantDestination.Home.route, latestRoute)
    }
}
