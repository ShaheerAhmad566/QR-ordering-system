package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LoanEntity
import com.example.domain.SaleCalculator
import com.example.ui.PosViewModel
import com.example.ui.theme.*

@Composable
fun TenderCheckoutScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val total by viewModel.cartTotal.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val checkoutMethod by viewModel.checkoutMethod.collectAsState()
    val cashReceived by viewModel.cashReceived.collectAsState()
    val loans by viewModel.loans.collectAsState()

    var isProcessing by remember { mutableStateOf(false) }

    // Khata specific state
    var khataQuery by remember { mutableStateOf("") }
    var selectedKhataCustomer by remember { mutableStateOf<LoanEntity?>(null) }
    var isNewKhataCustomer by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }

    val nextKhataNumber = remember(loans) {
        val maxId = loans.maxOfOrNull { it.id } ?: 0
        100 + maxId + 1
    }
    val nextKhataCode = "KH-$nextKhataNumber"

    // Filter existing khata accounts (Only GIVEN - credit customers)
    val existingKhataCustomers = remember(loans) {
        loans.filter { it.direction == "GIVEN" }
    }

    val filteredKhataCustomers = remember(existingKhataCustomers, khataQuery) {
        if (khataQuery.isBlank()) existingKhataCustomers else {
            val q = khataQuery.trim().lowercase()
            existingKhataCustomers.filter {
                it.khataCode.lowercase().contains(q) ||
                it.id.toString() == q ||
                it.personName.lowercase().contains(q)
            }
        }
    }

    // Auto-select if query exactly matches an ID or code
    LaunchedEffect(khataQuery) {
        val q = khataQuery.trim().lowercase()
        val exactMatch = existingKhataCustomers.find {
            it.khataCode.lowercase() == q || it.id.toString() == q
        }
        if (exactMatch != null) {
            selectedKhataCustomer = exactMatch
            isNewKhataCustomer = false
        }
    }

    BackHandler {
        onNavigateBack()
    }

    val isCash = checkoutMethod == "CASH"
    val isJazzCash = checkoutMethod == "JAZZCASH"
    val isKhata = checkoutMethod == "KHATA"

    val diff = cashReceived - total
    val isUnderpaid = isCash && diff < 0
    val changeAmount = if (isCash) SaleCalculator.calculateChange(total, cashReceived) else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HighContrastScreenBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
                .padding(bottom = 32.dp)
                .testTag("tender_checkout_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // Order Token & Counter Metatags
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AmberPrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "NEW SALE CHECKOUT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary
                    )
                }
            }

            Text(
                text = "${cartItems.sumOf { it.quantity }} Items in Order",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
        }

        // High-Contrast Total Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("payable_banner"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL PAYABLE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PKR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Rs.",
                        style = MaterialTheme.typography.titleLarge,
                        color = AmberPrimary
                    )
                    Text(
                        text = "$total",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }

        // 3 Payment Method Selector Tabs (CASH, JAZZCASH, KHATA)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Cash Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCash) Color.White else Color.Transparent)
                    .clickable { viewModel.setCheckoutMethod("CASH") }
                    .testTag("method_cash_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = "Cash",
                        tint = if (isCash) VelocityGreen else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "CASH",
                        fontSize = 12.sp,
                        fontWeight = if (isCash) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCash) Color(0xFF0F172A) else Color(0xFF64748B)
                    )
                }
            }

            // JazzCash Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isJazzCash) Color(0xFFBE123C) else Color.Transparent)
                    .clickable { viewModel.setCheckoutMethod("JAZZCASH") }
                    .testTag("method_jazzcash_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isJazzCash) Color.White else Color(0xFFBE123C))
                    )
                    Text(
                        text = "JAZZCASH",
                        fontSize = 12.sp,
                        fontWeight = if (isJazzCash) FontWeight.Bold else FontWeight.Medium,
                        color = if (isJazzCash) Color.White else Color(0xFF64748B)
                    )
                }
            }

            // Khata / Ledger Tab
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isKhata) AmberPrimary else Color.Transparent)
                    .clickable { viewModel.setCheckoutMethod("KHATA") }
                    .testTag("method_khata_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = "Khata",
                        tint = if (isKhata) Color.White else AmberPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "KHATA (UDHAR)",
                        fontSize = 11.5.sp,
                        fontWeight = if (isKhata) FontWeight.Bold else FontWeight.Medium,
                        color = if (isKhata) Color.White else Color(0xFF0F172A)
                    )
                }
            }
        }

        // ================= KHATA / LEDGER INTERFACE =================
        if (isKhata) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customer Khata Selection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        // Toggle between Existing & New
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!isNewKhataCustomer) AmberPrimary else Color.Transparent)
                                    .clickable { isNewKhataCustomer = false }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Existing ID",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isNewKhataCustomer) Color.White else Color(0xFF64748B)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isNewKhataCustomer) AmberPrimary else Color.Transparent)
                                    .clickable {
                                        isNewKhataCustomer = true
                                        selectedKhataCustomer = null
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "+ New Khata",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNewKhataCustomer) Color.White else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    if (!isNewKhataCustomer) {
                        // Search by ID or Name
                        OutlinedTextField(
                            value = khataQuery,
                            onValueChange = { khataQuery = it },
                            label = { Text("Type Customer ID (e.g. 101 or KH-101) or Name", color = Color(0xFF334155)) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = AmberPrimary)
                            },
                            trailingIcon = {
                                if (khataQuery.isNotEmpty()) {
                                    IconButton(onClick = { khataQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF0F172A))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = AmberPrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                cursorColor = AmberPrimary
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick Chips for fast selection
                        Text(
                            text = "Existing Customers (Tap to Select):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredKhataCustomers.forEach { customer ->
                                val isSelected = selectedKhataCustomer?.id == customer.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AmberPrimary else Color(0xFFF1F5F9))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) AmberPrimary else Color(0xFFCBD5E1),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedKhataCustomer = customer
                                            khataQuery = customer.khataCode
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = customer.khataCode,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isSelected) Color.White else AmberPrimary
                                            )
                                            Text(
                                                text = customer.personName,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color(0xFF0F172A)
                                            )
                                        }
                                        Text(
                                            text = "Bal: Rs ${customer.remainingBalance}",
                                            fontSize = 10.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }

                        // Selected Customer Ledger Summary Box
                        selectedKhataCustomer?.let { customer ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Account: ${customer.personName} (${customer.khataCode})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF92400E)
                                        )
                                        Text(
                                            text = "VERIFIED",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = VelocityGreen
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Current Outstanding Khata:", fontSize = 11.5.sp, color = Color(0xFF78350F))
                                        Text("Rs ${customer.remainingBalance}", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF92400E))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("+ This Order Total:", fontSize = 11.5.sp, color = Color(0xFF78350F))
                                        Text("Rs $total", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF92400E))
                                    }
                                    HorizontalDivider(color = Color(0xFFFDE68A), modifier = Modifier.padding(vertical = 2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("New Total Khata Balance:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF92400E))
                                        Text(
                                            text = "Rs ${customer.remainingBalance + total}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // NEW KHATA CUSTOMER FORM
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Badge, contentDescription = "ID", tint = AmberPrimary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(
                                        text = "AUTO-ASSIGNED KHATA ID: #$nextKhataCode",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "This unique ID is assigned permanently to this customer.",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF78350F)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = newCustomerName,
                            onValueChange = { newCustomerName = it },
                            label = { Text("Customer Full Name *", color = Color(0xFF334155)) },
                            placeholder = { Text("e.g. Asif Mobile Shop", color = Color(0xFF94A3B8)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = AmberPrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                cursorColor = AmberPrimary
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newCustomerPhone,
                            onValueChange = { newCustomerPhone = it },
                            label = { Text("Mobile Number (Optional)", color = Color(0xFF334155)) },
                            placeholder = { Text("e.g. 0300-1234567", color = Color(0xFF94A3B8)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = AmberPrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                cursorColor = AmberPrimary
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // ================= CASH KEYPAD & PRESETS =================
        if (isCash) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cash Received",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Rs.", style = MaterialTheme.typography.titleMedium, color = Color(0xFF64748B))
                            Text(
                                text = "$cashReceived",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.testTag("cash_received_display")
                            )
                        }
                    }

                    if (cashReceived > 0) {
                        IconButton(
                            onClick = { viewModel.clearCashReceived() },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Quick Preset Buttons
            val presets = listOf(
                "Exact" to total,
                "500" to 500,
                "1000" to 1000,
                "5000" to 5000
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (label, amt) ->
                    PresetButton(
                        label = label,
                        amount = amt,
                        isHighlighted = cashReceived == amt,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setCashPreset(amt) }
                    )
                }
            }

            // Numeric Touch Keypad
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("0", "00", "+500")
                )

                rows.forEach { rowDigits ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowDigits.forEach { digit ->
                            KeypadButton(
                                text = digit,
                                isAccent = digit == "+500",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.appendCashDigit(digit) }
                            )
                        }
                    }
                }
            }

            // Change Due or Underpaid Banner
            val bannerBg = if (isUnderpaid) Color(0xFFFEF2F2) else Color(0xFFF0FDF4)
            val bannerBorder = if (isUnderpaid) AlertRed else VelocityGreen

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("change_due_banner"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = bannerBg),
                border = BorderStroke(1.dp, bannerBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUnderpaid) "SHORTFALL (UNDERPAID):" else "CHANGE TO RETURN:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnderpaid) AlertRed else VelocityGreen
                    )
                    Text(
                        text = "Rs. ${if (isUnderpaid) -diff else changeAmount}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isUnderpaid) AlertRed else VelocityGreen
                    )
                }
            }
        }

        // ================= JAZZCASH BANNER =================
        if (isJazzCash) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "QR", tint = Color(0xFFBE123C))
                        Text("JazzCash Till / QR Code", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        text = "Ask customer to scan the counter JazzCash QR code or send to Till # 0300-XXXXXXX. Confirm transaction SMS before releasing order.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        // Master Confirm Action Button
        val canConfirm = when {
            isCash -> !isUnderpaid
            isJazzCash -> true
            isKhata -> (selectedKhataCustomer != null) || (isNewKhataCustomer && newCustomerName.isNotBlank())
            else -> false
        }

        // Helper instruction banner for Khata if customer not yet selected
        if (isKhata && !canConfirm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Notice", tint = Color(0xFFB45309), modifier = Modifier.size(20.dp))
                    Text(
                        text = "Please tap an Existing Customer chip above, or type Customer Name to complete this Khata sale.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }

        Button(
            onClick = {
                if (!canConfirm || isProcessing) return@Button
                isProcessing = true
                if (isKhata) {
                    viewModel.confirmKhataCheckout(
                        existingCustomerId = selectedKhataCustomer?.id,
                        newCustomerName = newCustomerName,
                        newCustomerPhone = newCustomerPhone
                    ) { message ->
                        isProcessing = false
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                } else {
                    viewModel.confirmOrderCheckout { orderNum ->
                        isProcessing = false
                        Toast.makeText(context, "Order $orderNum Paid & Completed via $checkoutMethod!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = canConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("confirm_payment_btn"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isKhata) Color(0xFFD97706) else AmberPrimary,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE2E8F0),
                disabledContentColor = Color(0xFF334155)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RECORDING ORDER...", fontWeight = FontWeight.Bold)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isKhata) Icons.Default.AccountBalance else Icons.Default.Verified,
                        contentDescription = "Confirm",
                        tint = if (canConfirm) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = when {
                            isKhata && canConfirm -> "CHARGE TO KHATA: ${selectedKhataCustomer?.personName ?: newCustomerName} (Rs. $total)"
                            isKhata && !canConfirm -> "SELECT CUSTOMER FOR KHATA (Rs. $total)"
                            isJazzCash -> "CONFIRM JAZZCASH (Rs. $total)"
                            isCash && !canConfirm -> "ENTER CASH RECEIVED (Min Rs. $total)"
                            else -> "CONFIRM CASH SALE (Rs. $total)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canConfirm) Color.White else Color(0xFF334155)
                    )
                }
            }
        }

        TextButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Cancel & Return to Cart",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        }
    }
    }
}

@Composable
fun PresetButton(
    label: String,
    amount: Int,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(54.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) AmberPrimary.copy(alpha = 0.2f) else Color.White
        ),
        border = if (isHighlighted) BorderStroke(1.5.dp, AmberPrimary) else CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = if (isHighlighted) AmberPrimary else Color(0xFF64748B)
            )
            Text(
                text = "$amount",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isHighlighted) AmberPrimary else Color(0xFF0F172A)
            )
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    isAccent: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() }
            .testTag("keypad_$text"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAccent) AmberPrimary.copy(alpha = 0.15f) else Color.White
        ),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = if (isAccent) 13.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAccent) AmberPrimary else Color(0xFF0F172A)
            )
        }
    }
}
