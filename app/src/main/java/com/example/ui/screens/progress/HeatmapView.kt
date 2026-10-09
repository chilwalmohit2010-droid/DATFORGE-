package com.example.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HeatmapCard(
    heatmapDays: List<HeatmapDay>,
    modifier: Modifier = Modifier
) {
    var selectedDay by remember { mutableStateOf<HeatmapDay?>(null) }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

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
                        text = "Activity Heatmap",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "90-day habit density & execution",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x2622C55E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4022C55E))
                ) {
                    Text(
                        text = "Last 13 Weeks",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreenBright,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selected cell inspection badge
            if (selectedDay != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x4D0B101D),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDay!!.date.format(dayFormatter),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (selectedDay!!.completionCount == 1) "1 habit completed" else "${selectedDay!!.completionCount} habits completed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedDay!!.completionCount > 0) NeonGreenBright else TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Grid: 7 rows (Mon-Sun) across 13 columns (weeks)
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day of week labels on left
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier.size(17.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index % 2 == 0) {
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 13 weeks of 7 days
                val weeks = heatmapDays.chunked(7)
                weeks.forEach { weekDays ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        weekDays.forEach { day ->
                            val isSelected = selectedDay?.date == day.date
                            val cellColor = when {
                                day.completionCount >= 3 -> NeonGreenBright
                                day.completionCount == 2 -> NeonGreen.copy(alpha = 0.75f)
                                day.completionCount == 1 -> Color(0x5922C55E)
                                else -> Color(0x331E293B)
                            }

                            Box(
                                modifier = Modifier
                                    .size(17.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(cellColor)
                                    .border(
                                        width = when {
                                            isSelected -> 1.5.dp
                                            day.isToday -> 1.dp
                                            else -> 0.dp
                                        },
                                        color = when {
                                            isSelected -> Color.White
                                            day.isToday -> NeonGreenBright
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(3.dp)
                                    )
                                    .clickable {
                                        selectedDay = if (selectedDay?.date == day.date) null else day
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap any cell for details",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Less", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(11.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x331E293B)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.size(11.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x5922C55E)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.size(11.dp).clip(RoundedCornerShape(2.dp)).background(NeonGreen.copy(alpha = 0.75f)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.size(11.dp).clip(RoundedCornerShape(2.dp)).background(NeonGreenBright))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("More", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}
