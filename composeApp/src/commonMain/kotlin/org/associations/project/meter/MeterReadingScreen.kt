package org.associations.project.meter

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
import androidx.compose.ui.text.style.TextAlign
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

        var zoneExpanded by remember { mutableStateOf(false) }
        var monthExpanded by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.message) {
                uiState.message?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearMessage()
                }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
                        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                                // Header Row with Title and Edit Button
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text = Strings.addReadings,
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold
                                        )

                                        // Edit Mode Toggle
                                        Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                if (uiState.isCurrentMonth) {
                                                        FilledTonalButton(
                                                                onClick = {
                                                                        viewModel.toggleEditMode()
                                                                },
                                                                colors =
                                                                        if (uiState.isEditMode)
                                                                                ButtonDefaults
                                                                                        .filledTonalButtonColors(
                                                                                                containerColor =
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .primaryContainer
                                                                                        )
                                                                        else
                                                                                ButtonDefaults
                                                                                        .filledTonalButtonColors()
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                if (uiState.isEditMode
                                                                                )
                                                                                        Icons.Default
                                                                                                .Check
                                                                                else
                                                                                        Icons.Default
                                                                                                .Edit,
                                                                        contentDescription = null,
                                                                        modifier =
                                                                                Modifier.size(18.dp)
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(4.dp)
                                                                )
                                                                Text(
                                                                        if (uiState.isEditMode)
                                                                                "تم التفعيل"
                                                                        else "تفعيل التعديل"
                                                                )
                                                        }
                                                } else {
                                                        AssistChip(
                                                                onClick = {},
                                                                label = { Text("وضع القراءة فقط") },
                                                                leadingIcon = {
                                                                        Icon(
                                                                                Icons.Default.Lock,
                                                                                null,
                                                                                Modifier.size(16.dp)
                                                                        )
                                                                }
                                                        )
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Filters: Zone on top row, Year + Month on second row
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Zone Selector - Full width
                                        Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors =
                                                        CardDefaults.cardColors(
                                                                containerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceVariant
                                                        )
                                        ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                        Text(
                                                                text = Strings.selectZone,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        ExposedDropdownMenuBox(
                                                                expanded = zoneExpanded,
                                                                onExpandedChange = {
                                                                        zoneExpanded = it
                                                                }
                                                        ) {
                                                                OutlinedTextField(
                                                                        value =
                                                                                uiState.zones
                                                                                        .find {
                                                                                                it.id ==
                                                                                                        uiState.selectedZoneId
                                                                                        }
                                                                                        ?.name
                                                                                        ?: "",
                                                                        onValueChange = {},
                                                                        readOnly = true,
                                                                        singleLine = true,
                                                                        trailingIcon = {
                                                                                ExposedDropdownMenuDefaults
                                                                                        .TrailingIcon(
                                                                                                expanded =
                                                                                                        zoneExpanded
                                                                                        )
                                                                        },
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .menuAnchor(),
                                                                        colors =
                                                                                ExposedDropdownMenuDefaults
                                                                                        .outlinedTextFieldColors(),
                                                                        textStyle =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyMedium
                                                                )
                                                                ExposedDropdownMenu(
                                                                        expanded = zoneExpanded,
                                                                        onDismissRequest = {
                                                                                zoneExpanded = false
                                                                        }
                                                                ) {
                                                                        uiState.zones.forEach { zone
                                                                                ->
                                                                                DropdownMenuItem(
                                                                                        text = {
                                                                                                Text(
                                                                                                        zone.name
                                                                                                )
                                                                                        },
                                                                                        onClick = {
                                                                                                viewModel
                                                                                                        .selectZone(
                                                                                                                zone.id
                                                                                                        )
                                                                                                zoneExpanded =
                                                                                                        false
                                                                                        }
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        }

                                        // Year + Month on same row
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                // Year Selector
                                                Card(
                                                        modifier = Modifier.weight(1f),
                                                        colors =
                                                                CardDefaults.cardColors(
                                                                        containerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceVariant
                                                                )
                                                ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                                Text(
                                                                        text = "السنة",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelSmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        4.dp
                                                                                )
                                                                )
                                                                var yearExpanded by remember {
                                                                        mutableStateOf(false)
                                                                }
                                                                ExposedDropdownMenuBox(
                                                                        expanded = yearExpanded,
                                                                        onExpandedChange = {
                                                                                yearExpanded = it
                                                                        }
                                                                ) {
                                                                        OutlinedTextField(
                                                                                value =
                                                                                        uiState.selectedYear
                                                                                                .toString(),
                                                                                onValueChange = {},
                                                                                readOnly = true,
                                                                                singleLine = true,
                                                                                trailingIcon = {
                                                                                        ExposedDropdownMenuDefaults
                                                                                                .TrailingIcon(
                                                                                                        expanded =
                                                                                                                yearExpanded
                                                                                                )
                                                                                },
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                                                .menuAnchor(),
                                                                                colors =
                                                                                        ExposedDropdownMenuDefaults
                                                                                                .outlinedTextFieldColors(),
                                                                                textStyle =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyMedium
                                                                        )
                                                                        ExposedDropdownMenu(
                                                                                expanded =
                                                                                        yearExpanded,
                                                                                onDismissRequest = {
                                                                                        yearExpanded =
                                                                                                false
                                                                                }
                                                                        ) {
                                                                                uiState.availableYears
                                                                                        .forEach {
                                                                                                year
                                                                                                ->
                                                                                                DropdownMenuItem(
                                                                                                        text = {
                                                                                                                Text(
                                                                                                                        year.toString()
                                                                                                                )
                                                                                                        },
                                                                                                        onClick = {
                                                                                                                viewModel
                                                                                                                        .selectYear(
                                                                                                                                year
                                                                                                                        )
                                                                                                                yearExpanded =
                                                                                                                        false
                                                                                                        }
                                                                                                )
                                                                                        }
                                                                        }
                                                                }
                                                        }
                                                }

                                                // Month Selector
                                                Card(
                                                        modifier = Modifier.weight(1f),
                                                        colors =
                                                                CardDefaults.cardColors(
                                                                        containerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceVariant
                                                                )
                                                ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                                Text(
                                                                        text = "الشهر",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelSmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        4.dp
                                                                                )
                                                                )
                                                                ExposedDropdownMenuBox(
                                                                        expanded = monthExpanded,
                                                                        onExpandedChange = {
                                                                                monthExpanded = it
                                                                        }
                                                                ) {
                                                                        OutlinedTextField(
                                                                                value =
                                                                                        uiState.selectedMonth
                                                                                                .displayName,
                                                                                onValueChange = {},
                                                                                readOnly = true,
                                                                                singleLine = true,
                                                                                trailingIcon = {
                                                                                        ExposedDropdownMenuDefaults
                                                                                                .TrailingIcon(
                                                                                                        expanded =
                                                                                                                monthExpanded
                                                                                                )
                                                                                },
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                                                .menuAnchor(),
                                                                                colors =
                                                                                        ExposedDropdownMenuDefaults
                                                                                                .outlinedTextFieldColors(),
                                                                                textStyle =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyMedium
                                                                        )
                                                                        ExposedDropdownMenu(
                                                                                expanded =
                                                                                        monthExpanded,
                                                                                onDismissRequest = {
                                                                                        monthExpanded =
                                                                                                false
                                                                                }
                                                                        ) {
                                                                                uiState.availableMonths
                                                                                        .forEach {
                                                                                                month
                                                                                                ->
                                                                                                DropdownMenuItem(
                                                                                                        text = {
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
                                                                                                                                                        .primary,
                                                                                                                                        style =
                                                                                                                                                MaterialTheme
                                                                                                                                                        .typography
                                                                                                                                                        .labelSmall
                                                                                                                                )
                                                                                                                        }
                                                                                                                }
                                                                                                        },
                                                                                                        onClick = {
                                                                                                                viewModel
                                                                                                                        .selectMonth(
                                                                                                                                month
                                                                                                                        )
                                                                                                                monthExpanded =
                                                                                                                        false
                                                                                                        }
                                                                                                )
                                                                                        }
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Search Bar
                                OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = {
                                                searchQuery = it
                                                viewModel.setSearchQuery(it)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text(Strings.searchMembers) },
                                        leadingIcon = {
                                                Icon(
                                                        Icons.Default.Search,
                                                        contentDescription = null
                                                )
                                        },
                                        singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Table Header
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer
                                                )
                                ) {
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        horizontal = 16.dp,
                                                                        vertical = 12.dp
                                                                ),
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
                                                        style =
                                                                MaterialTheme.typography
                                                                        .labelMedium,
                                                        textAlign = TextAlign.Center
                                                )
                                                Text(
                                                        text = Strings.previousReading,
                                                        modifier = Modifier.weight(1f),
                                                        fontWeight = FontWeight.Bold,
                                                        style =
                                                                MaterialTheme.typography
                                                                        .labelMedium,
                                                        textAlign = TextAlign.Center
                                                )
                                                Text(
                                                        text = Strings.currentReading,
                                                        modifier = Modifier.weight(1.5f),
                                                        fontWeight = FontWeight.Bold,
                                                        style =
                                                                MaterialTheme.typography
                                                                        .labelMedium,
                                                        textAlign = TextAlign.Center
                                                )
                                                Text(
                                                        text = Strings.consumption,
                                                        modifier = Modifier.weight(1f),
                                                        fontWeight = FontWeight.Bold,
                                                        style =
                                                                MaterialTheme.typography
                                                                        .labelMedium,
                                                        textAlign = TextAlign.Center
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Readings List - Using weight to prevent summary card overlap
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
                                                                        }
                                                                )
                                                        }
                                                }
                                        }
                                }

                                // Summary Card - Fixed at bottom
                                if (uiState.enteredCount > 0 || uiState.isEditMode) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors =
                                                        CardDefaults.cardColors(
                                                                containerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .tertiaryContainer
                                                        )
                                        ) {
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(16.dp),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Row(
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(32.dp)
                                                        ) {
                                                                Column(
                                                                        horizontalAlignment =
                                                                                Alignment
                                                                                        .CenterHorizontally
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        "${uiState.enteredCount}",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .headlineSmall,
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        )
                                                                        Text(
                                                                                text =
                                                                                        "قراءات مدخلة",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .labelMedium
                                                                        )
                                                                }
                                                                Column(
                                                                        horizontalAlignment =
                                                                                Alignment
                                                                                        .CenterHorizontally
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        "${uiState.totalConsumption} ${Strings.m3}",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .headlineSmall,
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        )
                                                                        Text(
                                                                                text =
                                                                                        "إجمالي الاستهلاك",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .labelMedium
                                                                        )
                                                                }
                                                        }

                                                        // Save Button
                                                        if (uiState.isEditMode &&
                                                                        uiState.enteredCount > 0
                                                        ) {
                                                                Button(
                                                                        onClick = {
                                                                                viewModel
                                                                                        .saveAllReadings()
                                                                        },
                                                                        enabled = !uiState.isSaving
                                                                ) {
                                                                        if (uiState.isSaving) {
                                                                                CircularProgressIndicator(
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        16.dp
                                                                                                ),
                                                                                        strokeWidth =
                                                                                                2.dp,
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onPrimary
                                                                                )
                                                                        } else {
                                                                                Icon(
                                                                                        Icons.Default
                                                                                                .Save,
                                                                                        contentDescription =
                                                                                                null
                                                                                )
                                                                        }
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.width(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        Text(Strings.saveReadings)
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
fun MeterReadingRow(
        entry: MeterReadingEntry,
        isEditMode: Boolean,
        onReadingChange: (String) -> Unit
) {
        Card(
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
                                        OutlinedTextField(
                                                value = entry.currentReading,
                                                onValueChange = {
                                                        onReadingChange(
                                                                it.filter { c -> c.isDigit() }
                                                        )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                textStyle =
                                                        LocalTextStyle.current.copy(
                                                                textAlign = TextAlign.Center
                                                        ),
                                                placeholder = {
                                                        Text(
                                                                text = Strings.enterReading,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium,
                                                                textAlign = TextAlign.Center,
                                                                modifier = Modifier.fillMaxWidth()
                                                        )
                                                },
                                                colors =
                                                        OutlinedTextFieldDefaults.colors(
                                                                focusedContainerColor =
                                                                        if (entry.hasInvoice)
                                                                                Color(0xFFE8F5E9)
                                                                        else Color.Transparent,
                                                                unfocusedContainerColor =
                                                                        if (entry.hasInvoice)
                                                                                Color(0xFFE8F5E9)
                                                                        else Color.Transparent
                                                        )
                                        )
                                } else if (entry.hasInvoice) {
                                        Text(
                                                text = "${entry.currentReading} (تمت الفوترة)",
                                                color = Color(0xFF2E7D32),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                        )
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
                }
        }
}
