package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PosViewModel.DailySalesTrend
import com.example.ui.theme.*

@Composable
fun SalesTrendChart(
    trends: List<DailySalesTrend>,
    modifier: Modifier = Modifier
) {
    if (trends.isEmpty()) return

    val maxSales = remember(trends) {
        val highest = trends.maxOfOrNull { it.salesAmount } ?: 7000
        maxOf(highest, 6000)
    }

    val totalWeekSales = remember(trends) { trends.sumOf { it.salesAmount } }
    val avgDailySales = remember(trends) { totalWeekSales / trends.size }

    // Interactive selected day state
    var selectedDay by remember {
        mutableStateOf(trends.find { it.isToday } ?: trends.firstOrNull())
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_sales_chart_card"),
        shape = RoundedCornerShape(16.dp),
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
            // Chart Header & High-Level Metrics
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = "Trend",
                            tint = AmberPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Weekly Sales Trends",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Daily performance • Current Week (PKR)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Week Summary Pill
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Rs $totalWeekSales",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = VelocityGreen
                    )
                    Text(
                        text = "Avg: Rs $avgDailySales/day",
                        fontSize = 10.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Interactive Tooltip Info Box for Selected Day
            selectedDay?.let { day ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Detail", tint = AmberPrimary, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${day.dayName} (${day.dateLabel})${if (day.isToday) " • ACTIVE TODAY" else ""}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        Text(
                            text = "Rs ${day.salesAmount}  •  ${day.orderCount} Orders",
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (day.isToday) AmberPrimary else VelocityGreen
                        )
                    }
                }
            }

            // The Recharts-style Bar Chart Canvas / Layout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(top = 4.dp)
            ) {
                // Background Guide Lines (Y-Axis references: 6k, 4k, 2k, 0)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("6k", "4k", "2k", "0").forEach { label ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.5.sp,
                                color = Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(22.dp)
                            )
                            HorizontalDivider(
                                color = Color(0xFFE2E8F0).copy(alpha = 0.7f),
                                thickness = 0.8.dp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Vertical Bars (Mon to Sun)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 28.dp, end = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    trends.forEach { trend ->
                        val isSelected = selectedDay?.dayName == trend.dayName
                        val targetHeightRatio = (trend.salesAmount.toFloat() / maxSales.toFloat()).coerceIn(0.08f, 1f)

                        val animatedHeightRatio by animateFloatAsState(
                            targetValue = targetHeightRatio,
                            animationSpec = tween(durationMillis = 650),
                            label = "bar_height"
                        )

                        val barBrush = when {
                            trend.isToday -> Brush.verticalGradient(
                                colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                            )
                            trend.salesAmount >= 5500 -> Brush.verticalGradient(
                                colors = listOf(Color(0xFF22C55E), Color(0xFF16A34A))
                            )
                            else -> Brush.verticalGradient(
                                colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedDay = trend }
                        ) {
                            // Value text over bar
                            Text(
                                text = "${trend.salesAmount / 1000}.${(trend.salesAmount % 1000) / 100}k",
                                fontSize = 9.sp,
                                fontWeight = if (isSelected || trend.isToday) FontWeight.Black else FontWeight.Normal,
                                color = if (trend.isToday) AmberPrimary else Color(0xFF475569),
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // The Bar Pill
                            Box(
                                modifier = Modifier
                                    .width(if (isSelected) 26.dp else 22.dp)
                                    .fillMaxHeight(animatedHeightRatio * 0.78f)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(barBrush)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) Color(0xFF0F172A) else Color.Transparent,
                                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Day Label below Bar
                            Text(
                                text = trend.dayName,
                                fontSize = 10.5.sp,
                                fontWeight = if (trend.isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (trend.isToday) AmberPrimary else Color(0xFF334155)
                            )

                            // Active Today Dot Indicator
                            if (trend.isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(AmberPrimary)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // Legend Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFFF59E0B), label = "Active Today")
                LegendItem(color = Color(0xFF0284C7), label = "Weekday Sales")
                LegendItem(color = Color(0xFF16A34A), label = "Weekend Peak")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
    }
}
