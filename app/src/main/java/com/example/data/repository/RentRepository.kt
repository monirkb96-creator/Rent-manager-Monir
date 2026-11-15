package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.AuditLog
import com.example.data.model.BankAccount
import com.example.data.model.BankInfoJson
import com.example.data.model.BankTransaction
import com.example.data.model.Building
import com.example.data.model.CashTransaction
import com.example.data.model.Expense
import com.example.data.model.Flat
import com.example.data.model.MessageItem
import com.example.data.model.MonthlyBill
import com.example.data.model.NotificationItem
import com.example.data.model.PaymentRecord
import com.example.data.model.Receipt
import com.example.data.model.Tenant
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.util.FormatUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class RentRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun currentEmail(): String = auth.currentUser?.email.orEmpty()

    private inline fun <reified T> observeCollection(
        path: String,
        queryBuilder: (FirebaseFirestore) -> com.google.firebase.firestore.Query
    ): Flow<List<T>> {
        val uid = auth.currentUser?.uid
        return queryBuilder(db)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    // --- ADMIN REALTIME STREAMS (STRICTLY SCOPED TO CURRENT OWNER) ---
    private fun currentUidOrEmpty(): String = auth.currentUser?.uid.orEmpty()

    fun observeBuildings(): Flow<List<Building>> =
        observeCollection("buildings") {
            it.collection("buildings").whereEqualTo("ownerId", currentUidOrEmpty())
        }

    fun observeFlats(): Flow<List<Flat>> =
        observeCollection("flats") {
            it.collection("flats").whereEqualTo("ownerId", currentUidOrEmpty())
        }

    fun observeTenants(): Flow<List<Tenant>> =
        observeCollection("tenants") {
            it.collection("tenants").whereEqualTo("ownerId", currentUidOrEmpty())
        }

    fun observeBills(): Flow<List<MonthlyBill>> =
        observeCollection("monthly_bills") {
            it.collection("monthly_bills").whereEqualTo("ownerId", currentUidOrEmpty())
        }

    fun observePayments(): Flow<List<PaymentRecord>> =
        observeCollection("payments") {
            it.collection("payments").whereEqualTo("ownerId", currentUidOrEmpty())
        }

    fun observeBankAccounts(): Flow<List<BankAccount>> =
        observeCollection("bank_accounts") {
            it.collection("bank_accounts").whereEqualTo("ownerId", currentUidOrEmpty())
        }

    fun observeBankTransactions(): Flow<List<BankTransaction>> =
        observeCollection("bank_transactions") {
            it.collection("bank_transactions").whereEqualTo("ownerId", currentUidOrEmpty())
        }

    fun observeCashTransactions(): Flow<List<CashTransaction>> =
        observeCollection("cash_transactions") {
            it.collection("cash_transactions").whereEqualTo("ownerId", currentUidOrEmpty())
        }

    fun observeExpenses(): Flow<List<Expense>> =
        observeCollection("expenses") {
            it.collection("expenses").whereEqualTo("ownerId", currentUidOrEmpty())
        }

    fun observeMessages(): Flow<List<MessageItem>> =
        observeCollection("messages") {
            it.collection("messages").whereEqualTo("ownerId", currentUidOrEmpty())
        }

    fun observeAnnouncements(): Flow<List<Announcement>> =
        observeCollection("announcements") {
            it.collection("announcements").whereEqualTo("ownerId", currentUidOrEmpty())
        }

    fun observeNotifications(uid: String): Flow<List<NotificationItem>> =
        observeCollection("notifications") {
            it.collection("notifications").whereEqualTo("recipientUid", uid)
        }

    fun observeReceipts(): Flow<List<Receipt>> =
        observeCollection("receipts") {
            it.collection("receipts").whereEqualTo("ownerId", currentUidOrEmpty())
        }

    fun observeAuditLogs(): Flow<List<AuditLog>> =
        observeCollection("audit_logs") {
            it.collection("audit_logs").whereEqualTo("ownerId", currentUidOrEmpty())
        }

    fun observeAppSettings(): Flow<AppSettings?> =
        db.collection("settings").document("app_config")
            .snapshots()
            .map { snap ->
                snap.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                handleFirestoreError(firestoreError ?: error, OperationType.GET, "settings/app_config")
                throw error
            }

    private fun observeSettingsDoc(docId: String): Flow<AppSettings?> =
        db.collection("settings").document(docId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(null)
            }

    fun observeOwnerScopedSettings(ownerUid: String): Flow<AppSettings?> {
        if (ownerUid.isBlank()) return flowOf(null)
        return combine(
            observeSettingsDoc("owner_$ownerUid"),
            observeSettingsDoc("app_config")
        ) { ownerDoc, legacyDoc ->
            ownerDoc ?: legacyDoc?.takeIf { it.ownerUid == ownerUid }
        }
    }

    // --- TENANT SCOPED REALTIME STREAMS ---
    fun observeTenantFlats(uid: String): Flow<List<Flat>> =
        observeCollection("flats") {
            it.collection("flats").whereEqualTo("tenantUserId", uid)
        }

    fun observeTenantBills(uid: String): Flow<List<MonthlyBill>> =
        observeCollection("monthly_bills") {
            it.collection("monthly_bills").whereEqualTo("tenantUserId", uid)
        }

    fun observeTenantPayments(uid: String): Flow<List<PaymentRecord>> =
        observeCollection("payments") {
            it.collection("payments").whereEqualTo("tenantUserId", uid)
        }

    fun observeTenantReceipts(uid: String): Flow<List<Receipt>> =
        observeCollection("receipts") {
            it.collection("receipts").whereEqualTo("tenantUserId", uid)
        }

    fun observeTenantMessages(uid: String): Flow<List<MessageItem>> =
        observeCollection("messages") {
            it.collection("messages").whereEqualTo("recipientUid", uid)
        }

    fun observeBroadcastAnnouncements(): Flow<List<Announcement>> =
        observeCollection("announcements") {
            it.collection("announcements").whereEqualTo("targetScope", "all")
        }

    fun observeBroadcastAnnouncementsForOwner(ownerId: String): Flow<List<Announcement>> {
        if (ownerId.isBlank()) return flowOf(emptyList())
        return observeCollection<Announcement>("announcements") {
            it.collection("announcements").whereEqualTo("ownerId", ownerId)
        }.map { list -> list.filter { it.targetScope == "all" } }
    }

    // --- AUDIT LOG HELPER ---
    private suspend fun writeAuditLog(
        ownerId: String,
        role: String,
        action: String,
        entityType: String,
        entityId: String,
        summary: String
    ) {
        val uid = requireUserId()
        val ref = db.collection("audit_logs").document()
        val payload = mapOf(
            "ownerId" to ownerId,
            "actorUid" to uid,
            "actorEmail" to currentEmail(),
            "actorRole" to role,
            "action" to action,
            "entityType" to entityType,
            "entityId" to entityId,
            "summary" to summary.take(480),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            ref.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
        }
    }

    // --- BUILDING CRUD ---
    suspend fun saveBuilding(building: Building): Result<String> = runCatching {
        val uid = requireUserId()
        val isNew = building.id.isBlank()
        val ref = if (isNew) db.collection("buildings").document()
        else db.collection("buildings").document(building.id)

        if (isNew) {
            val payload = mapOf(
                "ownerId" to uid,
                "name" to building.name.trim(),
                "address" to building.address.trim(),
                "floorsCount" to building.floorsCount.coerceAtLeast(1),
                "totalFlats" to building.totalFlats.coerceAtLeast(1),
                "contactInfo" to building.contactInfo.trim(),
                "notes" to building.notes.trim(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            writeAuditLog(uid, "admin", "CREATE_BUILDING", "building", ref.id, "Created building ${building.name}")
        } else {
            val payload = mapOf(
                "name" to building.name.trim(),
                "address" to building.address.trim(),
                "floorsCount" to building.floorsCount.coerceAtLeast(1),
                "totalFlats" to building.totalFlats.coerceAtLeast(1),
                "contactInfo" to building.contactInfo.trim(),
                "notes" to building.notes.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.update(payload).await()
            writeAuditLog(uid, "admin", "UPDATE_BUILDING", "building", ref.id, "Updated building ${building.name}")
        }
        ref.id
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "buildings")
    }

    suspend fun deleteBuilding(building: Building): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(building.id.isNotBlank())
        db.collection("buildings").document(building.id).delete().await()
        writeAuditLog(uid, "admin", "DELETE_BUILDING", "building", building.id, "Deleted building ${building.name}")
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "buildings/${building.id}")
    }

    // --- FLAT CRUD ---
    suspend fun saveFlat(flat: Flat): Result<String> = runCatching {
        val uid = requireUserId()
        val isNew = flat.id.isBlank()
        val ref = if (isNew) db.collection("flats").document()
        else db.collection("flats").document(flat.id)

        val totalBill = flat.monthlyRent + flat.serviceCharge + flat.electricityBill +
            flat.waterBill + flat.gasBill + flat.otherCharges
        val remaining = (totalBill - flat.amountPaid).coerceAtLeast(0.0)
        val status = when {
            !flat.isOccupied -> "vacant"
            remaining <= 0.0 && totalBill > 0.0 -> "paid"
            flat.amountPaid > 0.0 -> "partial"
            else -> "unpaid"
        }

        if (isNew) {
            val payload = mapOf(
                "ownerId" to uid,
                "buildingId" to flat.buildingId,
                "buildingName" to flat.buildingName,
                "flatNumber" to flat.flatNumber.trim(),
                "floorNumber" to flat.floorNumber,
                "monthlyRent" to flat.monthlyRent,
                "serviceCharge" to flat.serviceCharge,
                "electricityBill" to flat.electricityBill,
                "waterBill" to flat.waterBill,
                "gasBill" to flat.gasBill,
                "otherCharges" to flat.otherCharges,
                "totalMonthlyBill" to totalBill,
                "amountPaid" to flat.amountPaid,
                "remainingDue" to remaining,
                "paymentStatus" to status,
                "tenantId" to flat.tenantId,
                "tenantUserId" to flat.tenantUserId,
                "tenantEmail" to flat.tenantEmail,
                "tenantName" to flat.tenantName,
                "tenantPhone" to flat.tenantPhone,
                "isOccupied" to flat.isOccupied,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            writeAuditLog(uid, "admin", "CREATE_FLAT", "flat", ref.id, "Created flat ${flat.flatNumber} in ${flat.buildingName}")
        } else {
            val payload = mapOf(
                "buildingId" to flat.buildingId,
                "buildingName" to flat.buildingName,
                "flatNumber" to flat.flatNumber.trim(),
                "floorNumber" to flat.floorNumber,
                "monthlyRent" to flat.monthlyRent,
                "serviceCharge" to flat.serviceCharge,
                "electricityBill" to flat.electricityBill,
                "waterBill" to flat.waterBill,
                "gasBill" to flat.gasBill,
                "otherCharges" to flat.otherCharges,
                "totalMonthlyBill" to totalBill,
                "amountPaid" to flat.amountPaid,
                "remainingDue" to remaining,
                "paymentStatus" to status,
                "tenantId" to flat.tenantId,
                "tenantUserId" to flat.tenantUserId,
                "tenantEmail" to flat.tenantEmail,
                "tenantName" to flat.tenantName,
                "tenantPhone" to flat.tenantPhone,
                "isOccupied" to flat.isOccupied,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.update(payload).await()
            writeAuditLog(uid, "admin", "UPDATE_FLAT", "flat", ref.id, "Updated flat ${flat.flatNumber}")
        }
        ref.id
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "flats")
    }

    // --- TENANT MANAGEMENT (ASSIGN, TRANSFER, VACATE, ACTIVATE/DEACTIVATE) ---
    suspend fun saveAndAssignTenant(
        tenant: Tenant,
        targetFlat: Flat
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val isNew = tenant.id.isBlank()
        val tenantRef = if (isNew) db.collection("tenants").document()
        else db.collection("tenants").document(tenant.id)
        val flatRef = db.collection("flats").document(targetFlat.id)

        // Check if a registered user already exists with this email to link tenantUserId immediately
        val normalizedEmail = tenant.email.trim()
        var resolvedTenantUserId = tenant.tenantUserId
        if (resolvedTenantUserId.isBlank() && normalizedEmail.isNotBlank()) {
            val existingUsers = try {
                db.collection("users").whereEqualTo("email", normalizedEmail).get().await()
            } catch (_: Exception) {
                null
            }
            resolvedTenantUserId = existingUsers?.documents?.firstOrNull()?.id.orEmpty()
        }

        val batch = db.batch()
        if (isNew) {
            batch.set(
                tenantRef,
                mapOf(
                    "ownerId" to uid,
                    "tenantUserId" to resolvedTenantUserId,
                    "name" to tenant.name.trim(),
                    "email" to normalizedEmail,
                    "phone" to tenant.phone.trim(),
                    "nidNumber" to tenant.nidNumber.trim(),
                    "buildingId" to targetFlat.buildingId,
                    "buildingName" to targetFlat.buildingName,
                    "flatId" to targetFlat.id,
                    "flatNumber" to targetFlat.flatNumber,
                    "status" to tenant.status.ifBlank { "active" },
                    "moveInDate" to tenant.moveInDate.ifBlank { FormatUtils.todayDdMmYyyy() },
                    "moveOutDate" to "",
                    "notes" to tenant.notes.trim(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            // If transferring from an old flat, mark old flat vacant
            if (tenant.flatId.isNotBlank() && tenant.flatId != targetFlat.id) {
                val oldFlatRef = db.collection("flats").document(tenant.flatId)
                batch.update(
                    oldFlatRef,
                    mapOf(
                        "isOccupied" to false,
                        "tenantId" to "",
                        "tenantUserId" to "",
                        "tenantEmail" to "",
                        "tenantName" to "",
                        "tenantPhone" to "",
                        "paymentStatus" to "vacant",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.update(
                tenantRef,
                mapOf(
                    "tenantUserId" to resolvedTenantUserId,
                    "name" to tenant.name.trim(),
                    "email" to normalizedEmail,
                    "phone" to tenant.phone.trim(),
                    "nidNumber" to tenant.nidNumber.trim(),
                    "buildingId" to targetFlat.buildingId,
                    "buildingName" to targetFlat.buildingName,
                    "flatId" to targetFlat.id,
                    "flatNumber" to targetFlat.flatNumber,
                    "status" to tenant.status,
                    "moveInDate" to tenant.moveInDate,
                    "notes" to tenant.notes.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        val totalBill = targetFlat.monthlyRent + targetFlat.serviceCharge +
            targetFlat.electricityBill + targetFlat.waterBill +
            targetFlat.gasBill + targetFlat.otherCharges
        val remaining = (totalBill - targetFlat.amountPaid).coerceAtLeast(0.0)
        val payStatus = when {
            remaining <= 0.0 && totalBill > 0.0 -> "paid"
            targetFlat.amountPaid > 0.0 -> "partial"
            else -> "unpaid"
        }

        batch.update(
            flatRef,
            mapOf(
                "isOccupied" to true,
                "tenantId" to tenantRef.id,
                "tenantUserId" to resolvedTenantUserId,
                "tenantEmail" to normalizedEmail,
                "tenantName" to tenant.name.trim(),
                "tenantPhone" to tenant.phone.trim(),
                "totalMonthlyBill" to totalBill,
                "remainingDue" to remaining,
                "paymentStatus" to payStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        if (resolvedTenantUserId.isNotBlank()) {
            batch.update(
                db.collection("users").document(resolvedTenantUserId),
                mapOf(
                    "tenantId" to tenantRef.id,
                    "buildingId" to targetFlat.buildingId,
                    "flatId" to targetFlat.id,
                    "phone" to tenant.phone.trim(),
                    "status" to if (tenant.status == "inactive") "inactive" else "active",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        val currentMonthKey = FormatUtils.currentMonthKey()
        val currentBillingMonth = FormatUtils.currentBillingMonth()
        val currentBillId = "${targetFlat.id}_$currentMonthKey"
        val currentBillRef = db.collection("monthly_bills").document(currentBillId)
        val existingBillSnap = try {
            currentBillRef.get().await()
        } catch (_: Exception) {
            null
        }
        if (tenant.status != "vacated" && totalBill > 0.0 && existingBillSnap?.exists() != true) {
            batch.set(
                currentBillRef,
                mapOf(
                    "ownerId" to uid,
                    "buildingId" to targetFlat.buildingId,
                    "buildingName" to targetFlat.buildingName,
                    "flatId" to targetFlat.id,
                    "flatNumber" to targetFlat.flatNumber,
                    "tenantId" to tenantRef.id,
                    "tenantUserId" to resolvedTenantUserId,
                    "tenantEmail" to normalizedEmail,
                    "tenantName" to tenant.name.trim(),
                    "billingMonth" to currentBillingMonth,
                    "monthKey" to currentMonthKey,
                    "monthlyRent" to targetFlat.monthlyRent,
                    "serviceCharge" to targetFlat.serviceCharge,
                    "electricityBill" to targetFlat.electricityBill,
                    "waterBill" to targetFlat.waterBill,
                    "gasBill" to targetFlat.gasBill,
                    "otherCharges" to targetFlat.otherCharges,
                    "previousDueCarriedForward" to 0.0,
                    "totalBill" to totalBill,
                    "amountPaid" to targetFlat.amountPaid,
                    "remainingDue" to remaining,
                    "status" to payStatus,
                    "dueDate" to "10-$currentBillingMonth",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.commit().await()
        writeAuditLog(
            uid,
            "admin",
            if (isNew) "ASSIGN_TENANT" else "UPDATE_TENANT",
            "tenant",
            tenantRef.id,
            "${tenant.name} assigned to ${targetFlat.buildingName} ${targetFlat.flatNumber}"
        )
        tenantRef.id
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "tenants")
    }

    suspend fun toggleTenantStatus(tenant: Tenant): Result<Unit> = runCatching {
        val uid = requireUserId()
        val newStatus = if (tenant.status == "active") "inactive" else "active"
        val batch = db.batch()
        batch.update(
            db.collection("tenants").document(tenant.id),
            mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp())
        )
        if (tenant.tenantUserId.isNotBlank()) {
            batch.update(
                db.collection("users").document(tenant.tenantUserId),
                mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp())
            )
        }
        batch.commit().await()
        writeAuditLog(uid, "admin", "TOGGLE_TENANT_STATUS", "tenant", tenant.id, "${tenant.name} -> $newStatus")
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "tenants/${tenant.id}")
    }

    suspend fun vacateTenant(tenant: Tenant): Result<Unit> = runCatching {
        val uid = requireUserId()
        val batch = db.batch()
        batch.update(
            db.collection("tenants").document(tenant.id),
            mapOf(
                "status" to "vacated",
                "moveOutDate" to FormatUtils.todayDdMmYyyy(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (tenant.flatId.isNotBlank()) {
            batch.update(
                db.collection("flats").document(tenant.flatId),
                mapOf(
                    "isOccupied" to false,
                    "tenantId" to "",
                    "tenantUserId" to "",
                    "tenantEmail" to "",
                    "tenantName" to "",
                    "tenantPhone" to "",
                    "paymentStatus" to "vacant",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        writeAuditLog(uid, "admin", "VACATE_TENANT", "tenant", tenant.id, "Vacated ${tenant.name} from flat ${tenant.flatNumber}")
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "tenants/${tenant.id}")
    }

    suspend fun deleteFlat(flat: Flat): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(flat.id.isNotBlank())
        db.collection("flats").document(flat.id).delete().await()
        writeAuditLog(uid, "admin", "DELETE_FLAT", "flat", flat.id, "Deleted flat ${flat.buildingName} ${flat.flatNumber}")
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "flats/${flat.id}")
    }

    suspend fun deleteTenant(tenant: Tenant): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(tenant.id.isNotBlank())
        val batch = db.batch()
        if (tenant.flatId.isNotBlank() && tenant.status != "vacated") {
            val flatRef = db.collection("flats").document(tenant.flatId)
            val flatSnap = flatRef.get().await()
            if (flatSnap.exists()) {
                batch.update(
                    flatRef,
                    mapOf(
                        "isOccupied" to false,
                        "tenantId" to "",
                        "tenantUserId" to "",
                        "tenantEmail" to "",
                        "tenantName" to "",
                        "tenantPhone" to "",
                        "paymentStatus" to "vacant",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }
        if (tenant.tenantUserId.isNotBlank()) {
            val userRef = db.collection("users").document(tenant.tenantUserId)
            val userSnap = userRef.get().await()
            if (userSnap.exists()) {
                batch.update(
                    userRef,
                    mapOf(
                        "tenantId" to "",
                        "buildingId" to "",
                        "flatId" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }
        batch.delete(db.collection("tenants").document(tenant.id))
        batch.commit().await()
        writeAuditLog(uid, "admin", "DELETE_TENANT", "tenant", tenant.id, "Deleted tenant ${tenant.name}")
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "tenants/${tenant.id}")
    }

    // --- AUTOMATIC MONTHLY BILL GENERATION (IDEMPOTENT / NO DUPLICATES) ---
    suspend fun generateMonthlyBillsForOccupiedFlats(
        flats: List<Flat>,
        existingBills: List<MonthlyBill>,
        monthKey: String,
        billingMonth: String,
        carryForwardUnpaid: Boolean
    ): Result<Int> = runCatching {
        val uid = requireUserId()
        val occupiedFlats = flats.filter { it.isOccupied && it.tenantId.isNotBlank() }
        var createdCount = 0
        val batch = db.batch()
        val dueDate = "10-$billingMonth"

        for (flat in occupiedFlats) {
            val billId = "${flat.id}_$monthKey"
            val billRef = db.collection("monthly_bills").document(billId)
            val alreadyExists = existingBills.any { it.id == billId } ||
                (try { billRef.get().await().exists() } catch (_: Exception) { false })
            if (alreadyExists) continue

            val previousDue = if (carryForwardUnpaid) {
                existingBills
                    .filter { it.flatId == flat.id && it.monthKey < monthKey && it.remainingDue > 0.0 }
                    .sumOf { it.remainingDue }
            } else 0.0

            val totalBill = flat.monthlyRent + flat.serviceCharge +
                flat.electricityBill + flat.waterBill +
                flat.gasBill + flat.otherCharges + previousDue

            batch.set(
                billRef,
                mapOf(
                    "ownerId" to uid,
                    "buildingId" to flat.buildingId,
                    "buildingName" to flat.buildingName,
                    "flatId" to flat.id,
                    "flatNumber" to flat.flatNumber,
                    "tenantId" to flat.tenantId,
                    "tenantUserId" to flat.tenantUserId,
                    "tenantEmail" to flat.tenantEmail,
                    "tenantName" to flat.tenantName,
                    "billingMonth" to billingMonth,
                    "monthKey" to monthKey,
                    "monthlyRent" to flat.monthlyRent,
                    "serviceCharge" to flat.serviceCharge,
                    "electricityBill" to flat.electricityBill,
                    "waterBill" to flat.waterBill,
                    "gasBill" to flat.gasBill,
                    "otherCharges" to flat.otherCharges,
                    "previousDueCarriedForward" to previousDue,
                    "totalBill" to totalBill,
                    "amountPaid" to 0.0,
                    "remainingDue" to totalBill,
                    "status" to "unpaid",
                    "dueDate" to dueDate,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.update(
                db.collection("flats").document(flat.id),
                mapOf(
                    "totalMonthlyBill" to totalBill,
                    "amountPaid" to 0.0,
                    "remainingDue" to totalBill,
                    "paymentStatus" to "unpaid",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (flat.tenantUserId.isNotBlank() || flat.tenantEmail.isNotBlank()) {
                val notifRef = db.collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "ownerId" to uid,
                        "recipientUid" to flat.tenantUserId,
                        "recipientEmail" to flat.tenantEmail,
                        "title" to "Monthly Bill Generated ($billingMonth)",
                        "body" to "Flat ${flat.flatNumber} bill is BDT ${totalBill.toInt()}. Due date: $dueDate.",
                        "type" to "bill_generated",
                        "relatedId" to billId,
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            createdCount++
        }

        if (createdCount > 0) {
            batch.commit().await()
            writeAuditLog(uid, "admin", "GENERATE_BILLS", "monthly_bill", monthKey, "Generated $createdCount monthly bills for $billingMonth")
        }
        createdCount
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "monthly_bills")
    }

    suspend fun ensureCurrentMonthBillForTenant(flat: Flat): Result<MonthlyBill> = runCatching {
        val uid = requireUserId()
        val monthKey = FormatUtils.currentMonthKey()
        val billingMonth = FormatUtils.currentBillingMonth()
        val billId = "${flat.id}_$monthKey"
        val billRef = db.collection("monthly_bills").document(billId)
        val existing = billRef.get().await()
        if (existing.exists()) {
            existing.toObject(MonthlyBill::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.copy(id = billId)
                ?: MonthlyBill(
                    id = billId,
                    ownerId = flat.ownerId,
                    buildingId = flat.buildingId,
                    buildingName = flat.buildingName,
                    flatId = flat.id,
                    flatNumber = flat.flatNumber,
                    tenantId = flat.tenantId,
                    tenantUserId = flat.tenantUserId.ifBlank { uid },
                    tenantEmail = flat.tenantEmail.ifBlank { currentEmail() },
                    tenantName = flat.tenantName,
                    billingMonth = billingMonth,
                    monthKey = monthKey,
                    monthlyRent = flat.monthlyRent,
                    serviceCharge = flat.serviceCharge,
                    electricityBill = flat.electricityBill,
                    waterBill = flat.waterBill,
                    gasBill = flat.gasBill,
                    otherCharges = flat.otherCharges,
                    previousDueCarriedForward = 0.0,
                    totalBill = flat.totalMonthlyBill,
                    amountPaid = flat.amountPaid,
                    remainingDue = (flat.totalMonthlyBill - flat.amountPaid).coerceAtLeast(0.0),
                    status = "unpaid",
                    dueDate = "10-$billingMonth"
                )
        } else {
            val totalBill = flat.totalMonthlyBill
            val dueDate = "10-$billingMonth"
            billRef.set(
                mapOf(
                    "ownerId" to flat.ownerId,
                    "buildingId" to flat.buildingId,
                    "buildingName" to flat.buildingName,
                    "flatId" to flat.id,
                    "flatNumber" to flat.flatNumber,
                    "tenantId" to flat.tenantId.ifBlank { flat.id },
                    "tenantUserId" to uid,
                    "tenantEmail" to flat.tenantEmail.ifBlank { currentEmail() },
                    "tenantName" to flat.tenantName,
                    "billingMonth" to billingMonth,
                    "monthKey" to monthKey,
                    "monthlyRent" to flat.monthlyRent,
                    "serviceCharge" to flat.serviceCharge,
                    "electricityBill" to flat.electricityBill,
                    "waterBill" to flat.waterBill,
                    "gasBill" to flat.gasBill,
                    "otherCharges" to flat.otherCharges,
                    "previousDueCarriedForward" to 0.0,
                    "totalBill" to totalBill,
                    "amountPaid" to 0.0,
                    "remainingDue" to totalBill,
                    "status" to "unpaid",
                    "dueDate" to dueDate,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            MonthlyBill(
                id = billId,
                ownerId = flat.ownerId,
                buildingId = flat.buildingId,
                buildingName = flat.buildingName,
                flatId = flat.id,
                flatNumber = flat.flatNumber,
                tenantId = flat.tenantId.ifBlank { flat.id },
                tenantUserId = uid,
                tenantEmail = flat.tenantEmail.ifBlank { currentEmail() },
                tenantName = flat.tenantName,
                billingMonth = billingMonth,
                monthKey = monthKey,
                monthlyRent = flat.monthlyRent,
                serviceCharge = flat.serviceCharge,
                electricityBill = flat.electricityBill,
                waterBill = flat.waterBill,
                gasBill = flat.gasBill,
                otherCharges = flat.otherCharges,
                previousDueCarriedForward = 0.0,
                totalBill = totalBill,
                amountPaid = 0.0,
                remainingDue = totalBill,
                status = "unpaid",
                dueDate = dueDate
            )
        }
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "monthly_bills")
    }

    // --- TENANT & ADMIN PAYMENTS + ATOMIC VERIFICATION ---
    suspend fun submitTenantPaymentRequest(
        bill: MonthlyBill,
        amount: Double,
        method: String,
        transactionId: String,
        referenceNumber: String,
        notes: String,
        targetBankAccountId: String = "",
        targetBankAccountName: String = "",
        receiptImageBase64: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(amount > 0.0) { "Amount must be positive" }
        val paymentRef = db.collection("payments").document()
        val batch = db.batch()
        batch.set(
            paymentRef,
            mapOf(
                "ownerId" to bill.ownerId,
                "billId" to bill.id,
                "buildingId" to bill.buildingId,
                "buildingName" to bill.buildingName,
                "flatId" to bill.flatId,
                "flatNumber" to bill.flatNumber,
                "tenantId" to bill.tenantId,
                "tenantUserId" to uid,
                "tenantEmail" to currentEmail(),
                "tenantName" to bill.tenantName,
                "amount" to amount,
                "method" to method,
                "transactionId" to transactionId,
                "referenceNumber" to referenceNumber,
                "targetBankAccountId" to targetBankAccountId,
                "targetBankAccountName" to targetBankAccountName,
                "receiptImageBase64" to receiptImageBase64,
                "gatewayStatus" to "pending",
                "verificationStatus" to "pending",
                "paymentDate" to FormatUtils.todayDdMmYyyy(),
                "notes" to notes,
                "verifiedBy" to "",
                "receiptId" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        val methodLabel = when (method) {
            "cash" -> "নগদ / হাতে (Cash)"
            "bank_transfer" -> "ব্যাংক ট্রান্সফার"
            "bkash" -> "bKash"
            "nagad" -> "Nagad"
            "rocket" -> "Rocket"
            else -> method.uppercase()
        }
        val proofNote = if (receiptImageBase64.isNotBlank()) " • প্রমাণ ছবি সংযুক্ত" else ""

        val ownerNotifRef = db.collection("notifications").document()
        batch.set(
            ownerNotifRef,
            mapOf(
                "ownerId" to bill.ownerId,
                "recipientUid" to bill.ownerId,
                "recipientEmail" to "",
                "title" to "নতুন পেমেন্ট ও প্রমাণ (${bill.flatNumber})",
                "body" to "${bill.tenantName} ৳${amount.toInt()} ($methodLabel)$proofNote। ওকে প্রেস করলে বিল গ্রহণ হবে।",
                "type" to "payment_submitted",
                "relatedId" to paymentRef.id,
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        writeAuditLog(bill.ownerId, "tenant", "SUBMIT_PAYMENT", "payment", paymentRef.id, "Submitted BDT $amount via $method for ${bill.flatNumber}")
        paymentRef.id
    }.onFailure {
        handleFirestoreError(it, OperationType.CREATE, "payments")
    }

    suspend fun verifyPendingPayment(
        payment: PaymentRecord,
        bill: MonthlyBill,
        targetBankAccount: BankAccount?
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(payment.verificationStatus != "verified") { "Payment is already verified." }

        val previousDue = bill.remainingDue
        val newAmountPaid = bill.amountPaid + payment.amount
        val newRemainingDue = (bill.totalBill - newAmountPaid).coerceAtLeast(0.0)
        val newBillStatus = if (newRemainingDue <= 0.0) "paid" else "partial"

        val receiptRef = db.collection("receipts").document()
        val receiptNumber = "RCP-${System.currentTimeMillis().toString().takeLast(6)}"

        val batch = db.batch()
        batch.update(
            db.collection("payments").document(payment.id),
            mapOf(
                "gatewayStatus" to "verified",
                "verificationStatus" to "verified",
                "verifiedBy" to uid,
                "receiptId" to receiptRef.id,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.update(
            db.collection("monthly_bills").document(bill.id),
            mapOf(
                "amountPaid" to newAmountPaid,
                "remainingDue" to newRemainingDue,
                "status" to newBillStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.update(
            db.collection("flats").document(bill.flatId),
            mapOf(
                "amountPaid" to newAmountPaid,
                "remainingDue" to newRemainingDue,
                "paymentStatus" to newBillStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.set(
            receiptRef,
            mapOf(
                "ownerId" to uid,
                "paymentId" to payment.id,
                "billId" to bill.id,
                "receiptNumber" to receiptNumber,
                "buildingId" to bill.buildingId,
                "buildingName" to bill.buildingName,
                "flatId" to bill.flatId,
                "flatNumber" to bill.flatNumber,
                "tenantId" to bill.tenantId,
                "tenantUserId" to payment.tenantUserId,
                "tenantEmail" to payment.tenantEmail,
                "tenantName" to payment.tenantName,
                "paymentDate" to payment.paymentDate,
                "amountPaid" to payment.amount,
                "paymentMethod" to payment.method,
                "transactionId" to payment.transactionId,
                "previousDue" to previousDue,
                "remainingDue" to newRemainingDue,
                "status" to newBillStatus,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        if (payment.method == "cash") {
            val cashRef = db.collection("cash_transactions").document()
            batch.set(
                cashRef,
                mapOf(
                    "ownerId" to uid,
                    "date" to payment.paymentDate,
                    "amount" to payment.amount,
                    "transactionType" to "cash_in",
                    "description" to "Rent collection: ${bill.buildingName} ${bill.flatNumber} (${bill.billingMonth})",
                    "paymentMethod" to "cash",
                    "referenceNumber" to payment.transactionId,
                    "buildingId" to bill.buildingId,
                    "flatId" to bill.flatId,
                    "createdBy" to uid,
                    "verificationStatus" to "verified",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        } else if (targetBankAccount != null) {
            val bankTxRef = db.collection("bank_transactions").document()
            batch.set(
                bankTxRef,
                mapOf(
                    "ownerId" to uid,
                    "bankAccountId" to targetBankAccount.id,
                    "bankAccountName" to targetBankAccount.accountName,
                    "date" to payment.paymentDate,
                    "amount" to payment.amount,
                    "transactionType" to "received",
                    "description" to "Rent via ${payment.method}: ${bill.buildingName} ${bill.flatNumber}",
                    "paymentMethod" to payment.method,
                    "referenceNumber" to payment.transactionId,
                    "buildingId" to bill.buildingId,
                    "flatId" to bill.flatId,
                    "createdBy" to uid,
                    "verificationStatus" to "verified",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("bank_accounts").document(targetBankAccount.id),
                mapOf(
                    "moneyReceived" to targetBankAccount.moneyReceived + payment.amount,
                    "currentBalance" to targetBankAccount.currentBalance + payment.amount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        if (payment.tenantUserId.isNotBlank() || payment.tenantEmail.isNotBlank()) {
            val tenantNotif = db.collection("notifications").document()
            batch.set(
                tenantNotif,
                mapOf(
                    "ownerId" to uid,
                    "recipientUid" to payment.tenantUserId,
                    "recipientEmail" to payment.tenantEmail,
                    "title" to "ভাড়া গ্রহণ করা হয়েছে ($receiptNumber)",
                    "body" to "${bill.flatNumber} ফ্ল্যাটের ৳${payment.amount.toInt()} বিল গ্রহণ করা হয়েছে। বকেয়া: ৳${newRemainingDue.toInt()}।",
                    "type" to "payment_verified",
                    "relatedId" to receiptRef.id,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // Auto-mark related owner notification as read when owner accepts the bill
        val relatedNotifs = try {
            db.collection("notifications")
                .whereEqualTo("ownerId", uid)
                .get()
                .await()
                .documents
                .filter {
                    it.getString("relatedId") == payment.id &&
                        it.getBoolean("isRead") != true
                }
        } catch (_: Exception) {
            emptyList()
        }
        relatedNotifs.forEach { nDoc ->
            batch.update(
                nDoc.reference,
                mapOf("isRead" to true, "updatedAt" to FieldValue.serverTimestamp())
            )
        }

        batch.commit().await()
        writeAuditLog(uid, "admin", "VERIFY_PAYMENT", "payment", payment.id, "Verified BDT ${payment.amount} and issued $receiptNumber")
        receiptRef.id
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "payments/${payment.id}")
    }

    suspend fun rejectPendingPayment(payment: PaymentRecord, reason: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val batch = db.batch()
        batch.update(
            db.collection("payments").document(payment.id),
            mapOf(
                "gatewayStatus" to "failed",
                "verificationStatus" to "rejected",
                "notes" to reason,
                "verifiedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (payment.tenantUserId.isNotBlank() || payment.tenantEmail.isNotBlank()) {
            batch.set(
                db.collection("notifications").document(),
                mapOf(
                    "ownerId" to uid,
                    "recipientUid" to payment.tenantUserId,
                    "recipientEmail" to payment.tenantEmail,
                    "title" to "Payment Verification Declined",
                    "body" to "Payment ${payment.transactionId} was declined: $reason",
                    "type" to "payment_rejected",
                    "relatedId" to payment.id,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        writeAuditLog(uid, "admin", "REJECT_PAYMENT", "payment", payment.id, "Rejected payment ${payment.transactionId}")
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "payments/${payment.id}")
    }

    suspend fun recordAdminDirectPayment(
        bill: MonthlyBill,
        amount: Double,
        method: String,
        referenceNumber: String,
        targetBankAccount: BankAccount?,
        notes: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(amount > 0.0)
        val paymentId = db.collection("payments").document().id
        val tranId = "ADM_${System.currentTimeMillis() % 1000000}_${UUID.randomUUID().toString().take(4).uppercase()}"
        val previousDue = bill.remainingDue
        val newAmountPaid = bill.amountPaid + amount
        val newRemainingDue = (bill.totalBill - newAmountPaid).coerceAtLeast(0.0)
        val newBillStatus = if (newRemainingDue <= 0.0) "paid" else "partial"
        val receiptRef = db.collection("receipts").document()
        val receiptNumber = "RCP-${System.currentTimeMillis().toString().takeLast(6)}"
        val today = FormatUtils.todayDdMmYyyy()

        val batch = db.batch()
        batch.set(
            db.collection("payments").document(paymentId),
            mapOf(
                "ownerId" to uid,
                "billId" to bill.id,
                "buildingId" to bill.buildingId,
                "buildingName" to bill.buildingName,
                "flatId" to bill.flatId,
                "flatNumber" to bill.flatNumber,
                "tenantId" to bill.tenantId,
                "tenantUserId" to bill.tenantUserId,
                "tenantEmail" to bill.tenantEmail,
                "tenantName" to bill.tenantName,
                "amount" to amount,
                "method" to method,
                "transactionId" to tranId,
                "referenceNumber" to referenceNumber.ifBlank { tranId },
                "gatewayStatus" to "verified",
                "verificationStatus" to "verified",
                "paymentDate" to today,
                "notes" to notes,
                "verifiedBy" to uid,
                "receiptId" to receiptRef.id,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection("monthly_bills").document(bill.id),
            mapOf(
                "amountPaid" to newAmountPaid,
                "remainingDue" to newRemainingDue,
                "status" to newBillStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection("flats").document(bill.flatId),
            mapOf(
                "amountPaid" to newAmountPaid,
                "remainingDue" to newRemainingDue,
                "paymentStatus" to newBillStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            receiptRef,
            mapOf(
                "ownerId" to uid,
                "paymentId" to paymentId,
                "billId" to bill.id,
                "receiptNumber" to receiptNumber,
                "buildingId" to bill.buildingId,
                "buildingName" to bill.buildingName,
                "flatId" to bill.flatId,
                "flatNumber" to bill.flatNumber,
                "tenantId" to bill.tenantId,
                "tenantUserId" to bill.tenantUserId,
                "tenantEmail" to bill.tenantEmail,
                "tenantName" to bill.tenantName,
                "paymentDate" to today,
                "amountPaid" to amount,
                "paymentMethod" to method,
                "transactionId" to tranId,
                "previousDue" to previousDue,
                "remainingDue" to newRemainingDue,
                "status" to newBillStatus,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        if (method == "cash") {
            batch.set(
                db.collection("cash_transactions").document(),
                mapOf(
                    "ownerId" to uid,
                    "date" to today,
                    "amount" to amount,
                    "transactionType" to "cash_in",
                    "description" to "Direct cash rent: ${bill.buildingName} ${bill.flatNumber}",
                    "paymentMethod" to "cash",
                    "referenceNumber" to tranId,
                    "buildingId" to bill.buildingId,
                    "flatId" to bill.flatId,
                    "createdBy" to uid,
                    "verificationStatus" to "verified",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        } else if (targetBankAccount != null) {
            batch.set(
                db.collection("bank_transactions").document(),
                mapOf(
                    "ownerId" to uid,
                    "bankAccountId" to targetBankAccount.id,
                    "bankAccountName" to targetBankAccount.accountName,
                    "date" to today,
                    "amount" to amount,
                    "transactionType" to "received",
                    "description" to "Rent ($method): ${bill.buildingName} ${bill.flatNumber}",
                    "paymentMethod" to method,
                    "referenceNumber" to referenceNumber.ifBlank { tranId },
                    "buildingId" to bill.buildingId,
                    "flatId" to bill.flatId,
                    "createdBy" to uid,
                    "verificationStatus" to "verified",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("bank_accounts").document(targetBankAccount.id),
                mapOf(
                    "moneyReceived" to targetBankAccount.moneyReceived + amount,
                    "currentBalance" to targetBankAccount.currentBalance + amount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        if (bill.tenantUserId.isNotBlank() || bill.tenantEmail.isNotBlank()) {
            batch.set(
                db.collection("notifications").document(),
                mapOf(
                    "ownerId" to uid,
                    "recipientUid" to bill.tenantUserId,
                    "recipientEmail" to bill.tenantEmail,
                    "title" to "ভাড়া গ্রহণ করা হয়েছে ($receiptNumber)",
                    "body" to "${bill.flatNumber} ফ্ল্যাটের ৳${amount.toInt()} ($method) ভাড়া গ্রহণ করা হয়েছে। বকেয়া: ৳${newRemainingDue.toInt()}।",
                    "type" to "payment_verified",
                    "relatedId" to receiptRef.id,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.commit().await()
        writeAuditLog(uid, "admin", "RECORD_PAYMENT", "payment", paymentId, "Recorded BDT $amount ($method) for ${bill.flatNumber}")
        receiptRef.id
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "payments")
    }

    // --- BANK & CASH ACCOUNTING + EXPENSES ---
    suspend fun syncPublicBankAccountsToSettings(accounts: List<BankAccount>): Result<Unit> = runCatching {
        val uid = requireUserId()
        val encoded = BankInfoJson.encodeFromBankAccounts(accounts)
        val ownerSettingsRef = db.collection("settings").document("owner_$uid")
        val ownerSnap = ownerSettingsRef.get().await()
        if (ownerSnap.exists()) {
            ownerSettingsRef.update(
                mapOf(
                    "bankAccountsJson" to encoded,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val legacyAppSnap = try {
                db.collection("settings").document("app_config").get().await()
            } catch (_: Exception) {
                null
            }
            val sourceLegacy = legacyAppSnap?.takeIf { it.exists() && it.getString("ownerUid") == uid }
            ownerSettingsRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "appName" to (sourceLegacy?.getString("appName") ?: "Smart Rent Manager"),
                    "defaultLanguage" to (sourceLegacy?.getString("defaultLanguage") ?: "bn"),
                    "sslCommerzStoreId" to (sourceLegacy?.getString("sslCommerzStoreId") ?: ""),
                    "sslCommerzSandboxMode" to (sourceLegacy?.getBoolean("sslCommerzSandboxMode") ?: true),
                    "bkashNumber" to (sourceLegacy?.getString("bkashNumber") ?: ""),
                    "nagadNumber" to (sourceLegacy?.getString("nagadNumber") ?: ""),
                    "rocketNumber" to (sourceLegacy?.getString("rocketNumber") ?: ""),
                    "bankTransferInstructions" to (sourceLegacy?.getString("bankTransferInstructions") ?: ""),
                    "bankAccountsJson" to encoded,
                    "autoBillDay" to ((sourceLegacy?.getLong("autoBillDay")?.toInt()) ?: 1),
                    "carryForwardEnabled" to (sourceLegacy?.getBoolean("carryForwardEnabled") ?: true),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val configRef = db.collection("settings").document("app_config")
        val configSnap = try { configRef.get().await() } catch (_: Exception) { null }
        if (configSnap != null && configSnap.exists() && configSnap.getString("ownerUid") == uid) {
            configRef.update(
                mapOf(
                    "bankAccountsJson" to encoded,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "settings/owner_${auth.currentUser?.uid.orEmpty()}")
    }

    private suspend fun refreshPublicBankAccountsFromDb() {
        try {
            val uid = requireUserId()
            val snaps = db.collection("bank_accounts")
                .whereEqualTo("ownerId", uid)
                .get()
                .await()
            val list = snaps.toObjects(BankAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            syncPublicBankAccountsToSettings(list)
        } catch (_: Exception) {
        }
    }

    suspend fun addBankAccount(account: BankAccount): Result<String> = runCatching {
        val uid = requireUserId()
        val ref = db.collection("bank_accounts").document()
        ref.set(
            mapOf(
                "ownerId" to uid,
                "accountName" to account.accountName.trim(),
                "bankName" to account.bankName.trim(),
                "accountNumber" to account.accountNumber.trim(),
                "accountType" to account.accountType.trim(),
                "openingBalance" to account.openingBalance,
                "moneyReceived" to 0.0,
                "moneyTransferred" to 0.0,
                "bankExpenses" to 0.0,
                "currentBalance" to account.openingBalance,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        refreshPublicBankAccountsFromDb()
        writeAuditLog(uid, "admin", "ADD_BANK_ACCOUNT", "bank_account", ref.id, "Added ${account.bankName} - ${account.accountName}")
        ref.id
    }.onFailure {
        handleFirestoreError(it, OperationType.CREATE, "bank_accounts")
    }

    suspend fun recordBankTransaction(
        account: BankAccount,
        amount: Double,
        type: String, // "received" | "transferred" | "expense"
        description: String,
        referenceNumber: String,
        buildingId: String = "",
        flatId: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(amount > 0.0)
        val txRef = db.collection("bank_transactions").document()
        val batch = db.batch()
        batch.set(
            txRef,
            mapOf(
                "ownerId" to uid,
                "bankAccountId" to account.id,
                "bankAccountName" to account.accountName,
                "date" to FormatUtils.todayDdMmYyyy(),
                "amount" to amount,
                "transactionType" to type,
                "description" to description.trim(),
                "paymentMethod" to "bank_transfer",
                "referenceNumber" to referenceNumber.trim(),
                "buildingId" to buildingId,
                "flatId" to flatId,
                "createdBy" to uid,
                "verificationStatus" to "verified",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        val newReceived = if (type == "received") account.moneyReceived + amount else account.moneyReceived
        val newTransferred = if (type == "transferred") account.moneyTransferred + amount else account.moneyTransferred
        val newExpenses = if (type == "expense") account.bankExpenses + amount else account.bankExpenses
        val newBalance = if (type == "received") account.currentBalance + amount else account.currentBalance - amount
        batch.update(
            db.collection("bank_accounts").document(account.id),
            mapOf(
                "moneyReceived" to newReceived,
                "moneyTransferred" to newTransferred,
                "bankExpenses" to newExpenses,
                "currentBalance" to newBalance,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        writeAuditLog(uid, "admin", "BANK_TX", "bank_transaction", txRef.id, "$type BDT $amount (${account.accountName})")
        txRef.id
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "bank_transactions")
    }

    suspend fun recordCashTransaction(
        amount: Double,
        type: String, // "cash_in" | "cash_out"
        description: String,
        referenceNumber: String,
        buildingId: String = "",
        flatId: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(amount > 0.0)
        val txRef = db.collection("cash_transactions").document()
        txRef.set(
            mapOf(
                "ownerId" to uid,
                "date" to FormatUtils.todayDdMmYyyy(),
                "amount" to amount,
                "transactionType" to type,
                "description" to description.trim(),
                "paymentMethod" to "cash",
                "referenceNumber" to referenceNumber.trim(),
                "buildingId" to buildingId,
                "flatId" to flatId,
                "createdBy" to uid,
                "verificationStatus" to "verified",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        writeAuditLog(uid, "admin", "CASH_TX", "cash_transaction", txRef.id, "$type BDT $amount")
        txRef.id
    }.onFailure {
        handleFirestoreError(it, OperationType.CREATE, "cash_transactions")
    }

    suspend fun addExpense(
        expense: Expense,
        bankAccount: BankAccount?
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(expense.amount > 0.0)
        val expRef = db.collection("expenses").document()
        val batch = db.batch()
        val dateStr = expense.date.ifBlank { FormatUtils.todayDdMmYyyy() }

        batch.set(
            expRef,
            mapOf(
                "ownerId" to uid,
                "buildingId" to expense.buildingId,
                "buildingName" to expense.buildingName,
                "flatId" to expense.flatId,
                "category" to expense.category,
                "amount" to expense.amount,
                "date" to dateStr,
                "paymentSource" to expense.paymentSource,
                "bankAccountId" to (bankAccount?.id ?: ""),
                "description" to expense.description.trim(),
                "referenceNumber" to expense.referenceNumber.trim(),
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )

        if (expense.paymentSource == "cash") {
            batch.set(
                db.collection("cash_transactions").document(),
                mapOf(
                    "ownerId" to uid,
                    "date" to dateStr,
                    "amount" to expense.amount,
                    "transactionType" to "cash_out",
                    "description" to "Expense (${expense.category}): ${expense.description}",
                    "paymentMethod" to "cash",
                    "referenceNumber" to expense.referenceNumber,
                    "buildingId" to expense.buildingId,
                    "flatId" to expense.flatId,
                    "createdBy" to uid,
                    "verificationStatus" to "verified",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        } else if (bankAccount != null) {
            batch.set(
                db.collection("bank_transactions").document(),
                mapOf(
                    "ownerId" to uid,
                    "bankAccountId" to bankAccount.id,
                    "bankAccountName" to bankAccount.accountName,
                    "date" to dateStr,
                    "amount" to expense.amount,
                    "transactionType" to "expense",
                    "description" to "Expense (${expense.category}): ${expense.description}",
                    "paymentMethod" to "bank_transfer",
                    "referenceNumber" to expense.referenceNumber,
                    "buildingId" to expense.buildingId,
                    "flatId" to expense.flatId,
                    "createdBy" to uid,
                    "verificationStatus" to "verified",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("bank_accounts").document(bankAccount.id),
                mapOf(
                    "bankExpenses" to bankAccount.bankExpenses + expense.amount,
                    "currentBalance" to bankAccount.currentBalance - expense.amount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.commit().await()
        writeAuditLog(uid, "admin", "ADD_EXPENSE", "expense", expRef.id, "${expense.category}: BDT ${expense.amount}")
        expRef.id
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "expenses")
    }

    suspend fun deleteBankAccount(account: BankAccount): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(account.id.isNotBlank())
        val relatedTxs = db.collection("bank_transactions")
            .whereEqualTo("ownerId", uid)
            .get()
            .await()
            .documents
            .filter { it.getString("bankAccountId") == account.id }
        val batch = db.batch()
        relatedTxs.forEach { doc -> batch.delete(doc.reference) }
        batch.delete(db.collection("bank_accounts").document(account.id))
        batch.commit().await()
        refreshPublicBankAccountsFromDb()
        writeAuditLog(uid, "admin", "DELETE_BANK_ACCOUNT", "bank_account", account.id, "Deleted bank account ${account.accountName}")
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "bank_accounts/${account.id}")
    }

    suspend fun deleteBankTransaction(
        tx: BankTransaction,
        account: BankAccount?
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(tx.id.isNotBlank())
        val batch = db.batch()
        batch.delete(db.collection("bank_transactions").document(tx.id))
        if (account != null && account.id.isNotBlank()) {
            val newReceived = if (tx.transactionType == "received") (account.moneyReceived - tx.amount).coerceAtLeast(0.0) else account.moneyReceived
            val newTransferred = if (tx.transactionType == "transferred") (account.moneyTransferred - tx.amount).coerceAtLeast(0.0) else account.moneyTransferred
            val newExpenses = if (tx.transactionType == "expense") (account.bankExpenses - tx.amount).coerceAtLeast(0.0) else account.bankExpenses
            val newBalance = if (tx.transactionType == "received") account.currentBalance - tx.amount else account.currentBalance + tx.amount
            batch.update(
                db.collection("bank_accounts").document(account.id),
                mapOf(
                    "moneyReceived" to newReceived,
                    "moneyTransferred" to newTransferred,
                    "bankExpenses" to newExpenses,
                    "currentBalance" to newBalance,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        writeAuditLog(uid, "admin", "DELETE_BANK_TX", "bank_transaction", tx.id, "Deleted bank transaction ${tx.description}")
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "bank_transactions/${tx.id}")
    }

    suspend fun deleteCashTransaction(tx: CashTransaction): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(tx.id.isNotBlank())
        db.collection("cash_transactions").document(tx.id).delete().await()
        writeAuditLog(uid, "admin", "DELETE_CASH_TX", "cash_transaction", tx.id, "Deleted cash transaction ${tx.description}")
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "cash_transactions/${tx.id}")
    }

    suspend fun deleteExpense(
        expense: Expense,
        bankAccount: BankAccount?
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(expense.id.isNotBlank())
        val expectedDesc = "Expense (${expense.category}): ${expense.description}"
        val batch = db.batch()
        batch.delete(db.collection("expenses").document(expense.id))

        if (expense.paymentSource == "bank" && bankAccount != null && bankAccount.id.isNotBlank()) {
            batch.update(
                db.collection("bank_accounts").document(bankAccount.id),
                mapOf(
                    "bankExpenses" to (bankAccount.bankExpenses - expense.amount).coerceAtLeast(0.0),
                    "currentBalance" to bankAccount.currentBalance + expense.amount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val matchingBankTx = db.collection("bank_transactions")
                .whereEqualTo("ownerId", uid)
                .get()
                .await()
                .documents
                .firstOrNull {
                    it.getString("bankAccountId") == bankAccount.id &&
                        it.getString("transactionType") == "expense" &&
                        (it.getDouble("amount") ?: 0.0) == expense.amount &&
                        it.getString("description") == expectedDesc
                }
            matchingBankTx?.let { batch.delete(it.reference) }
        } else if (expense.paymentSource == "cash") {
            val matchingCashTx = db.collection("cash_transactions")
                .whereEqualTo("ownerId", uid)
                .get()
                .await()
                .documents
                .firstOrNull {
                    it.getString("transactionType") == "cash_out" &&
                        (it.getDouble("amount") ?: 0.0) == expense.amount &&
                        it.getString("description") == expectedDesc
                }
            matchingCashTx?.let { batch.delete(it.reference) }
        }

        batch.commit().await()
        writeAuditLog(uid, "admin", "DELETE_EXPENSE", "expense", expense.id, "Deleted expense ${expense.category}: BDT ${expense.amount}")
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "expenses/${expense.id}")
    }

    suspend fun deletePayment(
        payment: PaymentRecord,
        bill: MonthlyBill?,
        bankAccount: BankAccount?
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(payment.id.isNotBlank())
        val batch = db.batch()
        batch.delete(db.collection("payments").document(payment.id))

        if (payment.receiptId.isNotBlank()) {
            val rcpRef = db.collection("receipts").document(payment.receiptId)
            if (rcpRef.get().await().exists()) {
                batch.delete(rcpRef)
            }
        }

        if (payment.verificationStatus == "verified" && bill != null && bill.id.isNotBlank()) {
            val newPaid = (bill.amountPaid - payment.amount).coerceAtLeast(0.0)
            val newRemaining = (bill.totalBill - newPaid).coerceAtLeast(0.0)
            val newStatus = when {
                newRemaining <= 0.0 && bill.totalBill > 0.0 -> "paid"
                newPaid > 0.0 -> "partial"
                else -> "unpaid"
            }
            val billRef = db.collection("monthly_bills").document(bill.id)
            if (billRef.get().await().exists()) {
                batch.update(
                    billRef,
                    mapOf(
                        "amountPaid" to newPaid,
                        "remainingDue" to newRemaining,
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (bill.flatId.isNotBlank()) {
                val flatRef = db.collection("flats").document(bill.flatId)
                if (flatRef.get().await().exists()) {
                    batch.update(
                        flatRef,
                        mapOf(
                            "amountPaid" to newPaid,
                            "remainingDue" to newRemaining,
                            "paymentStatus" to newStatus,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            if (payment.method == "cash") {
                val matchingCash = db.collection("cash_transactions")
                    .whereEqualTo("ownerId", uid)
                    .get()
                    .await()
                    .documents
                    .firstOrNull { it.getString("referenceNumber") == payment.transactionId }
                matchingCash?.let { batch.delete(it.reference) }
            } else if (bankAccount != null && bankAccount.id.isNotBlank()) {
                batch.update(
                    db.collection("bank_accounts").document(bankAccount.id),
                    mapOf(
                        "moneyReceived" to (bankAccount.moneyReceived - payment.amount).coerceAtLeast(0.0),
                        "currentBalance" to bankAccount.currentBalance - payment.amount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val refMatch = payment.referenceNumber.ifBlank { payment.transactionId }
                val matchingBank = db.collection("bank_transactions")
                    .whereEqualTo("ownerId", uid)
                    .get()
                    .await()
                    .documents
                    .firstOrNull {
                        it.getString("bankAccountId") == bankAccount.id &&
                            it.getString("transactionType") == "received" &&
                            (it.getString("referenceNumber") == refMatch ||
                                it.getString("referenceNumber") == payment.transactionId)
                    }
                matchingBank?.let { batch.delete(it.reference) }
            }
        }

        batch.commit().await()
        writeAuditLog(uid, "admin", "DELETE_PAYMENT", "payment", payment.id, "Deleted payment ${payment.transactionId}")
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "payments/${payment.id}")
    }

    suspend fun deleteReceipt(receipt: Receipt): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(receipt.id.isNotBlank())
        db.collection("receipts").document(receipt.id).delete().await()
        writeAuditLog(uid, "admin", "DELETE_RECEIPT", "receipt", receipt.id, "Deleted receipt ${receipt.receiptNumber}")
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "receipts/${receipt.id}")
    }

    // --- MESSAGING & ANNOUNCEMENTS ---
    suspend fun sendPersonalMessage(
        recipientTenant: Tenant,
        title: String,
        body: String,
        type: String = "personal"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val msgRef = db.collection("messages").document()
        val batch = db.batch()
        batch.set(
            msgRef,
            mapOf(
                "ownerId" to uid,
                "senderUid" to uid,
                "senderName" to (auth.currentUser?.displayName ?: "Owner"),
                "senderRole" to "admin",
                "recipientUid" to recipientTenant.tenantUserId,
                "recipientEmail" to recipientTenant.email,
                "tenantId" to recipientTenant.id,
                "buildingId" to recipientTenant.buildingId,
                "flatId" to recipientTenant.flatId,
                "flatNumber" to recipientTenant.flatNumber,
                "title" to title.trim(),
                "body" to body.trim(),
                "type" to type,
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("notifications").document(),
            mapOf(
                "ownerId" to uid,
                "recipientUid" to recipientTenant.tenantUserId,
                "recipientEmail" to recipientTenant.email,
                "title" to title.trim(),
                "body" to body.trim(),
                "type" to "message",
                "relatedId" to msgRef.id,
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        msgRef.id
    }.onFailure {
        handleFirestoreError(it, OperationType.CREATE, "messages")
    }

    suspend fun sendTenantReplyToOwner(
        ownerId: String,
        tenant: Tenant?,
        title: String,
        body: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val msgRef = db.collection("messages").document()
        msgRef.set(
            mapOf(
                "ownerId" to ownerId,
                "senderUid" to uid,
                "senderName" to (tenant?.name ?: auth.currentUser?.displayName ?: "Tenant"),
                "senderRole" to "tenant",
                "recipientUid" to ownerId,
                "recipientEmail" to "",
                "tenantId" to (tenant?.id ?: ""),
                "buildingId" to (tenant?.buildingId ?: ""),
                "flatId" to (tenant?.flatId ?: ""),
                "flatNumber" to (tenant?.flatNumber ?: ""),
                "title" to title.trim(),
                "body" to body.trim(),
                "type" to "personal",
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        msgRef.id
    }.onFailure {
        handleFirestoreError(it, OperationType.CREATE, "messages")
    }

    suspend fun sendAnnouncement(
        title: String,
        body: String,
        category: String,
        targetBuilding: Building?,
        tenants: List<Tenant>
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val scope = if (targetBuilding == null) "all" else "building"
        val annRef = db.collection("announcements").document()
        val batch = db.batch()
        batch.set(
            annRef,
            mapOf(
                "ownerId" to uid,
                "targetScope" to scope,
                "buildingId" to (targetBuilding?.id ?: ""),
                "buildingName" to (targetBuilding?.name ?: "All Buildings"),
                "title" to title.trim(),
                "body" to body.trim(),
                "category" to category,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        val targetTenants = if (targetBuilding == null) {
            tenants.filter { it.status == "active" }
        } else {
            tenants.filter { it.status == "active" && it.buildingId == targetBuilding.id }
        }
        for (t in targetTenants.take(30)) {
            batch.set(
                db.collection("messages").document(),
                mapOf(
                    "ownerId" to uid,
                    "senderUid" to uid,
                    "senderName" to "Owner",
                    "senderRole" to "admin",
                    "recipientUid" to t.tenantUserId,
                    "recipientEmail" to t.email,
                    "tenantId" to t.id,
                    "buildingId" to t.buildingId,
                    "flatId" to t.flatId,
                    "flatNumber" to t.flatNumber,
                    "title" to title.trim(),
                    "body" to body.trim(),
                    "type" to if (category == "emergency") "emergency" else if (category == "maintenance") "maintenance" else "personal",
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        writeAuditLog(uid, "admin", "SEND_ANNOUNCEMENT", "announcement", annRef.id, "$scope: $title")
        annRef.id
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "announcements")
    }

    suspend fun markMessageRead(messageId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("messages").document(messageId).update(
            mapOf("isRead" to true, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "messages/$messageId")
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("notifications").document(notificationId).update(
            mapOf("isRead" to true, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "notifications/$notificationId")
    }

    suspend fun markAllNotificationsRead(notificationIds: List<String>): Result<Unit> = runCatching {
        requireUserId()
        if (notificationIds.isEmpty()) return@runCatching
        val batch = db.batch()
        notificationIds.take(40).forEach { id ->
            batch.update(
                db.collection("notifications").document(id),
                mapOf("isRead" to true, "updatedAt" to FieldValue.serverTimestamp())
            )
        }
        batch.commit().await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "notifications")
    }

    // --- SETTINGS & BACKUP ---
    suspend fun updateAppSettings(settings: AppSettings): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ownerSettingsRef = db.collection("settings").document("owner_$uid")
        val ownerSnap = ownerSettingsRef.get().await()
        val updateMap = mapOf(
            "appName" to settings.appName.trim().ifBlank { "Smart Rent Manager" },
            "defaultLanguage" to settings.defaultLanguage,
            "sslCommerzStoreId" to settings.sslCommerzStoreId.trim(),
            "sslCommerzSandboxMode" to settings.sslCommerzSandboxMode,
            "bkashNumber" to settings.bkashNumber.trim(),
            "nagadNumber" to settings.nagadNumber.trim(),
            "rocketNumber" to settings.rocketNumber.trim(),
            "bankTransferInstructions" to settings.bankTransferInstructions.trim(),
            "autoBillDay" to settings.autoBillDay.coerceIn(1, 28),
            "carryForwardEnabled" to settings.carryForwardEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (ownerSnap.exists()) {
            ownerSettingsRef.update(updateMap).await()
        } else {
            ownerSettingsRef.set(
                updateMap + mapOf(
                    "ownerUid" to uid,
                    "bankAccountsJson" to settings.bankAccountsJson,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val appConfigRef = db.collection("settings").document("app_config")
        val appConfigSnap = try { appConfigRef.get().await() } catch (_: Exception) { null }
        if (appConfigSnap != null && appConfigSnap.exists() && appConfigSnap.getString("ownerUid") == uid) {
            appConfigRef.update(updateMap).await()
        }

        writeAuditLog(uid, "admin", "UPDATE_SETTINGS", "settings", "owner_$uid", "Updated application & payment gateway settings")
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "settings/owner_${auth.currentUser?.uid.orEmpty()}")
    }

    fun buildBackupJson(
        buildings: List<Building>,
        flats: List<Flat>,
        tenants: List<Tenant>,
        bills: List<MonthlyBill>,
        payments: List<PaymentRecord>,
        expenses: List<Expense>
    ): String {
        val root = JSONObject()
        root.put("app", "Smart Rent Manager")
        root.put("exportedAt", FormatUtils.todayDdMmYyyy())
        root.put("buildingsCount", buildings.size)
        root.put("flatsCount", flats.size)
        root.put("tenantsCount", tenants.size)
        root.put("billsCount", bills.size)
        root.put("paymentsCount", payments.size)
        root.put("expensesCount", expenses.size)

        val bArr = JSONArray()
        buildings.forEach { b ->
            bArr.put(JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
                put("address", b.address)
                put("floorsCount", b.floorsCount)
                put("totalFlats", b.totalFlats)
            })
        }
        root.put("buildings", bArr)

        val fArr = JSONArray()
        flats.forEach { f ->
            fArr.put(JSONObject().apply {
                put("id", f.id)
                put("buildingName", f.buildingName)
                put("flatNumber", f.flatNumber)
                put("monthlyRent", f.monthlyRent)
                put("serviceCharge", f.serviceCharge)
                put("tenantName", f.tenantName)
                put("remainingDue", f.remainingDue)
            })
        }
        root.put("flats", fArr)

        val pArr = JSONArray()
        payments.forEach { p ->
            pArr.put(JSONObject().apply {
                put("id", p.id)
                put("flatNumber", p.flatNumber)
                put("amount", p.amount)
                put("method", p.method)
                put("transactionId", p.transactionId)
                put("verificationStatus", p.verificationStatus)
                put("paymentDate", p.paymentDate)
            })
        }
        root.put("payments", pArr)
        return root.toString(2)
    }
}
