package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BankAccount
import com.example.data.model.PaymentRecord
import com.example.i18n.AppLanguage
import com.example.i18n.LocalLanguage
import com.example.i18n.LocalStrings
import com.example.ui.components.ChartBarItem
import com.example.ui.components.FinancialComparisonChart
import com.example.ui.components.LanguageToggleChip
import com.example.ui.components.ReceiptSlipPreviewDialog
import com.example.ui.components.StatSummaryCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusPaidText
import com.example.ui.theme.StatusPartialText
import com.example.ui.theme.StatusUnpaidText
import com.example.ui.viewmodel.AdminDataSnapshot
import com.example.util.FormatUtils
import com.example.util.PdfAndExportManager

@Composable
fun AdminDashboardScreen(
    state: AdminDataSnapshot,
    onLanguageChange: (AppLanguage) -> Unit,
    onGenerateMonthlyBills: (Boolean) -> Unit,
    onVerifyPayment: (PaymentRecord, BankAccount?) -> Unit,
    onRejectPayment: (PaymentRecord, String) -> Unit,
    onNavigateToBuildings: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onMarkNotificationRead: (String) -> Unit = {},
    onMarkAllNotificationsRead: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    val context = LocalContext.current

    var showGenerateDialog by remember { mutableStateOf(false) }
    var carryForwardChecked by remember { mutableStateOf(state.settings.carryForwardEnabled) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showReportsDialog by remember { mutableStateOf(false) }
    var verifyBankTargetPayment by remember { mutableStateOf<PaymentRecord?>(null) }
    var previewSlipPayment by remember { mutableStateOf<PaymentRecord?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Emerald Header Banner
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = strings.roleAdminBadge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = strings.appName,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "${FormatUtils.toLocalizedDigits(FormatUtils.todayDdMmYyyy(), language)} (Asia/Dhaka)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showNotificationsDialog = true },
                                    modifier = Modifier.testTag("admin_notifications_button")
                                ) {
                                    val unread = state.notifications.count { !it.isRead }
                                    BadgedBox(
                                        badge = {
                                            if (unread > 0) {
                                                Badge { Text("$unread") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Notifications,
                                            contentDescription = strings.notifications,
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                LanguageToggleChip(
                                    currentLanguage = language,
                                    onToggle = onLanguageChange
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showGenerateDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_monthly_bills_button"),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimary,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.generateMonthlyBills, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { showReportsDialog = true },
                                modifier = Modifier.testTag("open_reports_center_button"),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("PDF / Excel")
                            }
                        }
                    }
                }
            }
        }

        // Urgent Alert Banner when tenant submits cash/bank payment proof waiting for "OK"
        if (state.pendingPayments.isNotEmpty()) {
            item {
                val firstPending = state.pendingPayments.first()
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = if (language == AppLanguage.BN) {
                                        "নতুন পেমেন্ট প্রমাণ এসেছে (${FormatUtils.formatInt(state.pendingPayments.size, language)}টি)"
                                    } else {
                                        "Pending Payment Proofs (${state.pendingPayments.size})"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(onClick = { showNotificationsDialog = true }) {
                                Text(if (language == AppLanguage.BN) "সব দেখুন" else "View All")
                            }
                        }
                        Text(
                            text = "${firstPending.buildingName} - ${firstPending.flatNumber} • ${firstPending.tenantName} • ${FormatUtils.formatBdt(firstPending.amount, language)} (${firstPending.method.uppercase()})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (firstPending.receiptImageBase64.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { previewSlipPayment = firstPending },
                                    modifier = Modifier.testTag("top_alert_view_proof_btn")
                                ) {
                                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.BN) "প্রমাণ দেখুন" else "View Proof")
                                }
                            }
                            Button(
                                onClick = {
                                    val preMatchedBank = firstPending.targetBankAccountId.takeIf { it.isNotBlank() }?.let { tid ->
                                        state.bankAccounts.firstOrNull { it.id == tid }
                                    }
                                    if (firstPending.method == "cash") {
                                        onVerifyPayment(firstPending, null)
                                    } else if (preMatchedBank != null) {
                                        onVerifyPayment(firstPending, preMatchedBank)
                                    } else if (state.bankAccounts.size > 1) {
                                        verifyBankTargetPayment = firstPending
                                    } else {
                                        onVerifyPayment(firstPending, state.bankAccounts.firstOrNull())
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("top_alert_accept_bill_btn")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.verifyPayment)
                            }
                        }
                    }
                }
            }
        }

        // 12 Summary KPI Cards (2 columns x 6 rows)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatSummaryCard(
                        title = strings.totalBuildings,
                        value = FormatUtils.formatInt(state.totalBuildings, language),
                        icon = Icons.Filled.Apartment,
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryCard(
                        title = strings.totalFlats,
                        value = FormatUtils.formatInt(state.totalFlats, language),
                        icon = Icons.Filled.Home,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatSummaryCard(
                        title = strings.occupiedFlats,
                        value = FormatUtils.formatInt(state.occupiedFlats, language),
                        icon = Icons.Filled.CheckCircle,
                        accentColor = StatusPaidText,
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryCard(
                        title = strings.vacantFlats,
                        value = FormatUtils.formatInt(state.vacantFlats, language),
                        icon = Icons.Filled.DoorFront,
                        accentColor = StatusPartialText,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatSummaryCard(
                        title = strings.totalMonthlyRent,
                        value = FormatUtils.formatBdt(state.totalMonthlyRent, language),
                        icon = Icons.Filled.Payments,
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryCard(
                        title = strings.totalServiceCharges,
                        value = FormatUtils.formatBdt(state.totalServiceCharges, language),
                        icon = Icons.Filled.ReceiptLong,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatSummaryCard(
                        title = strings.totalCollected,
                        value = FormatUtils.formatBdt(state.totalCollectedAmount, language),
                        icon = Icons.Filled.TrendingUp,
                        accentColor = StatusPaidText,
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryCard(
                        title = strings.totalOutstandingDue,
                        value = FormatUtils.formatBdt(state.totalOutstandingDues, language),
                        icon = Icons.Filled.MoneyOff,
                        accentColor = StatusUnpaidText,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatSummaryCard(
                        title = strings.totalBankBalance,
                        value = FormatUtils.formatBdt(state.totalBankBalance, language),
                        icon = Icons.Filled.AccountBalance,
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryCard(
                        title = strings.totalCashBalance,
                        value = FormatUtils.formatBdt(state.totalCashBalance, language),
                        icon = Icons.Filled.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatSummaryCard(
                        title = strings.monthlyIncome,
                        value = FormatUtils.formatBdt(state.monthlyIncome, language),
                        icon = Icons.Filled.AttachMoney,
                        accentColor = StatusPaidText,
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryCard(
                        title = strings.monthlyExpenses,
                        value = FormatUtils.formatBdt(state.monthlyExpenses, language),
                        icon = Icons.Filled.TrendingDown,
                        accentColor = StatusUnpaidText,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Interactive Financial Comparison Chart
        item {
            val chartItems = if (state.buildings.isNotEmpty()) {
                state.buildings.take(5).map { b ->
                    val bBills = state.bills.filter { it.buildingId == b.id }
                    ChartBarItem(
                        label = b.name.take(10),
                        primaryAmount = bBills.sumOf { it.amountPaid },
                        secondaryAmount = bBills.sumOf { it.remainingDue }
                    )
                }
            } else {
                listOf(
                    ChartBarItem(
                        label = state.currentBillingMonth,
                        primaryAmount = state.monthlyIncome,
                        secondaryAmount = state.monthlyExpenses
                    )
                )
            }
            FinancialComparisonChart(
                title = strings.financialChartTitle,
                items = chartItems,
                primaryLegend = strings.totalCollected,
                secondaryLegend = strings.totalOutstandingDue
            )
        }

        // Pending Payment Approvals Queue
        if (state.pendingPayments.isNotEmpty()) {
            item {
                Text(
                    text = "${strings.pendingApprovals} (${state.pendingPayments.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            items(state.pendingPayments, key = { it.id }) { payment ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${payment.buildingName} - ${payment.flatNumber}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${payment.tenantName} • ${payment.method.uppercase()} • Ref: ${payment.referenceNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(payment.verificationStatus)
                        }
                        Text(
                            text = FormatUtils.formatBdt(payment.amount, language),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (payment.targetBankAccountName.isNotBlank()) {
                            Text(
                                text = if (language == AppLanguage.BN) "ব্যাংক: ${payment.targetBankAccountName}" else "Target Bank: ${payment.targetBankAccountName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (payment.receiptImageBase64.isNotBlank()) {
                            OutlinedButton(
                                onClick = { previewSlipPayment = payment },
                                modifier = Modifier.fillMaxWidth().testTag("admin_dash_view_slip_${payment.id}")
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (language == AppLanguage.BN) "পেমেন্টের প্রমাণ / রিসিট দেখুন" else "View Payment Proof / Slip")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val preMatchedBank = payment.targetBankAccountId.takeIf { it.isNotBlank() }?.let { tid ->
                                        state.bankAccounts.firstOrNull { it.id == tid }
                                    }
                                    if (payment.method == "cash") {
                                        onVerifyPayment(payment, null)
                                    } else if (preMatchedBank != null) {
                                        onVerifyPayment(payment, preMatchedBank)
                                    } else if (state.bankAccounts.size > 1) {
                                        verifyBankTargetPayment = payment
                                    } else {
                                        onVerifyPayment(payment, state.bankAccounts.firstOrNull())
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("verify_payment_${payment.id}")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.verifyPayment)
                            }
                            OutlinedButton(
                                onClick = { onRejectPayment(payment, "Reference could not be matched") },
                                modifier = Modifier.testTag("reject_payment_${payment.id}")
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

        // Quick Shortcuts if empty
        if (state.buildings.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.noBuildingsYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Button(onClick = onNavigateToBuildings) {
                            Text(strings.addBuilding)
                        }
                    }
                }
            }
        }
    }

    if (showGenerateDialog) {
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text(strings.generateMonthlyBills) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (language == AppLanguage.BN) {
                            "মাস: ${state.currentBillingMonth} — ভাড়া দেওয়া ফ্ল্যাট: ${state.occupiedFlats}টি।\nএকই মাসে ডুপ্লিকেট বিল তৈরি হবে না।"
                        } else {
                            "Month: ${state.currentBillingMonth} — Occupied Flats: ${state.occupiedFlats}.\nDuplicate bills for the same month are automatically skipped."
                        }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = carryForwardChecked,
                            onCheckedChange = { carryForwardChecked = it }
                        )
                        Text(
                            text = if (language == AppLanguage.BN) "পূর্বের বকেয়া যোগ করুন (Carry Forward)"
                            else "Carry forward previous unpaid dues",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGenerateDialog = false
                        onGenerateMonthlyBills(carryForwardChecked)
                    },
                    modifier = Modifier.testTag("confirm_generate_bills")
                ) {
                    Text(strings.generateMonthlyBills)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showReportsDialog) {
        AlertDialog(
            onDismissRequest = { showReportsDialog = false },
            title = { Text(strings.reportsCenter) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (language == AppLanguage.BN) {
                            "ভবনভিত্তিক, ফ্ল্যাটভিত্তিক, ভাড়াটিয়া পেমেন্ট, বকেয়া, ব্যাংক/ক্যাশ ও আয়-ব্যয়ের পূর্ণাঙ্গ রিপোর্ট রপ্তানি করুন:"
                        } else {
                            "Export complete building, flat, tenant, dues, bank/cash & income-expense reports:"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = {
                            val pdfFile = PdfAndExportManager.generateFinancialReportPdf(
                                context = context,
                                title = "Monthly & Yearly Financial Summary (${state.currentBillingMonth})",
                                buildings = state.buildings,
                                flats = state.flats,
                                bills = state.bills,
                                payments = state.payments,
                                expenses = state.expenses,
                                bankTransactions = state.bankTransactions,
                                cashTransactions = state.cashTransactions
                            )
                            PdfAndExportManager.shareOrOpenFile(
                                context,
                                pdfFile,
                                "application/pdf",
                                strings.exportPdf
                            )
                            showReportsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_financial_pdf_btn")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.exportPdf)
                    }
                    OutlinedButton(
                        onClick = {
                            val csvFile = PdfAndExportManager.generateExcelCsvReport(
                                context = context,
                                buildings = state.buildings,
                                flats = state.flats,
                                bills = state.bills,
                                payments = state.payments,
                                expenses = state.expenses
                            )
                            PdfAndExportManager.shareOrOpenFile(
                                context,
                                csvFile,
                                "text/csv",
                                strings.exportExcel
                            )
                            showReportsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_financial_excel_btn")
                    ) {
                        Icon(Icons.Filled.TableChart, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.exportExcel)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReportsDialog = false }) { Text(strings.close) }
            }
        )
    }

    if (showNotificationsDialog) {
        val unreadCount = state.notifications.count { !it.isRead }
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.notifications)
                    if (unreadCount > 0) {
                        TextButton(onClick = onMarkAllNotificationsRead) {
                            Text(if (language == AppLanguage.BN) "সব পঠিত" else "Mark all read")
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.notifications.isEmpty()) {
                        Text(
                            text = if (language == AppLanguage.BN) "কোনো নোটিফিকেশন নেই।" else "No notifications yet.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        state.notifications.take(15).forEach { n ->
                            val linkedPayment = if (n.type == "payment_submitted" && n.relatedId.isNotBlank()) {
                                state.payments.firstOrNull { it.id == n.relatedId }
                            } else {
                                null
                            }
                            Surface(
                                color = if (!n.isRead) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = if (!n.isRead) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(n.title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                    Text(n.body, style = MaterialTheme.typography.bodySmall)

                                    if (linkedPayment != null) {
                                        if (linkedPayment.receiptImageBase64.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = { previewSlipPayment = linkedPayment },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(if (language == AppLanguage.BN) "প্রমাণ / রিসিট ছবি দেখুন" else "View Proof / Receipt")
                                            }
                                        }
                                        if (linkedPayment.verificationStatus == "pending") {
                                            Button(
                                                onClick = {
                                                    val preMatchedBank = linkedPayment.targetBankAccountId.takeIf { it.isNotBlank() }?.let { tid ->
                                                        state.bankAccounts.firstOrNull { it.id == tid }
                                                    }
                                                    onMarkNotificationRead(n.id)
                                                    if (linkedPayment.method == "cash") {
                                                        onVerifyPayment(linkedPayment, null)
                                                        showNotificationsDialog = false
                                                    } else if (preMatchedBank != null) {
                                                        onVerifyPayment(linkedPayment, preMatchedBank)
                                                        showNotificationsDialog = false
                                                    } else if (state.bankAccounts.size > 1) {
                                                        showNotificationsDialog = false
                                                        verifyBankTargetPayment = linkedPayment
                                                    } else {
                                                        onVerifyPayment(linkedPayment, state.bankAccounts.firstOrNull())
                                                        showNotificationsDialog = false
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("notif_accept_bill_${linkedPayment.id}")
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(strings.verifyPayment)
                                            }
                                        } else {
                                            Text(
                                                text = if (linkedPayment.verificationStatus == "verified") {
                                                    if (language == AppLanguage.BN) "✓ বিল গ্রহণ করা হয়েছে" else "✓ Bill Accepted"
                                                } else {
                                                    if (language == AppLanguage.BN) "বাতিল করা হয়েছে" else "Rejected"
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = StatusPaidText
                                            )
                                        }
                                    } else if (!n.isRead) {
                                        TextButton(onClick = { onMarkNotificationRead(n.id) }) {
                                            Text(strings.markAsRead)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) { Text(strings.close) }
            }
        )
    }

    verifyBankTargetPayment?.let { targetPayment ->
        var pickedBank by remember(targetPayment.id) { mutableStateOf(state.bankAccounts.firstOrNull()) }
        AlertDialog(
            onDismissRequest = { verifyBankTargetPayment = null },
            title = {
                Text(if (language == AppLanguage.BN) "কোন ব্যাংকে জমা হবে?" else "Select Deposit Bank Account")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${targetPayment.buildingName} - ${targetPayment.flatNumber} • ${FormatUtils.formatBdt(targetPayment.amount, language)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    state.bankAccounts.forEach { acc ->
                        FilterChip(
                            selected = pickedBank?.id == acc.id,
                            onClick = { pickedBank = acc },
                            label = { Text("${acc.accountName} (${acc.bankName})") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        verifyBankTargetPayment = null
                        onVerifyPayment(targetPayment, pickedBank)
                    }
                ) { Text(strings.verifyPayment) }
            },
            dismissButton = {
                TextButton(onClick = { verifyBankTargetPayment = null }) { Text(strings.cancel) }
            }
        )
    }

    previewSlipPayment?.let { p ->
        ReceiptSlipPreviewDialog(
            base64Image = p.receiptImageBase64,
            title = if (language == AppLanguage.BN) "ভাড়াটিয়ার ব্যাংক রিসিট স্লিপ" else "Tenant Bank Receipt Slip",
            subtitle = "${p.buildingName} - ${p.flatNumber} • ${p.tenantName} • ${FormatUtils.formatBdt(p.amount, language)}",
            onDismiss = { previewSlipPayment = null }
        )
    }
}
