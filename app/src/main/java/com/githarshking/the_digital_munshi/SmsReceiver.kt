package com.githarshking.the_digital_munshi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.githarshking.the_digital_munshi.data.MunshiDatabase
import com.githarshking.the_digital_munshi.data.Transaction
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * This BroadcastReceiver "listens" for incoming SMS messages.
 * It's triggered by the Android system when an SMS arrives.
 */
class SmsReceiver : BroadcastReceiver() {

    // Simple regex to find amounts (e.g., Rs. 100, Rs 100, 100.00)
    // This is a basic parser; a real app would have a much more complex one.
    private val amountRegex = "(?:Rs\\.?|INR)\\s*([0-9,]+\\.?[0-9]*)".toRegex(RegexOption.IGNORE_CASE)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (context == null) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val msgBody = sms.messageBody
            val sender = sms.originatingAddress ?: "Unknown"

            // --- Simple Bank/UPI Filtering ---
            // We only care about messages that seem transactional.
            if (!isTransactional(sender, msgBody)) {
                continue // Skip this message
            }

            // --- Parse the Message ---
            val transaction = parseSms(msgBody)

            if (transaction != null) {
                // We have a transaction! Now, save it to the database.

                // Get the database DAO
                val dao = MunshiDatabase.getDatabase(context).transactionDao()

                // We must insert data on a background thread.
                // GlobalScope is okay for a simple hackathon receiver.
                GlobalScope.launch {
                    dao.insertTransaction(transaction)
                }
            }
        }
    }

    /**
     * A simple check to see if the message is from a bank.
     * A pro app would have a long list of known bank sender IDs.
     */
    private fun isTransactional(sender: String, body: String): Boolean {
        val lowerBody = body.toLowerCase()
        return (sender.contains("-") || sender.contains("BK")) && // Bank senders often have non-number IDs
                (lowerBody.contains("debited") ||
                        lowerBody.contains("credited") ||
                        lowerBody.contains("spent") ||
                        lowerBody.contains("received"))
    }

    /**
     * This is our core "AI" parser.
     * It tries to extract data from the SMS body.
     */
    private fun parseSms(body: String): Transaction? {
        try {
            val lowerBody = body.toLowerCase()
            val type: String

            // 1. Determine Type
            if (lowerBody.contains("debited") || lowerBody.contains("spent")) {
                type = "EXPENSE"
            } else if (lowerBody.contains("credited") || lowerBody.contains("received")) {
                type = "INCOME"
            } else {
                return null // Not a message we can parse
            }

            // 2. Extract Amount
            val match = amountRegex.find(lowerBody)
            val amount = match?.groups?.get(1)?.value?.replace(",", "")?.toDoubleOrNull()

            if (amount == null || amount == 0.0) {
                return null // No amount found
            }

            // 3. Create the Transaction object
            return Transaction(
                amount = amount,
                type = type,
                date = System.currentTimeMillis(), // Use current time for new SMS
                category = "SMS / Uncategorized", // Default category
                note = body.take(50), // Use first 50 chars of SMS as note
                source = "SMS"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null // Parsing failed
        }
    }
}