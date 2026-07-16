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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                title = { Text("Sprint", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        TextButton(onClick = { dropdownExpanded = true }) {
                            val selectedName = state.contexts.find { it.id == state.selectedContextId }?.name ?: "Global"
                            Text(selectedName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Global (All Contexts)", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    viewModel.selectContext(null)
                                    dropdownExpanded = false
                                }
                            )
                            Divider()
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
                                            Text(ctx.name)
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentDestination?.hierarchy?.any { it.route == "dashboard" } == true,
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Kanban") },
                    label = { Text("Kanban") },
                    selected = currentDestination?.hierarchy?.any { it.route == "kanban" } == true,
                    onClick = {
                        navController.navigate("kanban") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Retro") },
                    label = { Text("Retro") },
                    selected = currentDestination?.hierarchy?.any { it.route == "retro" } == true,
                    onClick = {
                        navController.navigate("retro") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
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
                SettingsScreen()
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
