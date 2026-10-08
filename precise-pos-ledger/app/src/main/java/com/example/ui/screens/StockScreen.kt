package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.ui.PosViewModel
import com.example.ui.theme.*
import com.example.util.MathEvaluator
import java.io.File
import java.io.FileOutputStream

@Composable
fun StockScreen(
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.stockSearchQuery.collectAsState()

    var selectedFilterCategory by remember { mutableStateOf("CORE") } // CORE, ALL, RAW, DRINKS, BURGERS
    var showAdjustStockModal by remember { mutableStateOf(false) }
    var selectedProductForAdjust by remember { mutableStateOf<ProductEntity?>(null) }
    var adjustMode by remember { mutableStateOf("ADD") } // ADD or DEDUCT
    var adjustExpression by remember { mutableStateOf("10") }
    var deductReason by remember { mutableStateOf("Wastage / Spoilage") }

    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddNewItemDialog by remember { mutableStateOf(false) }

    var activeImagePickerCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val fileName = "stock_img_${System.currentTimeMillis()}.jpg"
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

    // Core 11 stock items definition requested by user
    val coreStockNames = remember {
        listOf(
            "Zinger burger Bread",
            "Burger bread",
            "Shawarma Bread",
            "Chicken Patties",
            "Zinger meat(pieces)",
            "Eggs",
            "Fries in Kg/g",
            "Coke regular",
            "Pepsi Plastic",
            "Dew regular",
            "Dew Plastic"
        )
    }

    // Filtered inventory products
    val inventoryProducts = remember(products, searchQuery, selectedFilterCategory) {
        products.filter { prod ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                prod.name.contains(searchQuery, ignoreCase = true) ||
                        prod.sku.contains(searchQuery, ignoreCase = true) ||
                        prod.category.contains(searchQuery, ignoreCase = true)
            }
            val matchesCategory = when (selectedFilterCategory) {
                "CORE" -> coreStockNames.any { it.equals(prod.name, ignoreCase = true) } ||
                        prod.name.contains("Bread", ignoreCase = true) ||
                        prod.name.contains("Meat", ignoreCase = true) ||
                        prod.name.contains("Egg", ignoreCase = true) ||
                        prod.name.contains("Fries in", ignoreCase = true) ||
                        prod.name.contains("Dew", ignoreCase = true) ||
                        prod.name.contains("Coke", ignoreCase = true) ||
                        prod.name.contains("Pepsi", ignoreCase = true)
                "RAW" -> prod.category.contains("Raw", ignoreCase = true) || prod.category.contains("Add-ons", ignoreCase = true)
                "DRINKS" -> prod.category.equals("Drinks", ignoreCase = true)
                "BURGERS" -> prod.category.equals("Burgers", ignoreCase = true) || prod.category.equals("Shawarma", ignoreCase = true)
                else -> true
            }
            matchesSearch && matchesCategory
        }
    }

    val criticalCount = remember(products) {
        products.count { it.currentStock <= it.lowStockThreshold }
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
                .background(Color(0xFFF8FAFC))
                .testTag("stock_screen")
        ) {
            // Top Inventory Header & Controls (Clean Khata Ledger Bar)
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Title, Count Badge & New Item Action Button
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Stock & Inventory",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "${inventoryProducts.size} Items",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Kitchen supplies, recipes & store ledger",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = { showAddNewItemDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .defaultMinSize(minWidth = 105.dp)
                                .height(36.dp)
                                .testTag("add_inventory_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(15.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New Item",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Row 2: Search Input (Unclipped placeholder & centered text)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateStockSearchQuery(it) },
                        placeholder = { Text("Search stock items...", fontSize = 12.5.sp, color = Color(0xFF64748B)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64748B), modifier = Modifier.size(17.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateStockSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(15.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .testTag("stock_search_input")
                    )

                    // Row 3: Category Filter Tabs (Compact ledger pill strip)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filterCategories = listOf(
                            "CORE" to "Core (11)",
                            "RAW" to "Raw & Bakery",
                            "DRINKS" to "Cold Drinks",
                            "BURGERS" to "Burgers & Shawarma",
                            "ALL" to "All (${products.size})"
                        )

                        filterCategories.forEach { (catKey, label) ->
                            val isSelected = selectedFilterCategory == catKey
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) AmberPrimary else Color(0xFFF1F5F9),
                                border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
                                modifier = Modifier
                                    .clickable { selectedFilterCategory = catKey }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Compact Low Stock alert banner (only if critical items exist)
                    if (criticalCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFE4E6),
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Alert", tint = AlertRed, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "$criticalCount LOW STOCK ITEMS REQUIRE RESTOCKING",
                                    fontSize = 10.5.sp,
                                    color = AlertRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Inventory Items List (High-density, professional Khata / Ledger cards)
            if (inventoryProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No stock items match your filter.", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
                ) {
                    items(inventoryProducts, key = { it.id }) { item ->
                        val isCritical = item.currentStock <= item.lowStockThreshold

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stock_card_${item.id}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, if (isCritical) AlertRed.copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Row 1: Item Name + SKU + Category + Stock count pill
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)
                                    ) {
                                        ProductFoodThumbnail(
                                            imageUrl = item.imageUrl,
                                            productName = item.name,
                                            category = item.category,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                        )
                                        Text(
                                            text = item.name,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFF1F5F9)
                                        ) {
                                            Text(
                                                text = item.sku,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF475569),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    // Right: Current Stock Count Pill
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCritical) Color(0xFFFFE4E6) else Color(0xFFECFDF5),
                                        border = BorderStroke(1.dp, if (isCritical) AlertRed.copy(alpha = 0.5f) else VelocityGreen.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isCritical) AlertRed else VelocityGreen)
                                            )
                                            Text(
                                                text = "${if (item.currentStock % 1.0 == 0.0) item.currentStock.toInt().toString() else item.currentStock.toString()} ${item.unit}",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isCritical) AlertRed else Color(0xFF065F46)
                                            )
                                        }
                                    }
                                }

                                // Row 2: Financials (Cost | Sale | Margin) & Quick Action Steppers
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Financial details (Guaranteed no awkward multi-line break)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Cost: Rs ${item.costPrice}",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text("•", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                                        Text(
                                            text = "Sale: Rs ${item.price}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberPrimary
                                        )
                                        if (item.price > item.costPrice) {
                                            Text("•", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                                            Text(
                                                text = "+Rs ${item.price - item.costPrice}",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VelocityGreen
                                            )
                                        }
                                    }

                                    // Quick Stepper & Delete Action Buttons
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Quick Deduct 1 unit
                                        IconButton(
                                            onClick = {
                                                selectedProductForAdjust = item
                                                adjustMode = "DEDUCT"
                                                adjustExpression = "1"
                                                showAdjustStockModal = true
                                            },
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                                .testTag("quick_deduct_${item.id}")
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Deduct", tint = AlertRed, modifier = Modifier.size(13.dp))
                                        }

                                        // Quick Add 5 units
                                        IconButton(
                                            onClick = {
                                                selectedProductForAdjust = item
                                                adjustMode = "ADD"
                                                adjustExpression = "5"
                                                showAdjustStockModal = true
                                            },
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                                                .testTag("quick_add_${item.id}")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add", tint = AmberPrimary, modifier = Modifier.size(13.dp))
                                        }

                                        // Edit Item & Photo
                                        IconButton(
                                            onClick = { productToEdit = item },
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                                .testTag("edit_stock_item_${item.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Item & Photo", tint = Color(0xFF0F172A), modifier = Modifier.size(13.dp))
                                        }

                                        // Detailed Adjust Modal
                                        IconButton(
                                            onClick = {
                                                selectedProductForAdjust = item
                                                adjustMode = "ADD"
                                                adjustExpression = "10"
                                                showAdjustStockModal = true
                                            },
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                                .testTag("adjust_stock_btn_${item.id}")
                                        ) {
                                            Icon(Icons.Default.Tune, contentDescription = "Adjust", tint = Color(0xFF334155), modifier = Modifier.size(13.dp))
                                        }

                                        // Delete
                                        IconButton(
                                            onClick = { productToDelete = item },
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(Color(0xFFFEE2E2), RoundedCornerShape(6.dp))
                                                .testTag("delete_stock_item_${item.id}")
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Adjust Stock (+ Add / Restock or - Deduct / Spoilage / Wastage)
    if (showAdjustStockModal && selectedProductForAdjust != null) {
        val prod = selectedProductForAdjust!!
        val calculatedQty = MathEvaluator.evaluateToDouble(adjustExpression, fallback = 1.0)

        AlertDialog(
            onDismissRequest = { showAdjustStockModal = false },
            title = {
                Column {
                    Text(
                        text = "INVENTORY ADJUSTMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = prod.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Current Stock: ${if (prod.currentStock % 1.0 == 0.0) prod.currentStock.toInt().toString() else prod.currentStock.toString()} ${prod.unit}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Action Mode Toggle: Add Stock vs Deduct Units
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { adjustMode = "ADD" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (adjustMode == "ADD") VelocityGreen else Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = if (adjustMode == "ADD") Color.White else Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ Add Stock",
                                fontSize = 11.5.sp,
                                color = if (adjustMode == "ADD") Color.White else Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { adjustMode = "DEDUCT" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (adjustMode == "DEDUCT") AlertRed else Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.RemoveCircle, contentDescription = "Deduct", tint = if (adjustMode == "DEDUCT") Color.White else Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "- Deduct Units",
                                fontSize = 11.5.sp,
                                color = if (adjustMode == "DEDUCT") Color.White else Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // If Deducting, allow selecting reason
                    if (adjustMode == "DEDUCT") {
                        Text(
                            text = "Reason for Deduction:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Wastage / Spoilage", "Kitchen Prep / Mayo", "Personal / Staff Food", "Damaged / Dropped", "Recount Adjustment").forEach { reasonOption ->
                                val isSel = deductReason == reasonOption
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSel) AlertRed else Color(0xFFF1F5F9))
                                        .clickable { deductReason = reasonOption }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = reasonOption,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Color.White else Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }

                    // Units Input Field with Math Expression Support
                    OutlinedTextField(
                        value = adjustExpression,
                        onValueChange = { adjustExpression = it },
                        label = { Text(if (adjustMode == "ADD") "Units to Add (supports =8+3, 9-3, 10*2)" else "Units to Deduct / Remove") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Calculated Result Preview
                    Text(
                        text = "Calculated: ${if (calculatedQty % 1.0 == 0.0) calculatedQty.toInt().toString() else calculatedQty.toString()} ${prod.unit}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (adjustMode == "ADD") VelocityGreen else AlertRed
                    )

                    // Quick presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 5, 10, 20).forEach { preset ->
                            OutlinedButton(
                                onClick = { adjustExpression = "$preset" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text("$preset", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = MathEvaluator.evaluateToDouble(adjustExpression, fallback = 1.0)
                        if (qty > 0) {
                            if (adjustMode == "ADD") {
                                viewModel.replenishStock(
                                    productId = prod.id,
                                    productName = prod.name,
                                    quantityToAdd = qty,
                                    unitCost = prod.costPrice
                                )
                                Toast.makeText(context, "+$qty added to ${prod.name}", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.deductStock(
                                    productId = prod.id,
                                    productName = prod.name,
                                    quantityToDeduct = qty,
                                    reason = deductReason
                                )
                                Toast.makeText(context, "-$qty deducted from ${prod.name} ($deductReason)", Toast.LENGTH_SHORT).show()
                            }
                            showAdjustStockModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (adjustMode == "ADD") VelocityGreen else AlertRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (adjustMode == "ADD") "Confirm Restock" else "Confirm Deduction", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustStockModal = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Modal Dialog: Remove / Delete Item Completely from Stock Confirmation
    if (productToDelete != null) {
        val prod = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            icon = {
                Icon(Icons.Default.DeleteForever, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Remove from Stock?", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Are you sure you want to remove \"${prod.name}\" from your stock inventory?",
                        fontSize = 14.sp,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "SKU: ${prod.sku} • Current Stock: ${prod.currentStock} ${prod.unit}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "This item will be deleted from your active stock list.",
                        fontSize = 12.sp,
                        color = AlertRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(prod.id)
                        productToDelete = null
                        Toast.makeText(context, "${prod.name} removed from stock", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Yes, Delete Item", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Dialog: Add New Stock Item
    if (showAddNewItemDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Raw Prep & Bakery") }
        var unit by remember { mutableStateOf("pcs") }
        var costPriceExpr by remember { mutableStateOf("20") }
        var salePriceExpr by remember { mutableStateOf("40") }
        var stockQtyExpr by remember { mutableStateOf("10") }
        var sku by remember { mutableStateOf("STK-${(10..99).random()}") }
        var itemImageUrl by remember { mutableStateOf("food_zinger") }

        val evaluatedCost = MathEvaluator.evaluateToInt(costPriceExpr, 20)
        val evaluatedStock = MathEvaluator.evaluateToDouble(stockQtyExpr, 10.0)

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
                Text("Add New Stock Item", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
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
                            imageUrl = itemImageUrl,
                            productName = name.ifBlank { "Item" },
                            category = category,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Item Photo:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("Pick from phone gallery or choose preset", fontSize = 11.sp, color = Color(0xFF64748B))

                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    activeImagePickerCallback = { path ->
                                        itemImageUrl = path
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
                                    .testTag("pick_stock_photo_btn")
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
                            val isSelected = itemImageUrl == imgKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { itemImageUrl = imgKey },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Item Name") },
                        placeholder = { Text("e.g. Zinger burger Bread, Cheese Slices") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU Code") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit (pcs, Kg, bottles)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costPriceExpr,
                            onValueChange = { costPriceExpr = it },
                            label = { Text("Cost Price (Rs)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = salePriceExpr,
                            onValueChange = { salePriceExpr = it },
                            label = { Text("Sale Price (Rs)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = stockQtyExpr,
                        onValueChange = { stockQtyExpr = it },
                        label = { Text("Initial Stock (supports =8+3)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Result: $evaluatedStock $unit (Cost: Rs $evaluatedCost)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelocityGreen
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = MathEvaluator.evaluateToInt(costPriceExpr, 0)
                        val sale = MathEvaluator.evaluateToInt(salePriceExpr, 0)
                        val stock = MathEvaluator.evaluateToDouble(stockQtyExpr, 1.0)
                        if (name.isNotBlank()) {
                            viewModel.addNewMenuItem(
                                name = name,
                                category = category,
                                price = sale,
                                costPrice = cost,
                                sku = sku,
                                code = "#${(10..99).random()}",
                                imageUrl = itemImageUrl
                            )
                            showAddNewItemDialog = false
                            Toast.makeText(context, "Item $name added to menu & inventory", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Save Item", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNewItemDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Modal: Edit Stock Item & Picture
    if (productToEdit != null) {
        val prod = productToEdit!!
        var editName by remember { mutableStateOf(prod.name) }
        var editPrice by remember { mutableStateOf(prod.price.toString()) }
        var editCost by remember { mutableStateOf(prod.costPrice.toString()) }
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
                    text = "Edit Stock Item & Photo",
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
                    // Picture Preview & Picker
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
                                    .testTag("edit_pick_photo_btn")
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
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editCost,
                            onValueChange = { editCost = it },
                            label = { Text("Cost Price (Rs)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editPrice,
                            onValueChange = { editPrice = it },
                            label = { Text("Sale Price (Rs)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editSku,
                            onValueChange = { editSku = it },
                            label = { Text("SKU") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editCategory,
                            onValueChange = { editCategory = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedPrice = editPrice.toIntOrNull() ?: prod.price
                        val parsedCost = editCost.toIntOrNull() ?: prod.costPrice
                        if (editName.isNotBlank() && parsedPrice >= 0) {
                            viewModel.updateProduct(
                                prod.copy(
                                    name = editName,
                                    price = parsedPrice,
                                    costPrice = parsedCost,
                                    category = editCategory,
                                    sku = editSku,
                                    imageUrl = editImageUrl
                                )
                            )
                            productToEdit = null
                            Toast.makeText(context, "Item $editName updated!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToEdit = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }
}
