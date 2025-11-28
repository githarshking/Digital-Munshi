package com.githarshking.the_digital_munshi // Your package name

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.githarshking.the_digital_munshi.data.MunshiDatabase
import com.githarshking.the_digital_munshi.data.Transaction
import com.githarshking.the_digital_munshi.ui.theme.DigitalMunshiTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Make sure you have these imports for permissions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DigitalMunshiTheme {
                // This is the new entry point
                MunshiApp()
            }
        }
    }
}

/**
 * NEW: This composable is now our permission checker.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MunshiApp() {
    // --- Permission Handling ---
    val smsPermissionState = rememberPermissionState(
        android.Manifest.permission.READ_SMS
    )

    if (smsPermissionState.status.isGranted) {
        // Permission is GRANTED: Show the main app
        MainAppScreen()
    } else {
        // Permission is NOT GRANTED: Show a request screen
        PermissionRequestScreen(
            onRequestPermission = {
                smsPermissionState.launchPermissionRequest()
            }
        )
    }
}

/**
 * NEW: A screen to show users who haven't granted the permission.
 */
@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Permission Needed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "To automatically read bank and UPI transactions, " +
                    "this app needs permission to read your SMS messages.",
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}


/**
 * RENAMED: This is your *original* MunshiApp() function,
 * now renamed to "MainAppScreen" and with new logic for editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    // --- Database & State Setup ---
    val context = LocalContext.current
    val dao = remember { MunshiDatabase.getDatabase(context).transactionDao() }
    val transactions by dao.getAllTransactions().collectAsState(initial = emptyList())

    // --- State for Dialogs ---
    var showAddDialog by remember { mutableStateOf(false) }

    // NEW: This state holds the transaction we want to edit.
    // null = adding a new one
    // not-null = editing an existing one
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

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
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // UPDATED: When we click "+", we set transactionToEdit to null
                // to signal we are "adding" a new item.
                transactionToEdit = null
                showAddDialog = true
            }) {
                Text("Add", modifier = Modifier.padding(16.dp))
            }
        }
    ) { innerPadding ->

        // UPDATED: We pass the onEdit lambda to our home screen
        HomeScreenBody(
            modifier = Modifier.padding(innerPadding),
            transactions = transactions,
            onEdit = { transaction ->
                // When a list item is clicked, this lambda is called.
                // We set the transaction to edit...
                transactionToEdit = transaction
                // ...and show the dialog.
                showAddDialog = true
            }
        )

        // --- Dialog Logic ---
        if (showAddDialog) {
            AddTransactionDialog(
                // UPDATED: We pass the transaction-to-edit to the dialog
                transactionToEdit = transactionToEdit,
                onDismiss = { showAddDialog = false },
                onConfirm = { transaction ->
                    CoroutineScope(Dispatchers.IO).launch {
                        // This one line handles BOTH adding and updating,
                        // thanks to our database's OnConflictStrategy.REPLACE
                        dao.insertTransaction(transaction)
                    }
                    showAddDialog = false
                }
            )
        }
    }
}

/**
 * UPDATED: This now takes an 'onEdit' function.
 */
@Composable
fun HomeScreenBody(
    modifier: Modifier = Modifier,
    transactions: List<Transaction>,
    onEdit: (Transaction) -> Unit // NEW parameter
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No transactions yet. Tap '+' to add one!",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions) { transaction ->
                    // UPDATED: We pass the onEdit lambda to the item
                    TransactionItem(
                        transaction = transaction,
                        onClick = { onEdit(transaction) } // NEW
                    )
                }
            }
        }
    }
}

/**
 * UPDATED: This now takes an 'onClick' function and is clickable.
 */
@Composable
fun TransactionItem(transaction: Transaction, onClick: () -> Unit) { // NEW parameter
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // NEW: This makes the card clickable
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
                    text = transaction.note ?: "",
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
 * HEAVILY UPDATED: This dialog now handles both "Add" and "Edit".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    transactionToEdit: Transaction?, // NEW: The transaction to edit (or null)
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit
) {
    // UPDATED: All state is now "remembered" based on
    // whether we are adding or editing.
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf(transactionToEdit?.note ?: "") }
    var isExpense by remember { mutableStateOf(transactionToEdit?.type == "EXPENSE" || transactionToEdit == null) }
    var selectedCategory by remember { mutableStateOf(transactionToEdit?.category ?: "Groceries") }

    // UPDATED: Added the new default SMS category to the list
    val categories = listOf("Groceries", "Fuel", "Salary", "Sales", "Rent", "Other", "SMS / Uncategorized")

    AlertDialog(
        onDismissRequest = onDismiss,
        // UPDATED: Title is now dynamic
        title = { Text(if (transactionToEdit == null) "Add Transaction" else "Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") }
                )
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
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = { selectedCategory = it },
                    label = { Text("Category") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                // UPDATED: When confirming, we create a Transaction object,
                // but we make sure to keep the original ID, date, and source
                // if we are editing.
                val newTransaction = Transaction(
                    id = transactionToEdit?.id ?: 0L, // 0L for new, existing ID for edit
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    type = if (isExpense) "EXPENSE" else "INCOME",
                    date = transactionToEdit?.date ?: System.currentTimeMillis(), // Keep original date
                    category = selectedCategory.ifEmpty { "Other" },
                    note = note,
                    source = transactionToEdit?.source ?: "MANUAL" // Keep original source
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


/**
 * UPDATED: The preview now shows the "MainAppScreen"
 * so you don't see the permission request in your preview window.
 */
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DigitalMunshiTheme {
        MainAppScreen() // Changed from MunshiApp()
    }
}