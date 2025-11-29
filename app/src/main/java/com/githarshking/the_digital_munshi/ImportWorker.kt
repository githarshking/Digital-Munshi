package com.githarshking.the_digital_munshi

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.githarshking.the_digital_munshi.data.MunshiDatabase
import com.githarshking.the_digital_munshi.data.Transaction
import java.text.SimpleDateFormat
import java.util.Locale

class ImportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val fileUriString = inputData.getString("FILE_URI") ?: return Result.failure()
        val fileUri = Uri.parse(fileUriString)

        return try {
            val rawText = PdfUtils.extractTextFromPdf(applicationContext, fileUri)
            Log.d("ImportWorker", "Extracted ${rawText.length} chars")

            val parser = StatementParser()
            val parsedList = parser.parseStatement(rawText)
            Log.d("ImportWorker", "AI found ${parsedList.size} transactions")

            val db = MunshiDatabase.getDatabase(applicationContext)
            val dao = db.transactionDao()
            // Flexible date parser
            val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.US)

            var addedCount = 0

            parsedList.forEach { item ->
                val dateMillis = try {
                    dateFormat.parse(item.dateStr)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) { System.currentTimeMillis() }

                val count = dao.checkDuplicate(item.amount, dateMillis, item.type)

                if (count == 0) {
                    dao.insertTransaction(
                        Transaction(
                            amount = item.amount,
                            type = item.type,
                            date = dateMillis,
                            category = "Statement Import",
                            note = item.description.take(100),
                            source = "PDF_IMPORT",
                            counterparty = "Bank Statement",
                            isVerified = true,
                            transactionHash = (item.description + item.amount).hashCode().toString()
                        )
                    )
                    addedCount++
                }
            }

            Log.d("ImportWorker", "Successfully imported $addedCount transactions")
            Result.success()
        } catch (e: Exception) {
            Log.e("ImportWorker", "Import Failed", e)
            Result.failure()
        }
    }
}