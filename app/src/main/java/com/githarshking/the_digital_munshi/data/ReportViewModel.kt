package com.githarshking.the_digital_munshi.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The "brain" for our Report Screen. It takes the full list of
 * transactions and calculates all the summaries.
 */
class ReportViewModel(dao: TransactionDao) : ViewModel() {

    // 1. Get all transactions from the database
    private val allTransactions = dao.getAllTransactions()

    // 2. Calculate Total Income
    // .map transforms the list of transactions into a single number
    val totalIncome: StateFlow<Double> = allTransactions.map { list ->
        list.filter { it.type == "INCOME" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    // 3. Calculate Total Expense
    val totalExpense: StateFlow<Double> = allTransactions.map { list ->
        list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    // 4. Calculate Net Savings (Income - Expense)
    val netSavings: StateFlow<Double> = allTransactions.map { list ->
        val income = list.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    // 5. Calculate Expense Breakdown by Category
    // This creates a Map, e.g., {"Groceries": 150.0, "Fuel": 300.0}
    val expenseBreakdown: StateFlow<Map<String, Double>> = allTransactions.map { list ->
        list.filter { it.type == "EXPENSE" }
            .groupBy { it.category } // Group by category string
            .mapValues { entry -> entry.value.sumOf { it.amount } } // Sum up each group
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())
}

/**
 * This is a "Factory" class. It's a standard pattern
 * that tells Android *how* to create our ReportViewModel,
 * since our ViewModel needs the 'dao' to be passed into it.
 */
class ReportViewModelFactory(private val dao: TransactionDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}