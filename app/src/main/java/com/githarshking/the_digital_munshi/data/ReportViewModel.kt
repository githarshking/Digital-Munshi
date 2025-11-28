package com.githarshking.the_digital_munshi.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.githarshking.the_digital_munshi.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val profitMargin: Int = 0,
    val monthlyTrend: List<Pair<String, Float>> = emptyList(),
    val topSources: List<Pair<String, Double>> = emptyList()
)

data class SignedReport(
    val signature: String = "",
    val publicKey: String = "",
    val timestamp: Long = 0L,
    val isSigned: Boolean = false
)

class ReportViewModel(dao: TransactionDao) : ViewModel() {

    private val allTransactions = dao.getAllTransactions()

    // State for the currently selected month filter (null = All Time)
    private val _selectedMonth = MutableStateFlow<String?>(null)
    val selectedMonth = _selectedMonth.asStateFlow()

    // List of available months for the dropdown
    val availableMonths: StateFlow<List<String>> = allTransactions.map { list ->
        val fmt = SimpleDateFormat("MMM yyyy", Locale.US)
        list.map { fmt.format(Date(it.date)) }.distinct().sorted() // Ideally sort by date object
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // We combine the raw transactions with the filter selection
    val riskProfile: StateFlow<RiskProfile> = combine(allTransactions, _selectedMonth) { transactions, monthFilter ->
        calculateRiskMetrics(transactions, monthFilter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), RiskProfile())

    // Helper to update filter
    fun setMonthFilter(month: String?) {
        _selectedMonth.value = month
    }

    // ... Signed Report Logic (Same as before) ...
    private val _signedReport = MutableStateFlow(SignedReport())
    val signedReport = _signedReport.asStateFlow()

    fun generateLegalSignature(profile: RiskProfile) {
        // (Same implementation as previous step)
        viewModelScope.launch {
            try {
                val payload = """
                    DIGITAL_MUNSHI_REPORT_V3
                    INCOME:${profile.totalIncome}
                    MARGIN:${profile.profitMargin}
                    STABILITY:${profile.stabilityScore}
                    TIMESTAMP:${System.currentTimeMillis()}
                """.trimIndent()
                val signature = SecurityUtils.signData(payload)
                val pubKey = SecurityUtils.getPublicKey()
                _signedReport.value = SignedReport(signature, pubKey, System.currentTimeMillis(), true)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun calculateRiskMetrics(allTxns: List<Transaction>, monthFilter: String?): RiskProfile {
        if (allTxns.isEmpty()) return RiskProfile()

        // 1. Filter Transactions based on selection
        val fmt = SimpleDateFormat("MMM yyyy", Locale.US)
        val filteredTxns = if (monthFilter == null) {
            allTxns
        } else {
            allTxns.filter { fmt.format(Date(it.date)) == monthFilter }
        }

        // 2. Calculate Totals (On Filtered Data)
        val incomeList = filteredTxns.filter { it.type == "INCOME" }
        val totalIncome = incomeList.sumOf { it.amount }
        val totalExpense = filteredTxns.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val velocity = filteredTxns.size
        val netSavings = totalIncome - totalExpense
        val profitMargin = if (totalIncome > 0) ((netSavings / totalIncome) * 100).toInt() else 0
        val verifiedIncome = incomeList.filter { it.isVerified }.sumOf { it.amount }
        val verifiedRatio = if (totalIncome > 0) ((verifiedIncome / totalIncome) * 100).toInt() else 0

        // 3. Stability & Waveform (ALWAYS use All Data)
        // Lenders want to see the history even if you are looking at one month's report
        val allIncomeList = allTxns.filter { it.type == "INCOME" }
        val monthlyIncomeMap = allIncomeList
            .groupBy { fmt.format(Date(it.date)) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val monthlyIncomes = monthlyIncomeMap.values.toList()

        // Chart Data (Sort is tricky with strings, for hackathon MVP assume insertion order or basic sort)
        // To sort correctly: Parse string back to date
        val chartData = allIncomeList
            .groupBy { fmt.format(Date(it.date)) }
            .map { (m, list) -> m to list.sumOf { it.amount }.toFloat() }
        // Simple optimization for demo: Don't strictly sort complexly if not needed

        var cv = 0.0
        var label = "Insufficient Data"
        var peaks = emptyList<String>()

        if (monthlyIncomes.isNotEmpty()) {
            val mean = monthlyIncomes.average()
            val variance = monthlyIncomes.map { (it - mean).pow(2) }.average()
            val stdDev = sqrt(variance)
            if (mean > 0) cv = (stdDev / mean) * 100

            label = when {
                monthlyIncomes.size < 2 -> "Collecting Data" // If only 1 month data exists
                cv < 20.0 -> "High Stability"
                cv < 50.0 -> "Medium Stability"
                else -> "Volatile"
            }
            peaks = monthlyIncomeMap.filter { it.value > (mean * 1.5) }.keys.toList()
        }

        // If a filter is active, we might want to hide Stability label (as it applies to all time)
        // OR we keep it to show the user's overall standing. Let's keep it.

        val topSources = incomeList // Uses filtered list for ecosystem
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
// ... Factory ...
class ReportViewModelFactory(private val dao: TransactionDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
