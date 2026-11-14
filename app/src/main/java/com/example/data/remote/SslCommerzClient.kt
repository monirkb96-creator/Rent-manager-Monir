package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class GatewayInitiationResult(
    val isLiveSession: Boolean,
    val transactionId: String,
    val gatewayPageUrl: String?,
    val sessionKey: String?,
    val statusMessage: String
)

data class GatewayValidationResult(
    val isValid: Boolean,
    val validatedAmount: Double,
    val bankTranId: String?,
    val status: String
)

/**
 * SSLCOMMERZ (Bangladesh Payment Gateway) integration architecture.
 *
 * Production Note:
 * In live production, never ship your `store_passwd` inside the APK.
 * Route session initiation and IPN/val_id verification through your Firebase Cloud Function
 * (`initiateSslCommerzPayment` & `verifySslCommerzPayment`), which signs requests server-side
 * and writes verified payment records to Firestore.
 *
 * When a merchant Store ID / Cloud Function endpoint is not yet configured, this client
 * operates in transparent Sandbox Test Mode, creating a verifiable pending gateway transaction
 * for Admin/Webhook confirmation without falsely marking any unpaid bill as paid.
 */
class SslCommerzClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun generateTransactionId(prefix: String = "SRM"): String {
        val random = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        return "${prefix}_${System.currentTimeMillis() % 1000000}_$random"
    }

    suspend fun initiateSandboxSession(
        storeId: String,
        storePassword: String,
        amount: Double,
        tranId: String,
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        productName: String,
        sandboxMode: Boolean = true
    ): GatewayInitiationResult = withContext(Dispatchers.IO) {
        if (storeId.isBlank() || storePassword.isBlank()) {
            return@withContext GatewayInitiationResult(
                isLiveSession = false,
                transactionId = tranId,
                gatewayPageUrl = null,
                sessionKey = null,
                statusMessage = "Sandbox Mode: Merchant credentials not configured. Transaction queued for owner/gateway verification."
            )
        }

        val endpoint = if (sandboxMode) {
            "https://sandbox.sslcommerz.com/gwprocess/v4/api.php"
        } else {
            "https://securepay.sslcommerz.com/gwprocess/v4/api.php"
        }

        try {
            val formBody = FormBody.Builder()
                .add("store_id", storeId)
                .add("store_passwd", storePassword)
                .add("total_amount", String.format("%.2f", amount))
                .add("currency", "BDT")
                .add("tran_id", tranId)
                .add("success_url", "https://smartrent.example.com/payment/success")
                .add("fail_url", "https://smartrent.example.com/payment/fail")
                .add("cancel_url", "https://smartrent.example.com/payment/cancel")
                .add("cus_name", customerName.ifBlank { "Tenant" })
                .add("cus_email", customerEmail.ifBlank { "tenant@example.com" })
                .add("cus_phone", customerPhone.ifBlank { "01700000000" })
                .add("cus_add1", "Dhaka, Bangladesh")
                .add("cus_city", "Dhaka")
                .add("cus_country", "Bangladesh")
                .add("shipping_method", "NO")
                .add("product_name", productName)
                .add("product_category", "PropertyRent")
                .add("product_profile", "non-physical-goods")
                .build()

            val request = Request.Builder()
                .url(endpoint)
                .post(formBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val bodyString = response.body?.string().orEmpty()
            val json = JSONObject(bodyString)
            val status = json.optString("status")
            if (status == "SUCCESS") {
                GatewayInitiationResult(
                    isLiveSession = true,
                    transactionId = tranId,
                    gatewayPageUrl = json.optString("GatewayPageURL"),
                    sessionKey = json.optString("sessionkey"),
                    statusMessage = "SSLCOMMERZ session initiated"
                )
            } else {
                GatewayInitiationResult(
                    isLiveSession = false,
                    transactionId = tranId,
                    gatewayPageUrl = null,
                    sessionKey = null,
                    statusMessage = json.optString("failedreason", "Gateway initiation returned $status")
                )
            }
        } catch (e: Exception) {
            GatewayInitiationResult(
                isLiveSession = false,
                transactionId = tranId,
                gatewayPageUrl = null,
                sessionKey = null,
                statusMessage = e.localizedMessage ?: "Network error connecting to SSLCOMMERZ"
            )
        }
    }
}
