package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val phone: String = "",
    val role: String = "tenant", // "admin" | "tenant"
    val status: String = "active", // "active" | "inactive"
    val tenantId: String = "",
    val buildingId: String = "",
    val flatId: String = "",
    val preferredLanguage: String = "bn",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Building(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val address: String = "",
    val floorsCount: Int = 1,
    val totalFlats: Int = 1,
    val contactInfo: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Flat(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val buildingId: String = "",
    val buildingName: String = "",
    val flatNumber: String = "",
    val floorNumber: Int = 1,
    val monthlyRent: Double = 0.0,
    val serviceCharge: Double = 0.0,
    val electricityBill: Double = 0.0,
    val waterBill: Double = 0.0,
    val gasBill: Double = 0.0,
    val otherCharges: Double = 0.0,
    val totalMonthlyBill: Double = 0.0,
    val amountPaid: Double = 0.0,
    val remainingDue: Double = 0.0,
    val paymentStatus: String = "vacant", // "paid" | "partial" | "unpaid" | "vacant"
    val tenantId: String = "",
    val tenantUserId: String = "",
    val tenantEmail: String = "",
    val tenantName: String = "",
    val tenantPhone: String = "",
    @get:PropertyName("isOccupied") @set:PropertyName("isOccupied")
    var isOccupied: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Tenant(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val tenantUserId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val nidNumber: String = "",
    val buildingId: String = "",
    val buildingName: String = "",
    val flatId: String = "",
    val flatNumber: String = "",
    val status: String = "active", // "active" | "inactive" | "vacated"
    val moveInDate: String = "",
    val moveOutDate: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MonthlyBill(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val buildingId: String = "",
    val buildingName: String = "",
    val flatId: String = "",
    val flatNumber: String = "",
    val tenantId: String = "",
    val tenantUserId: String = "",
    val tenantEmail: String = "",
    val tenantName: String = "",
    val billingMonth: String = "", // MM-YYYY
    val monthKey: String = "", // YYYY-MM
    val monthlyRent: Double = 0.0,
    val serviceCharge: Double = 0.0,
    val electricityBill: Double = 0.0,
    val waterBill: Double = 0.0,
    val gasBill: Double = 0.0,
    val otherCharges: Double = 0.0,
    val previousDueCarriedForward: Double = 0.0,
    val totalBill: Double = 0.0,
    val amountPaid: Double = 0.0,
    val remainingDue: Double = 0.0,
    val status: String = "unpaid", // "unpaid" | "partial" | "paid"
    val dueDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val billId: String = "",
    val buildingId: String = "",
    val buildingName: String = "",
    val flatId: String = "",
    val flatNumber: String = "",
    val tenantId: String = "",
    val tenantUserId: String = "",
    val tenantEmail: String = "",
    val tenantName: String = "",
    val amount: Double = 0.0,
    val method: String = "bkash", // "bkash" | "nagad" | "rocket" | "bank_transfer" | "card" | "sslcommerz" | "cash"
    val transactionId: String = "",
    val referenceNumber: String = "",
    val targetBankAccountId: String = "",
    val targetBankAccountName: String = "",
    val receiptImageBase64: String = "",
    val gatewayStatus: String = "pending", // "pending" | "verified" | "failed" | "cancelled"
    val verificationStatus: String = "pending", // "pending" | "verified" | "rejected"
    val paymentDate: String = "",
    val notes: String = "",
    val verifiedBy: String = "",
    val receiptId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BankAccount(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val accountName: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val accountType: String = "Current",
    val openingBalance: Double = 0.0,
    val moneyReceived: Double = 0.0,
    val moneyTransferred: Double = 0.0,
    val bankExpenses: Double = 0.0,
    val currentBalance: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BankTransaction(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val bankAccountId: String = "",
    val bankAccountName: String = "",
    val date: String = "",
    val amount: Double = 0.0,
    val transactionType: String = "received", // "received" | "transferred" | "expense"
    val description: String = "",
    val paymentMethod: String = "bank_transfer",
    val referenceNumber: String = "",
    val buildingId: String = "",
    val flatId: String = "",
    val createdBy: String = "",
    val verificationStatus: String = "verified",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CashTransaction(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val date: String = "",
    val amount: Double = 0.0,
    val transactionType: String = "cash_in", // "cash_in" | "cash_out"
    val description: String = "",
    val paymentMethod: String = "cash",
    val referenceNumber: String = "",
    val buildingId: String = "",
    val flatId: String = "",
    val createdBy: String = "",
    val verificationStatus: String = "verified",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val buildingId: String = "",
    val buildingName: String = "",
    val flatId: String = "",
    val category: String = "Building Maintenance",
    val amount: Double = 0.0,
    val date: String = "",
    val paymentSource: String = "cash", // "cash" | "bank"
    val bankAccountId: String = "",
    val description: String = "",
    val referenceNumber: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MessageItem(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderRole: String = "admin", // "admin" | "tenant"
    val recipientUid: String = "",
    val recipientEmail: String = "",
    val tenantId: String = "",
    val buildingId: String = "",
    val flatId: String = "",
    val flatNumber: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "personal", // "personal" | "reminder" | "emergency" | "maintenance"
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val targetScope: String = "all", // "all" | "building"
    val buildingId: String = "",
    val buildingName: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "notice", // "notice" | "emergency" | "maintenance"
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val recipientUid: String = "",
    val recipientEmail: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "general",
    val relatedId: String = "",
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Receipt(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val paymentId: String = "",
    val billId: String = "",
    val receiptNumber: String = "",
    val buildingId: String = "",
    val buildingName: String = "",
    val flatId: String = "",
    val flatNumber: String = "",
    val tenantId: String = "",
    val tenantUserId: String = "",
    val tenantEmail: String = "",
    val tenantName: String = "",
    val paymentDate: String = "",
    val amountPaid: Double = 0.0,
    val paymentMethod: String = "",
    val transactionId: String = "",
    val previousDue: Double = 0.0,
    val remainingDue: Double = 0.0,
    val status: String = "verified",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    val actorRole: String = "admin",
    val action: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "app_config",
    val ownerUid: String = "",
    val appName: String = "Smart Rent Manager",
    val defaultLanguage: String = "bn",
    val sslCommerzStoreId: String = "",
    val sslCommerzSandboxMode: Boolean = true,
    val bkashNumber: String = "",
    val nagadNumber: String = "",
    val rocketNumber: String = "",
    val bankTransferInstructions: String = "",
    val bankAccountsJson: String = "",
    val autoBillDay: Int = 1,
    val carryForwardEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class PublicBankInfo(
    val id: String = "",
    val accountName: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val accountType: String = "Current"
)

object BankInfoJson {
    fun encodeFromBankAccounts(accounts: List<BankAccount>): String {
        val arr = org.json.JSONArray()
        accounts.forEach { acc ->
            arr.put(
                org.json.JSONObject().apply {
                    put("id", acc.id)
                    put("accountName", acc.accountName)
                    put("bankName", acc.bankName)
                    put("accountNumber", acc.accountNumber)
                    put("accountType", acc.accountType)
                }
            )
        }
        return arr.toString()
    }

    fun decodeList(json: String): List<PublicBankInfo> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(json)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val bankName = obj.optString("bankName")
                    val accountNumber = obj.optString("accountNumber")
                    if (bankName.isNotBlank() || accountNumber.isNotBlank()) {
                        add(
                            PublicBankInfo(
                                id = obj.optString("id"),
                                accountName = obj.optString("accountName"),
                                bankName = bankName,
                                accountNumber = accountNumber,
                                accountType = obj.optString("accountType", "Current")
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
