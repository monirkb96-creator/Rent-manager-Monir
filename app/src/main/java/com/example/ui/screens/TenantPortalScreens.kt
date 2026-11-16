package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.MonthlyBill
import com.example.data.model.PaymentRecord
import com.example.data.model.PublicBankInfo
import com.example.data.model.Receipt
import com.example.data.model.UserProfile
import com.example.data.remote.GatewayInitiationResult
import com.example.i18n.AppLanguage
import com.example.i18n.LocalLanguage
import com.example.i18n.LocalStrings
import com.example.ui.components.LanguageToggleChip
import com.example.ui.components.ReceiptDetailDialog
import com.example.ui.components.ReceiptSlipPreviewDialog
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.TenantDataSnapshot
import com.example.util.FormatUtils
import com.example.util.PdfAndExportManager
import com.example.util.ReceiptImageUtils
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun TenantHomeScreen(
    profile: UserProfile,
    state: TenantDataSnapshot,
    gatewayResult: GatewayInitiationResult?,
    onDismissGatewayResult: () -> Unit,
    onSubmitPayment: (MonthlyBill, Double, String, String, String, String, String, String) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    val context = LocalContext.current
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimary,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp).testTag("tenant_pay_now_button")
                            ) {
                                Icon(Icons.Filled.Payments, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "পেমেন্ট করুন" else strings.payNow,
                                    fontWeight = FontWeight.Bold
                                )
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
                        BillBreakdownRow(
                            if (language == AppLanguage.BN) "মোট প্রদেয়" else "Total Payable",
                            FormatUtils.formatBdt(state.totalPayable, language),
                            bold = true
                        )
                        BillBreakdownRow(strings.amountPaid, FormatUtils.formatBdt(state.totalPaid, language), bold = true)
                        BillBreakdownRow(strings.remainingDue, FormatUtils.formatBdt(state.remainingDue, language), bold = true)
                    }
                }
            }
        }

        // Owner Payment / Bank Account Summary on Home if configured
        if (state.ownerBankAccounts.isNotEmpty() ||
            state.settings.bkashNumber.isNotBlank() ||
            state.settings.nagadNumber.isNotBlank() ||
            state.settings.rocketNumber.isNotBlank()
        ) {
            item {
                OwnerPaymentAccountsCard(
                    bankAccounts = state.ownerBankAccounts,
                    settings = state.settings,
                    language = language,
                    context = context,
                    onPayWithReceipt = state.currentBill?.takeIf { it.remainingDue > 0.0 }?.let { b ->
                        { payTargetBill = b }
                    }
                )
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
            bankAccounts = state.ownerBankAccounts,
            onDismiss = { payTargetBill = null },
            onSubmit = { amt, method, ref, notes, bankId, bankName, receiptB64 ->
                payTargetBill = null
                onSubmitPayment(bill, amt, method, ref, notes, bankId, bankName, receiptB64)
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
    onSubmitPayment: (MonthlyBill, Double, String, String, String, String, String, String) -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    var payTarget by remember { mutableStateOf<MonthlyBill?>(null) }
    val billsToShow = state.displayBills

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

        if (billsToShow.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = if (state.flat == null) {
                            if (language == AppLanguage.BN) {
                                "আপনার অ্যাকাউন্ট এখনও কোনো ফ্ল্যাটে যুক্ত করা হয়নি। মালিক আপনাকে ফ্ল্যাটে যুক্ত করলে মাসিক বিল এখানে দেখাবে।"
                            } else {
                                "Your account is not assigned to a flat yet. Once assigned, your bills will appear here."
                            }
                        } else {
                            if (language == AppLanguage.BN) {
                                "এই মাসে এখনও কোনো বকেয়া বিল নেই।"
                            } else {
                                "No pending monthly bills right now."
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(billsToShow, key = { it.id }) { bill ->
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
                            Text(
                                text = "${FormatUtils.toLocalizedDigits(bill.billingMonth, language)} • ${bill.flatNumber}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            StatusBadge(bill.status)
                        }
                        BillBreakdownRow(strings.monthlyRent, FormatUtils.formatBdt(bill.monthlyRent, language))
                        BillBreakdownRow(strings.serviceCharge, FormatUtils.formatBdt(bill.serviceCharge, language))
                        if (bill.electricityBill > 0.0) {
                            BillBreakdownRow(strings.electricityBill, FormatUtils.formatBdt(bill.electricityBill, language))
                        }
                        if (bill.waterBill > 0.0) {
                            BillBreakdownRow(strings.waterBill, FormatUtils.formatBdt(bill.waterBill, language))
                        }
                        if (bill.gasBill > 0.0) {
                            BillBreakdownRow(strings.gasBill, FormatUtils.formatBdt(bill.gasBill, language))
                        }
                        if (bill.otherCharges > 0.0) {
                            BillBreakdownRow(strings.otherCharges, FormatUtils.formatBdt(bill.otherCharges, language))
                        }
                        HorizontalDivider()
                        BillBreakdownRow(strings.totalMonthlyBill, FormatUtils.formatBdt(bill.totalBill, language), bold = true)
                        BillBreakdownRow(strings.amountPaid, FormatUtils.formatBdt(bill.amountPaid, language))
                        BillBreakdownRow(strings.remainingDue, FormatUtils.formatBdt(bill.remainingDue, language), bold = true)

                        if (bill.remainingDue > 0.0) {
                            Button(
                                onClick = { payTarget = bill },
                                modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 4.dp)
                            ) {
                                Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "পেমেন্ট করুন / রিসিট আপলোড" else strings.payNow
                                )
                            }
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
            bankAccounts = state.ownerBankAccounts,
            onDismiss = { payTarget = null },
            onSubmit = { amt, method, ref, notes, bankId, bankName, receiptB64 ->
                payTarget = null
                onSubmitPayment(bill, amt, method, ref, notes, bankId, bankName, receiptB64)
            }
        )
    }
}

@Composable
fun TenantPaymentsScreen(
    state: TenantDataSnapshot,
    onSubmitPayment: (MonthlyBill, Double, String, String, String, String, String, String) -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    val context = LocalContext.current

    var selectedReceipt by remember { mutableStateOf<Receipt?>(null) }
    var slipPreviewPayment by remember { mutableStateOf<PaymentRecord?>(null) }
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
                Text(strings.navPayments, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                state.currentBill?.let { b ->
                    if (b.remainingDue > 0.0) {
                        Button(
                            onClick = { payTarget = b },
                            modifier = Modifier.testTag("tenant_payments_pay_btn")
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (language == AppLanguage.BN) "পেমেন্ট ও রিসিট" else strings.payNow)
                        }
                    }
                }
            }
        }

        // Always visible Owner Bank Accounts & Mobile Banking Card
        item {
            OwnerPaymentAccountsCard(
                bankAccounts = state.ownerBankAccounts,
                settings = state.settings,
                language = language,
                context = context,
                onPayWithReceipt = state.currentBill?.takeIf { it.remainingDue > 0.0 }?.let { b ->
                    { payTarget = b }
                }
            )
        }

        item {
            Text(
                text = strings.paymentHistory,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (state.payments.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) {
                            "এখনও কোনো পেমেন্ট জমা দেওয়া হয়নি। উপরে দেওয়া মালিকের ব্যাংক অ্যাকাউন্ট বা মোবাইল ব্যাংকিং নম্বরে টাকা পাঠিয়ে \"ব্যাংক রিসিট আপলোড ও পেমেন্ট করুন\" বাটনে চাপ দিন।"
                        } else {
                            "No payments submitted yet. Transfer to the owner's bank or MFS account above and tap \"Pay & Upload Receipt\"."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.payments, key = { it.id }) { p ->
                val linkedReceipt = state.receipts.firstOrNull { it.paymentId == p.id || it.id == p.receiptId }
                val monthLabel = FormatUtils.formatBillingMonthLabel(
                    FormatUtils.resolvePaymentBillingMonth(p, state.bills),
                    language
                )
                val methodLabel = FormatUtils.formatPaymentMethodLabel(p.method, language)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.BN) "ভাড়ার মাস: $monthLabel" else "Rent Month: $monthLabel",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    FormatUtils.formatBdt(p.amount, language),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("$methodLabel • ${FormatUtils.toLocalizedDigits(p.paymentDate, language)}", style = MaterialTheme.typography.bodySmall)
                                if (p.targetBankAccountName.isNotBlank()) {
                                    Text(
                                        text = if (language == AppLanguage.BN) "ব্যাংক: ${p.targetBankAccountName}" else "Bank: ${p.targetBankAccountName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text("Tx: ${p.transactionId} | Ref: ${p.referenceNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusBadge(p.verificationStatus)
                        }

                        if (p.receiptImageBase64.isNotBlank()) {
                            OutlinedButton(
                                onClick = { slipPreviewPayment = p },
                                modifier = Modifier.fillMaxWidth().testTag("tenant_view_slip_${p.id}")
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (language == AppLanguage.BN) "আপলোডকৃত রিসিট ছবি দেখুন" else "View Uploaded Receipt Slip")
                            }
                        }

                        if (linkedReceipt != null) {
                            Button(
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
    }

    payTarget?.let { bill ->
        PayBillDialog(
            bill = bill,
            settings = state.settings,
            bankAccounts = state.ownerBankAccounts,
            onDismiss = { payTarget = null },
            onSubmit = { amt, method, ref, notes, bankId, bankName, receiptB64 ->
                payTarget = null
                onSubmitPayment(bill, amt, method, ref, notes, bankId, bankName, receiptB64)
            }
        )
    }

    slipPreviewPayment?.let { p ->
        ReceiptSlipPreviewDialog(
            base64Image = p.receiptImageBase64,
            title = if (language == AppLanguage.BN) "আপলোডকৃত পেমেন্ট রিসিট" else "Uploaded Payment Slip",
            subtitle = "${FormatUtils.formatBdt(p.amount, language)} • ${p.method.uppercase()} • ${p.paymentDate}",
            onDismiss = { slipPreviewPayment = null }
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
private fun OwnerPaymentAccountsCard(
    bankAccounts: List<PublicBankInfo>,
    settings: AppSettings,
    language: AppLanguage,
    context: Context,
    onPayWithReceipt: (() -> Unit)?
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (language == AppLanguage.BN) "মালিকের ব্যাংক ও পেমেন্ট অ্যাকাউন্ট" else "Owner Bank & Payment Accounts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (bankAccounts.isEmpty() &&
                settings.bkashNumber.isBlank() &&
                settings.nagadNumber.isBlank() &&
                settings.rocketNumber.isBlank() &&
                settings.bankTransferInstructions.isBlank()
            ) {
                Text(
                    text = if (language == AppLanguage.BN) {
                        "মালিক এখনও ব্যাংক বা বিকাশ/নগদ নম্বর সেটআপ সিঙ্ক করেননি। মালিককে তাঁর অ্যাপ একবার খুলতে অথবা সেটিংসে নম্বর যোগ করতে বলুন।"
                    } else {
                        "Owner bank or MFS accounts are not synced yet. Ask the owner to open the Admin app once or add numbers in Settings."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                bankAccounts.forEach { acc ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(acc.accountName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("${acc.bankName} • ${acc.accountType}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = if (language == AppLanguage.BN) "অ্যাকাউন্ট নম্বর: ${acc.accountNumber}" else "Account No: ${acc.accountNumber}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    copyToClipboard(
                                        context = context,
                                        label = acc.bankName,
                                        value = acc.accountNumber,
                                        toastText = if (language == AppLanguage.BN) "অ্যাকাউন্ট নম্বর কপি হয়েছে" else "Account number copied"
                                    )
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = if (language == AppLanguage.BN) "নম্বর কপি করুন" else "Copy account number",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (settings.bkashNumber.isNotBlank() || settings.nagadNumber.isNotBlank() || settings.rocketNumber.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (settings.bkashNumber.isNotBlank()) {
                            MfsNumberRow("bKash", settings.bkashNumber, language, context)
                        }
                        if (settings.nagadNumber.isNotBlank()) {
                            MfsNumberRow("Nagad", settings.nagadNumber, language, context)
                        }
                        if (settings.rocketNumber.isNotBlank()) {
                            MfsNumberRow("Rocket", settings.rocketNumber, language, context)
                        }
                    }
                }

                if (settings.bankTransferInstructions.isNotBlank()) {
                    Text(
                        text = settings.bankTransferInstructions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onPayWithReceipt != null) {
                Button(
                    onClick = onPayWithReceipt,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("upload_bank_receipt_cta")
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BN) "ব্যাংক রিসিট আপলোড ও পেমেন্ট করুন" else "Pay & Upload Bank Receipt",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MfsNumberRow(
    provider: String,
    number: String,
    language: AppLanguage,
    context: Context
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text("$provider: $number", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            }
            IconButton(
                onClick = {
                    copyToClipboard(
                        context = context,
                        label = provider,
                        value = number,
                        toastText = if (language == AppLanguage.BN) "$provider নম্বর কপি হয়েছে" else "$provider number copied"
                    )
                },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy $provider number",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, value: String, toastText: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
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
    onSwitchToOwner: (() -> Unit)? = null,
    onSignOut: () -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    var showUpgradeConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
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

        if (onSwitchToOwner != null && state.flat == null && state.bills.isEmpty() && state.payments.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (language == AppLanguage.BN) "আপনি কি বাড়ির মালিক?" else "Are you a Property Owner?",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (language == AppLanguage.BN) {
                            "এই অ্যাকাউন্টটি এখনও কোনো ফ্ল্যাটে যুক্ত নেই। আপনি চাইলে এখনই নিজের ভবন ও ভাড়াটিয়া পরিচালনার জন্য আলাদা মালিকানা (Owner) অ্যাকাউন্ট তৈরি করতে পারেন।"
                        } else {
                            "This account is not assigned to any flat yet. Create your own isolated Property Owner workspace to manage your buildings and tenants."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { showUpgradeConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("switch_to_owner_account_btn")
                    ) {
                        Icon(Icons.Filled.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (language == AppLanguage.BN) "মালিকানা (Owner) অ্যাকাউন্ট হিসেবে শুরু করুন"
                            else "Switch to Owner Account"
                        )
                    }
                }
            }
        }

        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("tenant_sign_out_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.signOut)
        }
    }

    if (showUpgradeConfirm && onSwitchToOwner != null) {
        AlertDialog(
            onDismissRequest = { showUpgradeConfirm = false },
            title = {
                Text(
                    if (language == AppLanguage.BN) "মালিকানা অ্যাকাউন্ট চালু করবেন?"
                    else "Create Owner Workspace?"
                )
            },
            text = {
                Text(
                    if (language == AppLanguage.BN) {
                        "আপনার নিজের জন্য সম্পূর্ণ আলাদা বাড়ি/ফ্ল্যাট ম্যানেজমেন্ট ওয়ার্কস্পেস চালু হবে।"
                    } else {
                        "This will initialize your own isolated Owner dashboard for managing your buildings and tenants."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpgradeConfirm = false
                        onSwitchToOwner()
                    },
                    modifier = Modifier.testTag("confirm_switch_to_owner_btn")
                ) {
                    Text(if (language == AppLanguage.BN) "ওকে, চালু করুন" else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeConfirm = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun BillBreakdownRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (bold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall)
        Text(value, style = if (bold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PayBillDialog(
    bill: MonthlyBill,
    settings: AppSettings,
    bankAccounts: List<PublicBankInfo>,
    onDismiss: () -> Unit,
    onSubmit: (Double, String, String, String, String, String, String) -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var method by remember(bankAccounts) {
        mutableStateOf(if (bankAccounts.isNotEmpty()) "bank_transfer" else "bkash")
    }
    var selectedBank by remember(bankAccounts) { mutableStateOf(bankAccounts.firstOrNull()) }
    var amount by remember(bill.id) { mutableStateOf(bill.remainingDue.toInt().toString()) }
    var ref by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var receiptImageBase64 by remember { mutableStateOf("") }
    var isEncodingSlip by remember { mutableStateOf(false) }
    var slipError by remember { mutableStateOf<String?>(null) }

    val slipPreviewBitmap = remember(receiptImageBase64) {
        ReceiptImageUtils.decodeBase64ToImageBitmap(receiptImageBase64)
    }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val handlePickedUri: (Uri?) -> Unit = { uri ->
        if (uri != null) {
            isEncodingSlip = true
            slipError = null
            scope.launch {
                val res = ReceiptImageUtils.encodeReceiptUriToBase64(context, uri)
                isEncodingSlip = false
                res.onSuccess { b64 -> receiptImageBase64 = b64 }
                    .onFailure { err ->
                        slipError = if (language == AppLanguage.BN) {
                            "ছবি পড়া যায়নি, অনুগ্রহ করে গ্যালারি থেকে অন্য ছবি বা ছোট স্ক্রিনশট দিন (${err.localizedMessage ?: "error"})"
                        } else {
                            err.localizedMessage ?: "Failed to load image"
                        }
                    }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = handlePickedUri
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = handlePickedUri
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val capturedUri = pendingCameraUri
        pendingCameraUri = null
        if (success && capturedUri != null) {
            handlePickedUri(capturedUri)
        }
    }

    val billMonthLabel = FormatUtils.formatBillingMonthLabel(
        bill.billingMonth.ifBlank { FormatUtils.currentBillingMonth() },
        language
    )

    val methods = listOf(
        "cash" to strings.cash,
        "bank_transfer" to strings.bankTransfer,
        "bkash" to strings.bkash,
        "nagad" to strings.nagad,
        "rocket" to strings.rocket,
        "card" to strings.debitCreditCard,
        "sslcommerz" to strings.sslCommerzGateway
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (language == AppLanguage.BN) "ভাড়া পেমেন্ট (${bill.flatNumber})"
                else "${strings.payNow} (${bill.flatNumber})"
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) "ভাড়ার মাস: $billMonthLabel" else "Billing Month: $billMonthLabel",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "${strings.remainingDue}: ${FormatUtils.formatBdt(bill.remainingDue, language)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    methods.forEach { (key, label) ->
                        FilterChip(
                            selected = method == key,
                            onClick = { method = key },
                            label = { Text(label) }
                        )
                    }
                }

                // Owner bank accounts or MFS numbers for the selected method
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        when (method) {
                            "bank_transfer" -> {
                                if (bankAccounts.isNotEmpty()) {
                                    Text(
                                        text = if (language == AppLanguage.BN) "মালিকের ব্যাংক অ্যাকাউন্ট (টাকা পাঠাতে চাপ দিয়ে কপি করুন):" else "Owner Bank Account (select & copy):",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    bankAccounts.forEach { acc ->
                                        val isPicked = selectedBank?.id == acc.id
                                        Surface(
                                            color = if (isPicked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isPicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedBank = acc }
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(acc.accountName, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                                    Text("${acc.bankName} • ${acc.accountType}", style = MaterialTheme.typography.bodySmall)
                                                    Text("A/C: ${acc.accountNumber}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        copyToClipboard(
                                                            context = context,
                                                            label = acc.bankName,
                                                            value = acc.accountNumber,
                                                            toastText = if (language == AppLanguage.BN) "অ্যাকাউন্ট নম্বর কপি হয়েছে" else "Account number copied"
                                                        )
                                                    },
                                                    modifier = Modifier.minimumInteractiveComponentSize()
                                                ) {
                                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                    }
                                }
                                if (settings.bankTransferInstructions.isNotBlank()) {
                                    Text(settings.bankTransferInstructions, style = MaterialTheme.typography.bodySmall)
                                } else if (bankAccounts.isEmpty()) {
                                    Text(
                                        text = if (language == AppLanguage.BN) "ব্যাংক অ্যাকাউন্টে টাকা পাঠিয়ে নিচে রিসিট স্লিপ আপলোড করুন।" else "Transfer to Owner Bank Account & upload the receipt slip below.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            "cash" -> Text(
                                text = if (language == AppLanguage.BN) {
                                    "মালিককে হাতে/নগদে ভাড়া পরিশোধ করে নিচে নগদ রসিদ বা প্রমাণের ছবি আপলোড করুন। জমা দিলে মালিকের কাছে নোটিফিকেশন যাবে এবং মালিক ওকে প্রেস করলে বিল গ্রহণ হবে।"
                                } else {
                                    "Paid cash hand-to-hand? Upload a photo of your cash receipt or proof below. Owner will be notified to accept the bill."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                            "bkash" -> Text("bKash: ${settings.bkashNumber.ifBlank { "মালিকের সাথে যোগাযোগ করুন" }}", style = MaterialTheme.typography.bodySmall)
                            "nagad" -> Text("Nagad: ${settings.nagadNumber.ifBlank { "মালিকের সাথে যোগাযোগ করুন" }}", style = MaterialTheme.typography.bodySmall)
                            "rocket" -> Text("Rocket: ${settings.rocketNumber.ifBlank { "মালিকের সাথে যোগাযোগ করুন" }}", style = MaterialTheme.typography.bodySmall)
                            else -> Text("SSLCOMMERZ ${if (settings.sslCommerzSandboxMode) "Sandbox" else "Live"} Gateway Initiation", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Receipt / Slip Image Upload Section
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (language == AppLanguage.BN) "পেমেন্টের প্রমাণ / রিসিট ছবি আপলোড (নগদ রসিদ / ব্যাংক স্লিপ)" else "Upload Payment Proof / Receipt Photo",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (isEncodingSlip) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(if (language == AppLanguage.BN) "ছবি প্রস্তুত হচ্ছে..." else "Processing image...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        slipPreviewBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp,
                                contentDescription = "Selected Receipt Slip",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                        slipError?.let { err ->
                            Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("pick_receipt_slip_btn")
                            ) {
                                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "গ্যালারি" else "Gallery",
                                    maxLines = 1
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    try {
                                        filePickerLauncher.launch("image/*")
                                    } catch (e: Exception) {
                                        slipError = e.localizedMessage ?: "File picker unavailable"
                                    }
                                },
                                modifier = Modifier.testTag("pick_receipt_file_btn")
                            ) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "ফাইল" else "Files",
                                    maxLines = 1
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val dir = File(context.cacheDir, "camera_proofs").apply { mkdirs() }
                                        val file = File(dir, "proof_${System.currentTimeMillis()}.jpg")
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        pendingCameraUri = uri
                                        cameraLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        slipError = e.localizedMessage ?: "Camera unavailable"
                                    }
                                },
                                modifier = Modifier.testTag("camera_receipt_slip_btn")
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "ছবি তুলুন" else "Camera",
                                    maxLines = 1
                                )
                            }
                        }
                        if (receiptImageBase64.isNotBlank()) {
                            TextButton(
                                onClick = { receiptImageBase64 = "" },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(strings.delete, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = FormatUtils.toAsciiDigits(it).filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(strings.paymentAmount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("tenant_pay_amount_input")
                )
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = {
                        Text(
                            if (receiptImageBase64.isNotBlank()) {
                                "${strings.transactionRef} (${if (language == AppLanguage.BN) "ঐচ্ছিক" else "Optional"})"
                            } else {
                                strings.transactionRef
                            }
                        )
                    },
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
                    val amt = FormatUtils.parseLocalizedDouble(amount) ?: 0.0
                    if (amt > 0.0) {
                        val bankId = if (method == "bank_transfer") selectedBank?.id.orEmpty() else ""
                        val bankName = if (method == "bank_transfer" && selectedBank != null) {
                            "${selectedBank!!.accountName} (${selectedBank!!.bankName})"
                        } else ""
                        onSubmit(amt, method, ref, notes, bankId, bankName, receiptImageBase64)
                    }
                },
                enabled = !isEncodingSlip && (FormatUtils.parseLocalizedDouble(amount) ?: 0.0) > 0.0,
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
