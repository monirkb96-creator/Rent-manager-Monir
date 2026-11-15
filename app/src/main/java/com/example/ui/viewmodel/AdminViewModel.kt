package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.AuditLog
import com.example.data.model.BankAccount
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
import com.example.data.repository.RentRepository
import com.example.util.FormatUtils
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

data class AdminDataSnapshot(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val buildings: List<Building> = emptyList(),
    val flats: List<Flat> = emptyList(),
    val tenants: List<Tenant> = emptyList(),
    val bills: List<MonthlyBill> = emptyList(),
    val payments: List<PaymentRecord> = emptyList(),
    val bankAccounts: List<BankAccount> = emptyList(),
    val bankTransactions: List<BankTransaction> = emptyList(),
    val cashTransactions: List<CashTransaction> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val messages: List<MessageItem> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val receipts: List<Receipt> = emptyList(),
    val auditLogs: List<AuditLog> = emptyList(),
    val settings: AppSettings = AppSettings()
) {
    val totalBuildings: Int get() = buildings.size
    val totalFlats: Int get() = flats.size
    val occupiedFlats: Int get() = flats.count { it.isOccupied }
    val vacantFlats: Int get() = flats.count { !it.isOccupied }
    val totalMonthlyRent: Double get() = flats.filter { it.isOccupied }.sumOf { it.monthlyRent }
    val totalServiceCharges: Double get() = flats.filter { it.isOccupied }.sumOf { it.serviceCharge }
    val totalCollectedAmount: Double get() = payments.filter { it.verificationStatus == "verified" }.sumOf { it.amount }
    val totalOutstandingDues: Double get() = bills.sumOf { it.remainingDue }
    val totalBankBalance: Double get() = bankAccounts.sumOf { it.currentBalance }
    val totalCashBalance: Double get() {
        val cashIn = cashTransactions.filter { it.transactionType == "cash_in" }.sumOf { it.amount }
        val cashOut = cashTransactions.filter { it.transactionType == "cash_out" }.sumOf { it.amount }
        return cashIn - cashOut
    }
    val currentMonthKey: String get() = FormatUtils.currentMonthKey()
    val currentBillingMonth: String get() = FormatUtils.currentBillingMonth()
    val monthlyIncome: Double
        get() = payments.filter {
            it.verificationStatus == "verified" && it.paymentDate.endsWith(currentBillingMonth)
        }.sumOf { it.amount }
    val monthlyExpenses: Double
        get() = expenses.filter { it.date.endsWith(currentBillingMonth) }.sumOf { it.amount }
    val pendingPayments: List<PaymentRecord>
        get() = payments.filter { it.verificationStatus == "pending" }
}

class AdminViewModel(
    private val repository: RentRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private fun <T> safeFlow(flow: Flow<T>, defaultValue: T, label: String): Flow<T> =
        flow.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w("AdminViewModel", "Error observing $label", firestoreError ?: error)
            emit(defaultValue)
        }

    private val corePropertyFlow = combine(
        safeFlow(repository.observeBuildings(), emptyList(), "buildings"),
        safeFlow(repository.observeFlats(), emptyList(), "flats"),
        safeFlow(repository.observeTenants(), emptyList(), "tenants"),
        safeFlow(repository.observeBills(), emptyList(), "monthly_bills"),
        safeFlow(repository.observePayments(), emptyList(), "payments")
    ) { b, f, t, bills, pay ->
        Quint(b, f, t, bills, pay)
    }

    private val accountingFlow = combine(
        safeFlow(repository.observeBankAccounts(), emptyList(), "bank_accounts"),
        safeFlow(repository.observeBankTransactions(), emptyList(), "bank_transactions"),
        safeFlow(repository.observeCashTransactions(), emptyList(), "cash_transactions"),
        safeFlow(repository.observeExpenses(), emptyList(), "expenses"),
        safeFlow(repository.observeReceipts(), emptyList(), "receipts")
    ) { ba, bt, ct, exp, rcp ->
        Quint(ba, bt, ct, exp, rcp)
    }

    private val commsFlow = combine(
        safeFlow(repository.observeMessages(), emptyList(), "messages"),
        safeFlow(repository.observeAnnouncements(), emptyList(), "announcements"),
        safeFlow(repository.observeNotifications(currentUserId), emptyList(), "notifications"),
        safeFlow(repository.observeAuditLogs(), emptyList(), "audit_logs"),
        safeFlow(repository.observeAppSettings(), null, "settings")
    ) { msg, ann, notif, logs, cfg ->
        Quint(msg, ann, notif, logs, cfg ?: AppSettings())
    }

    val uiState: StateFlow<AdminDataSnapshot> = combine(
        corePropertyFlow,
        accountingFlow,
        commsFlow
    ) { core, acct, comms ->
        AdminDataSnapshot(
            isLoading = false,
            buildings = core.first.sortedBy { it.name },
            flats = core.second.sortedBy { it.flatNumber },
            tenants = core.third.sortedBy { it.name },
            bills = core.fourth.sortedByDescending { it.monthKey },
            payments = core.fifth.sortedByDescending { it.createdAt?.seconds ?: 0L },
            bankAccounts = acct.first.sortedBy { it.accountName },
            bankTransactions = acct.second.sortedByDescending { it.createdAt?.seconds ?: 0L },
            cashTransactions = acct.third.sortedByDescending { it.createdAt?.seconds ?: 0L },
            expenses = acct.fourth.sortedByDescending { it.createdAt?.seconds ?: 0L },
            receipts = acct.fifth.sortedByDescending { it.createdAt?.seconds ?: 0L },
            messages = comms.first.sortedByDescending { it.createdAt?.seconds ?: 0L },
            announcements = comms.second.sortedByDescending { it.createdAt?.seconds ?: 0L },
            notifications = comms.third.sortedByDescending { it.createdAt?.seconds ?: 0L },
            auditLogs = comms.fourth.sortedByDescending { it.createdAt?.seconds ?: 0L },
            settings = comms.fifth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AdminDataSnapshot(isLoading = true)
    )

    fun clearMessage() {
        _actionMessage.value = null
    }

    private fun runAction(successText: String, block: suspend () -> Result<Any>) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = block()
            _isBusy.value = false
            _actionMessage.value = res.fold(
                onSuccess = { successText },
                onFailure = { it.localizedMessage ?: "Operation failed" }
            )
        }
    }

    fun saveBuilding(building: Building) = runAction("Building saved") {
        repository.saveBuilding(building)
    }

    fun deleteBuilding(building: Building) = runAction("Building deleted") {
        repository.deleteBuilding(building)
    }

    fun saveFlat(flat: Flat) = runAction("Flat saved") {
        repository.saveFlat(flat)
    }

    fun deleteFlat(flat: Flat) = runAction("Flat deleted") {
        repository.deleteFlat(flat)
    }

    fun saveAndAssignTenant(tenant: Tenant, flat: Flat) = runAction("Tenant assigned to ${flat.flatNumber}") {
        repository.saveAndAssignTenant(tenant, flat)
    }

    fun toggleTenantStatus(tenant: Tenant) = runAction("Tenant status updated") {
        repository.toggleTenantStatus(tenant)
    }

    fun vacateTenant(tenant: Tenant) = runAction("Flat vacated") {
        repository.vacateTenant(tenant)
    }

    fun deleteTenant(tenant: Tenant) = runAction("Tenant deleted") {
        repository.deleteTenant(tenant)
    }

    fun generateMonthlyBills(carryForward: Boolean) {
        val snap = uiState.value
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.generateMonthlyBillsForOccupiedFlats(
                flats = snap.flats,
                existingBills = snap.bills,
                monthKey = snap.currentMonthKey,
                billingMonth = snap.currentBillingMonth,
                carryForwardUnpaid = carryForward
            )
            _isBusy.value = false
            _actionMessage.value = res.fold(
                onSuccess = { count ->
                    if (count == 0) "All occupied flats already have bills for ${snap.currentBillingMonth}"
                    else "Generated $count monthly bill(s) for ${snap.currentBillingMonth}"
                },
                onFailure = { it.localizedMessage ?: "Failed to generate bills" }
            )
        }
    }

    fun verifyPayment(payment: PaymentRecord, targetBank: BankAccount?) {
        val bill = uiState.value.bills.firstOrNull { it.id == payment.billId } ?: return
        runAction("Payment verified & receipt generated") {
            repository.verifyPendingPayment(payment, bill, targetBank)
        }
    }

    fun rejectPayment(payment: PaymentRecord, reason: String) = runAction("Payment rejected") {
        repository.rejectPendingPayment(payment, reason)
    }

    fun recordDirectPayment(
        bill: MonthlyBill,
        amount: Double,
        method: String,
        reference: String,
        bankAccount: BankAccount?,
        notes: String
    ) = runAction("Payment recorded & receipt issued") {
        repository.recordAdminDirectPayment(bill, amount, method, reference, bankAccount, notes)
    }

    fun addBankAccount(account: BankAccount) = runAction("Bank account added") {
        repository.addBankAccount(account)
    }

    fun recordBankTx(account: BankAccount, amount: Double, type: String, desc: String, ref: String) =
        runAction("Bank transaction recorded") {
            repository.recordBankTransaction(account, amount, type, desc, ref)
        }

    fun recordCashTx(amount: Double, type: String, desc: String, ref: String) =
        runAction("Cash transaction recorded") {
            repository.recordCashTransaction(amount, type, desc, ref)
        }

    fun addExpense(expense: Expense, bankAccount: BankAccount?) =
        runAction("Expense recorded") {
            repository.addExpense(expense, bankAccount)
        }

    fun deleteBankAccount(account: BankAccount) =
        runAction("Bank account deleted") {
            repository.deleteBankAccount(account)
        }

    fun deleteBankTx(tx: BankTransaction) {
        val account = uiState.value.bankAccounts.firstOrNull { it.id == tx.bankAccountId }
        runAction("Bank transaction deleted") {
            repository.deleteBankTransaction(tx, account)
        }
    }

    fun deleteCashTx(tx: CashTransaction) =
        runAction("Cash transaction deleted") {
            repository.deleteCashTransaction(tx)
        }

    fun deleteExpense(expense: Expense) {
        val account = if (expense.bankAccountId.isNotBlank()) {
            uiState.value.bankAccounts.firstOrNull { it.id == expense.bankAccountId }
        } else null
        runAction("Expense deleted") {
            repository.deleteExpense(expense, account)
        }
    }

    fun deletePayment(payment: PaymentRecord) {
        val snap = uiState.value
        val bill = snap.bills.firstOrNull { it.id == payment.billId }
        val refMatch = payment.referenceNumber.ifBlank { payment.transactionId }
        val matchedTx = snap.bankTransactions.firstOrNull {
            it.transactionType == "received" &&
                (it.referenceNumber == refMatch || it.referenceNumber == payment.transactionId)
        }
        val account = if (matchedTx != null) {
            snap.bankAccounts.firstOrNull { it.id == matchedTx.bankAccountId }
        } else {
            null
        }
        runAction("Payment deleted") {
            repository.deletePayment(payment, bill, account)
        }
    }

    fun deleteReceipt(receipt: Receipt) =
        runAction("Receipt deleted") {
            repository.deleteReceipt(receipt)
        }

    fun sendPersonalMessage(tenant: Tenant, title: String, body: String, type: String = "personal") =
        runAction("Message sent to ${tenant.name}") {
            repository.sendPersonalMessage(tenant, title, body, type)
        }

    fun sendAnnouncement(title: String, body: String, category: String, building: Building?) =
        runAction("Announcement broadcast sent") {
            repository.sendAnnouncement(title, body, category, building, uiState.value.tenants)
        }

    fun updateSettings(settings: AppSettings) =
        runAction("Settings updated") {
            repository.updateAppSettings(settings)
        }

    fun buildBackupJson(): String {
        val snap = uiState.value
        return repository.buildBackupJson(
            snap.buildings,
            snap.flats,
            snap.tenants,
            snap.bills,
            snap.payments,
            snap.expenses
        )
    }
}

private data class Quint<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
