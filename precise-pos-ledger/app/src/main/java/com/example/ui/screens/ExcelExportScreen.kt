package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.ui.PosViewModel
import com.example.ui.theme.*

@Composable
fun ExcelExportScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var reportType by remember { mutableStateOf("ALL") }
    var dateRangeType by remember { mutableStateOf("TODAY") }
    var customStartDate by remember { mutableStateOf("2026-10-01") }
    var customEndDate by remember { mutableStateOf("2026-10-31") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importTextData by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isReadingFile by remember { mutableStateOf(false) }

    val csvFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                isReadingFile = true
                val inputStream = context.contentResolver.openInputStream(uri)
                val text = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                isReadingFile = false
                if (text.isNotBlank()) {
                    importTextData = text
                    selectedFileName = uri.lastPathSegment ?: "backup.csv"
                    viewModel.importBackupCsv(text) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        if (success) {
                            showImportDialog = false
                            importTextData = ""
                        }
                    }
                } else {
                    Toast.makeText(context, "Selected CSV file is empty!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isReadingFile = false
                Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    BackHandler {
        onNavigateBack()
    }

    val reportTypes = listOf(
        "ALL" to "All records (sales, expenses, stock, cash)",
        "SALES" to "Sales only",
        "EXPENSES" to "Expenses only",
        "STOCK" to "Stock movements",
        "DAY_CLOSE" to "Cash and JazzCash closing"
    )

    val dateRanges = listOf(
        "TODAY" to "One day (02 Oct 2026)",
        "MONTH" to "Full month (October 2026)",
        "CUSTOM" to "Custom start and end dates"
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
                .widthIn(max = 840.dp)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
                .padding(bottom = 80.dp)
                .testTag("excel_export_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                }
                Column {
                    Text(
                        text = "Export to Excel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Financial & Inventory Reports",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(VelocityGreenContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = "Excel", tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(
                        text = ".XLSX / CSV",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Section 1: Report Type Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. SELECT REPORT TYPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 0.5.sp
                )

                reportTypes.forEach { (key, title) ->
                    val isSelected = reportType == key
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AmberPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { reportType = key }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { reportType = key },
                            colors = RadioButtonDefaults.colors(selectedColor = AmberPrimary)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Section 2: Date Range Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "2. CHOOSE DATE RANGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 0.5.sp
                )

                dateRanges.forEach { (key, title) ->
                    val isSelected = dateRangeType == key
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) VelocityGreenContainer.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { dateRangeType = key }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { dateRangeType = key },
                            colors = RadioButtonDefaults.colors(selectedColor = VelocityGreen)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (dateRangeType == "CUSTOM") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customStartDate,
                            onValueChange = { customStartDate = it },
                            label = { Text("Start Date", color = Color(0xFF334155)) },
                            colors = highContrastTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customEndDate,
                            onValueChange = { customEndDate = it },
                            label = { Text("End Date", color = Color(0xFF334155)) },
                            colors = highContrastTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Recommended Multi-Sheet Workbook Preview
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WORKBOOK SHEET STRUCTURE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text("6 Sheets Included", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = VelocityGreen)
                }

                val sheets = listOf(
                    "Daily Summary" to "Date-wise sales, expenses, cash and JazzCash closing",
                    "Sales" to "Each sale, order tokens, items and line totals",
                    "Expenses" to "Each expense with category and payment method",
                    "Stock Movements" to "Stock added, used, wasted or adjusted",
                    "Cash & JazzCash" to "Opening, received, paid, expected and actual",
                    "Products" to "Current menu items and saved price catalog"
                )

                sheets.forEach { (name, desc) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• $name",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.5.sp,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // Master Actions: Export & Import / Restore
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val rangeLabel = when (dateRangeType) {
                        "TODAY" -> "02 Oct 2026"
                        "MONTH" -> "October 2026"
                        else -> "$customStartDate to $customEndDate"
                    }
                    val exportData = viewModel.generateExportData(reportType, rangeLabel)
                    viewModel.shareExportFile(context, exportData, "TheBigBite_Report_${reportType}_$dateRangeType.csv")
                    viewModel.performAutoBackup()
                    Toast.makeText(context, "Generating CSV Export for Excel & Sheets...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("export_excel_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = Color.White, modifier = Modifier.size(22.dp))
                    Text(
                        text = "EXPORT EXCEL / CSV REPORT",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            OutlinedButton(
                onClick = { showImportDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("import_excel_btn"),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Import", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                    Text(
                        text = "IMPORT / RESTORE DATA FROM EXCEL BACKUP",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        }
    }
    }

    // Modal: Import / Restore Previous Data
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = AmberPrimary)
                    Text("Import & Restore Data", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Restore all sales, expenses, stock levels, and khata records from an exported The Big Bite CSV sheet:",
                        fontSize = 12.5.sp,
                        color = Color(0xFF475569)
                    )

                    // Option A: Direct File Picker
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Option 1: Choose File",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Button(
                                onClick = {
                                    csvFilePickerLauncher.launch("*/*")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("choose_csv_file_btn")
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Choose CSV", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isReadingFile) "Reading File..." else "Select .csv from Phone Storage",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                            if (selectedFileName != null) {
                                Text(
                                    text = "Selected: $selectedFileName",
                                    fontSize = 11.sp,
                                    color = VelocityGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Option B: Paste CSV Text
                    Text(
                        text = "— OR PASTE CSV CONTENT DIRECTLY —",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    OutlinedTextField(
                        value = importTextData,
                        onValueChange = { importTextData = it },
                        label = { Text("Paste Backup CSV Content", color = Color(0xFF334155)) },
                        placeholder = { Text("THE BIG BITE - SHAWARMA & BURGER HOUSE\n=== SALES SUMMARY & ORDERS ===\n...", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 6
                    )

                    Text(
                        text = "• Pro Tip: When you restore, orders, products, expenses, and loans are accurately parsed and merged into your local offline database without data loss.",
                        fontSize = 11.sp,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importTextData.isBlank()) {
                            Toast.makeText(context, "Please choose a file or paste CSV text first!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.importBackupCsv(importTextData) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                if (success) {
                                    showImportDialog = false
                                    importTextData = ""
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VelocityGreen),
                    modifier = Modifier.testTag("confirm_restore_btn")
                ) {
                    Text("Restore Into App", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }
}
