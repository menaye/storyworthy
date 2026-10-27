package com.example.storyworthy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.storyworthy.Entry
import com.example.storyworthy.ui.theme.StoryworthyTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters.previousOrSame
import kotlin.math.cos
import kotlin.math.sin

data class MoodCategory(
    val id: String,
    val emoji: String,
    val label: String,
    val color: Color
)

val defaultMoodCategories = listOf(
    MoodCategory("awful", "😩", "Awful", Color(0xFFEF5350)),
    MoodCategory("sad", "😔", "Sad", Color(0xFFFF9800)),
    MoodCategory("neutral", "😐", "Neutral", Color(0xFF78909C)),
    MoodCategory("happy", "😊", "Happy", Color(0xFF42A5F5)),
    MoodCategory("content", "😁", "Content", Color(0xFF26A69A)),
    MoodCategory("other", "😶", "Other", Color(0xFF673AB7))
)

fun classifyEntryMood(moodStr: String?): MoodCategory {
    if (moodStr.isNullOrBlank()) return defaultMoodCategories[2]
    val lower = moodStr.lowercase()
    return when {
        lower.contains("awful") || lower.contains("😩") -> defaultMoodCategories[0]
        lower.contains("sad") || lower.contains("😔") -> defaultMoodCategories[1]
        lower.contains("neutral") || lower.contains("😐") -> defaultMoodCategories[2]
        lower.contains("happy") || lower.contains("😊") -> defaultMoodCategories[3]
        lower.contains("content") || lower.contains("😁") || lower.contains("😃") -> defaultMoodCategories[4]
        else -> defaultMoodCategories[5]
    }
}

fun calculateCurrentStreak(entries: List<Entry>): Int {
    if (entries.isEmpty()) return 0
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    val dates = entries.mapNotNull { entry ->
        try {
            entry.date?.let { LocalDate.parse(it, formatter) }
        } catch (_: Exception) {
            try {
                entry.date?.let { LocalDate.parse(it) }
            } catch (_: Exception) { null }
        }
    }.toSet()

    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    var checkDate = when {
        dates.contains(today) -> today
        dates.contains(yesterday) -> yesterday
        else -> return 0
    }

    var streak = 0
    while (dates.contains(checkDate)) {
        streak++
        checkDate = checkDate.minusDays(1)
    }
    return streak
}

@Composable
fun StatsScreen(
    entries: List<Entry>,
    userStats: com.example.storyworthy.Stats? = null,
    onSearchClick: () -> Unit = {}
) {
    val totalEntries = userStats?.numEntries?.takeIf { it > 0 } ?: entries.size
    val currentStreak = remember(entries, userStats) {
        val calculated = calculateCurrentStreak(entries)
        if (userStats != null && !userStats.lastEntryDate.isNullOrBlank()) {
            val lastDate = try { LocalDate.parse(userStats.lastEntryDate) } catch (_: Exception) { null }
            val today = LocalDate.now()
            if (lastDate != null && lastDate < today.minusDays(1)) {
                0
            } else {
                userStats.currStreak
            }
        } else {
            calculated
        }
    }
    val maxStreak = remember(entries, userStats) {
        if (userStats != null && userStats.longestStreak > 0) userStats.longestStreak
        else maxOf(currentStreak, calculateMaxStreak(entries))
    }
    var showTimelineView by rememberSaveable { mutableStateOf(false) }

    // Mood Breakdown Calculations
    val moodCounts = remember(entries) {
        val counts = defaultMoodCategories.associateWith { 0 }.toMutableMap()
        for (entry in entries) {
            val cat = classifyEntryMood(entry.mood)
            counts[cat] = (counts[cat] ?: 0) + 1
        }
        counts
    }

    // Weekly Activity Calculations
    val weeklyActivity = remember(entries) {
        val startOfWeek = LocalDate.now().with(previousOrSame(DayOfWeek.SUNDAY))
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

        val days = (0..6).map { startOfWeek.plusDays(it.toLong()) }
        days.map { day ->
            val dayCount = entries.count { entry ->
                try {
                    val entryDate = entry.date?.let { LocalDate.parse(it, formatter) }
                    entryDate == day
                } catch (_: Exception) {
                    entry.date?.contains(day.toString()) == true
                }
            }
            Pair(day.dayOfWeek.name.take(3), dayCount)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Top Metrics Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Total Entries Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = "Total Entries",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Total Entries",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$totalEntries",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Current Streak Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                            contentDescription = "Current Streak",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Current Streak",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "$currentStreak days",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Max: $maxStreak",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Mood Distribution Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Card Header with Toggle Option in Top Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Favorite,
                            contentDescription = "Mood Distribution",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (showTimelineView) "Mood Timeline" else "Mood Distribution",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { showTimelineView = !showTimelineView }
                    ) {
                        Icon(
                            imageVector = if (showTimelineView) Icons.Outlined.PieChart else Icons.AutoMirrored.Outlined.ShowChart,
                            contentDescription = if (showTimelineView) "Switch to Pie Chart" else "Switch to Timeline",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showTimelineView) {
                    MoodTimelineLineGraph(entries = entries)
                } else {
                    MoodPieChart(
                        moodCounts = moodCounts,
                        totalCount = totalEntries
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Mood Counts Breakdown Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        defaultMoodCategories.forEach { cat ->
                            val count = moodCounts[cat] ?: 0
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(cat.emoji, fontSize = 28.sp)
                                Text(
                                    text = cat.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // This Week's Activity Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "This Week's Activity",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "This Week's Activity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Weekly Bar Chart
                val maxDayCount = (weeklyActivity.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weeklyActivity.forEach { (dayName, count) ->
                        val heightFraction = count.toFloat() / maxDayCount.toFloat()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .fillMaxHeight(0.7f * heightFraction.coerceAtLeast(0.1f))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (count > 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun MoodTimelineLineGraph(
    entries: List<Entry>
) {
    val chronologicalEntries = remember(entries) {
        entries.sortedWith(compareBy<Entry> { it.date }.thenBy { it.time }).takeLast(10)
    }

    if (chronologicalEntries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No entry history to display timeline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Y-Axis Mood Emoji Column (Top = Content 😁, Bottom = Awful 😩)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                defaultMoodCategories.reversed().forEach { cat ->
                    Text(cat.emoji, fontSize = 16.sp)
                }
            }

            // Canvas Line Chart
            val lineColor = MaterialTheme.colorScheme.primary
            val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val pointCount = chronologicalEntries.size
                    val stepX = if (pointCount > 1) width / (pointCount - 1) else width / 2f

                    // Draw 5 horizontal Y-axis guidelines for moods (0..4)
                    for (i in 0..4) {
                        val y = height - (i * (height / 4f))
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Compute Points (x, y, category)
                    val points = chronologicalEntries.mapIndexed { index, entry ->
                        val cat = classifyEntryMood(entry.mood)
                        val moodIndex = defaultMoodCategories.indexOf(cat).coerceIn(0, 4)
                        val x = if (pointCount > 1) index * stepX else width / 2f
                        val y = height - (moodIndex * (height / 4f))
                        Triple(Offset(x, y), cat, entry)
                    }

                    // Draw Connecting Line Segments
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = lineColor,
                            start = points[i].first,
                            end = points[i + 1].first,
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw Circle Point Nodes
                    for ((offset, cat, _) in points) {
                        drawCircle(
                            color = lineColor,
                            radius = 7.dp.toPx(),
                            center = offset
                        )
                        drawCircle(
                            color = cat.color,
                            radius = 5.dp.toPx(),
                            center = offset
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoodPieChart(
    moodCounts: Map<MoodCategory, Int>,
    totalCount: Int
) {
    val validTotal = totalCount.coerceAtLeast(1)

    Box(
        modifier = Modifier
            .size(220.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var currentStartAngle = -90f

            defaultMoodCategories.forEach { category ->
                val count = moodCounts[category] ?: 0
                val sweepAngle = if (totalCount == 0) 72f else (count.toFloat() / validTotal.toFloat()) * 360f
                if (sweepAngle > 0f) {
                    drawArc(
                        color = category.color,
                        startAngle = currentStartAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    currentStartAngle += sweepAngle
                }
            }
        }

        // Overlay Slice Labels around Pie
        var currentLabelAngle = -90f
        defaultMoodCategories.forEach { category ->
            val count = moodCounts[category] ?: 0
            val percentage = if (totalCount == 0) 0 else ((count.toFloat() / validTotal.toFloat()) * 100).toInt()
            val sweepAngle = if (totalCount == 0) 72f else (count.toFloat() / validTotal.toFloat()) * 360f

            if (percentage > 0 || totalCount == 0) {
                val midAngle = Math.toRadians((currentLabelAngle + sweepAngle / 2f).toDouble())
                val radiusPx = 95.dp.value
                val xOffset = (radiusPx * cos(midAngle)).dp
                val yOffset = (radiusPx * sin(midAngle)).dp

                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${category.emoji} $percentage%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = category.color
                    )
                }
            }
            currentLabelAngle += sweepAngle
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatsScreenPreview() {
    StoryworthyTheme {
        StatsScreen(
            entries = listOf(
                Entry().apply { mood = "😊 Happy"; date = "October 15, 2026" },
                Entry().apply { mood = "😁 Content"; date = "October 14, 2026" },
                Entry().apply { mood = "😊 Happy"; date = "October 13, 2026" },
                Entry().apply { mood = "😐 Neutral"; date = "October 12, 2026" },
                Entry().apply { mood = "😔 Sad"; date = "October 11, 2026" }
            )
        )
    }
}
