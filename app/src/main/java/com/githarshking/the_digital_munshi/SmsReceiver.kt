package com.githarshking.the_digital_munshi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.githarshking.the_digital_munshi.data.MunshiDatabase
import com.githarshking.the_digital_munshi.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.Normalizer // IMPORT THIS! The magic cleaner.

class SmsReceiver : BroadcastReceiver() {

    // Updated Regex to handle "INR 270.00" and "Rs 11.00"
    private val amountRegex = "(?i)(?:Rs\\.?|INR)\\s*([0-9,]+(?:\\.[0-9]+)?)".toRegex()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (context == null) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val rawBody = sms.messageBody ?: ""
            val sender = sms.originatingAddress ?: "Unknown"

            // --- STEP 1: THE MAGIC CLEANER (Normalization) ---
            // This converts "𝖽𝖾𝖻𝗂𝗍𝖾𝖽" -> "debited" and "𝖴𝖯𝖨" -> "UPI"
            // NFKC form is built exactly for this compatibility!
            val cleanBody = Normalizer.normalize(rawBody, Normalizer.Form.NFKC)

            Log.d("SmsReceiver", "Original: $rawBody")
            Log.d("SmsReceiver", "Cleaned:  $cleanBody")

            // --- FILTERING ---
            // For the hackathon, we REMOVED the bank sender check completely.
            // It will now accept SMS from ANY number (including your friends/test phone).

            // --- PARSING ---
            val transaction = parseSms(cleanBody)

            if (transaction != null) {
                Log.d("SmsReceiver", "✅ Parsed: ${transaction.type} ₹${transaction.amount}")

                val dao = MunshiDatabase.getDatabase(context).transactionDao()
                CoroutineScope(Dispatchers.IO).launch {
                    dao.insertTransaction(transaction)
                }
            } else {
                Log.d("SmsReceiver", "❌ Failed to parse. Text was cleaned but no keywords found.")
            }
        }
    }

    private fun parseSms(body: String): Transaction? {
        try {
            // Work with the cleaned, normalized text
            val lowerBody = body.toLowerCase()
            var type: String = "UNKNOWN"

            // --- LOGIC FOR "DEBITED" vs "CREDITED" ---
            // We check positions to handle messages like "Debited 11.00... Bal... credited (spam text)"

            val debitIndex = listOf("debited", "spent", "paid", "sent", "withdrawal", "purchase").minOfOrNull {
                val idx = lowerBody.indexOf(it)
                if (idx == -1) Int.MAX_VALUE else idx
            } ?: Int.MAX_VALUE

            val creditIndex = listOf("credited", "received", "deposited", "added", "refund").minOfOrNull {
                val idx = lowerBody.indexOf(it)
                if (idx == -1) Int.MAX_VALUE else idx
            } ?: Int.MAX_VALUE

            if (debitIndex == Int.MAX_VALUE && creditIndex == Int.MAX_VALUE) {
                return null // Neither found
            }

            // Whichever keyword appears FIRST determines the type
            type = if (debitIndex < creditIndex) "EXPENSE" else "INCOME"

            // --- EXTRACT AMOUNT ---
            val match = amountRegex.find(body) // Use 'body' (case sensitive) or 'lowerBody' doesn't matter for numbers

            val amountString = match?.groups?.get(1)?.value?.replace(",", "")
            val amount = amountString?.toDoubleOrNull()

            if (amount == null || amount == 0.0) {
                return null
            }

            return Transaction(
                amount = amount,
                type = type,
                date = System.currentTimeMillis(),
                category = "SMS / Uncategorized",
                note = body.take(60),
                source = "SMS"
            )
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error parsing SMS", e)
            return null
        }
    }
}