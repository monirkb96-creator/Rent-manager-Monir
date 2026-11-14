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
                monthKey = "2026-11",
                billingMonth = "11-2026",
                carryForwardUnpaid = false
            ).getOrThrow()
        }
        assertEquals(1, createdFirst)

        val bills = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeBills().first { list -> list.any { it.flatId == flatId && it.monthKey == "2026-11" } }
        }
        val bill = bills.first { it.flatId == flatId && it.monthKey == "2026-11" }
        assertEquals(26500.0, bill.totalBill, 0.01)

        // Calling again with existingBills should generate 0 duplicate bills
        val createdSecond = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.generateMonthlyBillsForOccupiedFlats(
                flats = listOf(occupiedFlat),
                existingBills = bills,
                monthKey = "2026-11",
                billingMonth = "11-2026",
                carryForwardUnpaid = false
            ).getOrThrow()
        }
        assertEquals(0, createdSecond)
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

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
