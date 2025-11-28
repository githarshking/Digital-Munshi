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
import java.text.Normalizer
import java.util.UUID

class SmsReceiver : BroadcastReceiver() {

    private val amountRegex = "(?i)(?:Rs\\.?|INR)\\s*([0-9,]+(?:\\.[0-9]+)?)".toRegex()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (context == null) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val rawBody = sms.messageBody ?: ""
            val sender = sms.originatingAddress ?: "Unknown"

            // Normalize text
            val cleanBody = Normalizer.normalize(rawBody, Normalizer.Form.NFKC)

            // Parse
            val transaction = parseSms(cleanBody, sender)

            if (transaction != null) {
                Log.d("SmsReceiver", "✅ Verified Transaction: ${transaction.counterparty} ₹${transaction.amount}")

                val dao = MunshiDatabase.getDatabase(context).transactionDao()
                CoroutineScope(Dispatchers.IO).launch {
                    // Ideally, check if transactionHash exists before inserting
                    // But for MVP, Room will just add it.
                    dao.insertTransaction(transaction)
                }
            }
        }
    }

    private fun parseSms(body: String, sender: String): Transaction? {
        try {
            val lowerBody = body.toLowerCase()
            var type: String = "UNKNOWN"

            // Logic for Income vs Expense
            val debitIndex = listOf("debited", "spent", "paid", "sent", "withdrawal", "purchase").minOfOrNull {
                val idx = lowerBody.indexOf(it)
                if (idx == -1) Int.MAX_VALUE else idx
            } ?: Int.MAX_VALUE

            val creditIndex = listOf("credited", "received", "deposited", "added", "refund").minOfOrNull {
                val idx = lowerBody.indexOf(it)
                if (idx == -1) Int.MAX_VALUE else idx
            } ?: Int.MAX_VALUE

            if (debitIndex == Int.MAX_VALUE && creditIndex == Int.MAX_VALUE) return null

            type = if (debitIndex < creditIndex) "EXPENSE" else "INCOME"

            // Extract Amount
            val match = amountRegex.find(body)
            val amountString = match?.groups?.get(1)?.value?.replace(",", "")
            val amount = amountString?.toDoubleOrNull()

            if (amount == null || amount == 0.0) return null

            // --- NEW FIELDS GENERATION ---

            // 1. Counterparty: Use the Sender ID (e.g. VM-HDFCBK)
            // In a real app, we would Regex extract "to Zomato" from the body.
            val counterpartyName = sender

            // 2. Unique Hash: Combine body + amount to create a fingerprint
            // This helps prevent adding the same SMS twice if the receiver triggers multiple times.
            val uniqueHash = (body + amount.toString()).hashCode().toString()

            return Transaction(
                amount = amount,
                type = type,
                date = System.currentTimeMillis(),
                category = "SMS / Uncategorized",
                note = body.take(60),
                source = "SMS",
                // Set new fields
                counterparty = counterpartyName,
                isVerified = true, // High Trust!
                transactionHash = uniqueHash
            )
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error parsing SMS", e)
            return null
        }
    }
}