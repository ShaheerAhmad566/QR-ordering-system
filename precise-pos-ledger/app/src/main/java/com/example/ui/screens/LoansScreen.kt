package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LoanEntity
import com.example.ui.PosViewModel
import com.example.ui.theme.*
import com.example.util.MathEvaluator

@Composable
fun LoansScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val loans by viewModel.loans.collectAsState()

    var showAddLoanDialog by remember { mutableStateOf(false) }
    var loanToRepay by remember { mutableStateOf<LoanEntity?>(null) }
    var loanToAddAmount by remember { mutableStateOf<LoanEntity?>(null) }
    var filterTab by remember { mutableStateOf("ALL") } // ALL, GIVEN, RECEIVED

    BackHandler {
        onNavigateBack()
    }

    val totalGiven = remember(loans) {
        loans.filter { it.direction == "GIVEN" && it.status == "OPEN" }.sumOf { it.remainingBalance }
    }
    val totalReceived = remember(loans) {
        loans.filter { it.direction == "RECEIVED" && it.status == "OPEN" }.sumOf { it.remainingBalance }
    }

    val filteredLoans = remember(loans, filterTab) {
        when (filterTab) {
            "GIVEN" -> loans.filter { it.direction == "GIVEN" }
            "RECEIVED" -> loans.filter { it.direction == "RECEIVED" }
            else -> loans
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HighContrastScreenBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 900.dp)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
                .padding(bottom = 80.dp)
                .testTag("loans_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // Top Header (Responsive Khata Ledger Bar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Loans & Khata",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Receivables (Lent) & Payables (Borrowed)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = { showAddLoanDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .defaultMinSize(minWidth = 110.dp)
                    .height(38.dp)
                    .testTag("new_loan_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Entry", modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "New Entry",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Summary Metric Cards (High Contrast Crisp White Cards)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Money to Receive (Given)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "To Receive (Lent)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Receive", tint = VelocityGreen, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "Rs $totalGiven",
                        style = CurrencyLg,
                        color = VelocityGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "${loans.count { it.direction == "GIVEN" && it.status == "OPEN" }} Outstanding Debts",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Money to Pay (Received)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "To Pay Back",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Pay", tint = AlertRed, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "Rs $totalReceived",
                        style = CurrencyLg,
                        color = AlertRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "${loans.count { it.direction == "RECEIVED" && it.status == "OPEN" }} Active Borrowings",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL" to "All (${loans.size})", "GIVEN" to "Receivables (${loans.count { it.direction == "GIVEN" }})", "RECEIVED" to "Payables (${loans.count { it.direction == "RECEIVED" }})").forEach { (tabKey, label) ->
                val isSel = filterTab == tabKey
                Button(
                    onClick = { filterTab = tabKey },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSel) AmberPrimary else Color.White,
                        contentColor = if (isSel) Color.White else Color(0xFF0F172A)
                    ),
                    border = if (!isSel) ButtonDefaults.outlinedButtonBorder else null,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(label, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }

        // Loans List Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KHATA LEDGER RECORDS (${filteredLoans.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569),
                letterSpacing = 0.5.sp
            )
            Text(
                text = "The Big Bite Master Log",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }

        if (filteredLoans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No loan entries recorded.", color = Color(0xFF64748B))
            }
        } else {
            filteredLoans.forEach { loan ->
                val isGiven = loan.direction == "GIVEN"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_card_${loan.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Title row: direction badge + person name + status badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isGiven) Color(0xFFDCFCE7) else Color(0xFFFFE4E6))
                                        .border(1.dp, if (isGiven) VelocityGreen else AlertRed, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isGiven) "TO RECEIVE (LENT)" else "TO PAY (BORROWED)",
                                        fontSize = 10.sp,
                                        color = if (isGiven) VelocityGreen else AlertRed,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Text(
                                    text = loan.personName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (loan.status == "OPEN") AmberPrimary.copy(alpha = 0.15f) else Color(0xFFDCFCE7))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = loan.status,
                                    fontSize = 11.sp,
                                    color = if (loan.status == "OPEN") AmberPrimary else VelocityGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Financial Breakdown Box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Original Principal",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Rs ${loan.originalAmount}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isGiven) "Shop to Collect" else "Shop Must Pay",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Rs ${loan.remainingBalance}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isGiven) VelocityGreen else AlertRed,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        if (loan.notes.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Notes", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                Text(
                                    text = loan.notes,
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { loanToAddAmount = loan },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("add_amount_btn_${loan.id}")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add More", tint = AmberPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add Amount",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (loan.status == "OPEN") {
                                Button(
                                    onClick = { loanToRepay = loan },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                    border = ButtonDefaults.outlinedButtonBorder,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(38.dp)
                                        .testTag("repay_btn_${loan.id}")
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = "Repay", tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isGiven) "Settle (Pay)" else "Pay Back",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Add Loan Entry (with inline math expression support)
    if (showAddLoanDialog) {
        var personName by remember { mutableStateOf("") }
        var direction by remember { mutableStateOf("GIVEN") }
        var amountExpr by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf("2026-10-15") }

        val evaluatedAmt = MathEvaluator.evaluateToInt(amountExpr, fallback = 0)

        AlertDialog(
            onDismissRequest = { showAddLoanDialog = false },
            title = {
                Text(
                    text = "New Loan / Khata Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Direction:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { direction = "GIVEN" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (direction == "GIVEN") VelocityGreen else Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Shop Lent (Given)",
                                fontSize = 11.5.sp,
                                color = if (direction == "GIVEN") Color.White else Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { direction = "RECEIVED" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (direction == "RECEIVED") AlertRed else Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Shop Borrowed",
                                fontSize = 11.5.sp,
                                color = if (direction == "RECEIVED") Color.White else Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

        OutlinedTextField(
            value = personName,
            onValueChange = { personName = it },
            label = { Text("Person / Customer Name or ID (#KH-105)", color = Color(0xFF334155)) },
            placeholder = { Text("e.g. Aliyan, Dogar, KH-105, or #1", color = Color(0xFF94A3B8)) },
            supportingText = { Text("Tip: Typing an existing name or ID adds to their current balance!", fontSize = 11.sp, color = VelocityGreen) },
            colors = highContrastTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

                    OutlinedTextField(
                        value = amountExpr,
                        onValueChange = { amountExpr = it },
                        label = { Text("Amount (Rs) - supports =8+3 or 9-3", color = Color(0xFF334155)) },
                        placeholder = { Text("e.g. 1500 or =1000+500", color = Color(0xFF94A3B8)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (amountExpr.isNotEmpty()) {
                        Text(
                            text = "Result: Rs $evaluatedAmt",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelocityGreen
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Reason", color = Color(0xFF334155)) },
                        placeholder = { Text("e.g. Grocery credit, advance salary...", color = Color(0xFF94A3B8)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Expected Due Date", color = Color(0xFF334155)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = MathEvaluator.evaluateToInt(amountExpr, fallback = 0)
                        if (personName.isNotBlank() && parsed > 0) {
                            viewModel.recordLoan(personName, direction, parsed, notes, dueDate)
                            showAddLoanDialog = false
                            Toast.makeText(context, "Recorded Rs. $parsed for $personName", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Save Entry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLoanDialog = false }) {
                    Text("Cancel", color = Color(0xFF475569))
                }
            }
        )
    }

    // Modal: Record Loan Repayment (with inline math calculation support)
    if (loanToRepay != null) {
        val targetLoan = loanToRepay!!
        var repayAmountExpr by remember { mutableStateOf(targetLoan.remainingBalance.toString()) }
        var repayMethod by remember { mutableStateOf("CASH") }

        val evaluatedRepay = MathEvaluator.evaluateToInt(repayAmountExpr, fallback = targetLoan.remainingBalance)

        AlertDialog(
            onDismissRequest = { loanToRepay = null },
            title = {
                Text(
                    text = "Record Khata Repayment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Settling: ${targetLoan.personName} (Remaining: Rs ${targetLoan.remainingBalance})",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )

                    OutlinedTextField(
                        value = repayAmountExpr,
                        onValueChange = { repayAmountExpr = it },
                        label = { Text("Settlement Amount (Rs) - supports math", color = Color(0xFF334155)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (repayAmountExpr.isNotEmpty()) {
                        Text(
                            text = "Result: Rs $evaluatedRepay",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelocityGreen
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { repayMethod = "CASH" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (repayMethod == "CASH") AmberPrimary else Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Cash",
                                color = if (repayMethod == "CASH") Color.White else Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { repayMethod = "JAZZCASH" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (repayMethod == "JAZZCASH") JazzCashCrimson else Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "JazzCash",
                                color = if (repayMethod == "JAZZCASH") Color.White else Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = MathEvaluator.evaluateToInt(repayAmountExpr, fallback = 0)
                        if (parsed > 0) {
                            viewModel.recordLoanRepayment(targetLoan.id, parsed, repayMethod)
                            loanToRepay = null
                            Toast.makeText(context, "Recorded Rs. $parsed repayment for ${targetLoan.personName}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Confirm Payment", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToRepay = null }) {
                    Text("Cancel", color = Color(0xFF475569))
                }
            }
        )
    }

    // Modal: Add More Amount to Existing Ledger / Khata
    if (loanToAddAmount != null) {
        val targetLoan = loanToAddAmount!!
        var addAmountExpr by remember { mutableStateOf("") }
        var additionalNotes by remember { mutableStateOf("") }
        val evaluatedAdd = MathEvaluator.evaluateToInt(addAmountExpr, fallback = 0)
        val newProjectedBalance = targetLoan.remainingBalance + evaluatedAdd

        AlertDialog(
            onDismissRequest = { loanToAddAmount = null },
            title = {
                Text(
                    text = "Add Amount to Khata",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Customer: ${targetLoan.personName} (ID: #KH-${100 + targetLoan.id})",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Current Balance: Rs ${targetLoan.remainingBalance}",
                        fontSize = 12.5.sp,
                        color = Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = addAmountExpr,
                        onValueChange = { addAmountExpr = it },
                        label = { Text("Amount to Add (Rs) - supports math", color = Color(0xFF334155)) },
                        placeholder = { Text("e.g. 500 or =300+200", color = Color(0xFF94A3B8)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (addAmountExpr.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Adding: Rs $evaluatedAdd", fontWeight = FontWeight.Bold, color = VelocityGreen, fontSize = 13.sp)
                                Text("New Total Balance: Rs $newProjectedBalance", fontWeight = FontWeight.Black, color = Color(0xFF0F172A), fontSize = 14.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = additionalNotes,
                        onValueChange = { additionalNotes = it },
                        label = { Text("Notes / Reason (optional)", color = Color(0xFF334155)) },
                        placeholder = { Text("e.g. Extra order, additional credit", color = Color(0xFF94A3B8)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = MathEvaluator.evaluateToInt(addAmountExpr, fallback = 0)
                        if (parsed > 0) {
                            viewModel.recordLoan(
                                personName = targetLoan.personName,
                                direction = targetLoan.direction,
                                amount = parsed,
                                notes = additionalNotes,
                                dueDate = targetLoan.dueDate
                            )
                            loanToAddAmount = null
                            Toast.makeText(context, "Added Rs. $parsed to ${targetLoan.personName}. New Total: Rs. ${targetLoan.remainingBalance + parsed}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Add to Khata", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToAddAmount = null }) {
                    Text("Cancel", color = Color(0xFF475569))
                }
            }
        )
    }
    }
}
