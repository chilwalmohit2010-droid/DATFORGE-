package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class NavTab(
    val route: String,
    val title: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    HABITS("habits", "Habits", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    PROGRESS("progress", "Progress", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
    LEADERBOARD("leaderboard", "Leaderboard", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    JOURNAL("journal", "Journal", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    PROFILE("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person),
    FOCUS("focus", "Focus", Icons.Filled.Timer, Icons.Outlined.Timer)
}

/**
 * Premium iOS 27-Inspired Floating Liquid Glass Bottom Navigation Bar.
 * Layout: [Home] [Habits] [Progress] [+] [Leaderboard] [Journal] [Profile]
 * Balanced 7-position layout with equal-width slots (weight 1f each), placing the glowing
 * circular + button at the exact horizontal midpoint (position 4) of the navigation bar.
 */
@Composable
fun DisciplineBottomBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    onPlusClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false
) {
    val mainTabs = remember {
        listOf(
            NavTab.HOME,
            NavTab.HABITS,
            NavTab.PROGRESS,
            NavTab.LEADERBOARD,
            NavTab.JOURNAL,
            NavTab.PROFILE
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .shadow(
                    elevation = 28.dp,
                    shape = RoundedCornerShape(33.dp),
                    spotColor = Color(0xCC000000),
                    ambientColor = Color(0x4D00F0FF)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x66FFFFFF),
                            Color(0x2038BDF8),
                            Color(0x0DFFFFFF)
                        )
                    ),
                    shape = RoundedCornerShape(33.dp)
                )
                .clip(RoundedCornerShape(33.dp)),
            color = Color(0xF2080D1A),
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x1F38BDF8),
                                Color(0x05000000)
                            )
                        )
                    )
                    .padding(horizontal = 2.dp, vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Position 1: Home
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        BottomNavItem(
                            tab = NavTab.HOME,
                            isSelected = currentTab == NavTab.HOME,
                            isReducedMotion = isReducedMotion,
                            onSelect = { onTabSelected(NavTab.HOME) }
                        )
                    }

                    // Position 2: Habits (Dedicated Habit Tracker Tab)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        BottomNavItem(
                            tab = NavTab.HABITS,
                            isSelected = currentTab == NavTab.HABITS,
                            isReducedMotion = isReducedMotion,
                            onSelect = { onTabSelected(NavTab.HABITS) }
                        )
                    }

                    // Position 3: Progress
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        BottomNavItem(
                            tab = NavTab.PROGRESS,
                            isSelected = currentTab == NavTab.PROGRESS,
                            isReducedMotion = isReducedMotion,
                            onSelect = { onTabSelected(NavTab.PROGRESS) }
                        )
                    }

                    // Position 4: Central Prominent Glowing Circular + Action Button (Exact Center)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        CentralPlusButton(
                            onClick = onPlusClick,
                            isReducedMotion = isReducedMotion
                        )
                    }

                    // Position 5: Leaderboard
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        BottomNavItem(
                            tab = NavTab.LEADERBOARD,
                            isSelected = currentTab == NavTab.LEADERBOARD,
                            isReducedMotion = isReducedMotion,
                            onSelect = { onTabSelected(NavTab.LEADERBOARD) }
                        )
                    }

                    // Position 6: Journal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        BottomNavItem(
                            tab = NavTab.JOURNAL,
                            isSelected = currentTab == NavTab.JOURNAL,
                            isReducedMotion = isReducedMotion,
                            onSelect = { onTabSelected(NavTab.JOURNAL) }
                        )
                    }

                    // Position 7: Profile
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        BottomNavItem(
                            tab = NavTab.PROFILE,
                            isSelected = currentTab == NavTab.PROFILE,
                            isReducedMotion = isReducedMotion,
                            onSelect = { onTabSelected(NavTab.PROFILE) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: NavTab,
    isSelected: Boolean,
    isReducedMotion: Boolean,
    onSelect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (!isReducedMotion && isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "navItemPressScale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan else TextSecondary,
        animationSpec = if (isReducedMotion) tween(0) else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navItemColor"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected && !isReducedMotion) 1.08f else if (isSelected) 1f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "navItemIconScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (isSelected) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x3306B6D4),
                                Color(0x1406B6D4)
                            )
                        )
                    )
                } else Modifier
            )
            .border(
                width = 1.dp,
                brush = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0x8038BDF8),
                            Color(0x2638BDF8)
                        )
                    )
                } else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onSelect() }
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .padding(horizontal = 1.dp, vertical = 2.dp)
            .testTag("nav_tab_${tab.route}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
                    contentDescription = tab.title,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = tab.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = contentColor,
                fontSize = if (tab == NavTab.LEADERBOARD) 7.8.sp else 8.4.sp,
                letterSpacing = if (tab == NavTab.LEADERBOARD) (-0.45).sp else (-0.15).sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Center Circular Floating Glass + Button.
 * Features cyan-to-blue gradient, luminous halo, and tactile spring response.
 */
@Composable
private fun CentralPlusButton(
    onClick: () -> Unit,
    isReducedMotion: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (!isReducedMotion && isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "plusButtonScale"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                spotColor = Color(0xFF00F0FF),
                ambientColor = Color(0xFF2563EB)
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        NeonCyan,
                        Color(0xFF2563EB),
                        Color(0xFF1D4ED8)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xE6FFFFFF),
                        Color(0x6638BDF8)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .testTag("nav_central_plus_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Quick Actions",
            tint = Color.Black,
            modifier = Modifier.size(22.dp)
        )
    }
}
