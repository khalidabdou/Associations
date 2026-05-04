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
                    .padding(horizontal = 8.dp)
            ) {
                // Compact Month Filter with Icon
                var monthExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Month Selector (Compact)
                    ExposedDropdownMenuBox(
                        expanded = monthExpanded,
                        onExpandedChange = { monthExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedMonth?.displayName ?: Strings.allMonths,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                            modifier = Modifier.widthIn(max = 180.dp).menuAnchor(),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true,
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(Strings.allMonths, style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    viewModel.selectMonth(null)
                                    monthExpanded = false
                                }
                            )
                            uiState.availableMonths.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month.displayName, style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        viewModel.selectMonth(month)
                                        monthExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Compact Tabs
                TabRow(
                    selectedTabIndex = uiState.selectedTab,
                    modifier = Modifier.height(40.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.selectTab(index) },
                            text = { 
                                Text(
                                    title, 
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                ) 
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(invoices, key = { it.id }) { invoice ->
                                CompactInvoiceItem(
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
                            printFormat = uiState.printFormat,
                            logoPath = uiState.logoPath
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
                        OutlinedButton(onClick = {
                            viewModel.shareInvoice(model.id)
                            showPrintDialog = null
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مشاركة")
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
fun CompactInvoiceItem(
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
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Name and details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invoice.subscriberName ?: "-",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${formatCompactDate(invoice.issueDate)} · ${invoice.consumption}${Strings.m3}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Right: Amount and actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${invoice.totalAmount.toInt()}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = Strings.dhs,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Compact action icons
                IconButton(
                    onClick = onPrint,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = "Print",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isPaid) {
                    IconButton(
                        onClick = onMarkAsUnpaid,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Mark as unpaid",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onMarkAsPaid,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Mark as paid",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = Strings.delete,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Compact date formatter for mobile
private fun formatCompactDate(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${localDate.dayOfMonth}/${localDate.monthNumber}/${localDate.year}"
}

