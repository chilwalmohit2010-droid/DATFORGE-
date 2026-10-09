package com.example.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PeriodSelectorBar(
    selectedPeriod: AnalyticsPeriod,
    onPeriodSelected: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0x4DFFFFFF), Color(0x1AFFFFFF))),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x8C141C2E)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AnalyticsPeriod.values().forEach { period ->
                val isSelected = selectedPeriod == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Brush.verticalGradient(listOf(NeonGreenBright.copy(alpha = 0.25f), NeonGreen.copy(alpha = 0.15f)))
                            else androidx.compose.ui.graphics.SolidColor(Color.Transparent)
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) NeonGreenBright.copy(alpha = 0.6f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onPeriodSelected(period) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) NeonGreenBright else TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewMetricsGrid(
    stats: PeriodOverviewStats,
    period: AnalyticsPeriod,
    modifier: Modifier = Modifier
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Real Performance Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Filtered by ${period.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x3322C55E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4D22C55E))
                ) {
                    Text(
                        text = stats.scoreChangeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreenBright,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 x 3 Metrics Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        title = "CURRENT STREAK",
                        value = "${stats.currentStreak}d",
                        subtitle = "Active streak",
                        icon = Icons.Filled.LocalFireDepartment,
                        iconTint = NeonGreenBright,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "BEST STREAK",
                        value = "${stats.bestStreak}d",
                        subtitle = "All-time record",
                        icon = Icons.Filled.EmojiEvents,
                        iconTint = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "DISCIPLINE",
                        value = "${stats.disciplineScore}",
                        subtitle = "Overall Score",
                        icon = Icons.Filled.Shield,
                        iconTint = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        title = "COMPLETIONS",
                        value = "${stats.totalHabitCompletions}",
                        subtitle = "Habits done",
                        icon = Icons.Filled.CheckCircle,
                        iconTint = NeonGreenBright,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "AVG DAILY",
                        value = String.format("%.1f", stats.averageDailyCompletion),
                        subtitle = "Habits / day",
                        icon = Icons.Filled.Speed,
                        iconTint = ElectricBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "CONSISTENCY",
                        value = "${stats.habitConsistencyPercentage}%",
                        subtitle = "Scheduled done",
                        icon = Icons.Filled.TrendingUp,
                        iconTint = NeonGreenBright,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        title = "WATER SUCCESS",
                        value = "${stats.waterGoalSuccessRate}%",
                        subtitle = "Days target met",
                        icon = Icons.Filled.WaterDrop,
                        iconTint = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "FOCUS FLOW",
                        value = if (stats.totalFocusMinutes >= 60) "${stats.totalFocusMinutes / 60}h ${stats.totalFocusMinutes % 60}m" else "${stats.totalFocusMinutes}m",
                        subtitle = "Deep work mins",
                        icon = Icons.Filled.Psychology,
                        iconTint = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "NIGHT REVIEWS",
                        value = "${stats.nightReviewsCompleted}",
                        subtitle = "Entries logged",
                        icon = Icons.Filled.NightlightRound,
                        iconTint = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0x4D0B101D),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                fontSize = 16.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 9.sp
            )
        }
    }
}
