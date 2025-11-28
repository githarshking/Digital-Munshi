package com.githarshking.the_digital_munshi

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.githarshking.the_digital_munshi.data.MunshiDatabase

class CategorizationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val smsId = inputData.getLong("TRANSACTION_ID", -1L)
        val smsBody = inputData.getString("SMS_BODY") ?: return Result.failure()
        val sender = inputData.getString("SENDER") ?: "Unknown"

        if (smsId == -1L) return Result.failure()

        Log.d("MunshiAI", "Worker started for Txn ID: $smsId")

        return try {
            val db = MunshiDatabase.getDatabase(applicationContext)
            val dao = db.transactionDao()

            val transaction = dao.getTransactionById(smsId) ?: return Result.failure()

            // UPDATED: Passing applicationContext to GeminiClassifier
            val classifier = GeminiClassifier(applicationContext)
            val prediction = classifier.categorizeTransaction(smsBody, sender)

            Log.d("MunshiAI", "Gemini Output: $prediction")

            val updatedTxn = transaction.copy(
                category = prediction.category,
                isVerified = transaction.isVerified || prediction.confidence > 0.85,
                aiCategory = prediction.category,
                aiConfidence = prediction.confidence,
                aiReasoning = prediction.reasoning
            )

            dao.updateTransaction(updatedTxn)
            Log.d("MunshiAI", "Database Updated Successfully")

            Result.success()
        } catch (e: Exception) {
            Log.e("MunshiAI", "Worker Failed", e)
            Result.retry()
        }
    }
}