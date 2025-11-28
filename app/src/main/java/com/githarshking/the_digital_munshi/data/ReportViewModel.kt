package com.githarshking.the_digital_munshi.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.githarshking.the_digital_munshi.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

data class RiskProfile(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val velocity: Int = 0,
    val stabilityScore: Double = 0.0,
    val stabilityLabel: String = "N/A",
    val peakMonths: List<String> = emptyList(),
    val verifiedRatio: Int = 0,

    // --- NEW FIELDS FOR 5-ZONE LAYOUT ---
    val profitMargin: Int = 0, // Business Health (0-100%)
    val monthlyTrend: List<Pair<String, Float>> = emptyList(), // Chart Data (Month Label, Amount)
    val topSources: List<Pair<String, Double>> = emptyList()   // Ecosystem Data (Source, Amount)
)

data class SignedReport(
    val signature: String = "",
    val publicKey: String = "",
    val timestamp: Long = 0L,
    val isSigned: Boolean = false
)

class ReportViewModel(dao: TransactionDao) : ViewModel() {

    private val allTransactions = dao.getAllTransactions()

    val riskProfile: StateFlow<RiskProfile> = allTransactions.map { transactions ->
        calculateRiskMetrics(transactions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), RiskProfile())

    // We keep this for the raw data if needed, though Zone C replaces the pie chart
    val expenseBreakdown: StateFlow<Map<String, Double>> = allTransactions.map { list ->
        list.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    private val _signedReport = MutableStateFlow(SignedReport())
    val signedReport = _signedReport.asStateFlow()

    fun generateLegalSignature(profile: RiskProfile) {
        viewModelScope.launch {
            try {
                val payload = """
                    DIGITAL_MUNSHI_REPORT_V2
                    INCOME:${profile.totalIncome}
                    MARGIN:${profile.profitMargin}
                    STABILITY:${profile.stabilityScore}
                    TOP_SOURCE:${profile.topSources.firstOrNull()?.first ?: "None"}
                    TIMESTAMP:${System.currentTimeMillis()}
                """.trimIndent()

                val signature = SecurityUtils.signData(payload)
                val pubKey = SecurityUtils.getPublicKey()

                _signedReport.value = SignedReport(
                    signature = signature,
                    publicKey = pubKey,
                    timestamp = System.currentTimeMillis(),
                    isSigned = true
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateRiskMetrics(transactions: List<Transaction>): RiskProfile {
        if (transactions.isEmpty()) return RiskProfile()

        // 1. Basic Totals
        val incomeList = transactions.filter { it.type == "INCOME" }
        val totalIncome = incomeList.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val velocity = transactions.size
        val netSavings = totalIncome - totalExpense

        // 2. Business Health (Profit Margin)
        val profitMargin = if (totalIncome > 0) ((netSavings / totalIncome) * 100).toInt() else 0

        // 3. Trust Score
        val verifiedIncome = incomeList.filter { it.isVerified }.sumOf { it.amount }
        val verifiedRatio = if (totalIncome > 0) ((verifiedIncome / totalIncome) * 100).toInt() else 0

        // 4. Stability & Seasonality (Zone B Data)
        val fmt = SimpleDateFormat("MMM", Locale.US) // Short month name for chart
        val fullFmt = SimpleDateFormat("MMM yyyy", Locale.US)

        val monthlyIncomeMap = incomeList
            .groupBy { fullFmt.format(Date(it.date)) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val monthlyIncomes = monthlyIncomeMap.values.toList()

        // Prepare Chart Data (Sort by time roughly - for MVP we assume DB order or Sort map)
        // Note: In production, verify date sorting.
        val chartData = incomeList
            .groupBy { fmt.format(Date(it.date)) }
            .map { (month, list) -> month to list.sumOf { it.amount }.toFloat() }
        // If we have real dates, we should sort. For MVP hackathon, list order is often enough if data is new.

        // Standard Deviation Math
        var cv = 0.0
        var label = "Insufficient Data"
        var peaks = emptyList<String>()

        if (monthlyIncomes.isNotEmpty()) {
            val mean = monthlyIncomes.average()
            val variance = monthlyIncomes.map { (it - mean).pow(2) }.average()
            val stdDev = sqrt(variance)

            if (mean > 0) cv = (stdDev / mean) * 100

            label = when {
                monthlyIncomes.size < 2 -> "Collecting Data"
                cv < 20.0 -> "High Stability"
                cv < 50.0 -> "Medium Stability"
                else -> "Volatile"
            }
            peaks = monthlyIncomeMap.filter { it.value > (mean * 1.5) }.keys.toList()
        }

        // 5. Ecosystem (Zone C Data - Top Sources)
        val topSources = incomeList
            .groupBy { it.counterparty }
            .map { (source, list) -> source to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(5)

        return RiskProfile(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSavings = netSavings,
            velocity = velocity,
            stabilityScore = cv,
            stabilityLabel = label,
            peakMonths = peaks,
            verifiedRatio = verifiedRatio,
            profitMargin = profitMargin,
            monthlyTrend = chartData,
            topSources = topSources
        )
    }
}

class ReportViewModelFactory(private val dao: TransactionDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}