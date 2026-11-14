package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.Flat
import com.example.data.model.MessageItem
import com.example.data.model.MonthlyBill
import com.example.data.model.NotificationItem
import com.example.data.model.PaymentRecord
import com.example.data.model.Receipt
import com.example.data.remote.GatewayInitiationResult
import com.example.data.remote.SslCommerzClient
import com.example.data.repository.RentRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    val totalPayable: Double get() = bills.sumOf { it.totalBill }
    val totalPaid: Double get() = bills.sumOf { it.amountPaid }
    val remainingDue: Double get() = bills.sumOf { it.remainingDue }
    val currentBill: MonthlyBill? get() = bills.firstOrNull { it.remainingDue > 0.0 } ?: bills.firstOrNull()
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

    private val financialFlow = combine(
        safeFlow(repository.observeTenantFlats(currentUserId), emptyList(), "tenant_flats"),
        safeFlow(repository.observeTenantBills(currentUserId), emptyList(), "tenant_bills"),
        safeFlow(repository.observeTenantPayments(currentUserId), emptyList(), "tenant_payments"),
        safeFlow(repository.observeTenantReceipts(currentUserId), emptyList(), "tenant_receipts")
    ) { flats, bills, payments, receipts ->
        Quad(flats, bills, payments, receipts)
    }

    private val commsFlow = combine(
        safeFlow(repository.observeTenantMessages(currentUserId), emptyList(), "tenant_messages"),
        safeFlow(repository.observeBroadcastAnnouncements(), emptyList(), "announcements"),
        safeFlow(repository.observeNotifications(currentUserId), emptyList(), "notifications"),
        safeFlow(repository.observeAppSettings(), null, "settings")
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
        notes: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val tranId = sslCommerzClient.generateTransactionId(method.take(3).uppercase())

            // For online gateways (sslcommerz / card / bkash / nagad / rocket), initiate gateway session
            val isGatewayMethod = method in listOf("sslcommerz", "card", "bkash", "nagad", "rocket")
            if (isGatewayMethod) {
                val settings = uiState.value.settings
                val gwRes = sslCommerzClient.initiateSandboxSession(
                    storeId = settings.sslCommerzStoreId,
                    storePassword = "", // Never ship store_passwd in client APK
                    amount = amount,
                    tranId = tranId,
                    customerName = bill.tenantName,
                    customerEmail = bill.tenantEmail,
                    customerPhone = uiState.value.flat?.tenantPhone.orEmpty(),
                    productName = "Rent ${bill.buildingName} ${bill.flatNumber}",
                    sandboxMode = settings.sslCommerzSandboxMode
                )
                _gatewayResult.value = gwRes
            }

            val res = repository.submitTenantPaymentRequest(
                bill = bill,
                amount = amount,
                method = method,
                transactionId = tranId,
                referenceNumber = referenceNumber.ifBlank { tranId },
                notes = notes
            )
            _isBusy.value = false
            _actionMessage.value = res.fold(
                onSuccess = { "Payment submitted ($tranId). Awaiting gateway/owner verification." },
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
