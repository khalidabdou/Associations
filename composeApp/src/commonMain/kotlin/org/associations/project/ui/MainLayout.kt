package org.associations.project.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Navigation items for the app */
enum class NavItem(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", Strings.navDashboard, Icons.Default.Dashboard),
    Members("members", Strings.navMembers, Icons.Default.People),
    Readings("readings", Strings.navReadings, Icons.Default.Speed),
    Invoices("invoices", Strings.navInvoices, Icons.Default.Receipt),
    Treasury("treasury", Strings.navTreasury, Icons.Default.AccountBalance),
    Maintenance("maintenance", Strings.navMaintenance, Icons.Default.Build),
    Settings("settings", Strings.navSettings, Icons.Default.Settings)
}

/** Main layout with NavigationRail for desktop Uses RTL layout for Arabic */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
        currentRoute: String,
        onNavigate: (NavItem) -> Unit,
        showBackButton: Boolean = false,
        onBackClick: () -> Unit = {},
        title: String = Strings.appName,
        content: @Composable () -> Unit
) {
    // Force RTL layout for Arabic
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Navigation Rail on the right (RTL means it appears on right)
            NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    header = {
                        if (showBackButton) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = Strings.back
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
            ) {
                NavItem.entries.forEach { item ->
                    NavigationRailItem(
                            selected = currentRoute.startsWith(item.route),
                            onClick = { onNavigate(item) },
                            icon = {
                                Icon(imageVector = item.icon, contentDescription = item.label)
                            },
                            label = { Text(item.label) },
                            alwaysShowLabel = true,
                            colors =
                                    NavigationRailItemDefaults.colors(
                                            selectedIconColor =
                                                    MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor =
                                                    MaterialTheme.colorScheme.onPrimaryContainer,
                                            indicatorColor =
                                                    MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor =
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor =
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                    )
                }
            }

            // Main content area
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Top App Bar
                TopAppBar(
                        title = { Text(title) },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        titleContentColor =
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                        navigationIcon = {
                            if (showBackButton) {
                                IconButton(onClick = onBackClick) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = Strings.back
                                    )
                                }
                            }
                        }
                )

                // Content
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) { content() }
            }
        }
    }
}

/** Simplified top app bar for detail screens */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopAppBar(
        title: String,
        onBackClick: () -> Unit,
        actions: @Composable RowScope.() -> Unit = {}
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = Strings.back
                        )
                    }
                },
                actions = actions,
                colors =
                        TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
        )
    }
}

/** Standard screen scaffold with back button support */
@Composable
fun ScreenScaffold(
        title: String,
        onBackClick: (() -> Unit)? = null,
        floatingActionButton: @Composable () -> Unit = {},
        actions: @Composable RowScope.() -> Unit = {},
        content: @Composable (PaddingValues) -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
                topBar = {
                    if (onBackClick != null) {
                        DetailTopAppBar(title = title, onBackClick = onBackClick, actions = actions)
                    }
                },
                floatingActionButton = floatingActionButton
        ) { padding -> content(padding) }
    }
}
