package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PosViewModel
import com.example.ui.theme.*
import com.example.util.MathEvaluator

@Composable
fun ExpensesScreen(
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val expenses by viewModel.expenses.collectAsState()
    val wasteLogs by viewModel.wasteLogs.collectAsState()
    val currentBusinessDate by viewModel.currentBusinessDate.collectAsState()

    // Filter strictly by the active working business date
    val dayExpenses = remember(expenses, currentBusinessDate) {
        expenses.filter { it.businessDate == currentBusinessDate }
    }
    val dayWasteLogs = remember(wasteLogs, currentBusinessDate) {
        wasteLogs.filter { it.businessDate == currentBusinessDate }
    }

    var showExpenseModal by remember { mutableStateOf(false) }
    var showWasteModal by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<com.example.data.model.ExpenseEntity?>(null) }
    var wasteToEdit by remember { mutableStateOf<com.example.data.model.WasteLogEntity?>(null) }

    val totalCashExpense = remember(dayExpenses) {
        dayExpenses.filter { it.paymentMethod == "CASH" }.sumOf { it.amount }
    }
    val totalWasteCost = remember(dayWasteLogs) {
        dayWasteLogs.sumOf { it.estimatedCost }
    }

    // Pulsing animation for the "New Expense" button icon
    val infiniteTransition = rememberInfiniteTransition(label = "expense_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

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
                .background(Color(0xFFF9F9FF))
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
                .padding(bottom = 80.dp)
                .testTag("expenses_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "Expenses & Waste",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "The Big Bite • Cash Register Log",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Active Business Date Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(1.dp, AmberPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Date",
                            tint = AmberPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = currentBusinessDate,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            // ── "New Expense" Button ─────────────────────────────────────────────
            Button(
                onClick = { showExpenseModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("new_expense_btn")
            ) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = "New Expense",
                    modifier = Modifier
                        .size(20.dp)
                        .scale(pulseScale),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Record New Expense",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // ── Operational Health Metric Cards ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cash Outflow Card
                AnimatedKpiCard(
                    label = "Cash Outflow",
                    value = "Rs $totalCashExpense",
                    subtitle = "${dayExpenses.size} Entries · $currentBusinessDate",
                    icon = Icons.Default.Payments,
                    iconTint = AmberPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Inventory Loss Card
                AnimatedKpiCard(
                    label = "Inventory Loss",
                    value = "Rs $totalWasteCost",
                    subtitle = "${dayWasteLogs.size} Waste / Meals",
                    icon = Icons.Default.AutoDelete,
                    iconTint = AlertRed,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Audit Banner ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                SlateInverseSurface,
                                SlateInverseSurface.copy(alpha = 0.85f),
                                AmberPrimary.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AmberPrimary.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Counter Audit • The Big Bite",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Showing expenses for active day: $currentBusinessDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            // ── Today's Expenses Section ─────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = "Expenses",
                            tint = AmberPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Expenses — $currentBusinessDate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFDAE2FD))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Cash Counter",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF131B2E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (dayExpenses.isEmpty()) {
                            ExpenseEmptyState(
                                message = "No expenses recorded for $currentBusinessDate yet.",
                                actionLabel = "Tap 'Record New Expense' above to add one."
                            )
                        } else {
                            dayExpenses.forEachIndexed { index, exp ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(exp.id) { visible = true }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(250, delayMillis = index * 40)) +
                                            slideInVertically(tween(250, delayMillis = index * 40)) { it / 2 }
                                ) {
                                    ExpenseRow(
                                        title = exp.title,
                                        description = exp.description.ifEmpty { "${exp.category} • Paid" },
                                        category = exp.category,
                                        amount = exp.amount,
                                        paymentMethod = exp.paymentMethod,
                                        onEdit = { expenseToEdit = exp },
                                        onDelete = {
                                            viewModel.deleteExpense(exp.id)
                                            Toast.makeText(context, "Expense deleted", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = Color(0xFFE2E8F0)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL CASH EXPENSES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Day Total",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            AnimatedContent(
                                targetState = totalCashExpense,
                                transitionSpec = {
                                    slideInVertically { -it } + fadeIn() togetherWith
                                            slideOutVertically { it } + fadeOut()
                                },
                                label = "total_expense_anim"
                            ) { total ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Rs.", style = MaterialTheme.typography.titleSmall, color = AmberPrimary)
                                    Text(
                                        "$total",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = AmberPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Waste / Personal / Free Section ──────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Waste",
                            tint = AlertRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Waste / Personal / Free",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Kitchen Loss · $currentBusinessDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (dayWasteLogs.isEmpty()) {
                            ExpenseEmptyState(
                                message = "No waste or food loss logged for $currentBusinessDate.",
                                actionLabel = "Use '+ Log Waste / Personal' below to record kitchen loss."
                            )
                        } else {
                            dayWasteLogs.forEachIndexed { index, log ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(log.id) { visible = true }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(250, delayMillis = index * 40)) +
                                            slideInVertically(tween(250, delayMillis = index * 40)) { it / 2 }
                                ) {
                                    WasteRow(
                                        log = log,
                                        onEdit = { wasteToEdit = log },
                                        onDelete = {
                                            viewModel.deleteWaste(log.id)
                                            Toast.makeText(context, "Food loss entry removed", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = Color(0xFFE2E8F0)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "COGS WRITE-OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Estimated Cost",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            AnimatedContent(
                                targetState = totalWasteCost,
                                transitionSpec = {
                                    slideInVertically { -it } + fadeIn() togetherWith
                                            slideOutVertically { it } + fadeOut()
                                },
                                label = "waste_total_anim"
                            ) { total ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Rs.", style = MaterialTheme.typography.titleSmall, color = Color(0xFF64748B))
                                    Text(
                                        "$total",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = Color(0xFF0F172A),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Log Waste Button ─────────────────────────────────────────────────
            PressScaleButton(
                onClick = { showWasteModal = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("log_waste_btn"),
                containerColor = SlateInverseSurface
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Log Waste",
                        tint = VelocityGreenFixed,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "+ LOG WASTE / PERSONAL",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

    // ── Expense Modal ────────────────────────────────────────────────────────────
    if (showExpenseModal) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Food/Meal") }
        var amountExpr by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var method by remember { mutableStateOf("CASH") }

        val calculatedAmt = MathEvaluator.evaluateToInt(amountExpr, fallback = 0)

        AlertDialog(
            onDismissRequest = { showExpenseModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AmberPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddCard, null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Record Cash Outflow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Active day: $currentBusinessDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberPrimary
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Expense Title", color = Color(0xFF334155)) },
                        placeholder = { Text("e.g. Biryani, Eggs, Burger Bread", color = Color(0xFF94A3B8)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountExpr,
                        onValueChange = { amountExpr = it },
                        label = { Text("Amount (Rs.) — supports =8+3 or 9-3", color = Color(0xFF334155)) },
                        placeholder = { Text("e.g. 180 or =150+30", color = Color(0xFF94A3B8)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    AnimatedVisibility(visible = amountExpr.isNotEmpty()) {
                        Text(
                            text = "→ Rs $calculatedAmt",
                            style = MaterialTheme.typography.labelMedium,
                            color = VelocityGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description / Notes", color = Color(0xFF334155)) },
                        placeholder = { Text("Lunch meal for team, supplies...", color = Color(0xFF94A3B8)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { method = "CASH" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (method == "CASH") AmberPrimary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("Cash Drawer", color = if (method == "CASH") Color.White else Color(0xFF0F172A)) }
                        Button(
                            onClick = { method = "JAZZCASH" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (method == "JAZZCASH") JazzCashCrimson else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("JazzCash", color = if (method == "JAZZCASH") Color.White else Color(0xFF0F172A)) }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = MathEvaluator.evaluateToInt(amountExpr, fallback = 0)
                        if (title.isNotBlank() && parsed > 0) {
                            viewModel.recordExpense(title, category, parsed, method, desc)
                            showExpenseModal = false
                            Toast.makeText(context, "Recorded Rs. $parsed for $title", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) { Text("Record Expense", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showExpenseModal = false }) { Text("Cancel") }
            }
        )
    }

    // ── Waste Modal ──────────────────────────────────────────────────────────────
    if (showWasteModal) {
        var itemName by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("STAFF MEAL") }
        var location by remember { mutableStateOf("Counter Staff Break") }
        var estCostExpr by remember { mutableStateOf("160") }

        val calculatedCost = MathEvaluator.evaluateToInt(estCostExpr, fallback = 0)

        AlertDialog(
            onDismissRequest = { showWasteModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AlertRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteSweep, null, tint = AlertRed, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Quick Waste & Meal Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Active day: $currentBusinessDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberPrimary
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Item Description & Qty", color = Color(0xFF334155)) },
                        placeholder = { Text("e.g. 1 Shawarma, 1 Dew Regular", color = Color(0xFF94A3B8)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Reason Category:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF334155), fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("STAFF MEAL", "COURTESY", "BURNT", "SPOILED").forEach { r ->
                            val isSel = reason == r
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) AmberPrimary else Color(0xFFF1F5F9))
                                    .clickable { reason = r }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = r,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSel) Color.White else Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location / Notes", color = Color(0xFF334155)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = estCostExpr,
                        onValueChange = { estCostExpr = it },
                        label = { Text("Est. Cost (Rs.) — supports math e.g. =8+3", color = Color(0xFF334155)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    AnimatedVisibility(visible = estCostExpr.isNotEmpty()) {
                        Text(
                            text = "→ Rs $calculatedCost",
                            style = MaterialTheme.typography.labelMedium,
                            color = VelocityGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = MathEvaluator.evaluateToInt(estCostExpr, fallback = 0)
                        if (itemName.isNotBlank()) {
                            viewModel.recordWaste(itemName, reason, location, cost)
                            showWasteModal = false
                            Toast.makeText(context, "Logged for $itemName (~Rs. $cost)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) { Text("Confirm Log", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showWasteModal = false }) { Text("Cancel") }
            }
        )
    }

    // ── Edit Expense Modal ───────────────────────────────────────────────────────
    expenseToEdit?.let { exp ->
        var editTitle by remember { mutableStateOf(exp.title) }
        var editCategory by remember { mutableStateOf(exp.category) }
        var editAmountExpr by remember { mutableStateOf(exp.amount.toString()) }
        var editDesc by remember { mutableStateOf(exp.description) }
        var editMethod by remember { mutableStateOf(exp.paymentMethod) }

        val calculatedAmt = MathEvaluator.evaluateToInt(editAmountExpr, fallback = exp.amount)

        AlertDialog(
            onDismissRequest = { expenseToEdit = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AmberPrimary)
                    Text("Edit Expense Entry", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle, onValueChange = { editTitle = it },
                        label = { Text("Expense Title", color = Color(0xFF334155)) },
                        colors = highContrastTextFieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAmountExpr, onValueChange = { editAmountExpr = it },
                        label = { Text("Corrected Amount (Rs.)", color = Color(0xFF334155)) },
                        colors = highContrastTextFieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    AnimatedVisibility(visible = editAmountExpr.isNotEmpty()) {
                        Text("New Amount: Rs $calculatedAmt", style = MaterialTheme.typography.labelSmall, color = VelocityGreen, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = editDesc, onValueChange = { editDesc = it },
                        label = { Text("Description / Notes", color = Color(0xFF334155)) },
                        colors = highContrastTextFieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { editMethod = "CASH" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (editMethod == "CASH") AmberPrimary else Color(0xFFF1F5F9)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Cash", color = if (editMethod == "CASH") Color.White else Color(0xFF0F172A)) }
                        Button(
                            onClick = { editMethod = "JAZZCASH" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (editMethod == "JAZZCASH") JazzCashCrimson else Color(0xFFF1F5F9)),
                            modifier = Modifier.weight(1f)
                        ) { Text("JazzCash", color = if (editMethod == "JAZZCASH") Color.White else Color(0xFF0F172A)) }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateExpense(exp.copy(title = editTitle, category = editCategory, amount = MathEvaluator.evaluateToInt(editAmountExpr, fallback = exp.amount), description = editDesc, paymentMethod = editMethod))
                        expenseToEdit = null
                        Toast.makeText(context, "Expense updated successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) { Text("Save Changes", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { expenseToEdit = null }) { Text("Cancel") } }
        )
    }

    // ── Edit Waste Modal ─────────────────────────────────────────────────────────
    wasteToEdit?.let { log ->
        var editItemName by remember { mutableStateOf(log.itemName) }
        var editReason by remember { mutableStateOf(log.reasonCategory) }
        var editLocation by remember { mutableStateOf(log.locationOrNotes) }
        var editEstCostExpr by remember { mutableStateOf(log.estimatedCost.toString()) }

        val calculatedCost = MathEvaluator.evaluateToInt(editEstCostExpr, fallback = log.estimatedCost)

        AlertDialog(
            onDismissRequest = { wasteToEdit = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AmberPrimary)
                    Text("Edit Spoilage & Clarification", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = editItemName, onValueChange = { editItemName = it }, label = { Text("Item Name & Qty", color = Color(0xFF334155)) }, colors = highContrastTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    Text("Spoilage Reason:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF334155), fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("STAFF MEAL", "COURTESY", "BURNT", "SPOILED").forEach { r ->
                            val isSel = editReason == r
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (isSel) AmberPrimary else Color(0xFFF1F5F9)).clickable { editReason = r }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Text(r, style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White else Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    OutlinedTextField(value = editLocation, onValueChange = { editLocation = it }, label = { Text("Clarification / Notes", color = Color(0xFF334155)) }, colors = highContrastTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editEstCostExpr, onValueChange = { editEstCostExpr = it }, label = { Text("Est. Loss Cost (Rs.)", color = Color(0xFF334155)) }, colors = highContrastTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    AnimatedVisibility(visible = editEstCostExpr.isNotEmpty()) {
                        Text("Evaluated Cost: Rs $calculatedCost", style = MaterialTheme.typography.labelSmall, color = VelocityGreen, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateWaste(log.copy(itemName = editItemName, reasonCategory = editReason, locationOrNotes = editLocation, estimatedCost = MathEvaluator.evaluateToInt(editEstCostExpr, fallback = log.estimatedCost)))
                        wasteToEdit = null
                        Toast.makeText(context, "Food loss entry updated", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) { Text("Save Changes", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { wasteToEdit = null }) { Text("Cancel") } }
        )
    }
}

// ── Reusable Sub-Components ───────────────────────────────────────────────────

@Composable
private fun AnimatedKpiCard(
    label: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    AnimatedVisibility(
        visible = appeared,
        enter = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.92f),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                    Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (iconTint == AlertRed) AlertRed else Color(0xFF0F172A)
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (iconTint == AlertRed) Color(0xFF64748B) else VelocityGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    title: String,
    description: String,
    category: String,
    amount: Int,
    paymentMethod: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)
        ) {
            Box(
                modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(AmberPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    category.contains("Gas", ignoreCase = true) -> Icons.Default.PropaneTank
                    category.contains("Chicken", ignoreCase = true) || category.contains("Meat", ignoreCase = true) -> Icons.Default.EggAlt
                    category.contains("Bakery", ignoreCase = true) || category.contains("Bread", ignoreCase = true) -> Icons.Default.BakeryDining
                    else -> Icons.Default.ShoppingBag
                }
                Icon(icon, contentDescription = title, tint = AmberPrimary, modifier = Modifier.size(14.dp))
            }
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(description, fontSize = 10.5.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(shape = RoundedCornerShape(4.dp), color = if (paymentMethod == "CASH") Color(0xFFECFDF5) else Color(0xFFFFE4E6)) {
                Text(
                    text = if (paymentMethod == "CASH") "CASH" else "JAZZ",
                    fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    color = if (paymentMethod == "CASH") Color(0xFF065F46) else Color(0xFFBE123C),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            Text(
                "Rs $amount", fontSize = 13.sp, fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace, color = Color(0xFF0F172A)
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(24.dp).background(Color(0xFFFEF3C7), CircleShape)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AmberPrimary, modifier = Modifier.size(12.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp).background(Color(0xFFFEE2E2), CircleShape)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun WasteRow(
    log: com.example.data.model.WasteLogEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (log.reasonCategory) {
                    "BURNT" -> Icons.Default.LocalFireDepartment
                    "SPOILED" -> Icons.Default.Warning
                    "STAFF MEAL" -> Icons.Default.Badge
                    else -> Icons.Default.VolunteerActivism
                }
                Icon(icon, contentDescription = log.reasonCategory, tint = AlertRed, modifier = Modifier.size(16.dp))
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(log.itemName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(
                            when (log.reasonCategory) {
                                "BURNT" -> AlertRed; "SPOILED" -> AlertRedContainer
                                "STAFF MEAL" -> MaterialTheme.colorScheme.secondary
                                else -> VelocityGreenContainer
                            }
                        ).padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(log.reasonCategory, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.White)
                    }
                }
                Text(
                    text = if (log.locationOrNotes.isNotBlank()) "Clarification: ${log.locationOrNotes}" else "Clarification: Kitchen Spoilage / Loss",
                    style = MaterialTheme.typography.bodySmall, fontSize = 11.sp,
                    color = Color(0xFF334155), fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("~Rs ${log.estimatedCost}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            IconButton(onClick = onEdit, modifier = Modifier.size(26.dp).background(Color(0xFFFEF3C7), CircleShape)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AmberPrimary, modifier = Modifier.size(13.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(26.dp).background(Color(0xFFFEE2E2), CircleShape)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
private fun ExpenseEmptyState(message: String, actionLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "empty_state")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
            label = "empty_alpha"
        )
        Icon(
            Icons.Default.Inbox,
            contentDescription = "Empty",
            tint = Color(0xFFCBD5E1).copy(alpha = alpha),
            modifier = Modifier.size(48.dp)
        )
        Text(message, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
        Text(actionLabel, style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
    }
}

@Composable
private fun PressScaleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AmberPrimary,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_scale"
    )
    Button(
        onClick = onClick,
        modifier = modifier.scale(scale),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        interactionSource = interactionSource,
        content = content
    )
}
