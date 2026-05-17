package org.associations.project.billing

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.random.Random
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
    var showMarkPaidDialog by remember { mutableStateOf<Long?>(null) }
    var showMarkUnpaidDialog by remember { mutableStateOf<Long?>(null) }
    var showPrintCopiesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val tabs = listOf(Strings.unpaidInvoices, Strings.paidInvoices, Strings.allInvoices)
    val inSelectionMode = uiState.selectedIds.isNotEmpty()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (inSelectionMode) {
                    SelectionTopBar(
                        count = uiState.selectedIds.size,
                        onSelectAll = { viewModel.selectAllVisible() },
                        onClear = { viewModel.clearSelection() },
                        onPrint = { showPrintCopiesDialog = true }
                    )
                }
            }
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
                                    inSelectionMode = inSelectionMode,
                                    isSelected = uiState.selectedIds.contains(invoice.id),
                                    onToggleSelect = { viewModel.toggleSelection(invoice.id) },
                                    onMarkAsPaid = { showMarkPaidDialog = invoice.id },
                                    onMarkAsUnpaid = { showMarkUnpaidDialog = invoice.id },
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
                    .padding(12.dp)
                    .heightIn(max = 720.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                val vScroll = rememberScrollState()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(vScroll)
                ) {
                    Text("معاينة الطباعة", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))

                    val hScroll = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 520.dp)
                            .border(1.dp, Color.Gray)
                            .padding(4.dp)
                            .horizontalScroll(hScroll)
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
                            isPenaltyApplied = model.isPenaltyApplied
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
                        InvoiceTemplate(
                            invoice = mockInvoice,
                            subscriber = mockSubscriber,
                            associationName = uiState.associationName,
                            associationAddress = uiState.associationAddress,
                            associationPhone = uiState.associationPhone,
                            printFormat = uiState.printFormat,
                            logoPath = uiState.logoPath,
                            lateFeeAmount = uiState.lateFeeAmount,
                            monthlyFixedFee = uiState.monthlyFixedFee
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(onClick = {
                            viewModel.printInvoice(model.id)
                            showPrintDialog = null
                        }) {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Strings.print)
                        }
                        OutlinedButton(onClick = {
                            viewModel.shareInvoice(model.id)
                            showPrintDialog = null
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Strings.share)
                        }
                        TextButton(onClick = { showPrintDialog = null }) {
                            Text(Strings.close)
                        }
                    }
                }
            }
        }
    }

    // Print Copies Dialog
    if (showPrintCopiesDialog) {
        var copies by remember { mutableStateOf(1) }
        AlertDialog(
            onDismissRequest = { showPrintCopiesDialog = false },
            icon = { Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(Strings.printCopiesTitle) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Strings.printCountInvoices
                            .replace("{count}", "${uiState.selectedIds.size}")
                            .replace("{copies}", "$copies"),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FilledIconButton(
                            onClick = { copies = (copies - 1).coerceAtLeast(1) },
                            enabled = copies > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null)
                        }
                        Text(
                            text = "$copies",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledIconButton(
                            onClick = { copies = (copies + 1).coerceAtMost(20) },
                            enabled = copies < 20
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Strings.copies,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.printSelectedInvoices(copies)
                    showPrintCopiesDialog = false
                }) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.print)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrintCopiesDialog = false }) {
                    Text(Strings.cancel)
                }
            }
        )
    }

    // Mark-as-paid Yes/No Dialog
    showMarkPaidDialog?.let { invoiceId ->
        AlertDialog(
            onDismissRequest = { showMarkPaidDialog = null },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(Strings.markPaidTitle) },
            text = { Text(Strings.markPaidQuestion) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markAsPaid(invoiceId)
                    showMarkPaidDialog = null
                }) {
                    Text(Strings.yes)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkPaidDialog = null }) {
                    Text(Strings.no)
                }
            }
        )
    }

    // Mark-as-unpaid Yes/No Dialog
    showMarkUnpaidDialog?.let { invoiceId ->
        AlertDialog(
            onDismissRequest = { showMarkUnpaidDialog = null },
            icon = { Icon(Icons.Default.Undo, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(Strings.markUnpaidTitle) },
            text = { Text(Strings.markUnpaidQuestion) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markAsUnpaid(invoiceId)
                    showMarkUnpaidDialog = null
                }) {
                    Text(Strings.yes)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkUnpaidDialog = null }) {
                    Text(Strings.no)
                }
            }
        )
    }

    // Delete Dialog with code confirmation
    showDeleteDialog?.let { invoiceId ->
        DeleteWithCodeDialog(
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                viewModel.deleteInvoice(invoiceId)
                showDeleteDialog = null
            }
        )
    }
}

@Composable
private fun DeleteWithCodeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val expectedCode = remember { Random.nextInt(1000, 9999).toString() }
    var entered by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(Strings.deleteInvoiceTitle) },
        text = {
            Column {
                Text(Strings.deleteInvoiceMessage)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = expectedCode,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = entered,
                    onValueChange = {
                        entered = it.filter { c -> c.isDigit() }.take(4)
                        showError = false
                    },
                    label = { Text(Strings.deleteCodeLabel) },
                    singleLine = true,
                    isError = showError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Text(
                        Strings.deleteCodeMismatch,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = entered.length == 4,
                onClick = {
                    if (entered == expectedCode) onConfirm() else showError = true
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(Strings.delete)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    count: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onPrint: () -> Unit
) {
    TopAppBar(
        title = { Text("$count ${Strings.itemsSelected}") },
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = Strings.clearSelection)
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = Strings.selectAll)
            }
            IconButton(onClick = onPrint) {
                Icon(Icons.Default.Print, contentDescription = Strings.printSelected)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactInvoiceItem(
    invoice: InvoiceUiModel,
    inSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onMarkAsPaid: () -> Unit,
    onMarkAsUnpaid: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit
) {
    val isPaid = invoice.status == "PAID"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = onToggleSelect,
                onClick = { if (inSelectionMode) onToggleSelect() }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isPaid -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (inSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() }
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
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
                if (isPaid) {
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

