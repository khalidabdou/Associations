package org.associations.project.maintenance

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.associations.project.database.GetAllMaintenanceTickets
import org.associations.project.database.GetAllSubscribers
import org.associations.project.ui.Strings
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceListScreen(
    onNavigateToAddTicket: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val viewModel = koinViewModel<MaintenanceViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var showStatusDialog by remember { mutableStateOf<Pair<Long, String>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = Strings.addTicket)
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = Strings.maintenanceTickets,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                // Status Filter Chips - Horizontally Scrollable
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedStatus == null,
                        onClick = { viewModel.selectStatus(null) },
                        label = { Text("الكل") }
                    )
                    FilterChip(
                        selected = uiState.selectedStatus == "OPEN",
                        onClick = { viewModel.selectStatus("OPEN") },
                        label = { Text(Strings.open) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                    FilterChip(
                        selected = uiState.selectedStatus == "IN_PROGRESS",
                        onClick = { viewModel.selectStatus("IN_PROGRESS") },
                        label = { Text(Strings.inProgress) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFA000).copy(alpha = 0.2f)
                        )
                    )
                    FilterChip(
                        selected = uiState.selectedStatus == "RESOLVED",
                        onClick = { viewModel.selectStatus("RESOLVED") },
                        label = { Text(Strings.resolved) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                        )
                    )
                }

                // Tickets List
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.filteredTickets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = Strings.noTickets,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.filteredTickets, key = { it.id }) { ticket ->
                            TicketCard(
                                ticket = ticket,
                                onStatusChange = { showStatusDialog = Pair(ticket.id, ticket.status) },
                                onDelete = { showDeleteDialog = ticket.id }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Ticket Dialog
    if (showAddDialog) {
        AddTicketDialog(
            subscribers = uiState.subscribers,
            onDismiss = { showAddDialog = false },
            onConfirm = { subscriberId, issueType, description ->
                viewModel.addTicket(subscriberId, issueType, description)
                showAddDialog = false
            }
        )
    }

    // Delete Dialog
    showDeleteDialog?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(Strings.delete) },
            text = { Text(Strings.confirmDelete) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTicket(id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(Strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text(Strings.cancel) }
            }
        )
    }

    // Status Change Dialog
    showStatusDialog?.let { (id, currentStatus) ->
        AlertDialog(
            onDismissRequest = { showStatusDialog = null },
            title = { Text(Strings.ticketStatus) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusOption(Strings.open, "OPEN", currentStatus) {
                        viewModel.updateTicketStatus(id, "OPEN")
                        showStatusDialog = null
                    }
                    StatusOption(Strings.inProgress, "IN_PROGRESS", currentStatus) {
                        viewModel.updateTicketStatus(id, "IN_PROGRESS")
                        showStatusDialog = null
                    }
                    StatusOption(Strings.resolved, "RESOLVED", currentStatus) {
                        viewModel.updateTicketStatus(id, "RESOLVED")
                        showStatusDialog = null
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = null }) { Text(Strings.cancel) }
            }
        )
    }
}

@Composable
private fun StatusOption(
    label: String,
    value: String,
    currentValue: String,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (value == currentValue)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = if (value == currentValue) FontWeight.Bold else FontWeight.Normal)
            if (value == currentValue) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun TicketCard(
    ticket: GetAllMaintenanceTickets,
    onStatusChange: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (ticket.status) {
        "OPEN" -> MaterialTheme.colorScheme.error
        "IN_PROGRESS" -> Color(0xFFFFA000)
        "RESOLVED" -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusLabel = when (ticket.status) {
        "OPEN" -> Strings.open
        "IN_PROGRESS" -> Strings.inProgress
        "RESOLVED" -> Strings.resolved
        else -> ticket.status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = when (ticket.issueType) {
                        "Leak" -> Icons.Default.WaterDrop
                        "Broken Meter" -> Icons.Default.Speed
                        "Broken Pipe" -> Icons.Default.Plumbing
                        else -> Icons.Default.Build
                    },
                    contentDescription = null,
                    tint = statusColor
                )
                Column {
                    Text(
                        text = ticket.issueType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    ticket.subscriberName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ticket.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatTicketDate(ticket.reportedDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onStatusChange,
                    label = { Text(statusLabel, color = statusColor) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Circle,
                            contentDescription = null,
                            modifier = Modifier.size(8.dp),
                            tint = statusColor
                        )
                    }
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = Strings.delete,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTicketDialog(
    subscribers: List<GetAllSubscribers>,
    onDismiss: () -> Unit,
    onConfirm: (Long?, String, String?) -> Unit
) {
    var selectedSubscriberId by remember { mutableStateOf<Long?>(null) }
    var issueType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subscriberExpanded by remember { mutableStateOf(false) }

    val issueTypes = listOf(Strings.issueLeak, Strings.issueBrokenMeter, Strings.issueBrokenPipe, Strings.issueOther)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.addTicket) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Subscriber selection (optional)
                ExposedDropdownMenuBox(
                    expanded = subscriberExpanded,
                    onExpandedChange = { subscriberExpanded = it }
                ) {
                    OutlinedTextField(
                        value = subscribers.find { it.id == selectedSubscriberId }?.fullName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("${Strings.members} (${Strings.optional})") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subscriberExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = subscriberExpanded,
                        onDismissRequest = { subscriberExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("— بدون —") },
                            onClick = {
                                selectedSubscriberId = null
                                subscriberExpanded = false
                            }
                        )
                        subscribers.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text("${sub.fullName} (${sub.meterNumber})") },
                                onClick = {
                                    selectedSubscriberId = sub.id
                                    subscriberExpanded = false
                                }
                            )
                        }
                    }
                }

                // Issue Type
                Text(Strings.issueType, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    issueTypes.take(2).forEach { type ->
                        FilterChip(
                            selected = issueType == type,
                            onClick = { issueType = type },
                            label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    issueTypes.drop(2).forEach { type ->
                        FilterChip(
                            selected = issueType == type,
                            onClick = { issueType = type },
                            label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Strings.description) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedSubscriberId, issueType, description.ifBlank { null }) },
                enabled = issueType.isNotBlank()
            ) { Text(Strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
        }
    )
}

private fun formatTicketDate(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val datetime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${datetime.dayOfMonth}/${datetime.monthNumber}/${datetime.year}"
    } catch (e: Exception) {
        "-"
    }
}
