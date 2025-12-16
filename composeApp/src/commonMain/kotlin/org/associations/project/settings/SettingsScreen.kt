package org.associations.project.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.associations.project.database.PricingTier
import org.associations.project.database.Zone
import org.associations.project.ui.Strings
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit, onNavigateToActivation: (() -> Unit)? = null) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddZoneDialog by remember { mutableStateOf(false) }
    var showEditZoneDialog by remember { mutableStateOf<Zone?>(null) }
    var showDeleteZoneDialog by remember { mutableStateOf<Zone?>(null) }
    var showAddTranchDialog by remember { mutableStateOf(false) }
    var showEditTranchDialog by remember { mutableStateOf<PricingTier?>(null) }
    var showDeleteTranchDialog by remember { mutableStateOf<PricingTier?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Header
                    item {
                        Text(
                                text = Strings.settings,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                        )
                    }

                    // Zone Management Section
                    item {
                        SettingsSection(
                                title = Strings.zoneManagement,
                                icon = Icons.Default.LocationOn,
                                onAdd = { showAddZoneDialog = true }
                        ) {
                            if (uiState.zones.isEmpty()) {
                                Text(
                                        text = Strings.noZones,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    uiState.zones.forEach { zone ->
                                        ZoneCard(
                                                zone = zone,
                                                onEdit = { showEditZoneDialog = zone },
                                                onDelete = { showDeleteZoneDialog = zone }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Pricing Tiers Section
                    item {
                        SettingsSection(
                                title = Strings.pricingTranches,
                                icon = Icons.Default.AttachMoney,
                                onAdd = { showAddTranchDialog = true }
                        ) {
                            if (uiState.pricingTiers.isEmpty()) {
                                Text(
                                        text = "لا توجد شرائح تسعير",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    uiState.pricingTiers.forEach { tier ->
                                        PricingTierCard(
                                                tier = tier,
                                                onEdit = { showEditTranchDialog = tier },
                                                onDelete = { showDeleteTranchDialog = tier }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Association Details Section
                    item {
                        SettingsSection(title = "بيانات الجمعية", icon = Icons.Default.Info) {
                            Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                EditableSettingsRow(
                                        label = "اسم الجمعية",
                                        value = uiState.associationName,
                                        onEdit = { viewModel.showEditDialog("assocDetails") }
                                )
                                EditableSettingsRow(
                                        label = "العنوان",
                                        value = uiState.associationAddress.ifBlank { "-" },
                                        onEdit = { viewModel.showEditDialog("assocDetails") }
                                )
                                EditableSettingsRow(
                                        label = "الهاتف",
                                        value = uiState.associationPhone.ifBlank { "-" },
                                        onEdit = { viewModel.showEditDialog("assocDetails") }
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                        text = "إعدادات الطباعة",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                                selected = uiState.printFormat == "A4",
                                                onClick = { viewModel.updatePrintFormat("A4") }
                                        )
                                        Text("طابعة كبيرة (A4)")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                                selected = uiState.printFormat == "RECEIPT",
                                                onClick = { viewModel.updatePrintFormat("RECEIPT") }
                                        )
                                        Text("طابعة إيصالات (80mm)")
                                    }
                                }
                            }
                        }
                    }

                    // Billing Settings Section
                    item {
                        SettingsSection(
                                title = Strings.billingSettings,
                                icon = Icons.Default.Receipt
                        ) {
                            Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                EditableSettingsRow(
                                        label = Strings.lateFeePercent,
                                        value = "${uiState.lateFeePercent} درهم",
                                        onEdit = { viewModel.showEditDialog("lateFee") }
                                )
                                EditableSettingsRow(
                                        label = Strings.monthlyFixedFee,
                                        value = "${uiState.monthlyFixedFee} ${Strings.dhs}",
                                        onEdit = { viewModel.showEditDialog("monthlyFee") }
                                )
                                EditableSettingsRow(
                                        label = Strings.gracePeriod,
                                        value = "${uiState.gracePeriodDays}",
                                        onEdit = { viewModel.showEditDialog("gracePeriod") }
                                )
                                EditableSettingsRow(
                                        label = Strings.dueDateDays,
                                        value = "${uiState.dueDateDays}",
                                        onEdit = { viewModel.showEditDialog("dueDate") }
                                )
                            }
                        }
                    }

                    // License Activation Section
                    item {
                        SettingsSection(title = "تفعيل التطبيق", icon = Icons.Default.VpnKey) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Button(
                                        onClick = { onNavigateToActivation?.invoke() },
                                        modifier = Modifier.fillMaxWidth()
                                ) { Text("تفعيل الرخصة") }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Zone Dialog
    if (showAddZoneDialog) {
        ZoneDialog(
                title = Strings.addZone,
                onDismiss = { showAddZoneDialog = false },
                onConfirm = { name, description ->
                    viewModel.addZone(name, description)
                    showAddZoneDialog = false
                }
        )
    }

    // Edit Zone Dialog
    showEditZoneDialog?.let { zone ->
        ZoneDialog(
                title = Strings.editZone,
                initialName = zone.name,
                initialDescription = zone.description,
                onDismiss = { showEditZoneDialog = null },
                onConfirm = { name, description ->
                    viewModel.updateZone(zone.id, name, description)
                    showEditZoneDialog = null
                }
        )
    }

    // Delete Zone Confirmation
    showDeleteZoneDialog?.let { zone ->
        AlertDialog(
                onDismissRequest = { showDeleteZoneDialog = null },
                title = { Text(Strings.deleteZone) },
                text = { Text("${Strings.confirmDelete}\n${zone.name}") },
                confirmButton = {
                    TextButton(
                            onClick = {
                                viewModel.deleteZone(zone.id)
                                showDeleteZoneDialog = null
                            },
                            colors =
                                    ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                    )
                    ) { Text(Strings.delete) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteZoneDialog = null }) { Text(Strings.cancel) }
                }
        )
    }

    // Add Pricing Tier Dialog
    if (showAddTranchDialog) {
        PricingTierDialog(
                title = Strings.addTranche,
                onDismiss = { showAddTranchDialog = false },
                onConfirm = { min, max, price ->
                    viewModel.addPricingTier(min, max, price)
                    showAddTranchDialog = false
                }
        )
    }

    // Edit Pricing Tier Dialog
    showEditTranchDialog?.let { tier ->
        PricingTierDialog(
                title = Strings.editTranche,
                initialMin = tier.minUsage,
                initialMax = tier.maxUsage,
                initialPrice = tier.pricePerUnit,
                onDismiss = { showEditTranchDialog = null },
                onConfirm = { min, max, price ->
                    viewModel.updatePricingTier(tier.id, min, max, price)
                    showEditTranchDialog = null
                }
        )
    }

    // Delete Pricing Tier Confirmation
    showDeleteTranchDialog?.let { tier ->
        AlertDialog(
                onDismissRequest = { showDeleteTranchDialog = null },
                title = { Text(Strings.delete) },
                text = {
                    Text(
                            "${Strings.confirmDelete}\n${tier.minUsage} - ${tier.maxUsage} ${Strings.m3}"
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                viewModel.deletePricingTier(tier.id)
                                showDeleteTranchDialog = null
                            },
                            colors =
                                    ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                    )
                    ) { Text(Strings.delete) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteTranchDialog = null }) { Text(Strings.cancel) }
                }
        )
    }

    // Billing Settings Dialogs
    when (uiState.editingField) {
        "lateFee" ->
                BillingSettingDialog(
                        title = Strings.lateFeePercent,
                        label = Strings.lateFeePercent,
                        currentValue = uiState.lateFeePercent.toString(),
                        suffix = "درهم",
                        isPercent = true,
                        onDismiss = { viewModel.dismissEditDialog() },
                        onConfirm = { viewModel.updateLateFee(it) }
                )
        "monthlyFee" ->
                BillingSettingDialog(
                        title = Strings.monthlyFixedFee,
                        label = Strings.monthlyFixedFee,
                        currentValue = uiState.monthlyFixedFee.toString(),
                        suffix = Strings.dhs,
                        isPercent = true,
                        onDismiss = { viewModel.dismissEditDialog() },
                        onConfirm = { viewModel.updateMonthlyFee(it) }
                )
        "gracePeriod" ->
                BillingSettingDialog(
                        title = Strings.gracePeriod,
                        label = Strings.gracePeriod,
                        currentValue = uiState.gracePeriodDays.toString(),
                        onDismiss = { viewModel.dismissEditDialog() },
                        onConfirm = { viewModel.updateGracePeriod(it) }
                )
        "dueDate" ->
                BillingSettingDialog(
                        title = Strings.dueDateDays,
                        label = Strings.dueDateDays,
                        currentValue = uiState.dueDateDays.toString(),
                        onDismiss = { viewModel.dismissEditDialog() },
                        onConfirm = { viewModel.updateDueDate(it) }
                )
        "assocDetails" ->
                AssociationDetailsDialog(
                        initialName = uiState.associationName,
                        initialAddress = uiState.associationAddress,
                        initialPhone = uiState.associationPhone,
                        onDismiss = { viewModel.dismissEditDialog() },
                        onConfirm = { name, addr, phone ->
                            viewModel.updateAssociationDetails(name, addr, phone)
                        }
                )
    }
}

@Composable
fun AssociationDetailsDialog(
        initialName: String,
        initialAddress: String,
        initialPhone: String,
        onDismiss: () -> Unit,
        onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var address by remember { mutableStateOf(initialAddress) }
    var phone by remember { mutableStateOf(initialPhone) }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("تحديث بيانات الجمعية") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("اسم الجمعية") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                    )
                    OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("العنوان") },
                            modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("الهاتف") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = { onConfirm(name, address, phone) }, enabled = name.isNotBlank()) {
                    Text(Strings.save)
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

@Composable
fun SettingsSection(
        title: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onAdd: (() -> Unit)? = null,
        content: @Composable () -> Unit
) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                    )
                }
                onAdd?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Add, contentDescription = Strings.add)
                    }
                }
            }
            HorizontalDivider()
            content()
        }
    }
}

@Composable
fun ZoneCard(zone: Zone, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = zone.name, style = MaterialTheme.typography.bodyLarge)
            zone.description?.let {
                Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = Strings.edit)
            }
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

@Composable
fun PricingTierCard(tier: PricingTier, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = "${tier.minUsage} - ${tier.maxUsage} ${Strings.m3}",
                    style = MaterialTheme.typography.bodyLarge
            )
            Text(
                    text = "${tier.pricePerUnit} ${Strings.dhs} / ${Strings.m3}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
            )
        }
        Row {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = Strings.edit)
            }
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

@Composable
fun SettingsInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ZoneDialog(
        title: String,
        initialName: String = "",
        initialDescription: String? = null,
        onDismiss: () -> Unit,
        onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription ?: "") }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(Strings.zoneName) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                    )
                    OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text(Strings.zoneDescription) },
                            modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                        onClick = { onConfirm(name, description.ifBlank { null }) },
                        enabled = name.isNotBlank()
                ) { Text(Strings.save) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

@Composable
fun PricingTierDialog(
        title: String,
        initialMin: Long = 0,
        initialMax: Long = 0,
        initialPrice: Double = 0.0,
        onDismiss: () -> Unit,
        onConfirm: (Long, Long, Double) -> Unit
) {
    var minUsage by remember { mutableStateOf(initialMin.toString()) }
    var maxUsage by remember { mutableStateOf(initialMax.toString()) }
    var price by remember { mutableStateOf(initialPrice.toString()) }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                            value = minUsage,
                            onValueChange = { minUsage = it.filter { c -> c.isDigit() } },
                            label = { Text(Strings.minUsage) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                    )
                    OutlinedTextField(
                            value = maxUsage,
                            onValueChange = { maxUsage = it.filter { c -> c.isDigit() } },
                            label = { Text(Strings.maxUsage) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                    )
                    OutlinedTextField(
                            value = price,
                            onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(Strings.pricePerUnit) },
                            suffix = { Text(Strings.dhs) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                        onClick = {
                            val min = minUsage.toLongOrNull() ?: 0
                            val max = maxUsage.toLongOrNull() ?: 0
                            val p = price.toDoubleOrNull() ?: 0.0
                            onConfirm(min, max, p)
                        },
                        enabled =
                                minUsage.isNotBlank() && maxUsage.isNotBlank() && price.isNotBlank()
                ) { Text(Strings.save) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

@Composable
fun EditableSettingsRow(label: String, value: String, onEdit: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = Strings.edit,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BillingSettingDialog(
        title: String,
        label: String,
        currentValue: String,
        suffix: String = "",
        isPercent: Boolean = false,
        onDismiss: () -> Unit,
        onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(currentValue) }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                OutlinedTextField(
                        value = value,
                        onValueChange = { newValue ->
                            value = newValue.filter { c -> c.isDigit() || (isPercent && c == '.') }
                        },
                        label = { Text(label) },
                        suffix = { if (suffix.isNotBlank()) Text(suffix) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) {
                    Text(Strings.save)
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}
