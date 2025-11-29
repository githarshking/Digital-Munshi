package com.githarshking.the_digital_munshi

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class ParsedTransaction(
    val dateStr: String,
    val description: String,
    val amount: Double,
    val type: String
)

class StatementParser {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig { responseMimeType = "application/json" }
    )

    suspend fun parseStatement(rawText: String): List<ParsedTransaction> {
        return withContext(Dispatchers.IO) {
            val prompt = """
                You are a Data Extraction Agent. Convert this raw PDF Bank Statement text into JSON.
                
                RULES:
                1. Look for rows with dates (e.g., '03-Oct-2025', '29-Oct-2025').
                2. **TYPE LOGIC**:
                   - If text contains 'UPI/CR' or 'CR' or 'Deposit' or 'Credited', Type is 'INCOME'.
                   - If text contains 'UPI/DR' or 'DR' or 'Withdrawal' or 'Debited', Type is 'EXPENSE'.
                3. **AMOUNT**: Extract the numeric value strictly.
                4. **DESCRIPTION**: Extract the narrative (e.g. 'UPI/CR/5075.../Pranjal').
                
                INPUT TEXT:
                $rawText
                
                OUTPUT JSON SCHEMA:
                [
                  { "date": "dd-MMM-yyyy", "desc": "String", "amt": Double, "type": "INCOME/EXPENSE" }
                ]
            """.trimIndent()

            try {
                val response = model.generateContent(prompt)
                var jsonString = response.text ?: "[]"

                if (jsonString.contains("`")) {
                    jsonString = jsonString.replace("```json", "").replace("```", "").trim()
                }

                val results = ArrayList<ParsedTransaction>()
                val jsonArray = JSONArray(jsonString)

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    results.add(
                        ParsedTransaction(
                            dateStr = obj.optString("date"),
                            description = obj.optString("desc"),
                            amount = obj.optDouble("amt"),
                            type = obj.optString("type")
                        )
                    )
                }
                results
            } catch (e: Exception) {
                Log.e("StatementParser", "Parsing Failed", e)
                emptyList()
            }
        }
    }
}