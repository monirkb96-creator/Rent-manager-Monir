package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.UserProfile
import com.example.i18n.AppLanguage
import com.example.i18n.LocalLanguage
import com.example.i18n.LocalStrings
import com.example.ui.components.LanguageToggleChip
import com.example.ui.viewmodel.AdminDataSnapshot
import com.example.util.FormatUtils
import com.example.util.PdfAndExportManager

@Composable
fun AdminSettingsScreen(
    profile: UserProfile,
    state: AdminDataSnapshot,
    onLanguageChange: (AppLanguage) -> Unit,
    onSaveSettings: (AppSettings) -> Unit,
    onBuildBackupJson: () -> String,
    onResetAllTransactionsAndBills: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    val context = LocalContext.current

    var appName by remember(state.settings) { mutableStateOf(state.settings.appName) }
    var storeId by remember(state.settings) { mutableStateOf(state.settings.sslCommerzStoreId) }
    var sandboxMode by remember(state.settings) { mutableStateOf(state.settings.sslCommerzSandboxMode) }
    var bkashNo by remember(state.settings) { mutableStateOf(state.settings.bkashNumber) }
    var nagadNo by remember(state.settings) { mutableStateOf(state.settings.nagadNumber) }
    var rocketNo by remember(state.settings) { mutableStateOf(state.settings.rocketNumber) }
    var bankInstr by remember(state.settings) { mutableStateOf(state.settings.bankTransferInstructions) }
    var carryForward by remember(state.settings) { mutableStateOf(state.settings.carryForwardEnabled) }

    var showAuditLogsDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = strings.navSettings,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Admin Profile & Database Status
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(profile.displayName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(profile.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Cloud Firestore: Connected & Synced (${state.buildings.size} Buildings, ${state.flats.size} Flats)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Language Settings
        item {
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.switchLanguage, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                    LanguageToggleChip(currentLanguage = language, onToggle = onLanguageChange)
                }
            }
        }

        // Payment Gateway & Billing Settings
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (language == AppLanguage.BN) "পেমেন্ট গেটওয়ে ও ব্যাংক নির্দেশনা" else "Payment Gateway & Bank Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    OutlinedTextField(value = appName, onValueChange = { appName = it }, label = { Text("App Display Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = storeId, onValueChange = { storeId = it }, label = { Text("SSLCOMMERZ Store ID (Public)") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("SSLCOMMERZ Sandbox Mode", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = sandboxMode, onCheckedChange = { sandboxMode = it })
                    }
                    OutlinedTextField(value = bkashNo, onValueChange = { bkashNo = it }, label = { Text("bKash Merchant / Personal Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nagadNo, onValueChange = { nagadNo = it }, label = { Text("Nagad Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = rocketNo, onValueChange = { rocketNo = it }, label = { Text("Rocket Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bankInstr, onValueChange = { bankInstr = it }, label = { Text("Bank Transfer Instructions (Bank, A/C, Branch)") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Carry Forward Unpaid Dues", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = carryForward, onCheckedChange = { carryForward = it })
                    }

                    Button(
                        onClick = {
                            onSaveSettings(
                                state.settings.copy(
                                    appName = appName,
                                    sslCommerzStoreId = storeId,
                                    sslCommerzSandboxMode = sandboxMode,
                                    bkashNumber = bkashNo,
                                    nagadNumber = nagadNo,
                                    rocketNumber = rocketNo,
                                    bankTransferInstructions = bankInstr,
                                    carryForwardEnabled = carryForward
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.save)
                    }
                }
            }
        }

        // Data Export, Backup & Audit Logs
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.reportsCenter, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val file = PdfAndExportManager.generateFinancialReportPdf(
                                    context,
                                    "Complete Financial Report",
                                    state.buildings,
                                    state.flats,
                                    state.bills,
                                    state.payments,
                                    state.expenses,
                                    state.bankTransactions,
                                    state.cashTransactions
                                )
                                PdfAndExportManager.shareOrOpenFile(context, file, "application/pdf", strings.exportPdf)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                val file = PdfAndExportManager.generateExcelCsvReport(
                                    context,
                                    state.buildings,
                                    state.flats,
                                    state.bills,
                                    state.payments,
                                    state.expenses
                                )
                                PdfAndExportManager.shareOrOpenFile(context, file, "text/csv", strings.exportExcel)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.TableChart, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Excel")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val json = onBuildBackupJson()
                            val file = PdfAndExportManager.generateBackupJsonFile(context, json)
                            PdfAndExportManager.shareOrOpenFile(context, file, "application/json", strings.backupDatabase)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_backup_json_btn")
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.backupDatabase)
                    }
                    OutlinedButton(
                        onClick = { showAuditLogsDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("view_audit_logs_btn")
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("${strings.auditLogs} (${state.auditLogs.size})")
                    }
                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("settings_reset_all_tx_btn")
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (language == AppLanguage.BN) "সব লেনদেন ও বকেয়া রিসেট করুন (৳০)"
                            else "Reset All Transactions & Bills (Zero)"
                        )
                    }
                }
            }
        }

        // Sign Out
        item {
            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("admin_sign_out_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.signOut)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAuditLogsDialog) {
        AlertDialog(
            onDismissRequest = { showAuditLogsDialog = false },
            title = { Text(strings.auditLogs) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.auditLogs.isEmpty()) {
                        Text("No audit logs yet.")
                    } else {
                        state.auditLogs.take(25).forEach { log ->
                            Column {
                                Text("${log.action} • ${log.entityType}", style = MaterialTheme.typography.labelLarge)
                                Text(log.summary, style = MaterialTheme.typography.bodySmall)
                                Text("${log.actorEmail} • ${FormatUtils.formatTimestamp(log.createdAt, language)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HorizontalDivider(Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAuditLogsDialog = false }) { Text(strings.close) } }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(if (language == AppLanguage.BN) "সব লেনদেন ও বকেয়া রিসেট করবেন?" else "Reset All Transactions & Bills?")
            },
            text = {
                Text(
                    if (language == AppLanguage.BN) {
                        "সব পেমেন্ট, রসিদ, মাসিক বিল, ক্যাশ ও ব্যাংক লেনদেন এবং খরচ মুছে যাবে ও ড্যাশবোর্ডের হিসাব ৳০ হয়ে যাবে। আপনার ভবন ও ফ্ল্যাটগুলো ঠিক থাকবে।"
                    } else {
                        "All payments, receipts, monthly bills, cash/bank transactions, and expenses will be cleared so totals reset to zero. Your buildings and flats remain intact."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetAllTransactionsAndBills()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("settings_confirm_reset_all_btn")
                ) {
                    Text(if (language == AppLanguage.BN) "হ্যাঁ, রিসেট করুন (৳০)" else "Yes, Reset (Zero)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}
