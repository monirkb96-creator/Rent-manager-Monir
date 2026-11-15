package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Building
import com.example.data.model.Flat
import com.example.data.model.MonthlyBill
import com.example.data.model.Tenant
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class RentRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private suspend fun bootstrapOwner(email: String): String {
        val uid = signInTestUser(email)
        val user = checkNotNull(auth.currentUser)
        val authRepo = AuthRepository(firestore, auth)
        val res = withTimeout(DEFAULT_TIMEOUT_MS) { authRepo.initializeAsOwner(user) }
        assertTrue("Expected owner session, got $res", res is SessionResolution.Authenticated && res.isAdmin)
        return uid
    }

    @Test
    fun owner_saveBuildingAndObserve_succeedsWithRealtimeEmission() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(6)
        bootstrapOwner("owner_$uniqueSuffix@test.com")
        val repo = RentRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveBuilding(
                Building(
                    name = "Emerald Heights $uniqueSuffix",
                    address = "Uttara Sector 7, Dhaka",
                    floorsCount = 7,
                    totalFlats = 14,
                    contactInfo = "01711000000"
                )
            )
        }
        assertTrue("Building creation should succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val bldgId = createResult.getOrThrow()

        val buildings = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeBuildings().first { list -> list.any { it.id == bldgId } }
        }
        assertTrue(buildings.any { it.id == bldgId && it.name.contains(uniqueSuffix) })
    }

    @Test
    fun owner_generateMonthlyBills_isIdempotentAndPreventsDuplicates() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(6)
        val ownerUid = bootstrapOwner("owner_bill_$uniqueSuffix@test.com")
        val repo = RentRepository(firestore, auth)

        val bldgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveBuilding(
                Building(name = "Padma Villa $uniqueSuffix", address = "Mirpur, Dhaka", floorsCount = 5, totalFlats = 10)
            ).getOrThrow()
        }
        val flatId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveFlat(
                Flat(
                    ownerId = ownerUid,
                    buildingId = bldgId,
                    buildingName = "Padma Villa $uniqueSuffix",
                    flatNumber = "2A",
                    floorNumber = 2,
                    monthlyRent = 23000.0,
                    serviceCharge = 3500.0,
                    isOccupied = false
                )
            ).getOrThrow()
        }

        val savedFlat = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeFlats().first { list -> list.any { it.id == flatId } }.first { it.id == flatId }
        }
        val tenantEmail = "tenant_$uniqueSuffix@test.com"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveAndAssignTenant(
                Tenant(name = "Karim Mia", email = tenantEmail, phone = "01812345678", moveInDate = "01-11-2026"),
                savedFlat
            ).getOrThrow()
        }

        val occupiedFlat = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeFlats().first { list -> list.any { it.id == flatId && it.isOccupied } }.first { it.id == flatId }
        }
        val createdFirst = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.generateMonthlyBillsForOccupiedFlats(
                flats = listOf(occupiedFlat),
                existingBills = emptyList(),
                monthKey = "2026-12",
                billingMonth = "12-2026",
                carryForwardUnpaid = false
            ).getOrThrow()
        }
        assertEquals(1, createdFirst)

        val bills = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeBills().first { list -> list.any { it.flatId == flatId && it.monthKey == "2026-12" } }
        }
        val bill = bills.first { it.flatId == flatId && it.monthKey == "2026-12" }
        assertEquals(26500.0, bill.totalBill, 0.01)

        // Calling again with existingBills should generate 0 duplicate bills
        val createdSecond = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.generateMonthlyBillsForOccupiedFlats(
                flats = listOf(occupiedFlat),
                existingBills = bills,
                monthKey = "2026-12",
                billingMonth = "12-2026",
                carryForwardUnpaid = false
            ).getOrThrow()
        }
        assertEquals(0, createdSecond)

        // Calling for the already-initialized month even with empty existingBills is also idempotent
        val createdThird = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.generateMonthlyBillsForOccupiedFlats(
                flats = listOf(occupiedFlat),
                existingBills = emptyList(),
                monthKey = "2026-12",
                billingMonth = "12-2026",
                carryForwardUnpaid = false
            ).getOrThrow()
        }
        assertEquals(0, createdThird)
    }

    @Test
    fun unauthenticatedCaller_observeBuildings_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = RentRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeBuildings().first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected PERMISSION_DENIED exception", caught)
        val firestoreEx = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    @Test
    fun tenantCaller_cannotCreateBuildingOrQueryAllBuildings() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(6)
        bootstrapOwner("owner_sec_$uniqueSuffix@test.com")
        auth.signOut()

        // Sign in a separate tenant user (cannot escalate because settings/app_config already exists)
        signInTestUser("tenant_only_$uniqueSuffix@test.com")
        val tenantRepo = RentRepository(firestore, auth)

        val createBuildingRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            tenantRepo.saveBuilding(
                Building(name = "Unauthorized", address = "Dhaka", floorsCount = 2, totalFlats = 4)
            )
        }
        assertTrue(createBuildingRes.isFailure)
        val ex = createBuildingRes.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)

        var listCaught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                tenantRepo.observeBuildings().first()
            }
        } catch (t: Throwable) {
            listCaught = t
        }
        val listEx = checkNotNull(listCaught?.unwrapFirestoreException())
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, listEx.code)
    }

    @Test
    fun ownerBankSyncAndTenantBankSlipPayment_succeedsEndToEnd() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(6)
        val tenantEmail = "tenant_g2_$uniqueSuffix@test.com"
        val ownerUid = bootstrapOwner("owner_bank_$uniqueSuffix@test.com")
        auth.signOut()

        // Tenant signs in once so users/{tenantUid} exists with their email
        val tenantUid = signInTestUser(tenantEmail)
        val tenantUserInitial = checkNotNull(auth.currentUser)
        val initialTenantSession = withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).resolveUserSession(tenantUserInitial)
        }
        assertTrue(initialTenantSession is SessionResolution.Authenticated && !initialTenantSession.isAdmin)
        auth.signOut()

        // Switch back to Owner to set up building, flat, bank account, and assign tenant
        val reloggedOwnerUid = signInTestUser("owner_bank_$uniqueSuffix@test.com")
        assertEquals(ownerUid, reloggedOwnerUid)
        val ownerRepo = RentRepository(firestore, auth)

        val bldgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.saveBuilding(
                Building(name = "Adrita Manson $uniqueSuffix", address = "Dhaka", floorsCount = 4, totalFlats = 8)
            ).getOrThrow()
        }
        val flatId = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.saveFlat(
                Flat(
                    ownerId = ownerUid,
                    buildingId = bldgId,
                    buildingName = "Adrita Manson $uniqueSuffix",
                    flatNumber = "G2",
                    floorNumber = 0,
                    monthlyRent = 1000.0,
                    serviceCharge = 1000.0,
                    otherCharges = 100.0,
                    isOccupied = false
                )
            ).getOrThrow()
        }
        val bankId = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.addBankAccount(
                com.example.data.model.BankAccount(
                    accountName = "MD ABDUR RASHED MANJU",
                    bankName = "UCBL",
                    accountNumber = "7863241000893151",
                    accountType = "Current"
                )
            ).getOrThrow()
        }

        val savedFlat = withTimeout(FLOW_TIMEOUT_MS) {
            ownerRepo.observeFlats().first { list -> list.any { it.id == flatId } }.first { it.id == flatId }
        }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.saveAndAssignTenant(
                Tenant(name = "Monir Tenant", email = tenantEmail, phone = "01812345678", moveInDate = "15-11-2026"),
                savedFlat
            ).getOrThrow()
        }

        // Switch back to tenant session
        auth.signOut()
        val reloggedTenantUid = signInTestUser(tenantEmail)
        assertEquals(tenantUid, reloggedTenantUid)
        val tenantUser = checkNotNull(auth.currentUser)
        val session = withTimeout(DEFAULT_TIMEOUT_MS) { AuthRepository(firestore, auth).resolveUserSession(tenantUser) }
        assertTrue(session is SessionResolution.Authenticated && !session.isAdmin)

        val tenantRepo = RentRepository(firestore, auth)
        val settings = withTimeout(FLOW_TIMEOUT_MS) {
            tenantRepo.observeOwnerScopedSettings(ownerUid).first { it != null && it.bankAccountsJson.isNotBlank() }
        }!!
        val decodedBanks = com.example.data.model.BankInfoJson.decodeList(settings.bankAccountsJson)
        assertTrue(decodedBanks.any { it.id == bankId && it.accountNumber == "7863241000893151" })

        val tenantFlat = withTimeout(FLOW_TIMEOUT_MS) {
            tenantRepo.observeTenantFlats(tenantUid).first { it.isNotEmpty() }.first()
        }
        val bill = withTimeout(DEFAULT_TIMEOUT_MS) {
            tenantRepo.ensureCurrentMonthBillForTenant(tenantFlat).getOrThrow()
        }
        assertEquals(2100.0, bill.totalBill, 0.01)

        val payId = withTimeout(DEFAULT_TIMEOUT_MS) {
            tenantRepo.submitTenantPaymentRequest(
                bill = bill,
                amount = 2100.0,
                method = "bank_transfer",
                transactionId = "BNK_$uniqueSuffix",
                referenceNumber = "SLIP-$uniqueSuffix",
                notes = "Bank deposit slip uploaded",
                targetBankAccountId = bankId,
                targetBankAccountName = "MD ABDUR RASHED MANJU (UCBL)",
                receiptImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB"
            ).getOrThrow()
        }
        assertTrue(payId.isNotEmpty())
    }

    @Test
    fun multiOwnerIsolation_neighborCreatesIsolatedWorkspaceAndCannotSeeOwner1Data() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(6)
        bootstrapOwner("owner1_iso_$uniqueSuffix@test.com")
        val owner1Repo = RentRepository(firestore, auth)
        val owner1BldgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            owner1Repo.saveBuilding(
                Building(name = "Owner1 Tower $uniqueSuffix", address = "Gulshan", floorsCount = 6, totalFlats = 12)
            ).getOrThrow()
        }
        auth.signOut()

        // Neighbor signs in choosing "admin" role even though settings/app_config already exists
        val neighborEmail = "neighbor_iso_$uniqueSuffix@test.com"
        signInTestUser(neighborEmail)
        val neighborUser = checkNotNull(auth.currentUser)
        val neighborSession = withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).resolveUserSession(neighborUser, preferredRole = "admin")
        }
        assertTrue("Neighbor should resolve as Admin, got $neighborSession", neighborSession is SessionResolution.Authenticated && neighborSession.isAdmin)

        val neighborRepo = RentRepository(firestore, auth)
        val neighborBldgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            neighborRepo.saveBuilding(
                Building(name = "Neighbor Villa $uniqueSuffix", address = "Mirpur", floorsCount = 3, totalFlats = 6)
            ).getOrThrow()
        }
        val neighborBuildings = withTimeout(FLOW_TIMEOUT_MS) {
            neighborRepo.observeBuildings().first { list -> list.any { it.id == neighborBldgId } }
        }
        assertTrue(neighborBuildings.any { it.id == neighborBldgId })
        assertTrue("Owner 1 building leaked into Neighbor stream", neighborBuildings.none { it.id == owner1BldgId })
        auth.signOut()

        // Unassigned tenant who initially resolved as tenant can still upgrade to Owner cleanly
        val upgradeEmail = "upgrade_iso_$uniqueSuffix@test.com"
        signInTestUser(upgradeEmail)
        val upgradeUser = checkNotNull(auth.currentUser)
        val authRepo = AuthRepository(firestore, auth)
        val initialTenant = withTimeout(DEFAULT_TIMEOUT_MS) { authRepo.resolveUserSession(upgradeUser, preferredRole = "tenant") }
        assertTrue(initialTenant is SessionResolution.Authenticated && !initialTenant.isAdmin)
        val upgraded = withTimeout(DEFAULT_TIMEOUT_MS) { authRepo.upgradeUnassignedTenantToOwner(upgradeUser) }
        assertTrue("Unassigned tenant should upgrade to Owner, got $upgraded", upgraded is SessionResolution.Authenticated && upgraded.isAdmin)
    }

    @Test
    fun tenantCashPaymentWithProof_notifiesOwnerAndVerificationCreditsCashLedger() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(6)
        val ownerEmail = "owner_cash_$uniqueSuffix@test.com"
        val tenantEmail = "tenant_cash_$uniqueSuffix@test.com"
        val ownerUid = bootstrapOwner(ownerEmail)
        auth.signOut()

        val tenantUid = signInTestUser(tenantEmail)
        val tenantSession = withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).resolveUserSession(checkNotNull(auth.currentUser), preferredRole = "tenant")
        }
        assertTrue(tenantSession is SessionResolution.Authenticated && !tenantSession.isAdmin)
        auth.signOut()

        signInTestUser(ownerEmail)
        val ownerRepo = RentRepository(firestore, auth)
        val bldgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.saveBuilding(
                Building(name = "Cash Proof House $uniqueSuffix", address = "Uttara", floorsCount = 3, totalFlats = 6)
            ).getOrThrow()
        }
        val flatId = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.saveFlat(
                Flat(
                    ownerId = ownerUid,
                    buildingId = bldgId,
                    buildingName = "Cash Proof House $uniqueSuffix",
                    flatNumber = "1A",
                    floorNumber = 1,
                    monthlyRent = 5000.0,
                    serviceCharge = 500.0,
                    isOccupied = false
                )
            ).getOrThrow()
        }
        val savedFlat = withTimeout(FLOW_TIMEOUT_MS) {
            ownerRepo.observeFlats().first { list -> list.any { it.id == flatId } }.first { it.id == flatId }
        }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.saveAndAssignTenant(
                Tenant(name = "Kamal Tenant", email = tenantEmail, phone = "01799999999", moveInDate = "15-11-2026"),
                savedFlat
            ).getOrThrow()
        }
        auth.signOut()

        // Tenant submits hand-to-hand cash payment with proof image
        signInTestUser(tenantEmail)
        val tenantRepo = RentRepository(firestore, auth)
        val tenantFlat = withTimeout(FLOW_TIMEOUT_MS) {
            tenantRepo.observeTenantFlats(tenantUid).first { it.isNotEmpty() }.first()
        }
        val bill = withTimeout(DEFAULT_TIMEOUT_MS) {
            tenantRepo.ensureCurrentMonthBillForTenant(tenantFlat).getOrThrow()
        }
        val payId = withTimeout(DEFAULT_TIMEOUT_MS) {
            tenantRepo.submitTenantPaymentRequest(
                bill = bill,
                amount = 5500.0,
                method = "cash",
                transactionId = "CSH_$uniqueSuffix",
                referenceNumber = "SLIP-CSH-$uniqueSuffix",
                notes = "Paid cash hand-to-hand",
                receiptImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB"
            ).getOrThrow()
        }
        auth.signOut()

        // Owner sees unread notification and presses OK / Accept Bill
        signInTestUser(ownerEmail)
        val ownerNotifs = withTimeout(FLOW_TIMEOUT_MS) {
            ownerRepo.observeNotifications(ownerUid).first { list ->
                list.any { it.relatedId == payId && it.type == "payment_submitted" && !it.isRead }
            }
        }
        assertTrue(ownerNotifs.any { it.relatedId == payId && !it.isRead })

        val pendingPayment = withTimeout(FLOW_TIMEOUT_MS) {
            ownerRepo.observePayments().first { list -> list.any { it.id == payId } }.first { it.id == payId }
        }
        val currentBill = withTimeout(FLOW_TIMEOUT_MS) {
            ownerRepo.observeBills().first { list -> list.any { it.id == bill.id } }.first { it.id == bill.id }
        }
        val receiptId = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.verifyPendingPayment(pendingPayment, currentBill, null).getOrThrow()
        }
        assertTrue(receiptId.isNotEmpty())

        val cashTxs = withTimeout(FLOW_TIMEOUT_MS) {
            ownerRepo.observeCashTransactions().first { list ->
                list.any { it.transactionType == "cash_in" && it.referenceNumber == "CSH_$uniqueSuffix" }
            }
        }
        assertTrue(cashTxs.any { it.amount == 5500.0 })

        val readNotifs = withTimeout(FLOW_TIMEOUT_MS) {
            ownerRepo.observeNotifications(ownerUid).first { list ->
                list.any { it.relatedId == payId && it.isRead }
            }
        }
        assertTrue(readNotifs.any { it.relatedId == payId && it.isRead })
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
