package com.example.util

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

    fun toLocalizedDigits(input: String, language: AppLanguage): String {
        if (language == AppLanguage.EN) return input
        return buildString(input.length) {
            for (ch in input) {
                append(banglaDigits[ch] ?: ch)
            }
        }
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
