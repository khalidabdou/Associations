package org.associations.project.meter

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.associations.project.ui.Strings
import org.associations.project.utils.MonthYear
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterReadingScreen(onNavigateBack: () -> Unit) {
        val viewModel = koinViewModel<MeterReadingViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        var filterMenuExpanded by remember { mutableStateOf(false) }
        var zoneExpanded by remember { mutableStateOf(false) }
        var yearExpanded by remember { mutableStateOf(false) }
        var monthExpanded by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var editDialogEntry by remember { mutableStateOf<MeterReadingEntry?>(null) }
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
                                Surface(
                                        shadowElevation = 2.dp,
                                        color = MaterialTheme.colorScheme.surface
                                ) {
                                        Column(
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 16.dp,
                                                                vertical = 8.dp
                                                        )
                                        ) {
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically,
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(8.dp)
                                                ) {
                                                        OutlinedTextField(
                                                                value = searchQuery,
                                                                onValueChange = {
                                                                        searchQuery = it
                                                                        viewModel.setSearchQuery(it)
                                                                },
                                                                modifier =
                                                                        Modifier.weight(1f)
                                                                                .heightIn(
                                                                                        min = 48.dp
                                                                                ),
                                                                placeholder = {
                                                                        Text(
                                                                                Strings.searchMembers,
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyMedium
                                                                        )
                                                                },
                                                                leadingIcon = {
                                                                        Icon(
                                                                                Icons.Default
                                                                                        .Search,
                                                                                contentDescription =
                                                                                        null,
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                20.dp
                                                                                        )
                                                                        )
                                                                },
                                                                trailingIcon = {
                                                                        if (searchQuery.isNotEmpty()
                                                                        ) {
                                                                                IconButton(
                                                                                        onClick = {
                                                                                                searchQuery =
                                                                                                        ""
                                                                                                viewModel
                                                                                                        .setSearchQuery(
                                                                                                                ""
                                                                                                        )
                                                                                        }
                                                                                ) {
                                                                                        Icon(
                                                                                                Icons.Default
                                                                                                        .Clear,
                                                                                                null,
                                                                                                modifier =
                                                                                                        Modifier.size(
                                                                                                                16.dp
                                                                                                        )
                                                                                        )
                                                                                }
                                                                        }
                                                                },
                                                                singleLine = true,
                                                                shape = MaterialTheme.shapes.medium
                                                        )

                                                        Box {
                                                                IconButton(
                                                                        onClick = {
                                                                                filterMenuExpanded =
                                                                                        true
                                                                        },
                                                                        colors =
                                                                                IconButtonDefaults
                                                                                        .filledIconButtonColors(
                                                                                                containerColor =
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .secondaryContainer
                                                                                        )
                                                                ) {
                                                                        Icon(
                                                                                Icons.Default
                                                                                        .FilterList,
                                                                                contentDescription =
                                                                                        Strings.filter
                                                                        )
                                                                }

                                                                DropdownMenu(
                                                                        expanded =
                                                                                filterMenuExpanded,
                                                                        onDismissRequest = {
                                                                                filterMenuExpanded =
                                                                                        false
                                                                        }
                                                                ) {
                                                                        // Zone Selection
                                                                        DropdownMenuItem(
                                                                                text = {
                                                                                        Row(
                                                                                                verticalAlignment =
                                                                                                        Alignment
                                                                                                                .CenterVertically,
                                                                                                horizontalArrangement =
                                                                                                        Arrangement
                                                                                                                .SpaceBetween,
                                                                                                modifier =
                                                                                                        Modifier.fillMaxWidth()
                                                                                        ) {
                                                                                                Text(
                                                                                                        Strings.zone
                                                                                                )
                                                                                                Text(
                                                                                                        uiState.zones
                                                                                                                .find {
                                                                                                                        it.id ==
                                                                                                                                uiState.selectedZoneId
                                                                                                                }
                                                                                                                ?.name
                                                                                                                ?: "",
                                                                                                        style =
                                                                                                                MaterialTheme
                                                                                                                        .typography
                                                                                                                        .labelMedium,
                                                                                                        color =
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .primary
                                                                                                )
                                                                                        }
                                                                                },
                                                                                onClick = {
                                                                                        zoneExpanded =
                                                                                                true
                                                                                },
                                                                                leadingIcon = {
                                                                                        Icon(
                                                                                                Icons.Default
                                                                                                        .LocationOn,
                                                                                                null,
                                                                                                Modifier.size(
                                                                                                        18.dp
                                                                                                )
                                                                                        )
                                                                                }
                                                                        )

                                                                        // Year Selection
                                                                        DropdownMenuItem(
                                                                                text = {
                                                                                        Row(
                                                                                                verticalAlignment =
                                                                                                        Alignment
                                                                                                                .CenterVertically,
                                                                                                horizontalArrangement =
                                                                                                        Arrangement
                                                                                                                .SpaceBetween,
                                                                                                modifier =
                                                                                                        Modifier.fillMaxWidth()
                                                                                        ) {
                                                                                                Text(
                                                                                                        "السنة"
                                                                                                )
                                                                                                Text(
                                                                                                        uiState.selectedYear
                                                                                                                .toString(),
                                                                                                        style =
                                                                                                                MaterialTheme
                                                                                                                        .typography
                                                                                                                        .labelMedium,
                                                                                                        color =
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .primary
                                                                                                )
                                                                                        }
                                                                                },
                                                                                onClick = {
                                                                                        yearExpanded =
                                                                                                true
                                                                                },
                                                                                leadingIcon = {
                                                                                        Icon(
                                                                                                Icons.Default
                                                                                                        .CalendarToday,
                                                                                                null,
                                                                                                Modifier.size(
                                                                                                        18.dp
                                                                                                )
                                                                                        )
                                                                                }
                                                                        )

                                                                        // Month Selection
                                                                        DropdownMenuItem(
                                                                                text = {
                                                                                        Row(
                                                                                                verticalAlignment =
                                                                                                        Alignment
                                                                                                                .CenterVertically,
                                                                                                horizontalArrangement =
                                                                                                        Arrangement
                                                                                                                .SpaceBetween,
                                                                                                modifier =
                                                                                                        Modifier.fillMaxWidth()
                                                                                        ) {
                                                                                                Text(
                                                                                                        "الشهر"
                                                                                                )
                                                                                                Text(
                                                                                                        uiState.selectedMonth
                                                                                                                .displayName,
                                                                                                        style =
                                                                                                                MaterialTheme
                                                                                                                        .typography
                                                                                                                        .labelMedium,
                                                                                                        color =
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .primary
                                                                                                )
                                                                                        }
                                                                                },
                                                                                onClick = {
                                                                                        monthExpanded =
                                                                                                true
                                                                                },
                                                                                leadingIcon = {
                                                                                        Icon(
                                                                                                Icons.Default
                                                                                                        .DateRange,
                                                                                                null,
                                                                                                Modifier.size(
                                                                                                        18.dp
                                                                                                )
                                                                                        )
                                                                                }
                                                                        )

                                                                        Divider()

                                                                        // Edit Mode Toggle inside
                                                                        // Menu
                                                                        if (uiState.isCurrentMonth
                                                                        ) {
                                                                                DropdownMenuItem(
                                                                                        text = {
                                                                                                Text(
                                                                                                        if (uiState.isEditMode
                                                                                                        )
                                                                                                                "تعطيل التعديل"
                                                                                                        else
                                                                                                                "تفعيل التعديل"
                                                                                                )
                                                                                        },
                                                                                        onClick = {
                                                                                                viewModel
                                                                                                        .toggleEditMode()
                                                                                                filterMenuExpanded =
                                                                                                        false
                                                                                        },
                                                                                        leadingIcon = {
                                                                                                Icon(
                                                                                                        if (uiState.isEditMode
                                                                                                        )
                                                                                                                Icons.Default
                                                                                                                        .LockOpen
                                                                                                        else
                                                                                                                Icons.Default
                                                                                                                        .Edit,
                                                                                                        null,
                                                                                                        Modifier.size(
                                                                                                                18.dp
                                                                                                        )
                                                                                                )
                                                                                        }
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }

                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(top = 2.dp),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                text =
                                                                        "${uiState.zones.find { it.id == uiState.selectedZoneId }?.name ?: ""} - ${uiState.selectedMonth.displayName} ${uiState.selectedYear}",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )

                                                        if (uiState.isEditMode) {
                                                                Text(
                                                                        text = "وضع التعديل نشط",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelSmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error,
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                ) { padding ->
                        // Dialogs
                        if (zoneExpanded) {
                                AlertDialog(
                                        onDismissRequest = { zoneExpanded = false },
                                        title = { Text(Strings.selectZone) },
                                        text = {
                                                LazyColumn {
                                                        items(uiState.zones) { zone ->
                                                                TextButton(
                                                                        onClick = {
                                                                                viewModel
                                                                                        .selectZone(
                                                                                                zone.id
                                                                                        )
                                                                                zoneExpanded = false
                                                                                filterMenuExpanded =
                                                                                        false
                                                                        },
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                ) {
                                                                        Text(
                                                                                zone.name,
                                                                                textAlign =
                                                                                        TextAlign
                                                                                                .Right,
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                        )
                                                                }
                                                        }
                                                }
                                        },
                                        confirmButton = {}
                                )
                        }

                        if (yearExpanded) {
                                AlertDialog(
                                        onDismissRequest = { yearExpanded = false },
                                        title = { Text("السنة") },
                                        text = {
                                                LazyColumn {
                                                        items(uiState.availableYears) { year ->
                                                                TextButton(
                                                                        onClick = {
                                                                                viewModel
                                                                                        .selectYear(
                                                                                                year
                                                                                        )
                                                                                yearExpanded = false
                                                                                filterMenuExpanded =
                                                                                        false
                                                                        },
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                ) {
                                                                        Text(
                                                                                year.toString(),
                                                                                textAlign =
                                                                                        TextAlign
                                                                                                .Right,
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                        )
                                                                }
                                                        }
                                                }
                                        },
                                        confirmButton = {}
                                )
                        }

                        if (monthExpanded) {
                                AlertDialog(
                                        onDismissRequest = { monthExpanded = false },
                                        title = { Text("الشهر") },
                                        text = {
                                                LazyColumn {
                                                        items(uiState.availableMonths) { month ->
                                                                TextButton(
                                                                        onClick = {
                                                                                viewModel
                                                                                        .selectMonth(
                                                                                                month
                                                                                        )
                                                                                monthExpanded =
                                                                                        false
                                                                                filterMenuExpanded =
                                                                                        false
                                                                        },
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                ) {
                                                                        Row(
                                                                                horizontalArrangement =
                                                                                        Arrangement
                                                                                                .SpaceBetween,
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                        ) {
                                                                                Text(
                                                                                        month.displayName
                                                                                )
                                                                                if (month ==
                                                                                                MonthYear
                                                                                                        .current()
                                                                                ) {
                                                                                        Text(
                                                                                                "(الحالي)",
                                                                                                color =
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .primary
                                                                                        )
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                                        },
                                        confirmButton = {}
                                )
                        }

                        Column(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .padding(padding)
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                                // Table Header
                                TableHeader()

                                Spacer(modifier = Modifier.height(8.dp))

                                // Readings List
                                Box(modifier = Modifier.weight(1f)) {
                                        if (uiState.isLoading) {
                                                Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                ) { CircularProgressIndicator() }
                                        } else if (uiState.filteredReadings.isEmpty()) {
                                                Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Text(
                                                                text =
                                                                        if (searchQuery.isNotBlank()
                                                                        )
                                                                                "لا توجد نتائج"
                                                                        else Strings.noMembers,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                        } else {
                                                LazyColumn(
                                                        verticalArrangement =
                                                                Arrangement.spacedBy(4.dp)
                                                ) {
                                                        items(
                                                                uiState.filteredReadings,
                                                                key = { it.subscriberId }
                                                        ) { entry ->
                                                                MeterReadingRow(
                                                                        entry = entry,
                                                                        isEditMode =
                                                                                uiState.isEditMode,
                                                                        onReadingChange = {
                                                                                viewModel
                                                                                        .updateReading(
                                                                                                entry.subscriberId,
                                                                                                it
                                                                                        )
                                                                        },
                                                                        onClick = {
                                                                                if (uiState.isEditMode && !entry.hasInvoice) {
                                                                                        editDialogEntry = entry
                                                                                }
                                                                        },
                                                                        onShareNotification = {
                                                                                viewModel.shareNotification(
                                                                                        entry.subscriberId
                                                                                )
                                                                        }
                                                                )
                                                        }
                                                }
                                        }
                                }

                                // Summary + Save bar (replaces FAB to avoid conflict with bottom nav)
                                if (uiState.enteredCount > 0 || uiState.isEditMode) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        SummaryCard(
                                                uiState = uiState,
                                                showSaveAction = uiState.isEditMode && uiState.enteredCount > 0,
                                                onSave = { viewModel.saveAllReadings() }
                                        )
                                }
                        }
                }
        }

        // Edit Dialog
        editDialogEntry?.let { entry ->
                var readingValue by remember { mutableStateOf(entry.currentReading) }
                AlertDialog(
                        onDismissRequest = { editDialogEntry = null },
                        title = { Text(entry.subscriberName) },
                        text = {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text(
                                                text = "${Strings.previousReading}: ${entry.previousReading}",
                                                style = MaterialTheme.typography.bodyMedium
                                        )
                                        OutlinedTextField(
                                                value = readingValue,
                                                onValueChange = {
                                                        readingValue = it.filter { c -> c.isDigit() }
                                                },
                                                label = { Text(Strings.currentReading) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Number
                                                )
                                        )
                                        if (readingValue.isNotBlank()) {
                                                val consumption = readingValue.toIntOrNull()?.minus(entry.previousReading) ?: 0
                                                if (consumption >= 0) {
                                                        Text(
                                                                text = "${Strings.consumption}: $consumption ${Strings.m3}",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                        )
                                                }
                                        }
                                }
                        },
                        confirmButton = {
                                Button(
                                        onClick = {
                                                viewModel.updateReading(entry.subscriberId, readingValue)
                                                editDialogEntry = null
                                        },
                                        enabled = readingValue.isNotBlank()
                                ) {
                                        Text(Strings.save)
                                }
                        },
                        dismissButton = {
                                TextButton(onClick = { editDialogEntry = null }) {
                                        Text(Strings.cancel)
                                }
                        }
                )
        }
}

@Composable
fun TableHeader() {
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
        ) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        Text(
                                text = Strings.fullName,
                                modifier = Modifier.weight(2f),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                                text = Strings.meterNumber,
                                modifier = Modifier.weight(1.2f),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center
                        )
                        Text(
                                text = Strings.previousReading,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center
                        )
                        Text(
                                text = Strings.currentReading,
                                modifier = Modifier.weight(1.5f),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center
                        )
                        Text(
                                text = Strings.consumption,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center
                        )
                }
        }
}

@Composable
fun SummaryCard(
        uiState: MeterReadingUiState,
        showSaveAction: Boolean = false,
        onSave: () -> Unit = {}
) {
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                                text = "${uiState.enteredCount}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                                text = "قراءات مدخلة",
                                                style = MaterialTheme.typography.labelSmall
                                        )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                                text = "${uiState.totalConsumption} ${Strings.m3}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                                text = "إجمالي الاستهلاك",
                                                style = MaterialTheme.typography.labelSmall
                                        )
                                }
                        }

                        if (showSaveAction) {
                                Button(
                                        onClick = onSave,
                                        enabled = !uiState.isSaving,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                        if (uiState.isSaving) {
                                                CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                )
                                        } else {
                                                Icon(
                                                        Icons.Default.Save,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(Strings.saveReadings, style = MaterialTheme.typography.labelLarge)
                                }
                        }
                }
        }
}

@Composable
fun MeterReadingRow(
        entry: MeterReadingEntry,
        isEditMode: Boolean,
        onReadingChange: (String) -> Unit,
        onClick: () -> Unit = {},
        onShareNotification: () -> Unit = {}
) {
        Card(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor =
                                        when {
                                                entry.hasInvoice ->
                                                        Color(0xFF4CAF50)
                                                                .copy(
                                                                        alpha = 0.1f
                                                                ) // Green tint for invoiced
                                                entry.currentReading.isNotBlank() ->
                                                        MaterialTheme.colorScheme.primaryContainer
                                                                .copy(alpha = 0.3f)
                                                isEditMode -> MaterialTheme.colorScheme.surface
                                                else ->
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                                .copy(alpha = 0.5f)
                                        }
                        )
        ) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Text(
                                text = entry.subscriberName,
                                modifier = Modifier.weight(2f),
                                style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                                text = entry.meterNumber,
                                modifier = Modifier.weight(1.2f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                                text = "${entry.previousReading}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                        )

                        // Current Reading Input
                        Box(modifier = Modifier.weight(1.5f), contentAlignment = Alignment.Center) {
                                if (isEditMode) {
                                        val invoicedTextColor = Color(0xFF1B5E20)
                                        val invoicedBg = Color(0xFFE8F5E9)
                                        OutlinedTextField(
                                                value = entry.currentReading,
                                                onValueChange = {
                                                        onReadingChange(
                                                                it.filter { c -> c.isDigit() }
                                                        )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Number
                                                ),
                                                textStyle =
                                                        LocalTextStyle.current.copy(
                                                                textAlign = TextAlign.Center,
                                                                fontWeight = FontWeight.Bold
                                                        ),
                                                placeholder = {
                                                        Text(
                                                                text = Strings.enterReading,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium,
                                                                textAlign = TextAlign.Center,
                                                                modifier = Modifier.fillMaxWidth(),
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                },
                                                colors =
                                                        OutlinedTextFieldDefaults.colors(
                                                                focusedTextColor =
                                                                        if (entry.hasInvoice) invoicedTextColor
                                                                        else MaterialTheme.colorScheme.onSurface,
                                                                unfocusedTextColor =
                                                                        if (entry.hasInvoice) invoicedTextColor
                                                                        else MaterialTheme.colorScheme.onSurface,
                                                                cursorColor = MaterialTheme.colorScheme.primary,
                                                                focusedContainerColor =
                                                                        if (entry.hasInvoice) invoicedBg
                                                                        else MaterialTheme.colorScheme.surface,
                                                                unfocusedContainerColor =
                                                                        if (entry.hasInvoice) invoicedBg
                                                                        else MaterialTheme.colorScheme.surface
                                                        )
                                        )
                                } else if (entry.hasInvoice) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                        text = entry.currentReading,
                                                        color = Color(0xFF2E7D32),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                        text = "تمت الفوترة",
                                                        color = Color(0xFF2E7D32),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1
                                                )
                                        }
                                } else {
                                        Text(
                                                text = entry.currentReading.ifBlank { "-" },
                                                modifier = Modifier.fillMaxWidth(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center
                                        )
                                }
                        }

                        Text(
                                text = if (entry.consumption > 0) "${entry.consumption}" else "-",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                fontWeight =
                                        if (entry.consumption > 0) FontWeight.Bold
                                        else FontWeight.Normal,
                                color =
                                        if (entry.consumption > 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Notification share button (only when invoice exists)
                        if (entry.hasInvoice && !isEditMode) {
                                IconButton(
                                        onClick = onShareNotification,
                                        modifier = Modifier.size(32.dp)
                                ) {
                                        Icon(
                                                Icons.Default.Receipt,
                                                contentDescription = "إشعار الدفع",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                        )
                                }
                        }
                }
        }
}
