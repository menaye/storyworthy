package com.example.storyworthy

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.material3.FabPosition
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChartOutlined
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LargeFloatingActionButton
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.example.storyworthy.ui.AddEntryScreen
import com.example.storyworthy.ui.AiGuideScreen
import com.example.storyworthy.ui.AiGuideViewModel
import com.example.storyworthy.ui.AuthViewModel
import com.example.storyworthy.ui.AuthState
import com.example.storyworthy.ui.ConfirmEntry
import com.example.storyworthy.ui.EntryDetailScreen
import com.example.storyworthy.ui.HomeScreen
import com.example.storyworthy.ui.LoginScreen
import com.example.storyworthy.ui.QuotesScreen
import com.example.storyworthy.ui.ProfileScreen
import com.example.storyworthy.ui.SearchScreen
import com.example.storyworthy.ui.SignupScreen
import com.example.storyworthy.ui.StatsScreen
import com.example.storyworthy.ui.WriteEntryScreen
import kotlinx.serialization.Serializable

@Serializable
object RouteHome

@Serializable
object RouteQuotes

@Serializable
object RouteStats

@Serializable
object RouteProfile

@Serializable
object RouteSearch

@Serializable
data class RouteAddEntry(val initialContent: String = "")

@Serializable
data class RouteAiGuide(val mood: String = "Neutral", val initialQuote: String = "", val entryContext: String = "")

@Serializable
data class RouteWriteEntry(val mood: String = "Neutral", val initialContent: String = "")

@Serializable
object RouteLogin

@Serializable
object RouteSignup

@Serializable
data class RouteConfirmEntry(val mood: String = "Neutral", val entryTitle: String = "", val entryDate: String = "", val content : String = "", val tags: List<String> = emptyList(),
    val images: List<String> = emptyList())

@Serializable
data class RouteEntryDetail(val entryIndex: Int = 0)

class BottomBarNotchedShape(private val fabRadius: Dp, private val margin: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val radiusPx = with(density) { fabRadius.toPx() }
        val marginPx = with(density) { margin.toPx() }
        val notchRadius = radiusPx + marginPx
        val centerX = size.width / 2f

        path.moveTo(0f, 0f)
        path.lineTo(centerX - notchRadius - 20f, 0f)

        // Quadratic curve for a smoother transition to the notch
        path.quadraticTo(
            centerX - notchRadius, 0f,
            centerX - notchRadius, 10f
        )
        
        // The notch arc
        path.arcTo(
            rect = Rect(
                left = centerX - notchRadius,
                top = -notchRadius,
                right = centerX + notchRadius,
                bottom = notchRadius
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = -180f,
            forceMoveTo = false
        )

        path.lineTo(centerX + notchRadius + 20f, 0f)
        path.lineTo(size.width, 0f)
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()

        return Outline.Generic(path)
    }
}

sealed class BottomNavItem(val route: Any, val icon: ImageVector, val labelRes: Int) {
    object HomeItem : BottomNavItem(RouteHome, Icons.Default.Home, R.string.home)
    object QuotesItem : BottomNavItem(RouteQuotes, Icons.Default.FormatQuote, R.string.quotes)
    object StatsItem : BottomNavItem(RouteStats, Icons.Default.InsertChartOutlined, R.string.stats)
    object ProfileItem : BottomNavItem(RouteProfile, Icons.Default.Person, R.string.profile)
}

@Composable
fun StoryworthyApp(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.HomeItem,
        BottomNavItem.QuotesItem,
        BottomNavItem.StatsItem,
        BottomNavItem.ProfileItem
    )
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Success)?.user
    val startDestination = if (authViewModel.currentUser != null) RouteHome else RouteLogin

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Only show bottom bar on main screens
            val showBottomBar = items.any { item -> 
                currentDestination?.hierarchy?.any { 
                    it.route?.contains(item.route::class.qualifiedName!!) == true 
                } == true 
            } || currentDestination?.hierarchy?.any { it.route?.contains(RouteAddEntry::class.qualifiedName!!) == true } == true

            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .shadow(
                            elevation = 16.dp, 
                            shape = BottomBarNotchedShape(fabRadius = 40.dp, margin = 4.dp)
                        )
                        .clip(BottomBarNotchedShape(fabRadius = 40.dp, margin = 4.dp))
                ) {
                    // Left items
                    items.take(2).forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(imageVector = item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) },
                            selected = currentDestination?.hierarchy?.any { 
                                it.route?.contains(item.route::class.qualifiedName!!) == true 
                            } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        )
                    }

                    // Spacer item to make room for FAB in the center slot
                    NavigationBarItem(
                        icon = {},
                        label = {},
                        selected = false,
                        onClick = {},
                        enabled = false
                    )

                    // Right items
                    items.takeLast(2).forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(imageVector = item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) },
                            selected = currentDestination?.hierarchy?.any { 
                                it.route?.contains(item.route::class.qualifiedName!!) == true 
                            } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = items.any { item -> 
                currentDestination?.hierarchy?.any { 
                    it.route?.contains(item.route::class.qualifiedName!!) == true 
                } == true 
            } || currentDestination?.hierarchy?.any { it.route?.contains(RouteAddEntry::class.qualifiedName!!) == true } == true

            if (showBottomBar) {
                LargeFloatingActionButton(
                    onClick = {
                        navController.navigate(RouteAddEntry()) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 12.dp,
                        pressedElevation = 6.dp
                    ),
                    modifier = Modifier.offset(y = 74.dp)
                    .size(80.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Entry", modifier = Modifier.size(44.dp))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<RouteHome> { 
                HomeScreen(
                    authViewModel = authViewModel,
                    onEntryClick = { index ->
                        navController.navigate(RouteEntryDetail(entryIndex = index))
                    },
                    onSearchClick = {
                        navController.navigate(RouteSearch)
                    }
                ) 
            }
            composable<RouteSearch> {
                val entries by authViewModel.entries.collectAsState()
                SearchScreen(
                    entries = entries,
                    onBackClick = { navController.popBackStack() },
                    onEntryClick = { index ->
                        navController.navigate(RouteEntryDetail(entryIndex = index))
                    }
                )
            }
            composable<RouteEntryDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<RouteEntryDetail>()
                val entries by authViewModel.entries.collectAsState()
                EntryDetailScreen(
                    entries = entries,
                    initialIndex = route.entryIndex,
                    onBackClick = { navController.popBackStack() },
                    onDeleteEntry = { entry -> authViewModel.deleteEntry(entry) },
                    onToggleStar = { entry -> authViewModel.toggleStar(entry) }
                )
            }
            composable<RouteQuotes> {
                QuotesScreen(
                    onAddToEntryClick = { quoteText ->
                        navController.navigate(RouteAddEntry(initialContent = quoteText))
                    }
                )
            }
            composable<RouteStats> {
                val entries by authViewModel.entries.collectAsState()
                StatsScreen(
                    entries = entries,
                    onSearchClick = { navController.navigate(RouteSearch) }
                )
            }
            composable<RouteProfile> { 
                ProfileScreen(
                    authViewModel = authViewModel,
                    onLogoutClick = {
                        navController.navigate(RouteLogin) {
                            popUpTo(RouteHome) { inclusive = true }
                        }
                    }
                )
            }
            composable<RouteAddEntry> { backStackEntry ->
                val route = backStackEntry.toRoute<RouteAddEntry>()
                AddEntryScreen(
                    authViewModel = authViewModel,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onGuidedJournalingChoice = { mood, isGuided, remembersChoice, contextTitle ->
                        // Update user's preferences
                        if (remembersChoice) { currentUser?.userPrefs?.aiAssistStatus = isGuided }
                        if (isGuided) {
                            navController.navigate(RouteAiGuide(mood = mood, initialQuote = route.initialContent, entryContext = contextTitle))
                        } else {
                            navController.navigate(RouteWriteEntry(mood = mood, initialContent = route.initialContent))
                        }
                    }
                )
            }
            composable<RouteAiGuide> { backStackEntry ->
                val route = backStackEntry.toRoute<RouteAiGuide>()
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(navController.graph.id)
                }
                val aiGuideViewModel: AiGuideViewModel = viewModel(parentEntry)
                AiGuideScreen(
                    selectedMood = route.mood,
                    initialQuote = route.initialQuote,
                    entryContext = route.entryContext,
                    viewModel = aiGuideViewModel,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToWriteEntry = { notes ->
                        navController.navigate(RouteWriteEntry(initialContent = notes))
                    }
                )
            }
            composable<RouteWriteEntry> { backStackEntry ->
                val route = backStackEntry.toRoute<RouteWriteEntry>()
                WriteEntryScreen(
                    mood = route.mood,
                    initialContent = route.initialContent,
                    onBackClick = { navController.popBackStack() },
                    onSubmit = {
                        navController.navigate(RouteConfirmEntry()) {
                            popUpTo(RouteConfirmEntry()) { inclusive = false }
                        }
                    }
                )
            }
            composable<RouteConfirmEntry> {
                ConfirmEntry(
                    onReturnHomeClick = {
                        navController.navigate(RouteHome) {
                            popUpTo(RouteHome) { inclusive = true }
                        }
                    },
                    onWriteAnotherEntryClick = {
                        navController.navigate(RouteAddEntry()) {
                            popUpTo(RouteHome) { inclusive = false }
                        }
                    }
                )
            }
            composable<RouteLogin> {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToSignup = { navController.navigate(RouteSignup) },
                    onLoginSuccess = { 
                        navController.navigate(RouteHome) {
                            popUpTo(RouteLogin) { inclusive = true }
                        }
                    }
                )
            }
            composable<RouteSignup> {
                SignupScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = { navController.navigate(RouteLogin) },
                    onSignupSuccess = {
                        navController.navigate(RouteHome) {
                            popUpTo(RouteSignup) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}