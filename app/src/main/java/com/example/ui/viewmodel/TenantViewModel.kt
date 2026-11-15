package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.BankInfoJson
import com.example.data.model.Flat
import com.example.data.model.MessageItem
import com.example.data.model.MonthlyBill
import com.example.data.model.NotificationItem
import com.example.data.model.PaymentRecord
import com.example.data.model.PublicBankInfo
import com.example.data.model.Receipt
import com.example.data.remote.GatewayInitiationResult
import com.example.data.remote.SslCommerzClient
import com.example.data.repository.RentRepository
import com.example.util.FormatUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TenantDataSnapshot(
    val isLoading: Boolean = true,
    val flat: Flat? = null,
    val bills: List<MonthlyBill> = emptyList(),
    val payments: List<PaymentRecord> = emptyList(),
    val receipts: List<Receipt> = emptyList(),
    val messages: List<MessageItem> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val settings: AppSettings = AppSettings()
) {
    val displayBills: List<MonthlyBill>
        get() {
            if (bills.isNotEmpty()) return bills
            val assignedFlat = flat ?: return emptyList()
            if (!assignedFlat.isOccupied || assignedFlat.totalMonthlyBill <= 0.0) return emptyList()
            val monthKey = FormatUtils.currentMonthKey()
            val billingMonth = FormatUtils.currentBillingMonth()
            val total = assignedFlat.totalMonthlyBill
            val paid = assignedFlat.amountPaid
            val due = (total - paid).coerceAtLeast(0.0)
            val status = when {
                due <= 0.0 -> "paid"
                paid > 0.0 -> "partial"
                else -> "unpaid"
            }
            return listOf(
                MonthlyBill(
                    id = "${assignedFlat.id}_$monthKey",
                    ownerId = assignedFlat.ownerId,
                    buildingId = assignedFlat.buildingId,
                    buildingName = assignedFlat.buildingName,
                    flatId = assignedFlat.id,
                    flatNumber = assignedFlat.flatNumber,
                    tenantId = assignedFlat.tenantId,
                    tenantUserId = assignedFlat.tenantUserId,
                    tenantEmail = assignedFlat.tenantEmail,
                    tenantName = assignedFlat.tenantName,
                    billingMonth = billingMonth,
                    monthKey = monthKey,
                    monthlyRent = assignedFlat.monthlyRent,
                    serviceCharge = assignedFlat.serviceCharge,
                    electricityBill = assignedFlat.electricityBill,
                    waterBill = assignedFlat.waterBill,
                    gasBill = assignedFlat.gasBill,
                    otherCharges = assignedFlat.otherCharges,
                    previousDueCarriedForward = 0.0,
                    totalBill = total,
                    amountPaid = paid,
                    remainingDue = due,
                    status = status,
                    dueDate = FormatUtils.defaultDueDateDdMmYyyy()
                )
            )
        }

    val totalPayable: Double get() = displayBills.sumOf { it.totalBill }
    val totalPaid: Double get() = displayBills.sumOf { it.amountPaid }
    val remainingDue: Double get() = displayBills.sumOf { it.remainingDue }
    val currentBill: MonthlyBill? get() = displayBills.firstOrNull { it.remainingDue > 0.0 } ?: displayBills.firstOrNull()
    val ownerBankAccounts: List<PublicBankInfo> get() = BankInfoJson.decodeList(settings.bankAccountsJson)
    val unreadMessagesCount: Int get() = messages.count { !it.isRead }
}

class TenantViewModel(
    private val repository: RentRepository,
    private val currentUserId: String,
    private val sslCommerzClient: SslCommerzClient = SslCommerzClient()
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _gatewayResult = MutableStateFlow<GatewayInitiationResult?>(null)
    val gatewayResult: StateFlow<GatewayInitiationResult?> = _gatewayResult.asStateFlow()

    private fun <T> safeFlow(flow: Flow<T>, defaultValue: T, label: String): Flow<T> =
        flow.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w("TenantViewModel", "Error observing $label", firestoreError ?: error)
            emit(defaultValue)
        }

    private val tenantFlatsFlow = safeFlow(
        repository.observeTenantFlats(currentUserId),
        emptyList(),
        "tenant_flats"
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val ownerIdFlow = tenantFlatsFlow
        .map { flats -> flats.firstOrNull()?.ownerId.orEmpty() }
        .distinctUntilChanged()

    private val financialFlow = combine(
        tenantFlatsFlow,
        safeFlow(repository.observeTenantBills(currentUserId), emptyList(), "tenant_bills"),
        safeFlow(repository.observeTenantPayments(currentUserId), emptyList(), "tenant_payments"),
        safeFlow(repository.observeTenantReceipts(currentUserId), emptyList(), "tenant_receipts")
    ) { flats, bills, payments, receipts ->
        Quad(flats, bills, payments, receipts)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val commsFlow = combine(
        safeFlow(repository.observeTenantMessages(currentUserId), emptyList(), "tenant_messages"),
        ownerIdFlow.flatMapLatest { ownerId ->
            safeFlow(repository.observeBroadcastAnnouncementsForOwner(ownerId), emptyList(), "announcements")
        },
        safeFlow(repository.observeNotifications(currentUserId), emptyList(), "notifications"),
        ownerIdFlow.flatMapLatest { ownerId ->
            safeFlow(repository.observeOwnerScopedSettings(ownerId), null, "settings")
        }
    ) { msgs, anns, notifs, cfg ->
        Quad(msgs, anns, notifs, cfg ?: AppSettings())
    }

    val uiState: StateFlow<TenantDataSnapshot> = combine(financialFlow, commsFlow) { fin, comms ->
        TenantDataSnapshot(
            isLoading = false,
            flat = fin.first.firstOrNull(),
            bills = fin.second.sortedByDescending { it.monthKey },
            payments = fin.third.sortedByDescending { it.createdAt?.seconds ?: 0L },
            receipts = fin.fourth.sortedByDescending { it.createdAt?.seconds ?: 0L },
            messages = comms.first.sortedByDescending { it.createdAt?.seconds ?: 0L },
            announcements = comms.second.sortedByDescending { it.createdAt?.seconds ?: 0L },
            notifications = comms.third.sortedByDescending { it.createdAt?.seconds ?: 0L },
            settings = comms.fourth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TenantDataSnapshot(isLoading = true)
    )

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun clearGatewayResult() {
        _gatewayResult.value = null
    }

    fun submitPayment(
        bill: MonthlyBill,
        amount: Double,
        method: String,
        referenceNumber: String,
        notes: String,
        targetBankAccountId: String = "",
        targetBankAccountName: String = "",
        receiptImageBase64: String = ""
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val resolvedBill = if (uiState.value.bills.any { it.id == bill.id }) {
                bill
            } else {
                val flat = uiState.value.flat
                if (flat != null) {
                    val ensured = repository.ensureCurrentMonthBillForTenant(flat)
                    if (ensured.isFailure) {
                        _isBusy.value = false
                        _actionMessage.value = ensured.exceptionOrNull()?.localizedMessage
                            ?: "Unable to initialize monthly bill"
                        return@launch
                    }
                    ensured.getOrThrow()
                } else {
                    bill
                }
            }

            val tranId = sslCommerzClient.generateTransactionId(method.take(3).uppercase())

            // Initiate SSLCOMMERZ session only for card/sslcommerz gateway payments
            if (method in listOf("sslcommerz", "card")) {
                val settings = uiState.value.settings
                val gwRes = sslCommerzClient.initiateSandboxSession(
                    storeId = settings.sslCommerzStoreId,
                    storePassword = "", // Never ship store_passwd in client APK
                    amount = amount,
                    tranId = tranId,
                    customerName = resolvedBill.tenantName,
                    customerEmail = resolvedBill.tenantEmail,
                    customerPhone = uiState.value.flat?.tenantPhone.orEmpty(),
                    productName = "Rent ${resolvedBill.buildingName} ${resolvedBill.flatNumber}",
                    sandboxMode = settings.sslCommerzSandboxMode
                )
                _gatewayResult.value = gwRes
            }

            val finalRef = referenceNumber.ifBlank {
                if (receiptImageBase64.isNotBlank()) "SLIP-$tranId" else tranId
            }

            val res = repository.submitTenantPaymentRequest(
                bill = resolvedBill,
                amount = amount,
                method = method,
                transactionId = tranId,
                referenceNumber = finalRef,
                notes = notes,
                targetBankAccountId = targetBankAccountId,
                targetBankAccountName = targetBankAccountName,
                receiptImageBase64 = receiptImageBase64
            )
            _isBusy.value = false
            _actionMessage.value = res.fold(
                onSuccess = {
                    when {
                        method == "cash" ->
                            "নগদ পেমেন্ট ও প্রমাণ জমা হয়েছে ($tranId)। মালিক ওকে প্রেস করলে বিল গ্রহণ হবে।"
                        receiptImageBase64.isNotBlank() ->
                            "পেমেন্ট ও প্রমাণ রিসিট জমা হয়েছে ($tranId)। মালিক ওকে প্রেস করলে বিল গ্রহণ হবে।"
                        else ->
                            "পেমেন্ট জমা হয়েছে ($tranId)। মালিক যাচাই করে অনুমোদন করবেন।"
                    }
                },
                onFailure = { it.localizedMessage ?: "Failed to submit payment" }
            )
        }
    }

    fun markMessageRead(messageId: String) {
        viewModelScope.launch {
            repository.markMessageRead(messageId)
        }
    }

    fun replyToOwner(title: String, body: String) {
        val ownerId = uiState.value.flat?.ownerId ?: uiState.value.settings.ownerUid
        if (ownerId.isBlank()) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.sendTenantReplyToOwner(ownerId, null, title, body)
            _isBusy.value = false
            _actionMessage.value = res.fold(
                onSuccess = { "Message sent to Owner" },
                onFailure = { it.localizedMessage ?: "Failed to send message" }
            )
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
