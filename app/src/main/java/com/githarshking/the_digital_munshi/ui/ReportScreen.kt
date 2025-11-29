package com.githarshking.the_digital_munshi.ui

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.githarshking.the_digital_munshi.QrCodeUtils
import com.githarshking.the_digital_munshi.data.ReportViewModel
import java.util.UUID

// Define Professional Colors
val ProfessionalGreen = Color(0xFF2E7D32)
val ProfessionalRed = Color(0xFFC62828)
val StabilityGreen = Color(0xFF00C853)
val StabilityYellow = Color(0xFFFFAB00)
val StabilityOrange = Color(0xFFFF6D00)
val StabilityRed = Color(0xFFD50000)
val DarkGreyText = Color(0xFF49454F)
val SurfaceWhite = Color.White
val DividerColor = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onNavigateBack: () -> Unit
) {
    val riskProfile by viewModel.riskProfile.collectAsState()
    val signedReport by viewModel.signedReport.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    var showMonthDropdown by remember { mutableStateOf(false) }
    val userId = remember { "USR-${UUID.randomUUID().toString().take(8).uppercase()}" }
    var showQrDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showMonthDropdown = true }) {
                        Text(selectedMonth ?: "Risk Assessment", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Filter", tint = Color.White)
                    }

                    DropdownMenu(
                        expanded = showMonthDropdown,
                        onDismissRequest = { showMonthDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Time (Risk View)") },
                            onClick = { viewModel.setMonthFilter(null); showMonthDropdown = false }
                        )
                        availableMonths.forEach { month ->
                            DropdownMenuItem(
                                text = { Text(month) },
                                onClick = { viewModel.setMonthFilter(month); showMonthDropdown = false }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (signedReport.isSigned) {
                        IconButton(onClick = { showQrDialog = true }) {
                            Icon(Icons.Default.QrCode, contentDescription = "Share Report")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A237E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F7FA))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // HEADER
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("GENERATED FOR", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(userId, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                    if (selectedMonth != null) {
                        Text("FILTER: $selectedMonth", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1A237E))
                    }
                }
            }

            // ZONE A: KPIs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val isCollecting = riskProfile.stabilityScore == 0.0 || riskProfile.stabilityLabel.contains("Collecting")

                    // NEW: Loan Eligibility Hero Card
                    LoanEligibilityCard(amount = riskProfile.loanEligibility, isCollecting = isCollecting)

                    // Stability
                    StabilityKpiCard(
                        score = riskProfile.stabilityScore,
                        label = riskProfile.stabilityLabel,
                        isCollecting = isCollecting
                    )

                    // Business Health
                    BusinessHealthKpiCard(
                        profitMargin = riskProfile.profitMargin,
                        expenseRatio = 100 - riskProfile.profitMargin
                    )
                }
            }

            // ZONE B: CHARTS
            item {
                OutlinedCard(
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Seasonality Waveform", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkGreyText)
                        Spacer(Modifier.height(16.dp))

                        if (riskProfile.monthlyTrend.size > 1) {
                            BarChart(
                                data = riskProfile.monthlyTrend,
                                modifier = Modifier.fillMaxWidth().height(180.dp)
                            )
                        } else {
                            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Not enough data to generate chart", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                    Text("Add more past transactions", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // FINANCIAL ROW
            item {
                OutlinedCard(
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
                ) {
                    Row(
                        Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FinancialColumn("INCOME", "₹${riskProfile.totalIncome.toInt()}", ProfessionalGreen)

                        // Custom Divider to avoid version issues
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(DividerColor))

                        FinancialColumn("EXPENSE", "₹${riskProfile.totalExpense.toInt()}", ProfessionalRed)

                        // Custom Divider
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(DividerColor))

                        FinancialColumn("SAVINGS", "₹${riskProfile.netSavings.toInt()}", Color.Black)
                    }
                }
            }

            // ZONE C: ECOSYSTEM
            item {
                OutlinedCard(
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Revenue Sources", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkGreyText)
                        Spacer(Modifier.height(16.dp))
                        if (riskProfile.topSources.isEmpty()) {
                            Text("No verified sources.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        } else {
                            riskProfile.topSources.forEach { (source, amount) ->
                                EcosystemRow(source = source, amount = amount, totalIncome = riskProfile.totalIncome)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            // LEGAL FOOTER
            item {
                if (!signedReport.isSigned) {
                    CertificationSection(
                        deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                        onSignClicked = { viewModel.generateLegalSignature(context) }
                    )
                } else {
                    DigitalSealCard(signature = signedReport.signature)
                }

                if (signedReport.error != null) {
                    Toast.makeText(context, signedReport.error, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }

            item { Spacer(Modifier.height(30.dp)) }
        }

        if (showQrDialog) {
            QrShareDialog(
                qrContent = signedReport.payloadJson,
                onDismiss = { showQrDialog = false }
            )
        }
    }
}

// --- HELPER COMPOSABLES (MUST BE INCLUDED) ---

@Composable
fun LoanEligibilityCard(amount: Int, isCollecting: Boolean) {
    OutlinedCard(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        border = androidx.compose.foundation.BorderStroke(1.dp, ProfessionalGreen),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("ESTIMATED LOAN ELIGIBILITY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ProfessionalGreen)
            Spacer(Modifier.height(4.dp))
            if (isCollecting) {
                Text("Analyzing...", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("Add at least 2 months of data.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                Text("₹$amount", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = ProfessionalGreen)
                Text("Based on monthly surplus & stability.", style = MaterialTheme.typography.bodySmall, color = ProfessionalGreen)
            }
        }
    }
}

@Composable
fun StabilityKpiCard(score: Double, label: String, isCollecting: Boolean) {
    val statusColor = when {
        isCollecting -> Color.Gray
        score < 10 -> StabilityGreen
        score < 20 -> StabilityYellow
        score < 40 -> StabilityOrange
        else -> StabilityRed
    }
    val icon = if (isCollecting) Icons.Default.Info else Icons.Default.Verified

    OutlinedCard(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("STABILITY SCORE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            val heroValue = if (isCollecting) "N/A" else "${"%.1f".format(score)}%"
            Text(heroValue, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = statusColor)
            Text(if (isCollecting) "Collecting Data..." else label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Spacer(Modifier.height(4.dp))
            Text(if (isCollecting) "Need history" else "Coefficient of Variation", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun BusinessHealthKpiCard(profitMargin: Int, expenseRatio: Int) {
    OutlinedCard(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("BUSINESS HEALTH", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("PROFIT MARGIN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ProfessionalGreen)
                    Text("$profitMargin%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ProfessionalGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("EXPENSE RATIO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ProfessionalRed)
                    Text("$expenseRatio%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ProfessionalRed)
                }
            }
        }
    }
}

@Composable
fun FinancialColumn(label: String, amount: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun BarChart(data: List<Pair<String, Float>>, modifier: Modifier = Modifier) {
    val points = data.map { it.second }
    val max = points.maxOrNull() ?: 1f

    Canvas(modifier = modifier) {
        val chartHeight = size.height
        val chartWidth = size.width
        val barWidth = (chartWidth / points.size) * 0.6f
        val spacing = (chartWidth / points.size) * 0.4f

        drawLine(color = Color.LightGray, start = Offset(0f, chartHeight), end = Offset(chartWidth, chartHeight), strokeWidth = 2.dp.toPx())
        drawLine(color = Color.LightGray, start = Offset(0f, 0f), end = Offset(0f, chartHeight), strokeWidth = 2.dp.toPx())

        points.forEachIndexed { index, value ->
            val x = (index * (barWidth + spacing)) + (spacing / 2)
            val barHeight = (value / max) * chartHeight
            val y = chartHeight - barHeight
            drawRect(color = Color(0xFF3F51B5), topLeft = Offset(x, y), size = Size(barWidth, barHeight))
        }
    }
}

@Composable
fun CertificationSection(deviceName: String, onSignClicked: () -> Unit) {
    var isChecked by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBC02D))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Legal Certification (Section 65B)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(checked = isChecked, onCheckedChange = { isChecked = it }, colors = CheckboxDefaults.colors(checkedColor = Color.Black, checkmarkColor = Color.White))
                Text("I certify that this report was generated from electronic records residing on my personal device ($deviceName), and is a true representation of my financial transactions.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp), color = Color.Black)
            }
            Button(onClick = onSignClicked, enabled = isChecked, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E), disabledContainerColor = Color.Gray)) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign & Lock Report")
            }
        }
    }
}

@Composable
fun EcosystemRow(source: String, amount: Double, totalIncome: Double) {
    val percentage = (amount / totalIncome).toFloat()
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(source, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = DarkGreyText)
            Text("₹${amount.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = DarkGreyText)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFEEEEEE))) {
            Box(Modifier.fillMaxWidth(percentage).fillMaxHeight().background(Color(0xFF2196F3)))
        }
        Text("${(percentage * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun DigitalSealCard(signature: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Verified, contentDescription = "Verified", tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
            Text("DIGITALLY SIGNED", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            Spacer(Modifier.height(8.dp))
            Text(signature.take(20) + "...", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            Text("Tamper-proof record.", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }
    }
}

@Composable
fun QrShareDialog(qrContent: String, onDismiss: () -> Unit) {
    val qrBitmap = remember(qrContent) { QrCodeUtils.generateQrCode(qrContent) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lender Scan Code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Show this code to the loan officer.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                if (qrBitmap != null) Image(bitmap = qrBitmap, contentDescription = "QR Code", modifier = Modifier.size(200.dp), contentScale = ContentScale.Fit)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))) { Text("Close") }
            }
        }
    }
}