package org.associations.project.treasury

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.associations.project.database.TransactionTable
import org.associations.project.ui.Strings
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasuryScreen(onNavigateBack: () -> Unit) {
    val viewModel = koinViewModel<TreasuryViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showIncomeDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }

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
            topBar = {
                TopAppBar(
                    title = { Text(Strings.treasury) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        // Add Income Icon
                        IconButton(onClick = { showIncomeDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = Strings.addIncome,
                                tint = Color(0xFF4CAF50)
                            )
                        }
                        // Add Expense Icon
                        IconButton(onClick = { showExpenseDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.RemoveCircle,
                                contentDescription = Strings.addExpense,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Compact Balance Summary
                CompactBalanceCard(
                    totalIncome = uiState.totalIncome,
                    totalExpenses = uiState.totalExpenses,
                    balance = uiState.balance
                )

                // Compact Month Filter
                var monthExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                // Transactions List
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.transactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = Strings.noTransactions,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.transactions, key = { it.id }) { transaction ->
                            CompactTransactionItem(
                                transaction = transaction,
                                onEdit = { viewModel.showEditDialog(transaction) },
                                onDelete = { showDeleteDialog = transaction.id }
                            )
                        }
                    }
                }
            }
        }
    }

    // Income Dialog
    if (showIncomeDialog) {
        TransactionDialog(
            title = Strings.addIncome,
            isIncome = true,
            onDismiss = { showIncomeDialog = false },
            onConfirm = { amount, category, description ->
                viewModel.addIncome(amount, category, description)
                showIncomeDialog = false
            }
        )
    }

    // Expense Dialog
    if (showExpenseDialog) {
        TransactionDialog(
            title = Strings.addExpense,
            isIncome = false,
            onDismiss = { showExpenseDialog = false },
            onConfirm = { amount, category, description ->
                viewModel.addExpense(amount, category, description)
                showExpenseDialog = false
            }
        )
    }

    // Edit Dialog
    uiState.editingTransaction?.let { transaction ->
        TransactionDialog(
            title = Strings.edit,
            isIncome = transaction.type == "INCOME",
            initialAmount = transaction.amount,
            initialCategory = transaction.category,
            initialDescription = transaction.description,
            onDismiss = { viewModel.dismissEditDialog() },
            onConfirm = { amount, category, description ->
                viewModel.updateTransaction(transaction.id, transaction.type, category, amount, description)
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
                        viewModel.deleteTransaction(id)
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
fun CompactBalanceCard(
    totalIncome: Double,
    totalExpenses: Double,
    balance: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Income
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = Strings.income,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${totalIncome.toInt()}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
            // Expenses
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = Strings.expenses,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${totalExpenses.toInt()}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            // Balance
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = Strings.balance,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${balance.toInt()}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (balance >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun CompactTransactionItem(
    transaction: TransactionTable,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == "INCOME"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isIncome)
                Color(0xFF4CAF50).copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
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
            // Left: Category and details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                val descText = if (transaction.description != null) 
                    "${transaction.description} · " 
                else ""
                Text(
                    text = "$descText${formatCompactTransactionDate(transaction.date)}",
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
                    text = "${if (isIncome) "+" else "-"}${transaction.amount.toInt()}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
                Text(
                    text = Strings.dhs,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Compact action icons
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = Strings.edit,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
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
private fun formatCompactTransactionDate(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${localDate.dayOfMonth}/${localDate.monthNumber}/${localDate.year}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDialog(
    title: String,
    isIncome: Boolean,
    initialAmount: Double? = null,
    initialCategory: String? = null,
    initialDescription: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String?) -> Unit
) {
    var amount by remember { mutableStateOf(initialAmount?.toString() ?: "") }
    var category by remember { mutableStateOf(initialCategory ?: "") }
    var description by remember { mutableStateOf(initialDescription ?: "") }

    val incomeCategories = listOf("دفع فواتير", "رسوم اشتراك", "غرامات", "أخرى")
    val expenseCategories = listOf("صيانة", "رواتب", "معدات", "كهرباء", "أخرى")
    val categories = if (isIncome) incomeCategories else expenseCategories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(Strings.amount) },
                    suffix = { Text(Strings.dhs) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Category selector
                Text(Strings.category, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.take(2).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.drop(2).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Strings.description) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    onConfirm(amountValue, category.ifBlank { "أخرى" }, description.ifBlank { null })
                },
                enabled = amount.isNotBlank()
            ) {
                Text(Strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel)
            }
        }
    )
}



