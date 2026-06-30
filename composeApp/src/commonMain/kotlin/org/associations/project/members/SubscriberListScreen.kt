package org.associations.project.members

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.associations.project.database.GetAllSubscribers
import org.associations.project.repository.LicenseRepository
import org.associations.project.ui.Strings
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val MAX_MEMBERS_UNACTIVATED = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriberListScreen(onNavigateToDetail: (Long) -> Unit, onNavigateToEntry: () -> Unit) {
    val viewModel = koinViewModel<MembersViewModel>()
    val licenseRepository: LicenseRepository = koinInject()

    val subscribers by viewModel.subscribers.collectAsStateWithLifecycle()
    val zones by viewModel.zones.collectAsStateWithLifecycle()

    val isActivated = licenseRepository.isActivated()
    val canAddMember = isActivated || subscribers.size < MAX_MEMBERS_UNACTIVATED

    var searchQuery by remember { mutableStateOf("") }
    var selectedZoneFilter by remember { mutableStateOf<Long?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }

    val filteredSubscribers =
            remember(subscribers, searchQuery, selectedZoneFilter) {
                subscribers.filter { subscriber ->
                    val matchesSearch =
                            searchQuery.isEmpty() ||
                                    subscriber.fullName.contains(searchQuery, ignoreCase = true) ||
                                    subscriber.meterNumber.contains(searchQuery, ignoreCase = true)
                    val matchesZone =
                            selectedZoneFilter == null || subscriber.zoneId == selectedZoneFilter
                    matchesSearch && matchesZone
                }.sortedWith(compareBy(naturalOrder<String>()) { it.meterNumber })
            }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = Strings.membersList, style = MaterialTheme.typography.headlineSmall)
                Row {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Badge(
                                containerColor =
                                        if (selectedZoneFilter != null)
                                                MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                        ) { Icon(Icons.Default.FilterList, contentDescription = Strings.filter) }
                    }
                    FloatingActionButton(
                            onClick = {
                                if (canAddMember) {
                                    onNavigateToEntry()
                                } else {
                                    showLimitDialog = true
                                }
                            },
                            containerColor =
                                    if (canAddMember) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                    ) { Icon(Icons.Default.Add, contentDescription = Strings.addMember) }
                }
            }

            // Search Bar
            OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text(Strings.searchMembers) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedPlaceholderColor = Color.Gray,
                                    unfocusedPlaceholderColor = Color.Gray,
                                    focusedLeadingIconColor = Color.Gray,
                                    unfocusedLeadingIconColor = Color.Gray,
                                    focusedBorderColor = Color.Gray,
                                    unfocusedBorderColor = Color.LightGray,
                                    cursorColor = Color.Black,
                            )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subscribers List
            if (filteredSubscribers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                            text =
                                    if (subscribers.isEmpty()) Strings.noMembers
                                    else Strings.noMembers,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSubscribers, key = { it.id }) { subscriber ->
                        SubscriberCard(
                                subscriber = subscriber,
                                onClick = { onNavigateToDetail(subscriber.id) }
                        )
                    }
                }
            }
        }

        // Filter Dialog
        if (showFilterDialog) {
            AlertDialog(
                    onDismissRequest = { showFilterDialog = false },
                    title = { Text(Strings.selectZone) },
                    text = {
                        Column {
                            TextButton(
                                    onClick = {
                                        selectedZoneFilter = null
                                        showFilterDialog = false
                                    }
                            ) { Text(Strings.allZones) }
                            zones.forEach { zone ->
                                TextButton(
                                        onClick = {
                                            selectedZoneFilter = zone.id
                                            showFilterDialog = false
                                        }
                                ) { Text(zone.name) }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showFilterDialog = false }) { Text(Strings.close) }
                    }
            )
        }

        // Member limit dialog for non-activated apps
        if (showLimitDialog) {
            AlertDialog(
                    onDismissRequest = { showLimitDialog = false },
                    title = { Text("تم الوصول للحد الأقصى") },
                    text = {
                        Text(
                                "يمكنك إضافة $MAX_MEMBERS_UNACTIVATED مشتركين فقط في النسخة التجريبية. يرجى تفعيل التطبيق لإضافة المزيد."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showLimitDialog = false }) { Text(Strings.close) }
                    }
            )
        }
    }
}

@Composable
fun SubscriberCard(subscriber: GetAllSubscribers, onClick: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = subscriber.fullName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = "${Strings.meterNumber}: ${subscriber.meterNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = "${Strings.zone}: ${subscriber.zoneName ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status Badge
            Surface(
                    shape = MaterialTheme.shapes.small,
                    color =
                            if (subscriber.isActive == 1L)
                                    MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                        text = if (subscriber.isActive == 1L) Strings.active else Strings.suspended,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color =
                                if (subscriber.isActive == 1L)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
