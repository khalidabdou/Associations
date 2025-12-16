package org.associations.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.associations.project.database.GetAllSubscribers
import org.associations.project.database.Zone
import org.associations.project.navigation.NavGraph
import org.associations.project.navigation.Screen
import org.associations.project.ui.MainLayout
import org.associations.project.ui.NavItem
import org.associations.project.ui.Strings
import org.associations.project.viewmodel.ActivationViewModel
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    MaterialTheme {
        KoinContext {
            val activationViewModel = koinViewModel<ActivationViewModel>()
            val isActivated = activationViewModel.checkActivation()

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: ""

                // Determine if we're on a detail screen that needs back button
                val isDetailScreen =
                        currentRoute.contains("Detail") ||
                                currentRoute.contains("Entry") ||
                                currentRoute.contains("Add")

                MainLayout(
                        currentRoute =
                                when {
                                    currentRoute.contains("Dashboard") -> "dashboard"
                                    currentRoute.contains("Subscriber") -> "members"
                                    currentRoute.contains("Meter") ||
                                            currentRoute.contains("Reading") -> "readings"
                                    currentRoute.contains("Invoice") -> "invoices"
                                    currentRoute.contains("Treasury") ||
                                            currentRoute.contains("Transaction") -> "treasury"
                                    currentRoute.contains("Maintenance") ||
                                            currentRoute.contains("Ticket") -> "maintenance"
                                    currentRoute.contains("Settings") ||
                                            currentRoute.contains("Zone") -> "settings"
                                    else -> "dashboard"
                                },
                        onNavigate = { navItem ->
                            when (navItem) {
                                NavItem.Dashboard ->
                                        navController.navigate(Screen.Dashboard) {
                                            popUpTo(Screen.Dashboard) { inclusive = true }
                                        }
                                NavItem.Members ->
                                        navController.navigate(Screen.SubscriberList) {
                                            popUpTo(Screen.Dashboard)
                                        }
                                NavItem.Readings ->
                                        navController.navigate(Screen.MeterReading) {
                                            popUpTo(Screen.Dashboard)
                                        }
                                NavItem.Invoices ->
                                        navController.navigate(Screen.InvoicesList) {
                                            popUpTo(Screen.Dashboard)
                                        }
                                NavItem.Treasury ->
                                        navController.navigate(Screen.Treasury) {
                                            popUpTo(Screen.Dashboard)
                                        }
                                NavItem.Maintenance ->
                                        navController.navigate(Screen.MaintenanceList) {
                                            popUpTo(Screen.Dashboard)
                                        }
                                NavItem.Settings ->
                                        navController.navigate(Screen.Settings) {
                                            popUpTo(Screen.Dashboard)
                                        }
                            }
                        },
                        showBackButton = isDetailScreen,
                        onBackClick = { navController.popBackStack() },
                        title = Strings.appName
                ) { NavGraph(navController = navController, startDestination = Screen.Dashboard) }
            }
        }
    }
}

@Composable
fun ZonesList(zones: List<Zone>) {
    LazyColumn {
        items(zones) { zone ->
            ListItem(
                    headlineContent = { Text(zone.name) },
                    supportingContent = { Text(zone.description ?: "") }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun SubscribersList(subscribers: List<GetAllSubscribers>) {
    LazyColumn {
        items(subscribers) { subscriber ->
            ListItem(
                    headlineContent = { Text(subscriber.fullName) },
                    supportingContent = {
                        Text(
                                "Meter: ${subscriber.meterNumber} | Zone: ${subscriber.zoneName ?: "Unknown"}"
                        )
                    }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun AddZoneDialog(onDismiss: () -> Unit, onConfirm: (String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Zone") },
            text = {
                Column {
                    TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    TextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = { onConfirm(name, description.ifBlank { null }) }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddSubscriberDialog(
        zones: List<Zone>,
        onDismiss: () -> Unit,
        onConfirm: (String, String?, String, String?, Long) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var meterNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedZoneId by remember { mutableStateOf(zones.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Subscriber") },
            text = {
                Column {
                    TextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") }
                    )
                    TextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") }
                    )
                    TextField(
                            value = meterNumber,
                            onValueChange = { meterNumber = it },
                            label = { Text("Meter Number") }
                    )
                    TextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") }
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(zones.find { it.id == selectedZoneId }?.name ?: "Select Zone")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            zones.forEach { zone ->
                                DropdownMenuItem(
                                        text = { Text(zone.name) },
                                        onClick = {
                                            selectedZoneId = zone.id
                                            expanded = false
                                        }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                        onClick = {
                            if (selectedZoneId != null) {
                                onConfirm(
                                        fullName,
                                        phone.ifBlank { null },
                                        meterNumber,
                                        address.ifBlank { null },
                                        selectedZoneId!!
                                )
                            }
                        },
                        enabled =
                                selectedZoneId != null &&
                                        fullName.isNotBlank() &&
                                        meterNumber.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
