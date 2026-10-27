package com.example.storyworthy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.storyworthy.DailyEntries
import com.example.storyworthy.Entry
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    onEntryClick: (Int) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val entries by authViewModel.entries.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val user = (authState as? AuthState.Success)?.user
    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) } // 0 = Lists, 1 = Calendar
    var showEntryFilter by rememberSaveable { mutableStateOf(false) }
    var selectedYear by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMood by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        authViewModel.loadUserEntries()
    }

    val filteredEntries = remember(entries, selectedYear, selectedMood, selectedTag, favoritesOnly) {
        entries.filter { entry ->
            val matchesYear = selectedYear == null || (entry.date?.contains(selectedYear!!) == true)
            val matchesMood = selectedMood == null || (entry.mood?.equals(selectedMood, ignoreCase = true) == true)
            val matchesTag = selectedTag == null || (entry.tags?.contains(selectedTag) == true)
            val matchesFavorites = !favoritesOnly || entry.isStarred
            matchesYear && matchesMood && matchesTag && matchesFavorites
        }
    }

    val dailyEntriesList = remember(filteredEntries) {
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US)
        filteredEntries
            .groupBy { it.date?.takeIf { d -> d.isNotBlank() } ?: "Recent Entries" }
            .map { (dateStr, list) ->
                val sortedEntriesForDay = list.sortedByDescending { entry ->
                    try {
                        LocalTime.parse(entry.time, timeFormatter)
                    } catch (_: Exception) {
                        try {
                            LocalTime.parse(entry.time)
                        } catch (_: Exception) {
                            LocalTime.MIN
                        }
                    }
                }
                DailyEntries(date = dateStr, entries = sortedEntriesForDay)
            }
            .sortedByDescending { group ->
                try {
                    LocalDate.parse(group.date, dateFormatter)
                } catch (_: Exception) {
                    try {
                        LocalDate.parse(group.date)
                    } catch (_: Exception) {
                        LocalDate.MIN
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    HomeHeader(
                        name = user?.fName ?: "User",
                        onSearchClick = onSearchClick,
                        onFilterClick = { showEntryFilter = !showEntryFilter }
                    )
                    HomeTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                    AnimatedVisibility(
                        visible = showEntryFilter,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        EntryFilterPanel(
                            entries = entries,
                            selectedYear = selectedYear,
                            onYearSelected = { selectedYear = it },
                            selectedMood = selectedMood,
                            onMoodSelected = { selectedMood = it },
                            selectedTag = selectedTag,
                            onTagSelected = { selectedTag = it },
                            favoritesOnly = favoritesOnly,
                            onFavoritesToggle = { favoritesOnly = it },
                            onClearAll = {
                                selectedYear = null
                                selectedMood = null
                                selectedTag = null
                                favoritesOnly = false
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) { innerPadding ->
        if (selectedTab == 1) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                CalendarViewCard(
                    entries = entries,
                    onDayClick = { entry ->
                        val index = entries.indexOf(entry)
                        if (index >= 0) onEntryClick(index)
                    }
                )
            }
        } else if (filteredEntries.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.List,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = if (entries.isEmpty()) "There are no entries recorded." else "No entries match the selected filters.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (entries.isEmpty()) "Press the plus (+) button below to create one!" else "Try adjusting or clearing your filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(dailyEntriesList) { dailyEntries ->
                    DailyEntryCard(
                        dailyEntries = dailyEntries,
                        allEntries = entries,
                        onEntryClick = onEntryClick,
                        authViewModel = authViewModel
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Bottom padding for FAB
                }
            }
        }
    }
}

@Composable
fun EntryFilterPanel(
    entries: List<Entry>,
    selectedYear: String?,
    onYearSelected: (String?) -> Unit,
    selectedMood: String?,
    onMoodSelected: (String?) -> Unit,
    selectedTag: String?,
    onTagSelected: (String?) -> Unit,
    favoritesOnly: Boolean,
    onFavoritesToggle: (Boolean) -> Unit,
    onClearAll: () -> Unit
) {
    val availableYears = remember(entries) {
        entries.mapNotNull { it.date?.takeLast(4) }.distinct().sortedDescending()
    }
    val availableMoods = remember(entries) {
        entries.mapNotNull { it.mood?.takeIf { m -> m.isNotBlank() } }.distinct()
    }
    val availableTags = remember(entries) {
        entries.flatMap { it.tags.orEmpty() }.distinct()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Entries",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onClearAll) {
                    Text("Clear All", fontSize = 12.sp)
                }
            }

            // Favorites Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Favorites Only", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = favoritesOnly,
                    onCheckedChange = onFavoritesToggle
                )
            }

            // Years Filter
            if (availableYears.isNotEmpty()) {
                Text("Year", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedYear == null,
                        onClick = { onYearSelected(null) },
                        label = { Text("All Years") }
                    )
                    availableYears.forEach { year ->
                        FilterChip(
                            selected = selectedYear == year,
                            onClick = { onYearSelected(if (selectedYear == year) null else year) },
                            label = { Text(year) }
                        )
                    }
                }
            }

            // Mood Filter
            if (availableMoods.isNotEmpty()) {
                Text("Mood", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMood == null,
                        onClick = { onMoodSelected(null) },
                        label = { Text("All Moods") }
                    )
                    availableMoods.forEach { mood ->
                        FilterChip(
                            selected = selectedMood == mood,
                            onClick = { onMoodSelected(if (selectedMood == mood) null else mood) },
                            label = { Text(mood) }
                        )
                    }
                }
            }

            // Tags Filter
            if (availableTags.isNotEmpty()) {
                Text("Tags", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { onTagSelected(null) },
                        label = { Text("All Tags") }
                    )
                    availableTags.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { onTagSelected(if (selectedTag == tag) null else tag) },
                            label = { Text("#$tag") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(
    name: String,
    onSearchClick: () -> Unit = {},
    onFilterClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Hello, $name!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
            val formattedDate = LocalDate.now()
            val fullFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            Text(
                text = formattedDate.format(fullFormatter),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = onFilterClick) {
                Icon(Icons.Default.FilterAlt, contentDescription = "Filter", modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun HomeTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = { onTabSelected(0) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            elevation = if (selectedTab == 0) ButtonDefaults.buttonElevation(4.dp) else null
        ) {
            Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Lists")
        }
        Spacer(Modifier.width(16.dp))
        Button(
            onClick = { onTabSelected(1) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            elevation = if (selectedTab == 1) ButtonDefaults.buttonElevation(4.dp) else null
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Calendar")
        }
    }
}

@Composable
fun DailyEntryCard(
    dailyEntries: DailyEntries,
    allEntries: List<Entry>,
    onEntryClick: (Int) -> Unit = {},
    authViewModel: AuthViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            // Date Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dailyEntries.date,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            dailyEntries.entries.forEachIndexed { index, entry ->
                EntryItem(
                    entry = entry,
                    authViewModel = authViewModel,
                    onClick = {
                        val indexInAll = allEntries.indexOf(entry)
                        if (indexInAll >= 0) {
                            onEntryClick(indexInAll)
                        }
                    }
                )
                if (index < dailyEntries.entries.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun EntryItem(
    entry: Entry,
    authViewModel: AuthViewModel,
    onClick: () -> Unit = {}
) {
    val emoji = remember(entry.mood) { extractEmoji(entry.mood) }
    val moodLabel = remember(entry.mood) { extractMoodLabel(entry.mood) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(60.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFF4E0), // Placeholder for emoji bg
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 24.sp)
                }
            }
            Text(
                text = moodLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = entry.time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = { authViewModel.toggleStar(entry) }) {
                Icon(
                    imageVector = if (entry.isStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Star",
                    tint = if (entry.isStarred) Color(0xFFFFD700) else Color.LightGray
                )
            }
        }

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp
            ) {
                entry.tags.forEach { tag ->
                    TagChip(tag)
                }
            }
        }
    }
}

@Composable
fun TagChip(tag: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = tag,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun FlowRow(
    mainAxisSpacing: androidx.compose.ui.unit.Dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(mainAxisSpacing),
        verticalArrangement = Arrangement.spacedBy(crossAxisSpacing)
    ) {
        content()
    }
}

data class CalendarDayItem(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val entries: List<Entry>
)

fun getCalendarDays(yearMonth: YearMonth, allEntries: List<Entry>): List<CalendarDayItem> {
    val firstDayOfMonth = yearMonth.atDay(1)
    val sunStartIndex = firstDayOfMonth.dayOfWeek.value % 7

    val daysList = mutableListOf<CalendarDayItem>()

    val prevMonth = yearMonth.minusMonths(1)
    val daysInPrevMonth = prevMonth.lengthOfMonth()
    for (i in sunStartIndex - 1 downTo 0) {
        val date = prevMonth.atDay(daysInPrevMonth - i)
        daysList.add(CalendarDayItem(date, false, allEntries.filter { it.dateMatches(date) }))
    }

    for (day in 1..yearMonth.lengthOfMonth()) {
        val date = yearMonth.atDay(day)
        daysList.add(CalendarDayItem(date, true, allEntries.filter { it.dateMatches(date) }))
    }

    val totalDaysSoFar = daysList.size
    val remainingCells = if (totalDaysSoFar <= 35) 35 - totalDaysSoFar else 42 - totalDaysSoFar
    val nextMonth = yearMonth.plusMonths(1)
    for (day in 1..remainingCells) {
        val date = nextMonth.atDay(day)
        daysList.add(CalendarDayItem(date, false, allEntries.filter { it.dateMatches(date) }))
    }

    return daysList
}

fun Entry.dateMatches(targetDate: LocalDate): Boolean {
    if (this.date.isNullOrBlank()) return false
    try {
        val formattedTarget = targetDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        if (this.date.equals(formattedTarget, ignoreCase = true)) return true
    } catch (_: Exception) {}

    try {
        val targetYear = targetDate.year.toString()
        val targetDay = targetDate.dayOfMonth.toString()
        val monthName = targetDate.month.name.lowercase()
        val monthValue = targetDate.monthValue.toString()

        val entryLower = this.date.lowercase()
        val hasYear = entryLower.contains(targetYear)
        val hasDay = Regex("\\b$targetDay\\b").containsMatchIn(entryLower)
        val hasMonth = entryLower.contains(monthName) || entryLower.contains(" $monthValue ") || entryLower.startsWith("$monthValue/") || entryLower.endsWith("/$monthValue") || entryLower.contains("-$monthValue-")

        return hasYear && hasDay && hasMonth
    } catch (_: Exception) {
        return false
    }
}

@Composable
fun CalendarViewCard(
    entries: List<Entry>,
    onDayClick: (Entry) -> Unit = {}
) {
    var displayedYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }

    val daysList = remember(displayedYearMonth, entries) {
        getCalendarDays(displayedYearMonth, entries)
    }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.US) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { displayedYearMonth = displayedYearMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                }
                Text(
                    text = displayedYearMonth.format(monthFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { displayedYearMonth = displayedYearMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val weeks = daysList.chunked(7)
            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    week.forEach { dayItem ->
                        CalendarDayCell(
                            dayItem = dayItem,
                            isToday = dayItem.date == today,
                            onClick = {
                                if (dayItem.entries.isNotEmpty()) {
                                    onDayClick(dayItem.entries.first())
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("😊", fontSize = 8.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Has entry", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Multiple entries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    dayItem: CalendarDayItem,
    isToday: Boolean,
    onClick: () -> Unit = {}
) {
    val hasEntry = dayItem.entries.isNotEmpty()
    val multipleEntries = dayItem.entries.size > 1
    val firstEmoji = remember(dayItem.entries) {
        if (hasEntry) extractEmoji(dayItem.entries.first().mood) else null
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(
                if (hasEntry) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .then(
                if (isToday) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else if (hasEntry) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (firstEmoji != null) {
                Text(text = firstEmoji, fontSize = 18.sp)
            } else {
                Text(
                    text = dayItem.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (dayItem.isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }

            if (multipleEntries) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}