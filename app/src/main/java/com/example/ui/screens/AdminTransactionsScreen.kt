package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BankAccount
import com.example.data.model.BankTransaction
import com.example.data.model.Building
import com.example.data.model.CashTransaction
import com.example.data.model.Expense
import com.example.data.model.MonthlyBill
import com.example.data.model.PaymentRecord
import com.example.data.model.Receipt
import com.example.i18n.AppLanguage
import com.example.i18n.LocalLanguage
import com.example.i18n.LocalStrings
import com.example.ui.components.ReceiptDetailDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusPaidText
import com.example.ui.theme.StatusUnpaidText
import com.example.ui.viewmodel.AdminDataSnapshot
import com.example.util.FormatUtils
import com.example.util.PdfAndExportManager

@Composable
fun AdminTransactionsScreen(
    state: AdminDataSnapshot,
    onVerifyPayment: (PaymentRecord, BankAccount?) -> Unit,
    onRejectPayment: (PaymentRecord, String) -> Unit,
    onRecordDirectPayment: (MonthlyBill, Double, String, String, BankAccount?, String) -> Unit,
    onAddBankAccount: (BankAccount) -> Unit,
    onRecordBankTx: (BankAccount, Double, String, String, String) -> Unit,
    onRecordCashTx: (Double, String, String, String) -> Unit,
    onAddExpense: (Expense, BankAccount?) -> Unit,
    onDeletePayment: (PaymentRecord) -> Unit = {},
    onDeleteBankAccount: (BankAccount) -> Unit = {},
    onDeleteBankTx: (BankTransaction) -> Unit = {},
    onDeleteCashTx: (CashTransaction) -> Unit = {},
    onDeleteExpense: (Expense) -> Unit = {},
    onDeleteReceipt: (Receipt) -> Unit = {}
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        strings.navPayments,
        strings.bankAccounts,
        strings.cashTransactions,
        strings.expenses,
        strings.downloadReceipt.substringBefore(" ")
    )

    var showDirectPaymentDialog by remember { mutableStateOf(false) }
    var showAddBankDialog by remember { mutableStateOf(false) }
    var bankTxTarget by remember { mutableStateOf<BankAccount?>(null) }
    var showCashTxDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var selectedReceipt by remember { mutableStateOf<Receipt?>(null) }
    var pendingDelete by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        Button(
                            onClick = { showDirectPaymentDialog = true },
                            enabled = state.bills.any { it.remainingDue > 0.0 },
                            modifier = Modifier.fillMaxWidth().testTag("record_direct_payment_btn")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (language == AppLanguage.BN) "সরাসরি ভাড়া আদায় রেকর্ড করুন" else "Record Direct Cash / Bank Payment")
                        }
                    }
                    items(state.payments, key = { it.id }) { p ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${p.buildingName} - ${p.flatNumber}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("${p.tenantName} • ${p.method.uppercase()} • ${p.paymentDate}", style = MaterialTheme.typography.bodySmall)
                                        Text("Tx: ${p.transactionId} | Ref: ${p.referenceNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(FormatUtils.formatBdt(p.amount, language), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                            StatusBadge(p.verificationStatus)
                                        }
                                        IconButton(
                                            onClick = {
                                                pendingDelete = "${p.buildingName} - ${p.flatNumber} (${FormatUtils.formatBdt(p.amount, language)})" to {
                                                    onDeletePayment(p)
                                                }
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_payment_${p.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = strings.delete,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                if (p.verificationStatus == "pending") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onVerifyPayment(p, state.bankAccounts.firstOrNull()) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(strings.verifyPayment)
                                        }
                                        OutlinedButton(
                                            onClick = { onRejectPayment(p, "Unverified reference") }
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(strings.rejectPayment)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        Button(
                            onClick = { showAddBankDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("add_bank_account_btn")
                        ) {
                            Icon(Icons.Filled.AccountBalance, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.addBankAccount)
                        }
                    }
                    items(state.bankAccounts, key = { it.id }) { acc ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(acc.accountName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("${acc.bankName} • ${acc.accountType} (${acc.accountNumber})", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        FormatUtils.formatBdt(acc.currentBalance, language),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "Opening: ${FormatUtils.formatBdt(acc.openingBalance, language)} | Received: ${FormatUtils.formatBdt(acc.moneyReceived, language)} | Expenses: ${FormatUtils.formatBdt(acc.bankExpenses, language)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(onClick = { bankTxTarget = acc }) {
                                        Text(strings.addBankEntry)
                                    }
                                    IconButton(
                                        onClick = {
                                            pendingDelete = "${acc.accountName} (${acc.bankName})" to {
                                                onDeleteBankAccount(acc)
                                            }
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_bank_account_${acc.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = strings.delete,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                    items(state.bankTransactions, key = { it.id }) { tx ->
                        LedgerRowCard(
                            title = tx.description,
                            subtitle = "${tx.bankAccountName} • ${tx.date} • Ref: ${tx.referenceNumber}",
                            amount = tx.amount,
                            isPositive = tx.transactionType == "received",
                            language = language,
                            deleteTag = "delete_bank_tx_${tx.id}",
                            deleteContentDescription = strings.delete,
                            onDelete = {
                                pendingDelete = "${tx.description} (${FormatUtils.formatBdt(tx.amount, language)})" to {
                                    onDeleteBankTx(tx)
                                }
                            }
                        )
                    }
                }

                2 -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(strings.totalCashBalance, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        FormatUtils.formatBdt(state.totalCashBalance, language),
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Button(onClick = { showCashTxDialog = true }) {
                                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.addCashEntry)
                                }
                            }
                        }
                    }
                    items(state.cashTransactions, key = { it.id }) { tx ->
                        LedgerRowCard(
                            title = tx.description,
                            subtitle = "${tx.date} • ${tx.transactionType} • Ref: ${tx.referenceNumber}",
                            amount = tx.amount,
                            isPositive = tx.transactionType == "cash_in",
                            language = language,
                            deleteTag = "delete_cash_tx_${tx.id}",
                            deleteContentDescription = strings.delete,
                            onDelete = {
                                pendingDelete = "${tx.description} (${FormatUtils.formatBdt(tx.amount, language)})" to {
                                    onDeleteCashTx(tx)
                                }
                            }
                        )
                    }
                }

                3 -> {
                    item {
                        Button(
                            onClick = { showExpenseDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("add_expense_btn")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.addExpense)
                        }
                    }
                    items(state.expenses, key = { it.id }) { exp ->
                        LedgerRowCard(
                            title = "${exp.category}: ${exp.description}",
                            subtitle = "${exp.date} • ${exp.buildingName.ifBlank { "General" }} • ${exp.paymentSource.uppercase()}",
                            amount = exp.amount,
                            isPositive = false,
                            language = language,
                            deleteTag = "delete_expense_${exp.id}",
                            deleteContentDescription = strings.delete,
                            onDelete = {
                                pendingDelete = "${exp.category}: ${exp.description} (${FormatUtils.formatBdt(exp.amount, language)})" to {
                                    onDeleteExpense(exp)
                                }
                            }
                        )
                    }
                }

                4 -> {
                    items(state.receipts, key = { it.id }) { rcp ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedReceipt = rcp },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text("${rcp.receiptNumber} • ${rcp.flatNumber}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("${rcp.tenantName} • ${rcp.paymentDate}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        FormatUtils.formatBdt(rcp.amountPaid, language),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = {
                                            pendingDelete = "${rcp.receiptNumber} (${FormatUtils.formatBdt(rcp.amountPaid, language)})" to {
                                                onDeleteReceipt(rcp)
                                            }
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_receipt_${rcp.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = strings.delete,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDirectPaymentDialog) {
        DirectPaymentDialog(
            bills = state.bills.filter { it.remainingDue > 0.0 },
            bankAccounts = state.bankAccounts,
            onDismiss = { showDirectPaymentDialog = false },
            onSave = { bill, amt, method, ref, acc, notes ->
                showDirectPaymentDialog = false
                onRecordDirectPayment(bill, amt, method, ref, acc, notes)
            }
        )
    }

    if (showAddBankDialog) {
        AddBankAccountDialog(
            onDismiss = { showAddBankDialog = false },
            onSave = {
                showAddBankDialog = false
                onAddBankAccount(it)
            }
        )
    }

    bankTxTarget?.let { acc ->
        BankTxDialog(
            account = acc,
            onDismiss = { bankTxTarget = null },
            onSave = { amt, type, desc, ref ->
                bankTxTarget = null
                onRecordBankTx(acc, amt, type, desc, ref)
            }
        )
    }

    if (showCashTxDialog) {
        CashTxDialog(
            onDismiss = { showCashTxDialog = false },
            onSave = { amt, type, desc, ref ->
                showCashTxDialog = false
                onRecordCashTx(amt, type, desc, ref)
            }
        )
    }

    if (showExpenseDialog) {
        AddExpenseDialog(
            buildings = state.buildings,
            bankAccounts = state.bankAccounts,
            onDismiss = { showExpenseDialog = false },
            onSave = { exp, acc ->
                showExpenseDialog = false
                onAddExpense(exp, acc)
            }
        )
    }

    selectedReceipt?.let { rcp ->
        ReceiptDetailDialog(
            receipt = rcp,
            onDismiss = { selectedReceipt = null },
            onDownloadPdf = {
                val file = PdfAndExportManager.generateReceiptPdf(context, it, language)
                PdfAndExportManager.shareOrOpenFile(context, file, "application/pdf", strings.downloadReceipt)
            }
        )
    }

    pendingDelete?.let { (label, confirmAction) ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text(if (language == AppLanguage.BN) "তথ্যটি মুছে ফেলবেন?" else "Delete this entry?")
            },
            text = {
                Text(
                    if (language == AppLanguage.BN) {
                        "\"$label\" স্থায়ীভাবে মুছে ফেলা হবে এবং সংশ্লিষ্ট ব্যালেন্স স্বয়ংক্রিয়ভাবে সমন্বয় হবে।"
                    } else {
                        "\"$label\" will be permanently deleted and related balances will be adjusted."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDelete = null
                        confirmAction()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun LedgerRowCard(
    title: String,
    subtitle: String,
    amount: Double,
    isPositive: Boolean,
    language: AppLanguage,
    deleteTag: String,
    deleteContentDescription: String,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (isPositive) "+" else "-"}${FormatUtils.formatBdt(amount, language)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isPositive) StatusPaidText else StatusUnpaidText
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag(deleteTag)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = deleteContentDescription,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectPaymentDialog(
    bills: List<MonthlyBill>,
    bankAccounts: List<BankAccount>,
    onDismiss: () -> Unit,
    onSave: (MonthlyBill, Double, String, String, BankAccount?, String) -> Unit
) {
    val strings = LocalStrings.current
    var selectedBill by remember { mutableStateOf(bills.firstOrNull()) }
    var amount by remember { mutableStateOf(selectedBill?.remainingDue?.toInt()?.toString() ?: "") }
    var method by remember { mutableStateOf("cash") }
    var ref by remember { mutableStateOf("") }
    var selectedBank by remember { mutableStateOf(bankAccounts.firstOrNull()) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bills.take(6).forEach { b ->
                    FilterChip(
                        selected = selectedBill?.id == b.id,
                        onClick = {
                            selectedBill = b
                            amount = b.remainingDue.toInt().toString()
                        },
                        label = { Text("${b.flatNumber} (${b.billingMonth}) - Due ${b.remainingDue.toInt()}") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("cash", "bkash", "nagad", "bank_transfer").forEach { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(strings.paymentAmount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text(strings.transactionRef) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notes) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val b = selectedBill ?: return@Button
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    onSave(b, amt, method, ref, if (method == "cash") null else selectedBank, notes)
                },
                enabled = selectedBill != null && (amount.toDoubleOrNull() ?: 0.0) > 0.0
            ) { Text(strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun AddBankAccountDialog(
    onDismiss: () -> Unit,
    onSave: (BankAccount) -> Unit
) {
    val strings = LocalStrings.current
    var accountName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("Current") }
    var opening by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addBankAccount) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = accountName, onValueChange = { accountName = it }, label = { Text("Account Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bankName, onValueChange = { bankName = it }, label = { Text("Bank Name (e.g. BRAC, DBBL, Islami Bank)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text("Account Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = accountType, onValueChange = { accountType = it }, label = { Text("Account Type (Savings/Current)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = opening,
                    onValueChange = { opening = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(strings.openingBalance) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        BankAccount(
                            accountName = accountName,
                            bankName = bankName,
                            accountNumber = accountNumber,
                            accountType = accountType,
                            openingBalance = opening.toDoubleOrNull() ?: 0.0
                        )
                    )
                },
                enabled = accountName.isNotBlank() && bankName.isNotBlank() && accountNumber.isNotBlank()
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun BankTxDialog(
    account: BankAccount,
    onDismiss: () -> Unit,
    onSave: (Double, String, String, String) -> Unit
) {
    val strings = LocalStrings.current
    var type by remember { mutableStateOf("received") }
    var amount by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var ref by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.addBankEntry} (${account.accountName})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("received", "transferred", "expense").forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                    }
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(strings.paymentAmount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(strings.description) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ref, onValueChange = { ref = it }, label = { Text(strings.transactionRef) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(amount.toDoubleOrNull() ?: 0.0, type, desc, ref) },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0 && desc.isNotBlank()
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun CashTxDialog(
    onDismiss: () -> Unit,
    onSave: (Double, String, String, String) -> Unit
) {
    val strings = LocalStrings.current
    var type by remember { mutableStateOf("cash_in") }
    var amount by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var ref by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addCashEntry) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "cash_in", onClick = { type = "cash_in" }, label = { Text("Cash In") })
                    FilterChip(selected = type == "cash_out", onClick = { type = "cash_out" }, label = { Text("Cash Out") })
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(strings.paymentAmount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(strings.description) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ref, onValueChange = { ref = it }, label = { Text(strings.transactionRef) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(amount.toDoubleOrNull() ?: 0.0, type, desc, ref) },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0 && desc.isNotBlank()
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun AddExpenseDialog(
    buildings: List<Building>,
    bankAccounts: List<BankAccount>,
    onDismiss: () -> Unit,
    onSave: (Expense, BankAccount?) -> Unit
) {
    val strings = LocalStrings.current
    val categories = listOf("Building Maintenance", "Electricity Expenses", "Water Expenses", "Cleaning", "Security", "Repairs", "Other Expenses")
    var category by remember { mutableStateOf(categories.first()) }
    var selectedBuilding by remember { mutableStateOf(buildings.firstOrNull()) }
    var amount by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("cash") }
    var selectedBank by remember { mutableStateOf(bankAccounts.firstOrNull()) }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addExpense) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = source == "cash", onClick = { source = "cash" }, label = { Text(strings.cash) })
                    FilterChip(selected = source == "bank", onClick = { source = "bank" }, label = { Text("Bank") })
                }
                if (buildings.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        buildings.take(3).forEach { b ->
                            FilterChip(selected = selectedBuilding?.id == b.id, onClick = { selectedBuilding = b }, label = { Text(b.name.take(10)) })
                        }
                    }
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(strings.paymentAmount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(strings.description) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        Expense(
                            buildingId = selectedBuilding?.id ?: "",
                            buildingName = selectedBuilding?.name ?: "General",
                            category = category,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            date = FormatUtils.todayDdMmYyyy(),
                            paymentSource = source,
                            description = desc
                        ),
                        if (source == "bank") selectedBank else null
                    )
                },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0 && desc.isNotBlank()
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
