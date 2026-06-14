package org.associations.project.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import org.associations.project.utils.MonthYear
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

    // Re-verify activation against Supabase every time the settings screen is entered,
    // in case the admin flipped `is_active` to false remotely.
    LaunchedEffect(Unit) { viewModel.refreshActivationStatus() }

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

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                                selected = uiState.printFormat == "A4",
                                                onClick = { viewModel.updatePrintFormat("A4") }
                                        )
                                        Text("طابعة كبيرة (A4)")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                                selected = uiState.printFormat == "A5",
                                                onClick = { viewModel.updatePrintFormat("A5") }
                                        )
                                        Text("طابعة متوسطة (A5)")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                                selected = uiState.printFormat == "POS",
                                                onClick = { viewModel.updatePrintFormat("POS") }
                                        )
                                        Text("طابعة حرارية (80mm) POS")
                                    }
                                    // Test print button for POS
                                    if (uiState.printFormat == "POS") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        // Connection type toggle
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilterChip(
                                                selected = uiState.printerConnectionType == "BLUETOOTH",
                                                onClick = { viewModel.setPrinterConnectionType("BLUETOOTH") },
                                                label = { Text("بلوتوث") },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                                                }
                                            )
                                            FilterChip(
                                                selected = uiState.printerConnectionType == "USB",
                                                onClick = { viewModel.setPrinterConnectionType("USB") },
                                                label = { Text("USB") },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(16.dp))
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = {
                                                if (uiState.printerConnectionType == "USB") {
                                                    viewModel.showUsbTestPrint()
                                                } else {
                                                    viewModel.showBluetoothTestPrint()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                                            )
                                        ) {
                                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                if (uiState.printerConnectionType == "USB") "اختبار الطباعة عبر USB"
                                                else "اختبار الطباعة عبر البلوتوث"
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                // Logo Selection
                                Text(
                                        text = "شعار الجمعية",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                )

                                val currentLogoPath = uiState.logoPath
                                val imagePickerLauncher = org.associations.project.utils.rememberImagePickerLauncher { path ->
                                    if (path != null) {
                                        viewModel.updateLogo(path)
                                    }
                                }
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (currentLogoPath != null) {
                                        Text(
                                                text = currentLogoPath.substringAfterLast("/"),
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1
                                        )
                                        TextButton(
                                                onClick = { viewModel.updateLogo(null) },
                                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("إزالة")
                                        }
                                    } else {
                                        Text(
                                                text = "لم يتم اختيار شعار",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Button(
                                            onClick = { imagePickerLauncher() }
                                    ) {
                                        Text(if (currentLogoPath != null) "تغيير" else "اختيار")
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

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                // Past Month Editing Toggle
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                            text = "السماح بتعديل الأشهر السابقة",
                                            style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(
                                            checked = uiState.allowPastMonthEditing,
                                            onCheckedChange = {
                                                viewModel.updateAllowPastMonthEditing(it)
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // License Activation Section
                    item {
                        SettingsSection(title = "تفعيل التطبيق", icon = Icons.Default.VpnKey) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (uiState.isActivated) {
                                    // Show activation info when activated
                                    Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                text = "التطبيق مفعل",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    // Show activate button when not activated
                                    Button(
                                            onClick = { onNavigateToActivation?.invoke() },
                                            modifier = Modifier.fillMaxWidth()
                                    ) { Text("تفعيل الرخصة") }
                                }
                            }
                        }
                    }

                    // Backup & Data Section
                    item {
                        val backupLauncher =
                                org.associations.project.utils.rememberBackupLauncher(
                                        suggestedFileName = viewModel.suggestedBackupFileName(),
                                        onExport = { out -> viewModel.exportToStream(out) },
                                        onImport = { input -> viewModel.importFromStream(input) },
                                        onMessage = { msg -> viewModel.postMessage(msg) }
                                )
                        SettingsSection(
                                title = "النسخ الاحتياطي والبيانات",
                                icon = Icons.Default.Storage
                        ) {
                            Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Button(
                                            onClick = { backupLauncher.export() },
                                            modifier = Modifier.weight(1f)
                                    ) { Text("تصدير البيانات") }
                                    Button(
                                            onClick = { backupLauncher.import() },
                                            modifier = Modifier.weight(1f),
                                            colors =
                                                    ButtonDefaults.buttonColors(
                                                            containerColor =
                                                                    MaterialTheme.colorScheme
                                                                            .secondary
                                                    )
                                    ) { Text("استيراد البيانات") }
                                }
                                Button(
                                        onClick = { viewModel.showClearDataDialog() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.error
                                                )
                                ) { Text("محو جميع البيانات") }
                            }
                        }
                    }

                    // Monthly Report Section
                    item {
                        val reportExportLauncher = org.associations.project.utils.rememberReportExportLauncher(
                            suggestedFileName = viewModel.suggestedReportFileName(),
                            onExport = { out -> viewModel.exportMonthlyReport(out) },
                            onMessage = { msg -> viewModel.postMessage(msg) }
                        )
                        val csvExportLauncher = org.associations.project.utils.rememberCsvExportLauncher(
                            suggestedFileName = viewModel.suggestedCsvFileName(),
                            onExport = { out -> viewModel.exportMonthlyReportCsv(out) },
                            onMessage = { msg -> viewModel.postMessage(msg) }
                        )
                        SettingsSection(
                                title = "التقرير الشهري",
                                icon = Icons.Default.Print
                        ) {
                            Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Month selector
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { viewModel.previousReportMonth() }) {
                                        Icon(
                                                imageVector = Icons.Default.KeyboardArrowRight,
                                                contentDescription = "الشهر السابق"
                                        )
                                    }
                                    Text(
                                            text = uiState.reportMonth.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                            onClick = { viewModel.nextReportMonth() },
                                            enabled = uiState.reportMonth != MonthYear.current()
                                    ) {
                                        Icon(
                                                imageVector = Icons.Default.KeyboardArrowLeft,
                                                contentDescription = "الشهر التالي"
                                        )
                                    }
                                }

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                            onClick = { viewModel.printMonthlyReport() },
                                            modifier = Modifier.weight(1f),
                                            enabled = !uiState.isPrintingReport
                                    ) {
                                        if (uiState.isPrintingReport) {
                                            CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text("طباعة")
                                    }
                                    Button(
                                            onClick = { reportExportLauncher.export() },
                                            modifier = Modifier.weight(1f),
                                            enabled = !uiState.isExportingReport,
                                            colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary
                                            )
                                    ) {
                                        if (uiState.isExportingReport) {
                                            CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onSecondary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text("تصدير PDF")
                                    }
                                    Button(
                                            onClick = { csvExportLauncher.export() },
                                            modifier = Modifier.weight(1f),
                                            enabled = !uiState.isExportingCsv,
                                            colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.tertiary
                                            )
                                    ) {
                                        if (uiState.isExportingCsv) {
                                            CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onTertiary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text("تصدير CSV")
                                    }
                                }
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
        "clearData" -> {
            uiState.confirmationCode?.let { code ->
                ClearDataDialog(
                        code = code,
                        enteredCode = uiState.userEnteredCode,
                        onCodeChange = { viewModel.updateEnteredCode(it) },
                        onDismiss = { viewModel.dismissEditDialog() },
                        onConfirm = { viewModel.confirmClearData() }
                )
            }
        }
    }

    // Connection Type Picker Dialog (generic — shows either Bluetooth or USB list)

    // Bluetooth Test Print Picker Dialog
    if (uiState.showBluetoothTestDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelBluetoothTestPrint() },
            icon = { Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("اختيار طابعة بلوتوث") },
            text = {
                if (uiState.bluetoothPickerLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("جار البحث عن الطابعات...")
                    }
                } else if (uiState.bluetoothPrinters.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "لا توجد طابعات بلوتوث مقترنة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "تأكد من تفعيل البلوتوث واقتران الطابعة من إعدادات الجهاز",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(uiState.bluetoothPrinters) { printer ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { viewModel.selectBluetoothPrinterForTest(printer.address) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Print,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            printer.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            printer.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.cancelBluetoothTestPrint() }) {
                    Text(Strings.cancel)
                }
            }
        )
    }

    // USB Test Print Picker Dialog
    if (uiState.showUsbTestDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelUsbTestPrint() },
            icon = { Icon(Icons.Default.Usb, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("اختيار طابعة USB") },
            text = {
                if (uiState.usbPickerLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("جار البحث عن الطابعات...")
                    }
                } else if (uiState.usbPrinters.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "لا توجد طابعات USB متصلة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "تأكد من توصيل كابل USB بالطابعة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(uiState.usbPrinters) { printer ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { viewModel.selectUsbPrinterForTest(printer.deviceId) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Usb,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            printer.deviceName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "VID: ${printer.vendorId.toString(16)} PID: ${printer.productId.toString(16)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.cancelUsbTestPrint() }) {
                    Text(Strings.cancel)
                }
            }
        )
    }
}

@Composable
fun ClearDataDialog(
        code: String,
        enteredCode: String,
        onCodeChange: (String) -> Unit,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
) {
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("تأكيد مسح البيانات", color = MaterialTheme.colorScheme.error) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                            "هل أنت متأكد من رغبتك في مسح جميع البيانات؟ لا يمكن التراجع عن هذا الإجراء."
                    )
                    Text("للتأكيد، يرجى إدخال الرمز التالي: $code", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                            value = enteredCode,
                            onValueChange = {
                                if (it.length <= 4 && it.all { c -> c.isDigit() }) onCodeChange(it)
                            },
                            label = { Text("رمز التأكيد") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                        onClick = onConfirm,
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                ),
                        enabled = enteredCode.length == 4
                ) { Text("مسح البيانات") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
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
