package org.associations.project.members

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.associations.project.database.Invoice
import org.associations.project.ui.Strings
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriberDetailScreen(
        subscriberId: Long,
        onNavigateBack: () -> Unit,
        onNavigateToEdit: (Long) -> Unit
) {
    val viewModel = koinViewModel<MembersViewModel>()
    val subscribers by viewModel.subscribers.collectAsStateWithLifecycle()
    val invoices by viewModel.subscriberInvoices.collectAsStateWithLifecycle()

    val subscriber = subscribers.find { it.id == subscriberId }

    // Trigger loading of invoices for this subscriber
    LaunchedEffect(subscriberId) { viewModel.selectSubscriber(subscriberId) }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(Strings.memberDetails) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = Strings.back)
                            }
                        },
                        actions = {
                            IconButton(onClick = { onNavigateToEdit(subscriberId) }) {
                                Icon(Icons.Default.Edit, contentDescription = Strings.edit)
                            }
                        }
                )
            }
    ) { padding ->
        if (subscriber == null) {
            Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
            ) { Text("لم يتم العثور على المشترك") }
        } else {
            Column(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(padding)
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Profile Card
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                ) {
                    Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                                text = subscriber.fullName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                        )

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                    text = "${Strings.status}:",
                                    style = MaterialTheme.typography.bodyMedium
                            )
                            Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color =
                                            if (subscriber.isActive == 1L)
                                                    MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error
                            ) {
                                Text(
                                        text =
                                                if (subscriber.isActive == 1L) Strings.active
                                                else Strings.suspended,
                                        modifier =
                                                Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                // Information Section
                Text(
                        text = "المعلومات",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoRow(Strings.meterNumber, subscriber.meterNumber)
                        InfoRow(Strings.phone, subscriber.phone ?: "-")
                        InfoRow(Strings.address, subscriber.address ?: "-")
                        InfoRow(Strings.zone, subscriber.zoneName ?: "-")
                    }
                }

                // Reading History Section
                Text(
                        text = "سجل القراءات",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    if (invoices.isEmpty()) {
                        Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                    text = "لا توجد قراءات بعد",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            invoices.take(10).forEach { invoice ->
                                ReadingHistoryRow(invoice)
                                if (invoice != invoices.last()) {
                                    HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Invoice History Section
                Text(
                        text = "سجل الفواتير",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    if (invoices.isEmpty()) {
                        Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                    text = "لا توجد فواتير بعد",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            invoices.take(10).forEach { invoice ->
                                InvoiceHistoryRow(invoice)
                                if (invoice != invoices.last()) {
                                    HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant
                                    )
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
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ReadingHistoryRow(invoice: Invoice) {
    val dateStr = formatDate(invoice.issueDate)

    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                    text = "القراءة السابقة: ${invoice.previousReading}",
                    style = MaterialTheme.typography.bodySmall
            )
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text(
                    text = "القراءة الحالية: ${invoice.currentReading}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
            )
            Text(
                    text = "الاستهلاك: ${invoice.consumption} م³",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun InvoiceHistoryRow(invoice: Invoice) {
    val dateStr = formatDate(invoice.issueDate)
    val isPaid = invoice.status == "PAID"

    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                    shape = MaterialTheme.shapes.small,
                    color =
                            if (isPaid) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                        text = if (isPaid) "مدفوعة" else "غير مدفوعة",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color =
                                if (isPaid) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        Text(
                text = "${invoice.totalAmount} درهم",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color =
                        if (isPaid) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
        )
    }
}

private fun formatDate(epochMillis: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val monthNames =
                listOf(
                        "يناير",
                        "فبراير",
                        "مارس",
                        "أبريل",
                        "مايو",
                        "يونيو",
                        "يوليو",
                        "أغسطس",
                        "سبتمبر",
                        "أكتوبر",
                        "نوفمبر",
                        "ديسمبر"
                )
        "${monthNames[dt.monthNumber - 1]} ${dt.year}"
    } catch (e: Exception) {
        "تاريخ غير معروف"
    }
}
