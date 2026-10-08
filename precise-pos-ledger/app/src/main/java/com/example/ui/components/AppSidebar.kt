package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PosViewModel
import com.example.ui.Screen
import com.example.ui.theme.*

data class SidebarNavItem(
    val screen: Screen,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String? = null
)

@Composable
fun AppSidebar(
    viewModel: PosViewModel,
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onCloseSidebar: () -> Unit
) {
    val currentBusinessDate by viewModel.currentBusinessDate.collectAsState()
    val countedPhysicalCash by viewModel.countedPhysicalCash.collectAsState()
    val loans by viewModel.loans.collectAsState()
    val products by viewModel.products.collectAsState()

    val totalKhata = loans.filter { it.direction == "GIVEN" && it.status == "OPEN" }.sumOf { it.remainingBalance }
    val criticalCount = products.count { it.currentStock <= it.lowStockThreshold }

    ModalDrawerSheet(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(),
        drawerContainerColor = Color(0xFF0F172A),
        drawerContentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Sidebar Header with Restaurant Brand Identity
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .padding(20.dp)
                    .statusBarsPadding()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberPrimary)
                                .border(2.dp, Color(0xFFFEF08A), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BB",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        IconButton(
                            onClick = onCloseSidebar,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Column {
                        Text(
                            text = "THE BIG BITE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Shawarma & Burger House",
                            fontSize = 12.sp,
                            color = AmberPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Live Status Card in Sidebar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(VelocityGreen)
                                    )
                                    Text("Till #01 • Live", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                }
                                Text("📅 $currentBusinessDate", fontSize = 11.sp, color = AmberPrimary, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Drawer Float: Rs $countedPhysicalCash",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Navigation Items List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "MAIN TERMINAL",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )

                val mainItems = listOf(
                    SidebarNavItem(
                        screen = Screen.DASHBOARD,
                        title = "Dashboard",
                        subtitle = "Hero sliders & KPI overview",
                        icon = Icons.Default.Dashboard
                    ),
                    SidebarNavItem(
                        screen = Screen.SALE,
                        title = "POS Counter / New Sale",
                        subtitle = "Rapid touch register",
                        icon = Icons.Default.PointOfSale
                    ),
                    SidebarNavItem(
                        screen = Screen.STOCK,
                        title = "Stock & Inventory",
                        subtitle = "11 Core kitchen items",
                        icon = Icons.Default.Inventory2,
                        badge = if (criticalCount > 0) "$criticalCount LOW" else null
                    ),
                    SidebarNavItem(
                        screen = Screen.EXPENSES,
                        title = "Expenses & Purchases",
                        subtitle = "Cash & JazzCash outflows",
                        icon = Icons.Default.ReceiptLong
                    )
                )

                mainItems.forEach { item ->
                    SidebarRow(
                        item = item,
                        isSelected = currentScreen == item.screen,
                        onClick = {
                            onNavigate(item.screen)
                            onCloseSidebar()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "ACCOUNTS & AUDIT",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )

                val accountItems = listOf(
                    SidebarNavItem(
                        screen = Screen.LOANS,
                        title = "Loans & Khata Ledger",
                        subtitle = "Customer credit & borrowings",
                        icon = Icons.Default.AccountBalance,
                        badge = "Rs $totalKhata"
                    ),
                    SidebarNavItem(
                        screen = Screen.DAY_CLOSE,
                        title = "Day Close (EOD)",
                        subtitle = "Reconcile & advance next day",
                        icon = Icons.Default.LockClock
                    ),
                    SidebarNavItem(
                        screen = Screen.EXCEL_EXPORT,
                        title = "Excel / CSV Backup",
                        subtitle = "Export data & print slips",
                        icon = Icons.Default.CloudDownload
                    )
                )

                accountItems.forEach { item ->
                    SidebarRow(
                        item = item,
                        isSelected = currentScreen == item.screen,
                        onClick = {
                            onNavigate(item.screen)
                            onCloseSidebar()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Sidebar Footer Note
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "The Big Bite POS Pro v2.0",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = "Offline SQLite Engine • All records encrypted and saved locally.",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun SidebarRow(
    item: SidebarNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) AmberPrimary else Color.Transparent
    val textCol = if (isSelected) Color.White else Color(0xFFF1F5F9)
    val subCol = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFF94A3B8)
    val iconCol = if (isSelected) Color.White else AmberPrimary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sidebar_${item.screen.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = iconCol,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = item.title,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textCol
                    )
                    Text(
                        text = item.subtitle,
                        fontSize = 10.5.sp,
                        color = subCol
                    )
                }
            }

            if (item.badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.25f) else Color(0xFF1E293B))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else AmberPrimary
                    )
                }
            }
        }
    }
}
