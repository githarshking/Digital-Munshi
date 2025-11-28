package com.githarshking.the_digital_munshi // Your package name

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.githarshking.the_digital_munshi.data.MunshiDatabase // Import your database
import com.githarshking.the_digital_munshi.data.Transaction  // Import your Transaction
import com.githarshking.the_digital_munshi.ui.theme.DigitalMunshiTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DigitalMunshiTheme {
                MunshiApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MunshiApp() {
    // --- Database & State Setup ---
    val context = LocalContext.current
    // We get our DAO (database access) from the singleton Database instance
    val dao = remember { MunshiDatabase.getDatabase(context).transactionDao() }

    // This is the magic: .collectAsState() turns our 'Flow' from the DAO
    // into a 'State' object that Compose can watch.
    // When the data changes, the UI will *automatically* rebuild.
    val transactions by dao.getAllTransactions().collectAsState(initial = emptyList())

    // This state will control our "Add Transaction" dialog
    var showAddDialog by remember { mutableStateOf(false) }

    // --- UI Structure ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Digital Munshi") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        // This is the "+" button at the bottom
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                // We'll add an Icon here later, for now Text is fine
                Text("Add", modifier = Modifier.padding(16.dp))
            }
        }
    ) { innerPadding ->

        // Pass the transaction list and padding to our Home Screen
        HomeScreenBody(
            modifier = Modifier.padding(innerPadding),
            transactions = transactions
        )

        // --- Dialog Logic ---
        if (showAddDialog) {
            // When 'showAddDialog' is true, show this dialog
            AddTransactionDialog(
                onDismiss = { showAddDialog = false }, // What to do on "Cancel"
                onConfirm = { transaction ->
                    // On "Confirm", we launch a coroutine to
                    // insert the transaction into the database
                    CoroutineScope(Dispatchers.IO).launch {
                        dao.insertTransaction(transaction)
                    }
                    showAddDialog = false // Close the dialog
                }
            )
        }
    }
}

@Composable
fun HomeScreenBody(modifier: Modifier = Modifier, transactions: List<Transaction>) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (transactions.isEmpty()) {
            // Show this message if the list is empty
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No transactions yet. Tap '+' to add one!",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            // This is the "Recycler View" of Jetpack Compose.
            // It's a high-performance scrolling list.
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions) { transaction ->
                    TransactionItem(transaction = transaction)
                }
            }
        }
    }
}

/**
 * A single row in our transaction list.
 */
@Composable
fun TransactionItem(transaction: Transaction) {
    // A simple card to hold the transaction info
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = transaction.note ?: "", // Show note if it exists
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                text = "₹${"%.2f".format(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (transaction.type == "INCOME") Color(0xFF008000) else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

/**
 * This is the Dialog (popup) for adding a new transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit
) {
    // These 'remember' variables hold the state of our form fields
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) } // Default to Expense
    var selectedCategory by remember { mutableStateOf("Groceries") }

    // A fixed list of categories for our MVP
    val categories = listOf("Groceries", "Fuel", "Salary", "Sales", "Rent", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // --- Amount Field ---
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    // keyboardType = KeyboardType.Number -- We'll add this later
                )

                // --- Note Field ---
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") }
                )

                // --- Income/Expense Toggle ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isExpense,
                        onClick = { isExpense = true }
                    )
                    Text("Expense", modifier = Modifier.padding(end = 16.dp))
                    RadioButton(
                        selected = !isExpense,
                        onClick = { isExpense = false }
                    )
                    Text("Income")
                }

                // --- Category Dropdown (Spinner) ---
                // This is a simple version. A real one would be a dropdown.
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = { selectedCategory = it },
                    label = { Text("Category (e.g., Groceries, Salary)") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val newTransaction = Transaction(
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    type = if (isExpense) "EXPENSE" else "INCOME",
                    date = System.currentTimeMillis(), // Get the current time
                    category = selectedCategory.ifEmpty { "Other" },
                    note = note,
                    source = "MANUAL"
                )
                onConfirm(newTransaction)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- Preview Function (no changes needed) ---
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DigitalMunshiTheme {
        MunshiApp()
    }
}