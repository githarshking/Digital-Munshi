package com.githarshking.the_digital_munshi

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class TransactionPrediction(
    val category: String,
    val type: String,
    val reasoning: String,
    val confidence: Double
)

class GeminiClassifier(private val context: Context) {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    // FIX 1: Use the specific version "2.5-flash".
    // The alias "gemini-1.5-flash" sometimes fails on older SDKs.
    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    suspend fun categorizeTransaction(smsBody: String, sender: String): TransactionPrediction {
        return withContext(Dispatchers.IO) {

            val (name, occupation, desc) = UserPreferences.getUserDetails(context)

            val prompt = """
                You are a financial agent for $name, who works as a "$occupation".
                Job Description: "$desc".
                
                Analyze this SMS: "$smsBody" from Sender: "$sender".
                
                Task:
                1. Identify 'category' based on their job.
                2. Identify 'type' (Income/Expense).
                3. Provide 'reasoning' (max 10 words).
                4. Give 'confidence' (0.0 to 1.0).
                
                Output JSON Schema:
                { "category": "str", "type": "str", "reasoning": "str", "confidence": float }
            """.trimIndent()

            try {
                val response = model.generateContent(prompt)
                var jsonString = response.text ?: "{}"

                // FIX 2: Clean the Markdown Backticks
                // Gemini often sends ```json { ... } ``` which crashes the JSON parser
                if (jsonString.contains("`")) {
                    jsonString = jsonString.replace("```json", "")
                        .replace("```", "")
                        .trim()
                }

                Log.d("GeminiClassifier", "Cleaned JSON: $jsonString")

                val jsonObject = JSONObject(jsonString)

                TransactionPrediction(
                    category = jsonObject.optString("category", "Uncategorized"),
                    type = jsonObject.optString("type", "EXPENSE").uppercase(),
                    reasoning = jsonObject.optString("reasoning", "AI Prediction"),
                    confidence = jsonObject.optDouble("confidence", 0.0)
                )
            } catch (e: Exception) {
                Log.e("GeminiClassifier", "Critical AI Failure", e)
                // Return a safe object so the app doesn't crash
                TransactionPrediction(
                    category = "Uncategorized",
                    type = "EXPENSE",
                    reasoning = "AI Error: ${e.message}",
                    confidence = 0.0
                )
            }
        }
    }
}