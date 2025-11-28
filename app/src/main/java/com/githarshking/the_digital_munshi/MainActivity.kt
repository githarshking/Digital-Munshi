package com.githarshking.the_digital_munshi

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.githarshking.the_digital_munshi.ui.OnboardingScreen
import com.githarshking.the_digital_munshi.ui.ReportScreen
import com.githarshking.the_digital_munshi.ui.theme.DigitalMunshiTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MunshiApp() {
    val context = LocalContext.current

    // 1. CHECK ONBOARDING STATUS
    var isOnboarded by remember { mutableStateOf(UserPreferences.isOnboarded(context)) }

    if (!isOnboarded) {
        // 2. SHOW ONBOARDING
        OnboardingScreen(context = context, onFinished = { isOnboarded = true })
    } else {
        // 3. SHOW MAIN APP (If permissions granted)
        val permissionsState = rememberMultiplePermissionsState(
            permissions = listOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
        )

        if (permissionsState.allPermissionsGranted) {
            AppNavigation()
        } else {
            PermissionRequestScreen(onRequestPermission = { permissionsState.launchMultiplePermissionRequest() })
        }
    }
}

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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Permission Needed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRequestPermission) { Text("Grant Permissions") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(dao: TransactionDao, onNavigateToReport: () -> Unit) {
    val transactions by dao.getAllTransactions().collectAsState(initial = emptyList())
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    var showSmartDialog by remember { mutableStateOf(false) }
    var selectedSmartTransaction by remember { mutableStateOf<Transaction?>(null) }

    // NEW: Profile Dialog State
    var showProfileDialog by remember { mutableStateOf(false) }

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
                    // 1. PROFILE ICON (Left of Dashboard)
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "My Profile",
                            modifier = Modifier.size(30.dp) // Nice readable size
                        )
                    }

                    // 2. DASHBOARD ICON (Increased Size)
                    IconButton(
                        onClick = onNavigateToReport,
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "View Report",
                            modifier = Modifier.size(36.dp) // BIGGER SIZE (Easy to tap)
                        )
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

        // --- MAIN LIST ---
        HomeScreenBody(
            modifier = Modifier.padding(innerPadding),
            transactions = transactions,
            onEdit = { transaction ->
                if (transaction.aiCategory != null) {
                    selectedSmartTransaction = transaction
                    showSmartDialog = true
                } else {
                    transactionToEdit = transaction
                    showAddDialog = true
                }
            }
        )

        // --- DIALOGS ---

        if (showAddDialog) {
            AddTransactionDialog(
                transactionToEdit = transactionToEdit,
                onDismiss = { showAddDialog = false },
                onConfirm = { transaction ->
                    CoroutineScope(Dispatchers.IO).launch { dao.insertTransaction(transaction) }
                    showAddDialog = false
                }
            )
        }

        if (showSmartDialog && selectedSmartTransaction != null) {
            SmartTransactionDialog(
                transaction = selectedSmartTransaction!!,
                onDismiss = { showSmartDialog = false },
                onEditManually = {
                    showSmartDialog = false
                    transactionToEdit = selectedSmartTransaction
                    showAddDialog = true
                }
            )
        }

        // NEW: Profile Edit Dialog
        if (showProfileDialog) {
            ProfileEditDialog(
                context = context,
                onDismiss = { showProfileDialog = false }
            )
        }
    }
}

@Composable
fun ProfileEditDialog(context: Context, onDismiss: () -> Unit) {
    // Load existing user details to pre-fill the form
    val (currentName, currentOcc, currentDesc) = remember { UserPreferences.getUserDetails(context) }

    var name by remember { mutableStateOf(currentName) }
    var occupation by remember { mutableStateOf(currentOcc) }
    var description by remember { mutableStateOf(currentDesc) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
        title = { Text("My Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Updating your profile helps the AI categorize your transactions better.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = occupation, onValueChange = { occupation = it },
                    label = { Text("Occupation") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Work Context (for AI)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                UserPreferences.saveUser(context, name, occupation, description)
                onDismiss()
            }) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SmartTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onEditManually: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF673AB7)) },
        title = { Text("AI Insight", color = Color(0xFF673AB7)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("I categorized this as:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    transaction.category.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Divider()
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = transaction.aiReasoning ?: "Based on transaction patterns.", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Confidence Score", style = MaterialTheme.typography.labelSmall)
                        Text("${(transaction.aiConfidence * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = transaction.aiConfidence.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if(transaction.aiConfidence > 0.8) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))) { Text("Looks Good!") } },
        dismissButton = { TextButton(onClick = onEditManually) { Text("Wrong? Edit") } }
    )
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
    val dateStr = remember(transaction.date) { SimpleDateFormat("dd MMM", Locale.US).format(Date(transaction.date)) }
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = transaction.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (transaction.aiConfidence > 0.0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("✨ AI", fontSize = 10.sp, color = Color(0xFF673AB7), fontWeight = FontWeight.Bold, modifier = Modifier.background(Color(0xFFEDE7F6), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                    } else if (transaction.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2196F3), modifier = Modifier.size(16.dp))
                    }
                }
                Text(text = "$dateStr • ${transaction.note ?: ""}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                if(transaction.counterparty != "Unknown") Text(text = "From: ${transaction.counterparty}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
            }
            Text(text = "₹${"%.2f".format(transaction.amount)}", style = MaterialTheme.typography.bodyLarge, color = if (transaction.type == "INCOME") Color(0xFF008000) else Color.Red, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(transactionToEdit: Transaction?, onDismiss: () -> Unit, onConfirm: (Transaction) -> Unit) {
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf(transactionToEdit?.note ?: "") }
    var counterparty by remember { mutableStateOf(transactionToEdit?.counterparty ?: "") }
    var isExpense by remember { mutableStateOf(transactionToEdit?.type == "EXPENSE" || transactionToEdit == null) }
    var selectedCategory by remember { mutableStateOf(transactionToEdit?.category ?: "Groceries") }
    var selectedDateMillis by remember { mutableStateOf(transactionToEdit?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { selectedDateMillis = dateState.selectedDateMillis ?: System.currentTimeMillis(); showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dateState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transactionToEdit == null) "Add Transaction" else "Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(selectedDateMillis)),
                    onValueChange = {}, label = { Text("Date") }, readOnly = true,
                    trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarToday, contentDescription = "Select Date") } },
                    modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showDatePicker = true }
                )
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") })
                OutlinedTextField(value = counterparty, onValueChange = { counterparty = it }, label = { Text("Paid To / Received From") })
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (Optional)") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isExpense, onClick = { isExpense = true }); Text("Expense", modifier = Modifier.padding(end = 16.dp))
                    RadioButton(selected = !isExpense, onClick = { isExpense = false }); Text("Income")
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
                    date = selectedDateMillis,
                    category = selectedCategory.ifEmpty { "Other" },
                    note = note,
                    source = transactionToEdit?.source ?: "MANUAL",
                    counterparty = counterparty.ifEmpty { "Unknown" },
                    isVerified = transactionToEdit?.isVerified ?: false,
                    transactionHash = transactionToEdit?.transactionHash ?: UUID.randomUUID().toString(),
                    aiCategory = transactionToEdit?.aiCategory,
                    aiConfidence = transactionToEdit?.aiConfidence ?: 0.0,
                    aiReasoning = transactionToEdit?.aiReasoning
                )
                onConfirm(newTransaction)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DigitalMunshiTheme {
        MainAppScreen(
            dao = object : TransactionDao {
                override suspend fun insertTransaction(transaction: Transaction) = 0L
                override suspend fun updateTransaction(transaction: Transaction) {}
                override suspend fun getTransactionById(id: Long) = null
                override fun getAllTransactions() = kotlinx.coroutines.flow.flowOf(emptyList<Transaction>())
            },
            onNavigateToReport = {}
        )
    }
}