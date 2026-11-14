package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Receipt
import com.example.i18n.AppLanguage
import com.example.i18n.LocalLanguage
import com.example.i18n.LocalStrings
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidText
import com.example.ui.theme.StatusPartialBg
import com.example.ui.theme.StatusPartialText
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidText
import com.example.ui.theme.StatusVacantBg
import com.example.ui.theme.StatusVacantText
import com.example.util.FormatUtils

@Composable
fun StatSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    containerBg: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(containerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val (bg, fg, label) = when (status.lowercase()) {
        "paid", "verified", "active" -> Triple(
            StatusPaidBg, StatusPaidText,
            when (status.lowercase()) {
                "paid" -> strings.paid
                "verified" -> strings.verified
                else -> strings.active
            }
        )
        "partial", "pending" -> Triple(
            StatusPartialBg, StatusPartialText,
            if (status.lowercase() == "partial") strings.partial else strings.pending
        )
        "unpaid", "rejected", "failed", "inactive" -> Triple(
            StatusUnpaidBg, StatusUnpaidText,
            when (status.lowercase()) {
                "unpaid" -> strings.unpaid
                "rejected" -> strings.rejected
                "failed" -> strings.failed
                else -> strings.inactive
            }
        )
        else -> Triple(
            StatusVacantBg, StatusVacantText,
            when (status.lowercase()) {
                "vacant" -> strings.vacant
                "vacated" -> strings.vacated
                "cancelled" -> strings.cancelled
                else -> status
            }
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LanguageToggleChip(
    currentLanguage: AppLanguage,
    onToggle: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val nextLang = if (currentLanguage == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN
    FilterChip(
        selected = true,
        onClick = { onToggle(nextLang) },
        label = {
            Text(
                text = if (currentLanguage == AppLanguage.BN) "বাংলা / EN" else "EN / বাংলা",
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = "Language Switch",
                modifier = Modifier.size(16.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier.testTag("language_toggle_chip")
    )
}

data class ChartBarItem(
    val label: String,
    val primaryAmount: Double,
    val secondaryAmount: Double
)

@Composable
fun FinancialComparisonChart(
    title: String,
    items: List<ChartBarItem>,
    primaryLegend: String,
    secondaryLegend: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = StatusPartialText
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val language = LocalLanguage.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(primaryColor))
                    Text(primaryLegend, style = MaterialTheme.typography.labelMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(secondaryColor))
                    Text(secondaryLegend, style = MaterialTheme.typography.labelMedium)
                }
            }

            if (items.isEmpty()) {
                Text(
                    text = FormatUtils.formatBdt(0.0, language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxVal = items.maxOf { maxOf(it.primaryAmount, it.secondaryAmount, 1.0) }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val count = items.size.coerceAtLeast(1)
                    val groupWidth = size.width / count
                    val barWidth = (groupWidth * 0.28f).coerceAtMost(32.dp.toPx())
                    val gap = 6.dp.toPx()

                    items.forEachIndexed { idx, item ->
                        val centerX = groupWidth * idx + groupWidth / 2f
                        val h1 = ((item.primaryAmount / maxVal).toFloat() * (size.height - 12.dp.toPx())).coerceAtLeast(4f)
                        val h2 = ((item.secondaryAmount / maxVal).toFloat() * (size.height - 12.dp.toPx())).coerceAtLeast(4f)

                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(centerX - barWidth - gap / 2f, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(centerX - barWidth - gap / 2f, size.height - h1),
                            size = Size(barWidth, h1),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(centerX + gap / 2f, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = secondaryColor,
                            topLeft = Offset(centerX + gap / 2f, size.height - h2),
                            size = Size(barWidth, h2),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    items.forEach { item ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = FormatUtils.formatBdt(item.primaryAmount, language),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = primaryColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptDetailDialog(
    receipt: Receipt,
    onDismiss: () -> Unit,
    onDownloadPdf: (Receipt) -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = "Verified Receipt",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "SMART RENT MANAGER\n${receipt.receiptNumber}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReceiptRow(strings.buildingName, receipt.buildingName)
                ReceiptRow(strings.flatNumber, receipt.flatNumber)
                ReceiptRow(strings.tenantName, receipt.tenantName)
                ReceiptRow(strings.dateLabel, FormatUtils.toLocalizedDigits(receipt.paymentDate, language))
                ReceiptRow(strings.selectPaymentMethod, receipt.paymentMethod.uppercase())
                ReceiptRow(strings.transactionRef, receipt.transactionId)
                HorizontalDivider()
                ReceiptRow("Previous Due", FormatUtils.formatBdt(receipt.previousDue, language))
                ReceiptRow(strings.amountPaid, FormatUtils.formatBdt(receipt.amountPaid, language))
                ReceiptRow(strings.remainingDue, FormatUtils.formatBdt(receipt.remainingDue, language))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusBadge(receipt.status)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDownloadPdf(receipt) },
                modifier = Modifier.testTag("download_receipt_pdf_button")
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.downloadReceipt)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
    }
}
