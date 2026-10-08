package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ProductEntity
import com.example.ui.CartItem
import com.example.ui.PosViewModel
import com.example.ui.theme.*
import kotlin.math.roundToInt
import java.io.File
import java.io.FileOutputStream

@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateToCheckout: () -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val subtotal by viewModel.cartSubtotal.collectAsState()
    val discountAmount by viewModel.cartDiscountAmount.collectAsState()
    val discountPercentage by viewModel.discountPercentage.collectAsState()
    val total by viewModel.cartTotal.collectAsState()
    val orderTabs by viewModel.orderTabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()

    var showDiscountDialog by remember { mutableStateOf(false) }
    var showCancelOrderDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddNewItemDialog by remember { mutableStateOf(false) }

    var activeImagePickerCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val fileName = "product_img_${System.currentTimeMillis()}.jpg"
                val destFile = File(context.filesDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val localPath = destFile.absolutePath
                activeImagePickerCallback?.invoke(localPath)
                Toast.makeText(context, "Photo loaded from phone gallery!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val categories = listOf("All", "Burgers", "Shawarma", "Drinks", "Fries", "Add-ons")

    val filteredProducts = remember(products, selectedCategory) {
        val sellingItemsOnly = products.filter {
            !it.category.contains("Raw", ignoreCase = true) &&
            !it.category.contains("Bakery", ignoreCase = true) &&
            it.category != "Raw Prep & Bakery"
        }
        if (selectedCategory == "All") sellingItemsOnly else sellingItemsOnly.filter {
            it.category.equals(selectedCategory, ignoreCase = true)
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
                .widthIn(max = 1100.dp)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
                .testTag("pos_screen")
        ) {
        // Top Order Bar & Quick Status
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "New Sale",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Order #$activeTabId",
                                color = AmberPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "The Big Bite • Counter 01 • Ledger Seq: ${viewModel.generateUniqueOrderNumber()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF565E74)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val currentBusinessDate by viewModel.currentBusinessDate.collectAsState()
                    var showDateSwitcher by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF3C7))
                            .clickable { showDateSwitcher = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Date", tint = AmberPrimary, modifier = Modifier.size(12.dp))
                            Text(
                                text = "$currentBusinessDate ▾",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showDateSwitcher) {
                        AlertDialog(
                            onDismissRequest = { showDateSwitcher = false },
                            title = { Text("Select Working Date", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Current date: $currentBusinessDate", fontSize = 12.sp, color = Color.Gray)
                                    Button(
                                        onClick = {
                                            viewModel.setBusinessDate("2026-10-01")
                                            showDateSwitcher = false
                                            Toast.makeText(context, "Active: 2026-10-01 (Day 1)", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (currentBusinessDate == "2026-10-01") AmberPrimary else Color(0xFFF1F5F9)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("📅 2026-10-01 (Day 1 - Past)", color = if (currentBusinessDate == "2026-10-01") Color.White else Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.setBusinessDate("2026-10-02")
                                            showDateSwitcher = false
                                            Toast.makeText(context, "Active: 2026-10-02 (Day 2 / Today)", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (currentBusinessDate == "2026-10-02") VelocityGreen else Color(0xFFF1F5F9)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("📅 2026-10-02 (Day 2 / Today)", color = if (currentBusinessDate == "2026-10-02") Color.White else Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                TextButton(onClick = { showDateSwitcher = false }) { Text("Close") }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE7EEFF))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(VelocityGreen)
                        )
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF111C2D),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Multi-Tab Order Queue / Hold & Park Orders Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            orderTabs.forEach { tab ->
                val isActive = tab.id == activeTabId
                val itemCount = if (isActive) cartItems.sumOf { it.quantity } else tab.items.sumOf { it.quantity }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) AmberPrimary else Color.White)
                        .border(1.dp, if (isActive) AmberPrimary else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                        .clickable { viewModel.switchOrderTab(tab.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) Color.White else Color(0xFF0F172A)
                        )
                        if (itemCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isActive) Color.White.copy(alpha = 0.25f) else Color(0xFFFEF3C7))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$itemCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White else AmberPrimary
                                )
                            }
                        }
                        if (orderTabs.size > 1) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close Order",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { viewModel.closeOrderTab(tab.id) },
                                tint = if (isActive) Color.White else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // New Order Tab Button
            OutlinedButton(
                onClick = { viewModel.addNewOrderTab() },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Order", modifier = Modifier.size(14.dp), tint = Color(0xFF0F172A))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Hold & New Order", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            }
        }

        // Category Pills Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Button(
                    onClick = { viewModel.selectCategory(cat) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) AmberPrimary else Color.White,
                        contentColor = if (isSelected) Color.White else Color(0xFF1E293B)
                    ),
                    border = if (!isSelected) ButtonDefaults.outlinedButtonBorder else null,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("category_pill_$cat")
                ) {
                    val icon = when (cat) {
                        "All" -> Icons.Default.Apps
                        "Burgers" -> Icons.Default.LunchDining
                        "Shawarma" -> Icons.Default.DinnerDining
                        "Drinks" -> Icons.Default.LocalDrink
                        "Fries" -> Icons.Default.Fastfood
                        else -> Icons.Default.AddCircle
                    }
                    Icon(imageVector = icon, contentDescription = cat, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = cat, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Fast-Tap Menu Grid (3 Columns) with High-Contrast Readable Titles
        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MENU ITEMS (${filteredProducts.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF565E74),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                // Quick "+ Add Menu Item" button right on the Sale screen
                Button(
                    onClick = { showAddNewItemDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(14.dp), tint = AmberPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Add Item", style = MaterialTheme.typography.labelSmall, color = AmberPrimary)
                }
            }

            // Responsive Grid Layout: Adapts column count to phone vs tablet screen width
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val numCols = when {
                    maxWidth >= 900.dp -> 5
                    maxWidth >= 600.dp -> 4
                    maxWidth >= 380.dp -> 3
                    else -> 2
                }
                val chunkedProducts = filteredProducts.chunked(numCols)
                Column(modifier = Modifier.fillMaxWidth()) {
                    chunkedProducts.forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { product ->
                                val inCart = cartItems.find { it.product.id == product.id }
                                MenuTileHighContrast(
                                    product = product,
                                    cartQuantity = inCart?.quantity ?: 0,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("menu_item_${product.id}"),
                                    onClick = { viewModel.addToCart(product) },
                                    onEdit = { productToEdit = product }
                                )
                            }
                            repeat(numCols - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Kitchen Queue / Prep Station Context Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(VelocityGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Kitchen Prep",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Kitchen Queue: Low Wait",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111C2D)
                        )
                        Text(
                            text = "Est. Prep ~4 mins per order",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF565E74)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFDAE2FD))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "KOT #12",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF131B2E)
                    )
                }
            }
        }

        // Cart / Current Order Bottom Sheet Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .testTag("cart_card"),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Cart Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "Order Receipt",
                            tint = AmberPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "YOUR ORDER",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmberPrimaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${cartItems.sumOf { it.quantity }} items",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Cancel / Discard Active Order button
                        OutlinedButton(
                            onClick = { showCancelOrderDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancel Order", modifier = Modifier.size(13.dp), tint = AlertRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel Order", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                        }

                        IconButton(
                            onClick = { viewModel.clearCart() },
                            modifier = Modifier
                                .size(30.dp)
                                .background(AlertRedContainer, RoundedCornerShape(8.dp))
                                .testTag("clear_cart_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear Items",
                                tint = AlertRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Line Items List
                if (cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cart is empty. Tap any menu item to start order.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cartItems.forEach { item ->
                            CartLineItemHighContrast(
                                item = item,
                                onIncrement = { viewModel.updateCartQuantity(item.product.id, item.quantity + 1) },
                                onDecrement = { viewModel.updateCartQuantity(item.product.id, item.quantity - 1) }
                            )
                        }
                    }
                }

                // Financial Calculation Block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Subtotal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Rs $subtotal",
                            style = CurrencySm,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Discount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VelocityGreenContainer)
                                    .clickable { showDiscountDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .testTag("open_discount_dialog_btn")
                            ) {
                                Text(
                                    text = if (discountPercentage > 0.0) {
                                        val formatted = if (discountPercentage % 1.0 == 0.0) "${discountPercentage.toInt()}%" else "$discountPercentage%"
                                        "$formatted ▾"
                                    } else "0% ▾",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Rs $discountAmount",
                            style = CurrencySm,
                            color = VelocityGreen
                        )
                    }
                }

                // Total & Checkout Action Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "PAYABLE TOTAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tax inclusive (PRA 0%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "TOTAL:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AmberPrimary
                            )
                            Text(
                                text = "Rs $total",
                                style = CurrencyLg,
                                color = AmberPrimary
                            )
                        }
                    }

                    // Master Checkout CTA Button
                    Button(
                        onClick = onNavigateToCheckout,
                        enabled = cartItems.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("checkout_cta_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = "Payments",
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "CHECKOUT",
                                    style = MaterialTheme.typography.labelLarge,
                                    letterSpacing = 1.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Rs $total",
                                    style = CurrencyMd,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Proceed",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Edit Item Price / Card Details or Delete Card (Requested by user)
    if (productToEdit != null) {
        val prod = productToEdit!!
        var editName by remember { mutableStateOf(prod.name) }
        var editPrice by remember { mutableStateOf(prod.price.toString()) }
        var editCategory by remember { mutableStateOf(prod.category) }
        var editSku by remember { mutableStateOf(prod.sku) }
        var editImageUrl by remember { mutableStateOf(prod.imageUrl) }

        val imagePresets = listOf(
            "🍔 Burger" to "food_zinger",
            "🌯 Shawarma" to "food_shawarma",
            "🍟 Fries" to "food_fries",
            "🥤 Drink" to "food_drink",
            "🥪 Stack" to "img_slider_stack"
        )

        AlertDialog(
            onDismissRequest = { productToEdit = null },
            title = {
                Text(
                    text = "Edit Item & Picture",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Current Picture Preview & Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductFoodThumbnail(
                            imageUrl = editImageUrl,
                            productName = editName,
                            category = editCategory,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Select Item Picture:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("Pick from phone gallery or choose preset", fontSize = 11.sp, color = Color(0xFF64748B))

                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    activeImagePickerCallback = { path ->
                                        editImageUrl = path
                                    }
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose from Phone Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                            }
                        }
                    }

                    // Picture preset chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        imagePresets.forEach { (label, imgKey) ->
                            val isSelected = editImageUrl == imgKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { editImageUrl = imgKey },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editImageUrl,
                        onValueChange = { editImageUrl = it },
                        label = { Text("Image Key or Web URL", color = Color(0xFF334155)) },
                        placeholder = { Text("food_zinger, food_shawarma or http://...", color = Color(0xFF94A3B8)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Product Name", color = Color(0xFF334155)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { editPrice = it },
                        label = { Text("Menu Price (Rs)", color = Color(0xFF334155)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Category", color = Color(0xFF334155)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            viewModel.deleteProduct(prod.id)
                            productToEdit = null
                            Toast.makeText(context, "${prod.name} removed from menu", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRedContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete / Remove This Item", color = AlertRed, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedPrice = editPrice.toIntOrNull() ?: prod.price
                        if (editName.isNotBlank() && parsedPrice >= 0) {
                            viewModel.updateProductDetails(prod.id, editName, parsedPrice, editCategory, editSku, editImageUrl)
                            productToEdit = null
                            Toast.makeText(context, "Price updated to Rs $parsedPrice for $editName", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Add New Menu Item Directly from UI
    if (showAddNewItemDialog) {
        var newName by remember { mutableStateOf("") }
        var newPrice by remember { mutableStateOf("200") }
        var newCategory by remember { mutableStateOf("Burgers") }
        var newCost by remember { mutableStateOf("100") }
        var newSku by remember { mutableStateOf("NEW-01") }
        var newCode by remember { mutableStateOf("#99") }
        var newImageUrl by remember { mutableStateOf("food_zinger") }

        val imagePresets = listOf(
            "🍔 Burger" to "food_zinger",
            "🌯 Shawarma" to "food_shawarma",
            "🍟 Fries" to "food_fries",
            "🥤 Drink" to "food_drink",
            "🥪 Stack" to "img_slider_stack"
        )

        AlertDialog(
            onDismissRequest = { showAddNewItemDialog = false },
            title = {
                Text(
                    text = "Add New Menu Item",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Picture Preview & Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductFoodThumbnail(
                            imageUrl = newImageUrl,
                            productName = newName.ifBlank { "Item" },
                            category = newCategory,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Product Photo:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("Pick from phone gallery or choose preset", fontSize = 11.sp, color = Color(0xFF64748B))

                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    activeImagePickerCallback = { path ->
                                        newImageUrl = path
                                    }
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose from Phone Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                            }
                        }
                    }

                    // Picture Presets row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        imagePresets.forEach { (label, imgKey) ->
                            val isSelected = newImageUrl == imgKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { newImageUrl = imgKey },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newImageUrl,
                        onValueChange = { newImageUrl = it },
                        label = { Text("Image Key or Web URL", color = Color(0xFF334155)) },
                        placeholder = { Text("food_zinger, food_shawarma or http://...", color = Color(0xFF94A3B8)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Item Name", color = Color(0xFF334155)) },
                        placeholder = { Text("e.g. Crispy Burger", color = Color(0xFF94A3B8)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newPrice,
                            onValueChange = { newPrice = it },
                            label = { Text("Sale Price (Rs)", color = Color(0xFF334155)) },
                            colors = highContrastTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newCost,
                            onValueChange = { newCost = it },
                            label = { Text("Cost (Rs)", color = Color(0xFF334155)) },
                            colors = highContrastTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Category", color = Color(0xFF334155)) },
                        colors = highContrastTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newCode,
                            onValueChange = { newCode = it },
                            label = { Text("Key Code (e.g. #23)", color = Color(0xFF334155)) },
                            colors = highContrastTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newSku,
                            onValueChange = { newSku = it },
                            label = { Text("SKU", color = Color(0xFF334155)) },
                            colors = highContrastTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceNum = newPrice.toIntOrNull() ?: 0
                        val costNum = newCost.toIntOrNull() ?: 0
                        if (newName.isNotBlank() && priceNum > 0) {
                            viewModel.addNewMenuItem(newName, newCategory, priceNum, costNum, newSku, newCode, newImageUrl)
                            showAddNewItemDialog = false
                            Toast.makeText(context, "$newName added to menu", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Add to Menu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNewItemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Cancel Active Order Confirmation
    if (showCancelOrderDialog) {
        AlertDialog(
            onDismissRequest = { showCancelOrderDialog = false },
            title = {
                Text("Cancel & Discard Order #$activeTabId?", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            },
            text = {
                Text(
                    text = "This will remove Order #$activeTabId from your active held orders. Any other active orders will be renumbered consecutively from 1 onwards.\n\nNote: Completed sales in your ledger remain unaffected.",
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelActiveOrder()
                        showCancelOrderDialog = false
                        Toast.makeText(context, "Order #$activeTabId cancelled", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Yes, Discard Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelOrderDialog = false }) {
                    Text("Keep Order")
                }
            }
        )
    }

    // Daily Specials & Loyalty Discount Modal Dialog
    if (showDiscountDialog) {
        var customPctInput by remember {
            mutableStateOf(if (discountPercentage > 0.0) discountPercentage.toString() else "")
        }
        val evaluatedPct = customPctInput.toDoubleOrNull() ?: 0.0
        val previewDiscount = if (evaluatedPct > 0.0) {
            Math.round((subtotal * evaluatedPct) / 100.0).toInt().coerceIn(0, subtotal)
        } else 0
        val previewTotal = kotlin.math.max(0, subtotal - previewDiscount)

        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = {
                Text(
                    text = "Apply Custom or Preset Discount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enter any custom discount percentage (e.g. 2%, 7%, 3.5%) or pick a preset:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )

                    // Preset Percentage Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.0 to "0%", 2.0 to "2%", 5.0 to "5%", 7.0 to "7%", 10.0 to "10%").forEach { (pct, label) ->
                            Button(
                                onClick = {
                                    viewModel.applyDiscount(pct)
                                    showDiscountDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (discountPercentage == pct) AmberPrimary else Color(0xFFF1F5F9),
                                    contentColor = if (discountPercentage == pct) Color.White else Color(0xFF0F172A)
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Custom Percentage Input
                    OutlinedTextField(
                        value = customPctInput,
                        onValueChange = { customPctInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Custom Discount % (e.g. 2, 3.5, 7)", color = Color(0xFF334155)) },
                        placeholder = { Text("e.g. 2, 7, 3.5", color = Color(0xFF94A3B8)) },
                        colors = highContrastTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_discount_pct_input")
                    )

                    // Formula calculation preview box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "CALCULATION PREVIEW:",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = "• Discount = Subtotal (Rs $subtotal) × $evaluatedPct% / 100 = Rs $previewDiscount",
                                fontSize = 11.5.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "• Final Total = Rs $subtotal - Rs $previewDiscount = Rs $previewTotal",
                                fontSize = 12.sp,
                                color = Color(0xFF166534),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Apply Custom % Button
                    Button(
                        onClick = {
                            val parsed = customPctInput.toDoubleOrNull()
                            if (parsed != null && parsed >= 0.0) {
                                viewModel.applyDiscount(parsed)
                                showDiscountDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VelocityGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("apply_custom_discount_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Apply", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Apply ${if (evaluatedPct > 0) "$evaluatedPct%" else "Custom"} Discount",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Fixed Rupee Quick Discounts
                    Text("Or Fixed Rupee Reward:", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.applyFixedDiscount(50)
                                showDiscountDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-Rs 50", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                viewModel.applyFixedDiscount(100)
                                showDiscountDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-Rs 100", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDiscountDialog = false }) {
                    Text("Close", color = Color(0xFF64748B))
                }
            }
        )
    }
}
}

/**
 * High-Contrast Menu Tile with Food Photo & Polish:
 * - Real photo display for burgers, shawarma, fries, and drinks
 * - Crisp overlay badges for in-cart quantity & code
 * - Tabular bold Amber price
 * - Quick edit pencil icon to modify price or image preset
 */
@Composable
fun MenuTileHighContrast(
    product: ProductEntity,
    cartQuantity: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val isSelected = cartQuantity > 0

    Card(
        modifier = modifier
            .height(138.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFEF3C7).copy(alpha = 0.6f) else Color.White
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, AmberPrimary) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Image Thumbnail with Overlay badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                ProductFoodThumbnail(
                    imageUrl = product.imageUrl,
                    productName = product.name,
                    category = product.category,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AmberPrimary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "✓ $cartQuantity",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = product.code,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f))
                                .clickable { onEdit() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Price & Photo",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (product.currentStock > product.lowStockThreshold) VelocityGreen else AlertRed)
                        )
                    }
                }
            }

            // Bottom Title & Price area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = product.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rs ${product.price}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = AmberPrimary
                    )
                    Text(
                        text = if (product.currentStock <= product.lowStockThreshold) "Low" else "In Stock",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (product.currentStock <= product.lowStockThreshold) AlertRed else VelocityGreen
                    )
                }
            }
        }
    }
}

@Composable
fun ProductFoodThumbnail(
    imageUrl: String,
    productName: String,
    category: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drawableResId = remember(imageUrl, productName, category) {
        val directRes = if (imageUrl.isNotBlank()) {
            context.resources.getIdentifier(imageUrl, "drawable", context.packageName)
        } else 0
        if (directRes != 0) {
            directRes
        } else {
            val lower = productName.lowercase()
            val catLower = category.lowercase()
            when {
                lower.contains("zinger") || lower.contains("burger") || catLower == "burgers" -> R.drawable.food_zinger
                lower.contains("shawarma") || lower.contains("shawr") || catLower == "shawarma" -> R.drawable.food_shawarma
                lower.contains("fries") || catLower == "fries" -> R.drawable.food_fries
                lower.contains("dew") || lower.contains("coke") || lower.contains("pepsi") || lower.contains("drink") || catLower == "drinks" -> R.drawable.food_drink
                lower.contains("bread") || catLower.contains("bakery") -> R.drawable.img_slider_stack
                else -> 0
            }
        }
    }

    val isLocalFile = imageUrl.startsWith("/") || imageUrl.startsWith("file://") || imageUrl.startsWith("content://")
    val localFileExists = if (imageUrl.startsWith("/") && !imageUrl.startsWith("file://")) File(imageUrl).exists() else isLocalFile

    if ((imageUrl.startsWith("http://") || imageUrl.startsWith("https://") || (isLocalFile && localFileExists)) && imageUrl.isNotBlank()) {
        val imageModel: Any = if (imageUrl.startsWith("/") && !imageUrl.startsWith("file://")) {
            File(imageUrl)
        } else {
            imageUrl
        }
        AsyncImage(
            model = imageModel,
            contentDescription = productName,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else if (drawableResId != 0) {
        Image(
            painter = painterResource(id = drawableResId),
            contentDescription = productName,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            val icon = when {
                category.equals("Drinks", ignoreCase = true) -> Icons.Default.LocalDrink
                category.equals("Burgers", ignoreCase = true) -> Icons.Default.LunchDining
                category.equals("Shawarma", ignoreCase = true) -> Icons.Default.DinnerDining
                category.equals("Fries", ignoreCase = true) -> Icons.Default.Fastfood
                else -> Icons.Default.Restaurant
            }
            Icon(icon, contentDescription = productName, tint = AmberPrimary, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun CartLineItemHighContrast(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AmberPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.quantity}x",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AmberPrimary
                )
            }

            Column {
                Text(
                    text = item.product.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                if (item.notes.isNotEmpty()) {
                    Text(
                        text = item.notes,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier.size(26.dp).background(Color(0xFFE2E8F0), CircleShape)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp), tint = Color.Black)
            }
            Text(
                text = "Rs ${item.lineTotal}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF0F172A)
            )
            IconButton(
                onClick = onIncrement,
                modifier = Modifier.size(26.dp).background(Color(0xFFE2E8F0), CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp), tint = Color.Black)
            }
        }
    }
}
