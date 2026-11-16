package com.example.util

import com.example.data.model.MonthlyBill
import com.example.data.model.PaymentRecord
import com.example.i18n.AppLanguage
import com.google.firebase.Timestamp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FormatUtils {
    private val dhakaTz: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")

    private val banglaDigits = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )

    private val reverseBanglaDigits = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    private val bnMonths = mapOf(
        1 to "জানুয়ারি", 2 to "ফেব্রুয়ারি", 3 to "মার্চ", 4 to "এপ্রিল",
        5 to "মে", 6 to "জুন", 7 to "জুলাই", 8 to "আগস্ট",
        9 to "সেপ্টেম্বর", 10 to "অক্টোবর", 11 to "নভেম্বর", 12 to "ডিসেম্বর"
    )

    private val enMonths = mapOf(
        1 to "January", 2 to "February", 3 to "March", 4 to "April",
        5 to "May", 6 to "June", 7 to "July", 8 to "August",
        9 to "September", 10 to "October", 11 to "November", 12 to "December"
    )

    fun toLocalizedDigits(input: String, language: AppLanguage): String {
        if (language == AppLanguage.EN) return input
        return buildString(input.length) {
            for (ch in input) {
                append(banglaDigits[ch] ?: ch)
            }
        }
    }

    fun toAsciiDigits(input: String): String {
        return buildString(input.length) {
            for (ch in input) {
                append(reverseBanglaDigits[ch] ?: ch)
            }
        }
    }

    fun parseLocalizedDouble(input: String): Double? {
        return toAsciiDigits(input).trim().toDoubleOrNull()
    }

    fun formatBdt(amount: Double, language: AppLanguage = AppLanguage.BN): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = if (amount % 1.0 == 0.0) {
            DecimalFormat("#,##0", symbols)
        } else {
            DecimalFormat("#,##0.00", symbols)
        }
        val formatted = "৳${df.format(amount)}"
        return toLocalizedDigits(formatted, language)
    }

    fun formatInt(value: Int, language: AppLanguage = AppLanguage.BN): String {
        return toLocalizedDigits(value.toString(), language)
    }

    fun todayDdMmYyyy(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        sdf.timeZone = dhakaTz
        return sdf.format(Date())
    }

    fun currentMonthKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        sdf.timeZone = dhakaTz
        return sdf.format(Date())
    }

    fun currentBillingMonth(): String {
        val sdf = SimpleDateFormat("MM-yyyy", Locale.US)
        sdf.timeZone = dhakaTz
        return sdf.format(Date())
    }

    fun defaultDueDateDdMmYyyy(): String {
        val month = currentBillingMonth()
        return "10-$month"
    }

    fun resolvePaymentBillingMonth(payment: PaymentRecord, bills: List<MonthlyBill>): String {
        if (payment.billingMonth.isNotBlank()) return payment.billingMonth
        val matchedBill = bills.firstOrNull { it.id == payment.billId }
        if (matchedBill != null && matchedBill.billingMonth.isNotBlank()) {
            return matchedBill.billingMonth
        }
        val billSuffixMatch = Regex("_(\\d{4})-(\\d{2})$").find(payment.billId)
        if (billSuffixMatch != null) {
            val year = billSuffixMatch.groupValues[1]
            val month = billSuffixMatch.groupValues[2]
            return "$month-$year"
        }
        val dateMatch = Regex("(\\d{2}-\\d{4})$").find(toAsciiDigits(payment.paymentDate).trim())
        if (dateMatch != null) {
            return dateMatch.groupValues[1]
        }
        return currentBillingMonth()
    }

    fun formatBillingMonthLabel(monthStr: String, language: AppLanguage): String {
        val ascii = toAsciiDigits(monthStr).trim()
        val mmYyyy = Regex("^(\\d{2})-(\\d{4})$").find(ascii)
        val yyyyMm = Regex("^(\\d{4})-(\\d{2})$").find(ascii)
        val (mm, yyyy) = when {
            mmYyyy != null -> mmYyyy.groupValues[1].toIntOrNull() to mmYyyy.groupValues[2]
            yyyyMm != null -> yyyyMm.groupValues[2].toIntOrNull() to yyyyMm.groupValues[1]
            else -> null to ""
        }
        if (mm == null || mm !in 1..12 || yyyy.isBlank()) {
            return toLocalizedDigits(monthStr, language)
        }
        val mmPadded = mm.toString().padStart(2, '0')
        val name = if (language == AppLanguage.BN) bnMonths[mm] else enMonths[mm]
        return if (language == AppLanguage.BN) {
            toLocalizedDigits("$name $yyyy ($mmPadded-$yyyy)", AppLanguage.BN)
        } else {
            "$name $yyyy ($mmPadded-$yyyy)"
        }
    }

    fun formatPaymentMethodLabel(method: String, language: AppLanguage): String {
        return when (method.lowercase()) {
            "cash" -> if (language == AppLanguage.BN) "নগদ / হাতে (Cash)" else "Cash (Hand-to-Hand)"
            "bank_transfer" -> if (language == AppLanguage.BN) "ব্যাংক ট্রান্সফার" else "Bank Transfer"
            "bkash" -> if (language == AppLanguage.BN) "বিকাশ (bKash)" else "bKash"
            "nagad" -> if (language == AppLanguage.BN) "নগদ মোবাইল (Nagad)" else "Nagad"
            "rocket" -> if (language == AppLanguage.BN) "রকেট (Rocket)" else "Rocket"
            "card" -> if (language == AppLanguage.BN) "কার্ড" else "Card"
            "sslcommerz" -> "SSLCOMMERZ"
            else -> method.uppercase()
        }
    }

    fun formatTimestamp(ts: Timestamp?, language: AppLanguage = AppLanguage.BN): String {
        val date = ts?.toDate() ?: return todayDdMmYyyy()
        val sdf = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US)
        sdf.timeZone = dhakaTz
        return toLocalizedDigits(sdf.format(date), language)
    }

    fun sanitizeId(raw: String): String {
        return raw.trim()
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(64)
            .ifBlank { "id_${System.currentTimeMillis()}" }
    }
}
