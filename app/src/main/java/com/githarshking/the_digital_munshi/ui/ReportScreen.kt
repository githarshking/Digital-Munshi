package com.githarshking.the_digital_munshi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.githarshking.the_digital_munshi.data.ReportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onNavigateBack: () -> Unit // Function to go back
) {
    // Collect the calculated values from the ViewModel
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val netSavings by viewModel.netSavings.collectAsState()
    val expenseBreakdown by viewModel.expenseBreakdown.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kamayi Patra (Report)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- Totals Section ---
            item {
                Text(
                    "Monthly Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SummaryCard(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netSavings = netSavings
                )
            }

            // --- Breakdown Section ---
            item {
                Text(
                    "Expense Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (expenseBreakdown.isEmpty()) {
                item {
                    Text(
                        "No expenses logged yet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // Display a row for each category
                items(expenseBreakdown.toList()) { (category, amount) ->
                    CategoryRow(category = category, amount = amount)
                }
            }
        }
    }
}

@Composable
fun SummaryCard(totalIncome: Double, totalExpense: Double, netSavings: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryRow("Total Income", totalIncome, Color(0xFF008000))
            SummaryRow("Total Expense", totalExpense, Color.Red)
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            SummaryRow("Net Savings", netSavings, Color.Black)
        }
    }
}

@Composable
fun SummaryRow(label: String, amount: Double, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(
            "₹${"%.2f".format(amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun CategoryRow(category: String, amount: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category, style = MaterialTheme.typography.bodyLarge)
            Text(
                "₹${"%.2f".format(amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
        }
    }
}