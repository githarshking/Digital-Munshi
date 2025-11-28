package com.githarshking.the_digital_munshi.ui

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.githarshking.the_digital_munshi.QrCodeUtils
import com.githarshking.the_digital_munshi.data.ReportViewModel
import java.util.UUID

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showMonthDropdown = true }) {
                        Text(selectedMonth ?: "Risk Assessment (All Time)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Filter", tint = Color.White)
                    }

                    // Dropdown for Filtering
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

            // --- HEADER ---
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("GENERATED FOR", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(userId, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                    if (selectedMonth != null) {
                        Text("FILTER: $selectedMonth", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1A237E))
                    }
                }
            }

            // --- ZONE A: KPIs ---
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = if(selectedMonth == null) "STABILITY SCORE" else "MONTHLY STATUS",
                        value = riskProfile.stabilityLabel,
                        subtext = if(selectedMonth == null) "CV Score: ${"%.1f".format(riskProfile.stabilityScore)}%" else "For this month",
                        isGood = riskProfile.stabilityScore < 20 || riskProfile.stabilityLabel.contains("Collecting")
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "BUSINESS HEALTH",
                        value = "${riskProfile.profitMargin}% Margin",
                        subtext = "Expense Ratio: ${100 - riskProfile.profitMargin}%",
                        isGood = riskProfile.profitMargin > 20
                    )
                }
            }

            // --- ZONE B: CHARTS ---
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Seasonality Waveform (History)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        if (riskProfile.monthlyTrend.isNotEmpty()) {
                            SeasonalityChart(data = riskProfile.monthlyTrend, modifier = Modifier.fillMaxWidth().height(150.dp))
                        } else {
                            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("Insufficient data", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // --- TOTALS ROW ---
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("INCOME", style = MaterialTheme.typography.labelSmall); Text("₹${riskProfile.totalIncome.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }
                        Column { Text("EXPENSE", style = MaterialTheme.typography.labelSmall); Text("₹${riskProfile.totalExpense.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) }
                        Column { Text("SAVINGS", style = MaterialTheme.typography.labelSmall); Text("₹${riskProfile.netSavings.toInt()}", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            // --- ZONE C: ECOSYSTEM ---
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(if(selectedMonth == null) "Revenue Sources (All Time)" else "Revenue Sources ($selectedMonth)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        if (riskProfile.topSources.isEmpty()) {
                            Text("No verified sources.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            riskProfile.topSources.forEach { (source, amount) ->
                                EcosystemRow(source = source, amount = amount, totalIncome = riskProfile.totalIncome)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            // --- LEGAL FOOTER ---
            item {
                if (!signedReport.isSigned) {
                    CertificationSection(
                        deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                        onSignClicked = { viewModel.generateLegalSignature(riskProfile) }
                    )
                } else {
                    DigitalSealCard(signature = signedReport.signature)
                }
            }

            item { Spacer(Modifier.height(30.dp)) }
        }

        if (showQrDialog) {
            QrShareDialog(
                userId = userId,
                riskProfile = riskProfile,
                signedReport = signedReport,
                onDismiss = { showQrDialog = false }
            )
        }
    }
}

// ... (Include the rest of the helper functions: KpiCard, SeasonalityChart, EcosystemRow, CertificationSection, DigitalSealCard, QrShareDialog) ...
// They remain exactly as they were in the previous step. Copy them here.
// IMPORTANT: Re-paste the helper functions from the previous "Step 3: Update ReportScreen.kt" block
// to ensure the file is complete.

@Composable
fun KpiCard(modifier: Modifier, title: String, value: String, subtext: String, isGood: Boolean) {
    val color = if (isGood) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val textColor = if (isGood) Color(0xFF2E7D32) else Color(0xFFC62828)
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = textColor)
            Spacer(Modifier.height(4.dp))
            Text(subtext, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun SeasonalityChart(data: List<Pair<String, Float>>, modifier: Modifier = Modifier) {
    val points = data.map { it.second }
    val max = points.maxOrNull() ?: 1f
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (points.size.coerceAtLeast(1))
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = index * stepX + (stepX / 2)
            val y = height - ((value / max) * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color = Color(0xFF3F51B5), radius = 6.dp.toPx(), center = Offset(x, y))
        }
        drawPath(path = path, color = Color(0xFF3F51B5), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()
        drawPath(path = path, brush = Brush.verticalGradient(colors = listOf(Color(0xFF3F51B5).copy(alpha = 0.2f), Color.Transparent)))
    }
}

@Composable
fun EcosystemRow(source: String, amount: Double, totalIncome: Double) {
    val percentage = (amount / totalIncome).toFloat()
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(source, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("₹${amount.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFEEEEEE))) {
            Box(Modifier.fillMaxWidth(percentage).fillMaxHeight().background(Color(0xFF2196F3)))
        }
        Text("${(percentage * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun CertificationSection(deviceName: String, onSignClicked: () -> Unit) {
    var isChecked by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFC107))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Legal Certification (Section 65B)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFFA000))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(checked = isChecked, onCheckedChange = { isChecked = it })
                Text("I certify that this report was generated from electronic records residing on my personal device ($deviceName), and is a true representation of my financial transactions.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
            }
            Button(onClick = onSignClicked, enabled = isChecked, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign & Lock Report")
            }
        }
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
fun QrShareDialog(
    userId: String,
    riskProfile: com.githarshking.the_digital_munshi.data.RiskProfile,
    signedReport: com.githarshking.the_digital_munshi.data.SignedReport,
    onDismiss: () -> Unit
) {
    val payload = remember {
        QrCodeUtils.createSharePayload(
            userId = userId,
            income = riskProfile.totalIncome,
            score = riskProfile.stabilityScore,
            signature = signedReport.signature,
            publicKey = signedReport.publicKey
        )
    }
    val qrBitmap = remember(payload) { QrCodeUtils.generateQrCode(payload) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lender Scan Code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Show this code to the loan officer to instantly verify your data.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                if (qrBitmap != null) {
                    Image(bitmap = qrBitmap, contentDescription = "QR Code", modifier = Modifier.size(200.dp), contentScale = ContentScale.Fit)
                } else { Text("Error generating QR", color = Color.Red) }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))) { Text("Close") }
            }
        }
    }
}