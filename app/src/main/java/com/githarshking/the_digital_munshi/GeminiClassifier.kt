package com.githarshking.the_digital_munshi

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Define your prediction result
data class TransactionPrediction(
    val category: String,
    val type: String,
    val reasoning: String,
    val confidence: Double
)

// UPDATED: Accepts Context to read User Preferences
class GeminiClassifier(private val context: Context) {

    // Reads from BuildConfig
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    suspend fun categorizeTransaction(smsBody: String, sender: String): TransactionPrediction {
        return withContext(Dispatchers.IO) {

            // 1. GET USER CONTEXT
            val (name, occupation, desc) = UserPreferences.getUserDetails(context)

            val prompt = """
                You are a financial agent for $name, who works as a "$occupation".
                Job Description: "$desc".
                
                Analyze this SMS: "$smsBody" from Sender: "$sender".
                
                Task:
                1. Identify 'category' based on their job. (e.g., If they are a driver, 'Fuel' is a Business Expense).
                2. Identify 'type' (Income/Expense).
                3. Provide 'reasoning' (max 10 words) mentioning their job context if relevant.
                4. Give 'confidence' (0.0 to 1.0).
                
                Output JSON Schema:
                { "category": "str", "type": "str", "reasoning": "str", "confidence": float }
            """.trimIndent()

            try {
                val response = model.generateContent(prompt)
                val jsonString = response.text ?: "{}"

                val jsonObject = JSONObject(jsonString)

                TransactionPrediction(
                    category = jsonObject.optString("category", "Uncategorized"),
                    type = jsonObject.optString("type", "EXPENSE").uppercase(),
                    reasoning = jsonObject.optString("reasoning", "AI Prediction"),
                    confidence = jsonObject.optDouble("confidence", 0.0)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                TransactionPrediction("Uncategorized", "EXPENSE", "AI Error", 0.0)
            }
        }
    }
}