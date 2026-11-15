package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BankAccount
import com.example.data.model.BankTransaction
import com.example.i18n.AppLanguage
import com.example.i18n.LocalLanguage
import com.example.i18n.LocalStrings
import com.example.ui.theme.StatusPaidText
import com.example.ui.theme.StatusUnpaidText
import com.example.ui.viewmodel.AdminDataSnapshot
import com.example.util.FormatUtils

data class BankTxFlatDetails(
    val buildingName: String,
    val floorNumber: Int?,
    val flatNumber: String,
    val tenantName: String
)

fun resolveBankTxFlatDetails(
    tx: BankTransaction,
    state: AdminDataSnapshot
): BankTxFlatDetails? {
    val matchedPayment = if (tx.referenceNumber.isNotBlank()) {
        state.payments.firstOrNull {
            it.transactionId == tx.referenceNumber || it.referenceNumber == tx.referenceNumber
        }
    } else null

    val resolvedFlatId = tx.flatId.ifBlank { matchedPayment?.flatId.orEmpty() }
    val flat = if (resolvedFlatId.isNotBlank()) {
        state.flats.firstOrNull { it.id == resolvedFlatId }
    } else null

    val resolvedBuildingId = tx.buildingId.ifBlank { flat?.buildingId ?: matchedPayment?.buildingId.orEmpty() }
    val building = if (resolvedBuildingId.isNotBlank()) {
        state.buildings.firstOrNull { it.id == resolvedBuildingId }
    } else null

    val buildingName = building?.name?.ifBlank { null }
        ?: flat?.buildingName?.ifBlank { null }
        ?: matchedPayment?.buildingName?.ifBlank { null }
        ?: ""
    val flatNumber = flat?.flatNumber?.ifBlank { null }
        ?: matchedPayment?.flatNumber?.ifBlank { null }
        ?: ""
    val floorNumber = flat?.floorNumber
    val tenantName = matchedPayment?.tenantName?.ifBlank { null }
        ?: flat?.tenantName?.ifBlank { null }
        ?: ""

    if (buildingName.isBlank() && flatNumber.isBlank() && floorNumber == null) return null
    return BankTxFlatDetails(
        buildingName = buildingName,
        floorNumber = floorNumber,
        flatNumber = flatNumber,
        tenantName = tenantName
    )
}

@Composable
fun BankAccountDetailDialog(
    account: BankAccount,
    state: AdminDataSnapshot,
    onDismiss: () -> Unit,
    onAddDeposit: (BankAccount) -> Unit,
    onRequestDeleteTx: (BankTransaction) -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    var showOnlyDeposits by remember { mutableStateOf(true) }

    val accountTxs = remember(state.bankTransactions, account.id, showOnlyDeposits) {
        state.bankTransactions.filter { tx ->
            tx.bankAccountId == account.id &&
                (!showOnlyDeposits || tx.transactionType == "received")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = account.accountName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${account.bankName} • ${account.accountNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (language == AppLanguage.BN) "বর্তমান ব্যালেন্স" else "Current Balance",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = FormatUtils.formatBdt(account.currentBalance, language),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = if (language == AppLanguage.BN) {
                                "শুরু: ${FormatUtils.formatBdt(account.openingBalance, language)} • মোট জমা: ${FormatUtils.formatBdt(account.moneyReceived, language)}"
                            } else {
                                "Opening: ${FormatUtils.formatBdt(account.openingBalance, language)} • Deposits: ${FormatUtils.formatBdt(account.moneyReceived, language)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = showOnlyDeposits,
                        onClick = { showOnlyDeposits = true },
                        label = {
                            Text(if (language == AppLanguage.BN) "শুধু জমা" else "Deposits")
                        }
                    )
                    FilterChip(
                        selected = !showOnlyDeposits,
                        onClick = { showOnlyDeposits = false },
                        label = {
                            Text(if (language == AppLanguage.BN) "সব লেনদেন" else "All")
                        }
                    )
                }

                Button(
                    onClick = { onAddDeposit(account) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_add_bank_deposit_btn")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (language == AppLanguage.BN) "ব্যাংকে জমা যোগ করুন" else "Add Bank Deposit")
                }

                HorizontalDivider()

                Text(
                    text = if (language == AppLanguage.BN) {
                        "কোন তলা ও ফ্ল্যাট থেকে জমা হয়েছে (${FormatUtils.formatInt(accountTxs.size, language)})"
                    } else {
                        "Deposits by Floor & Flat (${accountTxs.size})"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                if (accountTxs.isEmpty()) {
                    Text(
                        text = if (language == AppLanguage.BN) {
                            "এই ব্যাংক অ্যাকাউন্টে এখনো কোনো জমা রেকর্ড নেই। কোনো ভাড়াটিয়া ব্যাংকে/বিকাশে পেমেন্ট করলে বা আপনি ফ্ল্যাট নির্বাচন করে ব্যাংকে জমা দিলে এখানে ভবন, তলা ও ফ্ল্যাট নম্বরসহ দেখা যাবে।"
                        } else {
                            "No deposits recorded for this bank account yet. When tenants pay via Bank/bKash/Nagad or you add a deposit linked to a flat, the building, floor, and flat number will appear here."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(accountTxs, key = { it.id }) { tx ->
                            val details = remember(tx, state.flats, state.buildings, state.payments) {
                                resolveBankTxFlatDetails(tx, state)
                            }
                            val isDeposit = tx.transactionType == "received"
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (details != null) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Layers,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                val floorText = details.floorNumber?.let { fl ->
                                                    if (language == AppLanguage.BN) "তলা ${FormatUtils.formatInt(fl, language)}"
                                                    else "Floor $fl"
                                                }
                                                val flatText = details.flatNumber.takeIf { it.isNotBlank() }?.let { fn ->
                                                    if (language == AppLanguage.BN) "ফ্ল্যাট $fn" else "Flat $fn"
                                                }
                                                val badgeLabel = listOfNotNull(
                                                    details.buildingName.takeIf { it.isNotBlank() },
                                                    floorText,
                                                    flatText
                                                ).joinToString(" • ")
                                                Text(
                                                    text = badgeLabel,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Apartment,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (language == AppLanguage.BN) "সাধারণ ব্যাংক লেনদেন" else "General Bank Entry",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            if (details?.tenantName?.isNotBlank() == true) {
                                                Text(
                                                    text = if (language == AppLanguage.BN) "ভাড়াটিয়া: ${details.tenantName}" else "Tenant: ${details.tenantName}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                            }
                                            Text(
                                                text = tx.description,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = "${tx.date} • ${tx.paymentMethod.uppercase()}${if (tx.referenceNumber.isNotBlank()) " • Ref: ${tx.referenceNumber}" else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${if (isDeposit) "+" else "-"}${FormatUtils.formatBdt(tx.amount, language)}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isDeposit) StatusPaidText else StatusUnpaidText
                                            )
                                            IconButton(
                                                onClick = { onRequestDeleteTx(tx) },
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("detail_delete_bank_tx_${tx.id}")
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}
