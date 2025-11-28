package com.githarshking.the_digital_munshi

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.githarshking.the_digital_munshi.data.*
import com.githarshking.the_digital_munshi.ui.ReportScreen
import com.githarshking.the_digital_munshi.ui.theme.DigitalMunshiTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState // CHANGED IMPORT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MunshiApp() {
    // --- FIX: Request BOTH permissions ---
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
    )

    // Check if all permissions are granted
    if (permissionsState.allPermissionsGranted) {
        AppNavigation()
    } else {
        PermissionRequestScreen(
            onRequestPermission = {
                permissionsState.launchMultiplePermissionRequest()
            }
        )
    }
}

// ... The rest of the file (AppNavigation, MainAppScreen, etc.) is IDENTICAL to Day 4 ...
// ... Paste the rest of your MainActivity.kt code here ...
// To save space, I am including the PermissionRequestScreen and Navigation below:

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val dao = remember { MunshiDatabase.getDatabase(context).transactionDao() }
    val reportViewModelFactory = ReportViewModelFactory(dao)

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MainAppScreen(dao = dao, onNavigateToReport = { navController.navigate("report") })
        }
        composable("report") {
            val reportViewModel: ReportViewModel = viewModel(factory = reportViewModelFactory)
            ReportScreen(viewModel = reportViewModel, onNavigateBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Permission Needed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("To automatically read bank and UPI transactions, this app needs permission to RECEIVE and READ your SMS messages.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermission) { Text("Grant Permissions") }
    }
}

// ... Copy MainAppScreen, HomeScreenBody, TransactionItem, AddTransactionDialog from your previous code ...
// ... They do not need changes, only the MunshiApp() function changed ...
// ... Just ensure you have the full file content ...

// For completeness, here is the MainAppScreen start so you know where to paste the rest
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(dao: TransactionDao, onNavigateToReport: () -> Unit) {
    val transactions by dao.getAllTransactions().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Digital Munshi") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onNavigateToReport) {
                        Icon(Icons.Default.Assessment, contentDescription = "View Report")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                transactionToEdit = null
                showAddDialog = true
            }) {
                Text("Add", modifier = Modifier.padding(16.dp))
            }
        }
    ) { innerPadding ->
        HomeScreenBody(
            modifier = Modifier.padding(innerPadding),
            transactions = transactions,
            onEdit = { transaction ->
                transactionToEdit = transaction
                showAddDialog = true
            }
        )

        if (showAddDialog) {
            AddTransactionDialog(
                transactionToEdit = transactionToEdit,
                onDismiss = { showAddDialog = false },
                onConfirm = { transaction ->
                    CoroutineScope(Dispatchers.IO).launch {
                        dao.insertTransaction(transaction)
                    }
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun HomeScreenBody(modifier: Modifier = Modifier, transactions: List<Transaction>, onEdit: (Transaction) -> Unit) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No transactions yet. Tap '+' to add one!", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions) { transaction ->
                    TransactionItem(transaction = transaction, onClick = { onEdit(transaction) })
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = transaction.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (transaction.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2196F3), modifier = Modifier.size(16.dp))
                    }
                }
                Text(text = transaction.note ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(text = "From: ${transaction.counterparty}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    transactionToEdit: Transaction?,
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit
) {
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf(transactionToEdit?.note ?: "") }
    var counterparty by remember { mutableStateOf(transactionToEdit?.counterparty ?: "") }
    var isExpense by remember { mutableStateOf(transactionToEdit?.type == "EXPENSE" || transactionToEdit == null) }
    var selectedCategory by remember { mutableStateOf(transactionToEdit?.category ?: "Groceries") }

    val categories = listOf("Groceries", "Fuel", "Salary", "Sales", "Rent", "Other", "SMS / Uncategorized")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transactionToEdit == null) "Add Transaction" else "Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") })
                OutlinedTextField(value = counterparty, onValueChange = { counterparty = it }, label = { Text("Paid To / Received From") })
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (Optional)") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isExpense, onClick = { isExpense = true })
                    Text("Expense", modifier = Modifier.padding(end = 16.dp))
                    RadioButton(selected = !isExpense, onClick = { isExpense = false })
                    Text("Income")
                }
                OutlinedTextField(value = selectedCategory, onValueChange = { selectedCategory = it }, label = { Text("Category") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val newTransaction = Transaction(
                    id = transactionToEdit?.id ?: 0L,
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    type = if (isExpense) "EXPENSE" else "INCOME",
                    date = transactionToEdit?.date ?: System.currentTimeMillis(),
                    category = selectedCategory.ifEmpty { "Other" },
                    note = note,
                    source = transactionToEdit?.source ?: "MANUAL",
                    counterparty = counterparty.ifEmpty { "Unknown" },
                    isVerified = transactionToEdit?.isVerified ?: false,
                    transactionHash = transactionToEdit?.transactionHash ?: UUID.randomUUID().toString()
                )
                onConfirm(newTransaction)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DigitalMunshiTheme {
        MainAppScreen(
            dao = object : TransactionDao {
                override suspend fun insertTransaction(transaction: Transaction) {}
                override fun getAllTransactions() = kotlinx.coroutines.flow.flowOf(emptyList<Transaction>())
                override fun getTransactionsBetweenDates(startDate: Long, endDate: Long) = kotlinx.coroutines.flow.flowOf(emptyList<Transaction>())
            },
            onNavigateToReport = {}
        )
    }
}