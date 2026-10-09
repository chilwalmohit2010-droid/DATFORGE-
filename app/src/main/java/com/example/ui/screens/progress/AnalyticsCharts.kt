package com.example.ui.screens.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.max

@Composable
fun AnalyticsChartsCard(
    dailyMetrics: List<DailyMetricPoint>,
    period: AnalyticsPeriod,
    modifier: Modifier = Modifier
) {
    var selectedChart by remember { mutableStateOf(AnalyticsChartType.HABITS) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0x59FFFFFF), Color(0x1AFFFFFF))),
                RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color(0x8C141C2E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0x14FFFFFF), Color.Transparent)))
                .padding(18.dp)
        ) {
            // Header with Chart Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Data Trends",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Visualized across ${period.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart Type Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x4D0B101D))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnalyticsChartType.values().forEach { chartType ->
                    val isSelected = selectedChart == chartType
                    val activeColor = when (chartType) {
                        AnalyticsChartType.HABITS -> NeonGreenBright
                        AnalyticsChartType.WATER -> CyanAccent
                        AnalyticsChartType.FOCUS -> Color(0xFFA855F7)
                        AnalyticsChartType.CONSISTENCY -> AmberWarning
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) activeColor.copy(alpha = 0.6f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedChart = chartType }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chartType.label.split(" ").first(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) activeColor else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Content based on selection
            when (selectedChart) {
                AnalyticsChartType.HABITS -> DailyHabitsBarChart(dailyMetrics, period)
                AnalyticsChartType.WATER -> WaterGoalChart(dailyMetrics, period)
                AnalyticsChartType.FOCUS -> FocusMinutesChart(dailyMetrics, period)
                AnalyticsChartType.CONSISTENCY -> OverallConsistencyChart(dailyMetrics, period)
            }
        }
    }
}

@Composable
private fun DailyHabitsBarChart(
    metrics: List<DailyMetricPoint>,
    period: AnalyticsPeriod
) {
    val maxCompletions = max(1, metrics.maxOfOrNull { it.habitsCompleted } ?: 1)
    val totalDone = metrics.sumOf { it.habitsCompleted }
    val avgPerDay = if (metrics.isNotEmpty()) totalDone.toFloat() / metrics.size.toFloat() else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daily Habit Execution",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "$totalDone total • ${String.format("%.1f", avgPerDay)}/day",
                style = MaterialTheme.typography.labelSmall,
                color = NeonGreenBright,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Chart Bars
        val scrollState = rememberScrollState()
        val isScrollable = metrics.size > 14

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isScrollable) Modifier.horizontalScroll(scrollState) else Modifier),
                horizontalArrangement = if (isScrollable) Arrangement.spacedBy(6.dp) else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                metrics.forEach { point ->
                    val ratio = (point.habitsCompleted.toFloat() / maxCompletions.toFloat()).coerceIn(0.06f, 1f)
                    val animatedRatio by animateFloatAsState(targetValue = ratio, label = "bar_habit")
                    val isPeak = point.habitsCompleted == maxCompletions && maxCompletions > 0

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = if (isScrollable) Modifier.width(22.dp) else Modifier.weight(1f)
                    ) {
                        if (point.habitsCompleted > 0) {
                            Text(
                                text = "${point.habitsCompleted}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (isPeak) NeonGreenBright else TextSecondary,
                                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
                            )
                        } else {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(if (metrics.size <= 7) 20.dp else if (metrics.size <= 14) 14.dp else 8.dp)
                                .fillMaxHeight(animatedRatio)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    when {
                                        point.habitsCompleted == 0 -> SolidColor(Color(0x331E293B))
                                        isPeak -> Brush.verticalGradient(listOf(NeonGreenBright, NeonGreen))
                                        point.habitCompletionRate >= 0.7f -> Brush.verticalGradient(listOf(NeonGreen, Color(0xFF059669)))
                                        else -> Brush.verticalGradient(listOf(ElectricBlue, Color(0xFF0284C7)))
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (metrics.size <= 7) point.dayLabel else if (metrics.size <= 30) point.date.dayOfMonth.toString() else point.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = if (isPeak) NeonGreenBright else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterGoalChart(
    metrics: List<DailyMetricPoint>,
    period: AnalyticsPeriod
) {
    val maxIntake = max(2500, metrics.maxOfOrNull { it.waterIntakeMl } ?: 2500)
    val metCount = metrics.count { it.isWaterGoalMet }
    val avgIntake = if (metrics.isNotEmpty()) metrics.map { it.waterIntakeMl }.average().toInt() else 0

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hydration Intake vs Goal",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "$metCount/${metrics.size} days met • ${avgIntake}ml avg",
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        val scrollState = rememberScrollState()
        val isScrollable = metrics.size > 14

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isScrollable) Modifier.horizontalScroll(scrollState) else Modifier),
                horizontalArrangement = if (isScrollable) Arrangement.spacedBy(6.dp) else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                metrics.forEach { point ->
                    val ratio = (point.waterIntakeMl.toFloat() / maxIntake.toFloat()).coerceIn(0.06f, 1f)
                    val animatedRatio by animateFloatAsState(targetValue = ratio, label = "bar_water")

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = if (isScrollable) Modifier.width(22.dp) else Modifier.weight(1f)
                    ) {
                        if (point.isWaterGoalMet) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(CyanAccent)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(6.dp))
                        }
                        Spacer(modifier = Modifier.height(3.dp))

                        Box(
                            modifier = Modifier
                                .width(if (metrics.size <= 7) 20.dp else if (metrics.size <= 14) 14.dp else 8.dp)
                                .fillMaxHeight(animatedRatio)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (point.isWaterGoalMet) Brush.verticalGradient(listOf(CyanAccent, Color(0xFF0284C7)))
                                    else if (point.waterIntakeMl > 0) Brush.verticalGradient(listOf(Color(0x8006B6D4), Color(0x3306B6D4)))
                                    else SolidColor(Color(0x331E293B))
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (metrics.size <= 7) point.dayLabel else if (metrics.size <= 30) point.date.dayOfMonth.toString() else point.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = if (point.isWaterGoalMet) CyanAccent else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusMinutesChart(
    metrics: List<DailyMetricPoint>,
    period: AnalyticsPeriod
) {
    val maxMinutes = max(60, metrics.maxOfOrNull { it.focusMinutes } ?: 60)
    val totalMins = metrics.sumOf { it.focusMinutes }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Deep Focus Flow",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            val hours = totalMins / 60
            val remMins = totalMins % 60
            val formatted = if (hours > 0) "${hours}h ${remMins}m" else "${remMins}m"
            Text(
                text = "$formatted total flow",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA855F7),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        val scrollState = rememberScrollState()
        val isScrollable = metrics.size > 14

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isScrollable) Modifier.horizontalScroll(scrollState) else Modifier),
                horizontalArrangement = if (isScrollable) Arrangement.spacedBy(6.dp) else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                metrics.forEach { point ->
                    val ratio = (point.focusMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.06f, 1f)
                    val animatedRatio by animateFloatAsState(targetValue = ratio, label = "bar_focus")

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = if (isScrollable) Modifier.width(22.dp) else Modifier.weight(1f)
                    ) {
                        if (point.focusMinutes > 0) {
                            Text(
                                text = "${point.focusMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = Color(0xFFA855F7)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(10.dp))
                        }
                        Spacer(modifier = Modifier.height(3.dp))

                        Box(
                            modifier = Modifier
                                .width(if (metrics.size <= 7) 20.dp else if (metrics.size <= 14) 14.dp else 8.dp)
                                .fillMaxHeight(animatedRatio)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (point.focusMinutes > 0) Brush.verticalGradient(listOf(Color(0xFFA855F7), Color(0xFF6366F1)))
                                    else SolidColor(Color(0x331E293B))
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (metrics.size <= 7) point.dayLabel else if (metrics.size <= 30) point.date.dayOfMonth.toString() else point.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = if (point.focusMinutes > 0) Color(0xFFA855F7) else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallConsistencyChart(
    metrics: List<DailyMetricPoint>,
    period: AnalyticsPeriod
) {
    val avgConsistency = if (metrics.isNotEmpty()) {
        (metrics.map { it.compositeConsistencyRate }.average() * 100).toInt()
    } else 0

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Overall Consistency Index",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "$avgConsistency% average score",
                style = MaterialTheme.typography.labelSmall,
                color = AmberWarning,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        val scrollState = rememberScrollState()
        val isScrollable = metrics.size > 14

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isScrollable) Modifier.horizontalScroll(scrollState) else Modifier),
                horizontalArrangement = if (isScrollable) Arrangement.spacedBy(6.dp) else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                metrics.forEach { point ->
                    val ratio = point.compositeConsistencyRate.coerceIn(0.06f, 1f)
                    val animatedRatio by animateFloatAsState(targetValue = ratio, label = "bar_consistency")
                    val pct = (point.compositeConsistencyRate * 100).toInt()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = if (isScrollable) Modifier.width(22.dp) else Modifier.weight(1f)
                    ) {
                        if (pct >= 50) {
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = AmberWarning
                            )
                        } else {
                            Spacer(modifier = Modifier.size(10.dp))
                        }
                        Spacer(modifier = Modifier.height(3.dp))

                        Box(
                            modifier = Modifier
                                .width(if (metrics.size <= 7) 20.dp else if (metrics.size <= 14) 14.dp else 8.dp)
                                .fillMaxHeight(animatedRatio)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    when {
                                        point.compositeConsistencyRate >= 0.7f -> Brush.verticalGradient(listOf(AmberWarning, Color(0xFFD97706)))
                                        point.compositeConsistencyRate >= 0.4f -> Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFB45309)))
                                        point.compositeConsistencyRate > 0f -> Brush.verticalGradient(listOf(Color(0x66F59E0B), Color(0x33F59E0B)))
                                        else -> SolidColor(Color(0x331E293B))
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (metrics.size <= 7) point.dayLabel else if (metrics.size <= 30) point.date.dayOfMonth.toString() else point.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = if (point.compositeConsistencyRate >= 0.7f) AmberWarning else TextSecondary
                        )
                    }
                }
            }
        }
    }
}
