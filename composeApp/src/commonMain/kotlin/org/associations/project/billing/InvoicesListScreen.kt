package org.associations.project.billing

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.associations.project.database.Invoice
import org.associations.project.database.Subscriber
import org.associations.project.ui.Strings
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesListScreen(onNavigateBack: () -> Unit) {
    val viewModel = koinViewModel<InvoicesViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var showPrintDialog by remember { mutableStateOf<InvoiceUiModel?>(null) }
    
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val tabs = listOf(Strings.unpaidInvoices, Strings.paidInvoices, Strings.allInvoices)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    text = Strings.invoices,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Filters Row: Month Selector
                var monthExpanded by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = Strings.monthFilter,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = monthExpanded,
                            onExpandedChange = { monthExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = uiState.selectedMonth?.displayName ?: Strings.allMonths,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = monthExpanded,
                                onDismissRequest = { monthExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(Strings.allMonths) },
                                    onClick = {
                                        viewModel.selectMonth(null)
                                        monthExpanded = false
                                    }
                                )
                                uiState.availableMonths.forEach { month ->
                                    DropdownMenuItem(
                                        text = { Text(month.displayName) },
                                        onClick = {
                                            viewModel.selectMonth(month)
                                            monthExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Tabs
                TabRow(selectedTabIndex = uiState.selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.selectTab(index) },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Invoice List
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val invoices = when (uiState.selectedTab) {
                        0 -> uiState.unpaidInvoices
                        1 -> uiState.paidInvoices
                        else -> uiState.allInvoices
                    }

                    if (invoices.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = Strings.noInvoices,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(invoices, key = { it.id }) { invoice ->
                                InvoiceCard(
                                    invoice = invoice,
                                    onMarkAsPaid = { viewModel.markAsPaid(invoice.id) },
                                    onMarkAsUnpaid = { viewModel.markAsUnpaid(invoice.id) },
                                    onDelete = { showDeleteDialog = invoice.id },
                                    onPrint = { showPrintDialog = invoice }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Print Preview Dialog
    showPrintDialog?.let { model ->
        Dialog(onDismissRequest = { showPrintDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 700.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("معاينة الطباعة", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.Gray)
                            .padding(4.dp)
                    ) {
                        // Reconstruct objects for template
                        val mockInvoice = Invoice(
                            id = model.id,
                            subscriberId = 0,
                            previousReading = model.previousReading,
                            currentReading = model.currentReading,
                            consumption = model.consumption,
                            totalAmount = model.totalAmount,
                            status = model.status,
                            issueDate = model.issueDate,
                            dueDate = model.dueOrPaidDate ?: 0,
                            isPenaltyApplied = 0
                        )
                        val mockSubscriber = Subscriber(
                            id = 0,
                            fullName = model.subscriberName ?: "",
                            phone = null,
                            meterNumber = model.meterNumber ?: "",
                            address = null,
                            zoneId = 0,
                            isActive = 1,
                            createdAt = 0
                        )
                        
                        // We use a Box to contain the template. In a real app we might need scrolling if it's long.
                        InvoiceTemplate(
                            invoice = mockInvoice,
                            subscriber = mockSubscriber,
                            associationName = uiState.associationName,
                            associationAddress = uiState.associationAddress,
                            associationPhone = uiState.associationPhone,
                            printFormat = uiState.printFormat
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(onClick = { 
                            viewModel.printInvoice(model.id)
                            showPrintDialog = null 
                        }) {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("طباعة")
                        }
                        TextButton(onClick = { showPrintDialog = null }) {
                            Text("إغلاق")
                        }
                    }
                }
            }
        }
    }

    // Delete Dialog
    showDeleteDialog?.let { invoiceId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(Strings.delete) },
            text = { Text(Strings.confirmDelete) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteInvoice(invoiceId)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(Strings.cancel)
                }
            }
        )
    }
}

@Composable
fun InvoiceCard(
    invoice: InvoiceUiModel,
    onMarkAsPaid: () -> Unit,
    onMarkAsUnpaid: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit
) {
    val isPaid = invoice.status == "PAID"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invoice.subscriberName ?: "-",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "${Strings.consumption}: ${invoice.consumption} ${Strings.m3}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(invoice.issueDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${invoice.totalAmount} ${Strings.dhs}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    IconButton(onClick = onPrint) {
                        Icon(Icons.Default.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (isPaid) {
                        IconButton(onClick = onMarkAsUnpaid) {
                            Icon(Icons.Default.Undo, contentDescription = "Mark as unpaid", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = onMarkAsPaid) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Mark as paid", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = Strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

fun formatDate(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val datetime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${datetime.dayOfMonth}/${datetime.monthNumber}/${datetime.year}"
    } catch (e: Exception) {
        "-"
    }
}
