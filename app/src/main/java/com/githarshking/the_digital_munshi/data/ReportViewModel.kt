package com.githarshking.the_digital_munshi.data

import android.content.Context
import android.os.Build
import android.widget.Toast
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
    val topSources: List<Pair<String, Double>> = emptyList()
)

data class SignedReport(
    val signature: String = "",
    val publicKey: String = "",
    val payloadJson: String = "", // Store the full JSON here
    val timestamp: Long = 0L,
    val isSigned: Boolean = false,
    val error: String? = null // New Error Field
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

    /**
     * Generates the Section 65B Compliant, Bank-Grade QR Payload.
     * Now requires Context to fetch Real Identity.
     */
    fun generateLegalSignature(context: Context) {
        viewModelScope.launch {
            try {
                // 1. FIX RACE CONDITION: Get the latest calculation explicitly
                val profile = riskProfile.first()

                // 2. SAFETY CHECK: Prevent "Ghost User" reports
                if (profile.totalIncome <= 0) {
                    _signedReport.value = SignedReport(error = "Insufficient Data: Cannot certify a report with zero income.")
                    return@launch
                }

                // 3. FETCH IDENTITY: Get real name from preferences
                val (name, occupation, _) = UserPreferences.getUserDetails(context)
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

                // 4. CREATE PAYLOAD: Generate the strict JSON Structure
                // We sign a simplified string for the signature, but distribute the full JSON
                val signaturePayload = "ID:$name|INC:${profile.totalIncome}|CV:${profile.stabilityScore}|TS:${System.currentTimeMillis()}"

                // 5. SIGN: Hardware backed signature
                val signature = SecurityUtils.signData(signaturePayload)
                val pubKey = SecurityUtils.getPublicKey()

                // 6. COMPILE: Create the final QR JSON
                val finalJson = QrCodeUtils.createCreditProfileJson(
                    userName = name,
                    userOccupation = occupation,
                    deviceName = deviceName,
                    income = profile.totalIncome,
                    savings = profile.netSavings,
                    stabilityScore = profile.stabilityScore,
                    stabilityLabel = profile.stabilityLabel,
                    profitMargin = profile.profitMargin,
                    signature = signature,
                    publicKey = pubKey
                )

                // 7. UPDATE STATE
                _signedReport.value = SignedReport(
                    signature = signature,
                    publicKey = pubKey,
                    payloadJson = finalJson, // Store this for the QR generator
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

    // ... calculateRiskMetrics logic remains identical to previous step ...
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