package com.githarshking.the_digital_munshi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.githarshking.the_digital_munshi.data.MunshiDatabase
import com.githarshking.the_digital_munshi.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.UUID

class SmsReceiver : BroadcastReceiver() {

    // Regex to find amounts like "Rs. 500" or "INR 1,200.00"
    private val amountRegex = "(?i)(?:Rs\\.?|INR)\\s*([0-9,]+(?:\\.[0-9]+)?)".toRegex()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (context == null) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val rawBody = sms.messageBody ?: ""
            val sender = sms.originatingAddress ?: "Unknown"

            // 1. Normalize (Fix fancy fonts)
            val cleanBody = Normalizer.normalize(rawBody, Normalizer.Form.NFKC)

            // 2. Initial Regex Parse (Fast & Offline)
            val transaction = parseSms(cleanBody, sender)

            if (transaction != null) {
                Log.d("SmsReceiver", "Basic Parse Success. Saving to DB...")

                val dao = MunshiDatabase.getDatabase(context).transactionDao()

                // 3. Save to DB & Trigger AI
                CoroutineScope(Dispatchers.IO).launch {
                    // Insert and get the new Row ID
                    val newId = dao.insertTransaction(transaction)

                    if (newId != -1L) {
                        Log.d("SmsReceiver", "Saved with ID: $newId. Triggering AI Worker...")
                        // 4. Fire the WorkManager task
                        triggerAiWorker(context, newId, cleanBody, sender)
                    }
                }
            }
        }
    }

    /**
     * Schedules the background AI task
     */
    private fun triggerAiWorker(context: Context, id: Long, body: String, sender: String) {
        val workData = workDataOf(
            "TRANSACTION_ID" to id,
            "SMS_BODY" to body,
            "SENDER" to sender
        )

        val workRequest = OneTimeWorkRequestBuilder<CategorizationWorker>()
            .setInputData(workData)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    /**
     * Local Regex Parsing Logic
     */
    private fun parseSms(body: String, sender: String): Transaction? {
        try {
            val lowerBody = body.toLowerCase()

            // --- Determine Type (Income vs Expense) ---
            val debitIndex = listOf("debited", "spent", "paid", "sent", "withdrawal", "purchase", "transfer").minOfOrNull {
                val idx = lowerBody.indexOf(it)
                if (idx == -1) Int.MAX_VALUE else idx
            } ?: Int.MAX_VALUE

            val creditIndex = listOf("credited", "received", "deposited", "added", "refund", "salary").minOfOrNull {
                val idx = lowerBody.indexOf(it)
                if (idx == -1) Int.MAX_VALUE else idx
            } ?: Int.MAX_VALUE

            if (debitIndex == Int.MAX_VALUE && creditIndex == Int.MAX_VALUE) {
                return null // Not a financial message
            }

            val type = if (debitIndex < creditIndex) "EXPENSE" else "INCOME"

            // --- Extract Amount ---
            val match = amountRegex.find(body)
            val amountString = match?.groups?.get(1)?.value?.replace(",", "")
            val amount = amountString?.toDoubleOrNull()

            if (amount == null || amount == 0.0) return null

            // --- Create Transaction Object ---
            return Transaction(
                amount = amount,
                type = type,
                date = System.currentTimeMillis(),
                category = "Processing...", // Placeholder for AI
                note = body.take(60),
                source = "SMS",
                counterparty = sender,
                isVerified = true, // It is verified because it came from SMS
                transactionHash = (body + amount).hashCode().toString()
            )
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error parsing SMS", e)
            return null
        }
    }
}