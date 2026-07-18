package com.najmi.sprint.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.sprint.core.ui.theme.SurfaceHero
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.najmi.sprint.feature.kanban.KanbanScreen
import com.najmi.sprint.feature.tracker.TrackerScreen
import com.najmi.sprint.feature.retro.RetroScreen
import com.najmi.sprint.ui.settings.SettingsScreen
import com.najmi.sprint.ui.settings.DebugConsoleScreen
import com.najmi.sprint.ui.context.ContextManagerScreen
import com.najmi.sprint.ui.project.ProjectManagerScreen
import com.najmi.sprint.ui.settings.IgnoredPackagesScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    var dropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sprint",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                actions = {
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                    val isSettingsRelated = currentRoute == "settings" || currentRoute == "debug_console" || 
                                            currentRoute == "context_manager" || currentRoute?.startsWith("project_manager") == true
                    
                    if (!isSettingsRelated) {
                        Box {
                            Surface(
                            onClick = { dropdownExpanded = true },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val selectedName = state.contexts.find { it.id == state.selectedContextId }?.name ?: "Global"
                                Text(
                                    selectedName,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Context",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Global (All Contexts)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                onClick = {
                                    viewModel.selectContext(null)
                                    dropdownExpanded = false
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                            )
                            state.contexts.forEach { ctx ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(parseColor(ctx.colorHex))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(ctx.name, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectContext(ctx.id)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceHero,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Dashboard") },
                    label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
                    selected = currentDestination?.hierarchy?.any { it.route == "dashboard" } == true,
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Kanban") },
                    label = { Text("Kanban", style = MaterialTheme.typography.labelSmall) },
                    selected = currentDestination?.hierarchy?.any { it.route == "kanban" } == true,
                    onClick = {
                        navController.navigate("kanban") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Retro") },
                    label = { Text("Retro", style = MaterialTheme.typography.labelSmall) },
                    selected = currentDestination?.hierarchy?.any { it.route == "retro" } == true,
                    onClick = {
                        navController.navigate("retro") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", style = MaterialTheme.typography.labelSmall) },
                    selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable("dashboard") {
                TrackerScreen()
            }
            composable("kanban") {
                KanbanScreen()
            }
            composable("retro") {
                RetroScreen()
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateToContextManager = { navController.navigate("context_manager") },
                    onNavigateToIgnoredPackages = { navController.navigate("ignored_packages") },
                    onNavigateToDebugConsole = { navController.navigate("debug_console") }
                )
            }
            composable("ignored_packages") {
                IgnoredPackagesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("debug_console") {
                DebugConsoleScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("context_manager") {
                ContextManagerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProjectManager = { contextId ->
                        navController.navigate("project_manager/$contextId")
                    }
                )
            }
            composable(
                route = "project_manager/{contextId}",
                arguments = listOf(androidx.navigation.navArgument("contextId") { type = androidx.navigation.NavType.StringType })
            ) {
                ProjectManagerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProjectDetail = { projectId ->
                        navController.navigate("project_detail/$projectId")
                    }
                )
            }
            composable(
                route = "project_detail/{projectId}",
                arguments = listOf(androidx.navigation.navArgument("projectId") { type = androidx.navigation.NavType.StringType })
            ) {
                com.najmi.sprint.ui.project.ProjectDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDocumentViewer = { projectId, documentId ->
                        navController.navigate("document_viewer/$projectId/$documentId")
                    }
                )
            }
            composable(
                route = "document_viewer/{projectId}/{documentId}",
                arguments = listOf(
                    androidx.navigation.navArgument("projectId") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("documentId") { type = androidx.navigation.NavType.StringType }
                )
            ) {
                com.najmi.sprint.ui.project.DocumentViewerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}
