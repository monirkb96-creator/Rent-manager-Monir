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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.MonthlyBill
import com.example.data.model.Receipt
import com.example.data.model.UserProfile
import com.example.data.remote.GatewayInitiationResult
import com.example.i18n.AppLanguage
import com.example.i18n.LocalLanguage
import com.example.i18n.LocalStrings
import com.example.ui.components.LanguageToggleChip
import com.example.ui.components.ReceiptDetailDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusPaidText
import com.example.ui.theme.StatusUnpaidText
import com.example.ui.viewmodel.TenantDataSnapshot
import com.example.util.FormatUtils
import com.example.util.PdfAndExportManager

@Composable
fun TenantHomeScreen(
    profile: UserProfile,
    state: TenantDataSnapshot,
    gatewayResult: GatewayInitiationResult?,
    onDismissGatewayResult: () -> Unit,
    onSubmitPayment: (MonthlyBill, Double, String, String, String) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    var payTargetBill by remember { mutableStateOf<MonthlyBill?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = strings.roleTenantBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        LanguageToggleChip(currentLanguage = language, onToggle = onLanguageChange)
                    }

                    Text(
                        text = state.flat?.tenantName?.ifBlank { profile.displayName } ?: profile.displayName,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    if (state.flat != null) {
                        Text(
                            text = "${state.flat.buildingName} • Flat ${state.flat.flatNumber} (Floor ${state.flat.floorNumber})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    } else {
                        Text(
                            text = if (language == AppLanguage.BN) {
                                "আপনার ইমেইল (${profile.email}) এখনও কোনো ফ্ল্যাটে যুক্ত করা হয়নি। মালিককে আপনার গুগল ইমেইল যুক্ত করতে বলুন।"
                            } else {
                                "Your Google email (${profile.email}) is not assigned to a flat yet. Ask the owner to assign your email."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(strings.remainingDue, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                            Text(
                                text = FormatUtils.formatBdt(state.remainingDue, language),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        val billToPay = state.currentBill
                        if (billToPay != null && billToPay.remainingDue > 0.0) {
                            Button(
                                onClick = { payTargetBill = billToPay },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimary,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp).testTag("tenant_pay_now_button")
                            ) {
                                Icon(Icons.Filled.Payments, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.payNow, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Itemized Breakdown of Assigned Flat / Current Bill
        state.flat?.let { flat ->
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${strings.totalMonthlyBill} - ${flat.flatNumber}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        HorizontalDivider()
                        BillBreakdownRow(strings.monthlyRent, FormatUtils.formatBdt(flat.monthlyRent, language))
                        BillBreakdownRow(strings.serviceCharge, FormatUtils.formatBdt(flat.serviceCharge, language))
                        BillBreakdownRow(strings.electricityBill, FormatUtils.formatBdt(flat.electricityBill, language))
                        BillBreakdownRow(strings.waterBill, FormatUtils.formatBdt(flat.waterBill, language))
                        BillBreakdownRow(strings.gasBill, FormatUtils.formatBdt(flat.gasBill, language))
                        BillBreakdownRow(strings.otherCharges, FormatUtils.formatBdt(flat.otherCharges, language))
                        HorizontalDivider()
                        BillBreakdownRow("Total Payable", FormatUtils.formatBdt(state.totalPayable, language), bold = true)
                        BillBreakdownRow(strings.amountPaid, FormatUtils.formatBdt(state.totalPaid, language), bold = true)
                        BillBreakdownRow(strings.remainingDue, FormatUtils.formatBdt(state.remainingDue, language), bold = true)
                    }
                }
            }
        }

        // Recent Announcements / Owner Messages
        if (state.announcements.isNotEmpty() || state.messages.isNotEmpty()) {
            item {
                Text(
                    text = "${strings.announcements} & ${strings.messages}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            items(state.announcements.take(3), key = { "ann_${it.id}" }) { ann ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(ann.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(ann.body, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    payTargetBill?.let { bill ->
        PayBillDialog(
            bill = bill,
            settings = state.settings,
            onDismiss = { payTargetBill = null },
            onSubmit = { amt, method, ref, notes ->
                payTargetBill = null
                onSubmitPayment(bill, amt, method, ref, notes)
            }
        )
    }

    gatewayResult?.let { res ->
        AlertDialog(
            onDismissRequest = onDismissGatewayResult,
            title = { Text("SSLCOMMERZ / Gateway") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Transaction ID: ${res.transactionId}", fontWeight = FontWeight.Bold)
                    Text(res.statusMessage)
                    if (!res.gatewayPageUrl.isNullOrBlank()) {
                        Text("Gateway URL: ${res.gatewayPageUrl}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismissGatewayResult) { Text(strings.close) } }
        )
    }
}

@Composable
fun TenantBillsScreen(
    state: TenantDataSnapshot,
    onSubmitPayment: (MonthlyBill, Double, String, String, String) -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    var payTarget by remember { mutableStateOf<MonthlyBill?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(strings.navMyBills, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
        items(state.bills, key = { it.id }) { bill ->
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
                        Text("${bill.billingMonth} • ${bill.flatNumber}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        StatusBadge(bill.status)
                    }
                    BillBreakdownRow(strings.monthlyRent, FormatUtils.formatBdt(bill.monthlyRent, language))
                    BillBreakdownRow(strings.serviceCharge, FormatUtils.formatBdt(bill.serviceCharge, language))
                    BillBreakdownRow(strings.totalMonthlyBill, FormatUtils.formatBdt(bill.totalBill, language), bold = true)
                    BillBreakdownRow(strings.amountPaid, FormatUtils.formatBdt(bill.amountPaid, language))
                    BillBreakdownRow(strings.remainingDue, FormatUtils.formatBdt(bill.remainingDue, language), bold = true)

                    if (bill.remainingDue > 0.0) {
                        Button(
                            onClick = { payTarget = bill },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(strings.payNow)
                        }
                    }
                }
            }
        }
    }

    payTarget?.let { bill ->
        PayBillDialog(
            bill = bill,
            settings = state.settings,
            onDismiss = { payTarget = null },
            onSubmit = { amt, method, ref, notes ->
                payTarget = null
                onSubmitPayment(bill, amt, method, ref, notes)
            }
        )
    }
}

@Composable
fun TenantPaymentsScreen(
    state: TenantDataSnapshot,
    onSubmitPayment: (MonthlyBill, Double, String, String, String) -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    val context = LocalContext.current

    var selectedReceipt by remember { mutableStateOf<Receipt?>(null) }
    var payTarget by remember { mutableStateOf<MonthlyBill?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.paymentHistory, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                state.currentBill?.let { b ->
                    if (b.remainingDue > 0.0) {
                        Button(onClick = { payTarget = b }) {
                            Text(strings.payNow)
                        }
                    }
                }
            }
        }

        items(state.payments, key = { it.id }) { p ->
            val linkedReceipt = state.receipts.firstOrNull { it.paymentId == p.id || it.id == p.receiptId }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(FormatUtils.formatBdt(p.amount, language), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            Text("${p.method.uppercase()} • ${p.paymentDate}", style = MaterialTheme.typography.bodySmall)
                            Text("Tx: ${p.transactionId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(p.verificationStatus)
                    }
                    if (linkedReceipt != null) {
                        OutlinedButton(
                            onClick = { selectedReceipt = linkedReceipt },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.downloadReceipt)
                        }
                    }
                }
            }
        }
    }

    payTarget?.let { bill ->
        PayBillDialog(
            bill = bill,
            settings = state.settings,
            onDismiss = { payTarget = null },
            onSubmit = { amt, method, ref, notes ->
                payTarget = null
                onSubmitPayment(bill, amt, method, ref, notes)
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
}

@Composable
fun TenantMessagesScreen(
    state: TenantDataSnapshot,
    onMarkRead: (String) -> Unit,
    onReplyToOwner: (String, String) -> Unit
) {
    val strings = LocalStrings.current
    var showReplyDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.messages, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Button(onClick = { showReplyDialog = true }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.sendMessage)
                }
            }
        }

        items(state.messages, key = { it.id }) { msg ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (msg.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(msg.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        if (!msg.isRead) {
                            TextButton(onClick = { onMarkRead(msg.id) }) {
                                Icon(Icons.Filled.MarkEmailRead, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.markAsRead)
                            }
                        }
                    }
                    Text(msg.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (state.announcements.isNotEmpty()) {
            item {
                Text(strings.announcements, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            items(state.announcements, key = { "bcast_${it.id}" }) { ann ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(ann.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(ann.body, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showReplyDialog) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReplyDialog = false },
            title = { Text(strings.sendMessage) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(strings.messageTitle) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text(strings.messageBody) }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReplyDialog = false
                        onReplyToOwner(title, body)
                    },
                    enabled = title.isNotBlank() && body.isNotBlank()
                ) { Text(strings.sendMessage) }
            },
            dismissButton = { TextButton(onClick = { showReplyDialog = false }) { Text(strings.cancel) } }
        )
    }
}

@Composable
fun TenantProfileScreen(
    profile: UserProfile,
    state: TenantDataSnapshot,
    onLanguageChange: (AppLanguage) -> Unit,
    onSignOut: () -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(strings.navProfile, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(profile.displayName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(profile.email, style = MaterialTheme.typography.bodySmall)
                state.flat?.let { flat ->
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text("${flat.buildingName} • Flat ${flat.flatNumber}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.switchLanguage, style = MaterialTheme.typography.titleSmall)
                LanguageToggleChip(currentLanguage = language, onToggle = onLanguageChange)
            }
        }

        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("tenant_sign_out_button"),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.signOut)
        }
    }
}

@Composable
private fun BillBreakdownRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (bold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall)
        Text(value, style = if (bold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PayBillDialog(
    bill: MonthlyBill,
    settings: com.example.data.model.AppSettings,
    onDismiss: () -> Unit,
    onSubmit: (Double, String, String, String) -> Unit
) {
    val strings = LocalStrings.current
    var method by remember { mutableStateOf("bkash") }
    var amount by remember { mutableStateOf(bill.remainingDue.toInt().toString()) }
    var ref by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val methods = listOf(
        "bkash" to strings.bkash,
        "nagad" to strings.nagad,
        "rocket" to strings.rocket,
        "bank_transfer" to strings.bankTransfer,
        "card" to strings.debitCreditCard,
        "sslcommerz" to strings.sslCommerzGateway
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.payNow} (${bill.flatNumber})") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Due: BDT ${bill.remainingDue.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                methods.forEach { (key, label) ->
                    FilterChip(
                        selected = method == key,
                        onClick = { method = key },
                        label = { Text(label) }
                    )
                }

                // Show owner instructions for MFS / Bank Transfer
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        when (method) {
                            "bkash" -> Text("bKash Number: ${settings.bkashNumber.ifBlank { "Configured by Owner" }}", style = MaterialTheme.typography.bodySmall)
                            "nagad" -> Text("Nagad Number: ${settings.nagadNumber.ifBlank { "Configured by Owner" }}", style = MaterialTheme.typography.bodySmall)
                            "rocket" -> Text("Rocket Number: ${settings.rocketNumber.ifBlank { "Configured by Owner" }}", style = MaterialTheme.typography.bodySmall)
                            "bank_transfer" -> Text(settings.bankTransferInstructions.ifBlank { "Transfer to Owner Bank Account & enter reference below." }, style = MaterialTheme.typography.bodySmall)
                            else -> Text("SSLCOMMERZ ${if (settings.sslCommerzSandboxMode) "Sandbox" else "Live"} Gateway Initiation", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(strings.paymentAmount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("tenant_pay_amount_input")
                )
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text(strings.transactionRef) },
                    modifier = Modifier.fillMaxWidth().testTag("tenant_pay_ref_input")
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
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onSubmit(amt, method, ref, notes)
                    }
                },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                modifier = Modifier.testTag("submit_tenant_payment_btn")
            ) {
                Text(strings.submitPayment)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
