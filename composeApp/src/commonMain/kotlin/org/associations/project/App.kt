package org.associations.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import org.associations.project.database.GetAllSubscribers
import org.associations.project.database.Zone
import org.associations.project.navigation.NavGraph
import org.koin.compose.KoinContext

@Composable
fun App() {
    MaterialTheme {
        KoinContext {
            val navController = rememberNavController()
            NavGraph(navController)
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
                supportingContent = { Text("Meter: ${subscriber.meterNumber} | Zone: ${subscriber.zoneName ?: "Unknown"}") }
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
                TextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, description.ifBlank { null }) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
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
                TextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") })
                TextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })
                TextField(value = meterNumber, onValueChange = { meterNumber = it }, label = { Text("Meter Number") })
                TextField(value = address, onValueChange = { address = it }, label = { Text("Address") })
                
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
                        onConfirm(fullName, phone.ifBlank { null }, meterNumber, address.ifBlank { null }, selectedZoneId!!)
                    }
                },
                enabled = selectedZoneId != null && fullName.isNotBlank() && meterNumber.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}