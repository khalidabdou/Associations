package org.associations.project.members

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.associations.project.ui.Strings
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriberEntryScreen(subscriberId: Long? = null, onNavigateBack: () -> Unit) {
    val viewModel = koinViewModel<MembersViewModel>()
    val zones by viewModel.zones.collectAsStateWithLifecycle()
    val subscribers by viewModel.subscribers.collectAsStateWithLifecycle()

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var meterNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedZoneId by remember { mutableStateOf<Long?>(null) }
    var isActive by remember { mutableStateOf(1L) }
    var expanded by remember { mutableStateOf(false) }
    var isDataLoaded by remember { mutableStateOf(false) }

    val isEditMode = subscriberId != null

    // Pre-fill form when editing
    LaunchedEffect(subscriberId, subscribers) {
        if (subscriberId != null && !isDataLoaded && subscribers.isNotEmpty()) {
            val subscriber = subscribers.find { it.id == subscriberId }
            subscriber?.let {
                fullName = it.fullName
                phone = it.phone ?: ""
                meterNumber = it.meterNumber
                address = it.address ?: ""
                selectedZoneId = it.zoneId
                isActive = it.isActive
                isDataLoaded = true
            }
        }
    }

    val isFormValid = fullName.isNotBlank() && meterNumber.isNotBlank() && selectedZoneId != null

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
                topBar = {
                    TopAppBar(
                            title = {
                                Text(if (isEditMode) Strings.editMember else Strings.addNewMember)
                            },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = Strings.back)
                                }
                            },
                            actions = {
                                IconButton(
                                        onClick = {
                                            if (isFormValid && selectedZoneId != null) {
                                                if (isEditMode && subscriberId != null) {
                                                    viewModel.updateSubscriber(
                                                            id = subscriberId,
                                                            fullName = fullName,
                                                            phone = phone.ifBlank { null },
                                                            meterNumber = meterNumber,
                                                            address = address.ifBlank { null },
                                                            zoneId = selectedZoneId!!,
                                                            isActive = isActive
                                                    )
                                                } else {
                                                    viewModel.addSubscriber(
                                                            fullName = fullName,
                                                            phone = phone.ifBlank { null },
                                                            meterNumber = meterNumber,
                                                            address = address.ifBlank { null },
                                                            zoneId = selectedZoneId!!
                                                    )
                                                }
                                                onNavigateBack()
                                            }
                                        },
                                        enabled = isFormValid
                                ) { Icon(Icons.Default.Save, contentDescription = Strings.save) }
                            }
                    )
                }
        ) { padding ->
            Column(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(padding)
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("${Strings.fullName} *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                )

                OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(Strings.phone) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                )

                OutlinedTextField(
                        value = meterNumber,
                        onValueChange = { meterNumber = it },
                        label = { Text("${Strings.meterNumber} *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                )

                OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(Strings.address) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                )

                // Zone Dropdown
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                            value = zones.find { it.id == selectedZoneId }?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("${Strings.zone} *") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                    ) {
                        zones.forEach { zone ->
                            DropdownMenuItem(
                                    text = { Text(zone.name) },
                                    onClick = {
                                        selectedZoneId = zone.id
                                        expanded = false
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                        onClick = {
                            if (isFormValid && selectedZoneId != null) {
                                if (isEditMode && subscriberId != null) {
                                    viewModel.updateSubscriber(
                                            id = subscriberId,
                                            fullName = fullName,
                                            phone = phone.ifBlank { null },
                                            meterNumber = meterNumber,
                                            address = address.ifBlank { null },
                                            zoneId = selectedZoneId!!,
                                            isActive = isActive
                                    )
                                } else {
                                    viewModel.addSubscriber(
                                            fullName = fullName,
                                            phone = phone.ifBlank { null },
                                            meterNumber = meterNumber,
                                            address = address.ifBlank { null },
                                            zoneId = selectedZoneId!!
                                    )
                                }
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isFormValid
                ) { Text(Strings.save) }
            }
        }
    }
}
