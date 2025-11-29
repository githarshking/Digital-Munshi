package com.githarshking.the_digital_munshi.data

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.githarshking.the_digital_munshi.QrCodeUtils
import com.githarshking.the_digital_munshi.SecurityUtils
import com.githarshking.the_digital_munshi.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val topSources: List<Pair<String, Double>> = emptyList(),

    // --- NEW: LOAN UNDERWRITING FIELDS ---
    val loanEligibility: Int = 0,
    val monthlySurplus: Double = 0.0
)

data class SignedReport(
    val signature: String = "",
    val publicKey: String = "",
    val payloadJson: String = "",
    val timestamp: Long = 0L,
    val isSigned: Boolean = false,
    val error: String? = null
)

class ReportViewModel(dao: TransactionDao) : ViewModel() {

    private val allTransactions = dao.getAllTransactions()

    private val _selectedMonth = MutableStateFlow<String?>(null)
    val selectedMonth = _selectedMonth.asStateFlow()

    val availableMonths: StateFlow<List<String>> = allTransactions.map { list ->
        val fmt = SimpleDateFormat("MMM yyyy", Locale.US)
        list.map { fmt.format(Date(it.date)) }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val riskProfile: StateFlow<RiskProfile> = combine(allTransactions, _selectedMonth) { transactions, monthFilter ->
        calculateRiskMetrics(transactions, monthFilter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), RiskProfile())

    fun setMonthFilter(month: String?) {
        _selectedMonth.value = month
    }

    private val _signedReport = MutableStateFlow(SignedReport())
    val signedReport = _signedReport.asStateFlow()

    fun generateLegalSignature(context: Context) {
        viewModelScope.launch {
            try {
                val profile = riskProfile.first()

                if (profile.totalIncome <= 0) {
                    _signedReport.value = SignedReport(error = "Insufficient Data: Cannot certify a report with zero income.")
                    return@launch
                }

                val (name, occupation, _) = UserPreferences.getUserDetails(context)
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

                val signaturePayload = "ID:$name|LOAN:${profile.loanEligibility}|CV:${profile.stabilityScore}|TS:${System.currentTimeMillis()}"

                val signature = SecurityUtils.signData(signaturePayload)
                val pubKey = SecurityUtils.getPublicKey()

                // UPDATED: Now passing loanEligibility to the JSON generator
                val finalJson = QrCodeUtils.createCreditProfileJson(
                    userName = name,
                    userOccupation = occupation,
                    deviceName = deviceName,
                    income = profile.totalIncome,
                    savings = profile.netSavings,
                    stabilityScore = profile.stabilityScore,
                    stabilityLabel = profile.stabilityLabel,
                    profitMargin = profile.profitMargin,
                    loanEligibility = profile.loanEligibility, // Pass the new value
                    signature = signature,
                    publicKey = pubKey
                )

                _signedReport.value = SignedReport(
                    signature = signature,
                    publicKey = pubKey,
                    payloadJson = finalJson,
                    timestamp = System.currentTimeMillis(),
                    isSigned = true,
                    error = null
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _signedReport.value = SignedReport(error = "Signing Failed: ${e.message}")
            }
        }
    }

    fun clearError() {
        _signedReport.value = _signedReport.value.copy(error = null)
    }

    private fun calculateRiskMetrics(allTxns: List<Transaction>, monthFilter: String?): RiskProfile {
        if (allTxns.isEmpty()) return RiskProfile()

        val fmt = SimpleDateFormat("MMM yyyy", Locale.US)
        val filteredTxns = if (monthFilter == null) {
            allTxns
        } else {
            allTxns.filter { fmt.format(Date(it.date)) == monthFilter }
        }

        val incomeList = filteredTxns.filter { it.type == "INCOME" }
        val totalIncome = incomeList.sumOf { it.amount }
        val totalExpense = filteredTxns.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val velocity = filteredTxns.size
        val netSavings = totalIncome - totalExpense
        val profitMargin = if (totalIncome > 0) ((netSavings / totalIncome) * 100).toInt() else 0
        val verifiedIncome = incomeList.filter { it.isVerified }.sumOf { it.amount }
        val verifiedRatio = if (totalIncome > 0) ((verifiedIncome / totalIncome) * 100).toInt() else 0

        // Calculate Stability (All Time)
        val allIncomeList = allTxns.filter { it.type == "INCOME" }
        val monthlyIncomeMap = allIncomeList
            .groupBy { fmt.format(Date(it.date)) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val monthlyIncomes = monthlyIncomeMap.values.toList()

        val chartData = allIncomeList
            .groupBy { fmt.format(Date(it.date)) }
            .map { (m, list) -> m to list.sumOf { it.amount }.toFloat() }

        var cv = 0.0
        var label = "Insufficient Data"
        var peaks = emptyList<String>()

        // Default Loan logic (Fallback)
        var estimatedLoanLimit = 0

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

            // --- LOAN ELIGIBILITY ALGORITHM ---
            val monthCount = monthlyIncomes.size.coerceAtLeast(1)

            // 1. Calculate Average Monthly Surplus (Total Savings / Months Active)
            // Note: We use 'netSavings' (filtered) if filter is active, or total net savings if not.
            // For underwriting, it's safer to use the 'Mean Average' of surplus if possible,
            // but for MVP we will use (Total Income - Total Expense) / Months.
            val totalAllTimeSavings = allTxns.filter { it.type == "INCOME" }.sumOf { it.amount } -
                    allTxns.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            val avgMonthlySurplus = if(totalAllTimeSavings > 0) totalAllTimeSavings / monthCount else 0.0

            // 2. Safe EMI Capacity (50% of Surplus)
            val safeEmi = avgMonthlySurplus * 0.50

            // 3. Base Loan (12 Months Tenure)
            val baseLoan = safeEmi * 12

            // 4. Risk Adjustment based on CV (Stability)
            val riskFactor = when {
                monthlyIncomes.size < 2 -> 0.0 // Too new
                cv < 20.0 -> 1.0  // 100% of Base
                cv < 50.0 -> 0.75 // 75% of Base
                else -> 0.0       // Too volatile
            }

            estimatedLoanLimit = (baseLoan * riskFactor).toInt()
        }

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
            topSources = topSources,
            // New Fields
            loanEligibility = estimatedLoanLimit,
            monthlySurplus = if (monthlyIncomes.isNotEmpty()) netSavings / monthlyIncomes.size else 0.0
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