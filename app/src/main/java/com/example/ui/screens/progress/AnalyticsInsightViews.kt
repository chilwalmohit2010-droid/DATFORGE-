package com.example.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun InsightsCard(
    insights: List<AnalyticsInsight>,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x3306B6D4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "BEHAVIORAL INSIGHTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x3306B6D4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4D06B6D4))
                ) {
                    Text(
                        text = "Data-driven",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                insights.forEach { insight ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x4D0B101D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val (icon, iconTint) = when (insight.iconName) {
                                "trending_up" -> Icons.Filled.TrendingUp to NeonGreenBright
                                "star" -> Icons.Filled.Star to AmberWarning
                                "calendar_today" -> Icons.Filled.CalendarMonth to CyanAccent
                                "psychology" -> Icons.Filled.Psychology to Color(0xFFA855F7)
                                "water_drop" -> Icons.Filled.WaterDrop to CyanAccent
                                "bolt" -> Icons.Filled.Bolt to AmberWarning
                                else -> Icons.Filled.Lightbulb to NeonGreenBright
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(iconTint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = insight.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    if (insight.highlight != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = iconTint.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = insight.highlight,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = iconTint,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = insight.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitBreakdownItemCard(
    habitStats: HabitBreakdownStats,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x8C141C2E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0x14FFFFFF), Color.Transparent)))
                .padding(14.dp)
        ) {
            // Title and Category row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(habitStats.colorHex))
                    )
                    Column {
                        Text(
                            text = habitStats.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = habitStats.categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Completion Rate Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        habitStats.completionRatePercentage >= 75 -> Color(0x3322C55E)
                        habitStats.completionRatePercentage >= 50 -> Color(0x3306B6D4)
                        habitStats.completionRatePercentage > 0 -> Color(0x33F59E0B)
                        else -> Color(0x26FFFFFF)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            habitStats.completionRatePercentage >= 75 -> Color(0x6622C55E)
                            habitStats.completionRatePercentage >= 50 -> Color(0x6606B6D4)
                            habitStats.completionRatePercentage > 0 -> Color(0x66F59E0B)
                            else -> Color(0x33FFFFFF)
                        }
                    )
                ) {
                    Text(
                        text = "${habitStats.completionRatePercentage}% Done",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            habitStats.completionRatePercentage >= 75 -> NeonGreenBright
                            habitStats.completionRatePercentage >= 50 -> CyanAccent
                            habitStats.completionRatePercentage > 0 -> AmberWarning
                            else -> TextSecondary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row: Current Streak, Best Streak, Period completions, Trend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak info
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("CURRENT", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                        Text("${habitStats.currentStreak}d streak", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Column {
                        Text("BEST", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                        Text("${habitStats.bestStreak}d record", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = AmberWarning)
                    }
                    Column {
                        Text("PERIOD", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                        Text("${habitStats.periodCompletions} done", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }

                // Trend Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (habitStats.isTrendingUp) {
                        true -> NeonGreenBright.copy(alpha = 0.15f)
                        false -> AmberWarning.copy(alpha = 0.15f)
                        null -> Color(0x26FFFFFF)
                    }
                ) {
                    Text(
                        text = habitStats.trendText,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (habitStats.isTrendingUp) {
                            true -> NeonGreenBright
                            false -> AmberWarning
                            null -> TextSecondary
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
