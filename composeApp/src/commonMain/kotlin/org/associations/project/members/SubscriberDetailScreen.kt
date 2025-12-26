package org.associations.project.members

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
        var showDeleteDialog by remember { mutableStateOf(false) }
        var deleteConfirmationName by remember { mutableStateOf("") }

        var showInvoiceDeleteDialog by remember { mutableStateOf(false) }
        var invoiceToDelete by remember { mutableStateOf<Invoice?>(null) }
        var deleteInvoiceConfirmationText by remember { mutableStateOf("") }

        // Trigger loading of invoices for this subscriber
        LaunchedEffect(subscriberId) { viewModel.selectSubscriber(subscriberId) }

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = { Text(Strings.memberDetails) },
                                navigationIcon = {
                                        IconButton(onClick = onNavigateBack) {
                                                Icon(
                                                        Icons.Default.ArrowBack,
                                                        contentDescription = Strings.back
                                                )
                                        }
                                },
                                actions = {
                                        IconButton(onClick = { onNavigateToEdit(subscriberId) }) {
                                                Icon(
                                                        Icons.Default.Edit,
                                                        contentDescription = Strings.edit
                                                )
                                        }
                                        IconButton(onClick = { showDeleteDialog = true }) {
                                                Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "حذف"
                                                )
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
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer
                                                )
                                ) {
                                        Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                Text(
                                                        text = subscriber.fullName,
                                                        style =
                                                                MaterialTheme.typography
                                                                        .headlineMedium,
                                                        fontWeight = FontWeight.Bold
                                                )

                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween
                                                ) {
                                                        Text(
                                                                text = "${Strings.status}:",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium
                                                        )
                                                        Surface(
                                                                shape = MaterialTheme.shapes.small,
                                                                color =
                                                                        if (subscriber.isActive ==
                                                                                        1L
                                                                        )
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                        else
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error
                                                        ) {
                                                                Text(
                                                                        text =
                                                                                if (subscriber
                                                                                                .isActive ==
                                                                                                1L
                                                                                )
                                                                                        Strings.active
                                                                                else
                                                                                        Strings.suspended,
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        horizontal =
                                                                                                8.dp,
                                                                                        vertical =
                                                                                                4.dp
                                                                                ),
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelSmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onPrimary
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
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(32.dp),
                                                        contentAlignment =
                                                                androidx.compose.ui.Alignment.Center
                                                ) {
                                                        Text(
                                                                text = "لا توجد قراءات بعد",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                        } else {
                                                Column(
                                                        modifier = Modifier.padding(16.dp),
                                                        verticalArrangement =
                                                                Arrangement.spacedBy(8.dp)
                                                ) {
                                                        invoices.take(10).forEach { invoice ->
                                                                ReadingHistoryRow(invoice)
                                                                if (invoice != invoices.last()) {
                                                                        HorizontalDivider(
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .outlineVariant
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
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(32.dp),
                                                        contentAlignment =
                                                                androidx.compose.ui.Alignment.Center
                                                ) {
                                                        Text(
                                                                text = "لا توجد فواتير بعد",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                        } else {
                                                Column(
                                                        modifier = Modifier.padding(16.dp),
                                                        verticalArrangement =
                                                                Arrangement.spacedBy(8.dp)
                                                ) {
                                                        invoices.take(10).forEach { invoice ->
                                                                InvoiceHistoryRow(
                                                                        invoice = invoice,
                                                                        onDelete = {
                                                                                invoiceToDelete =
                                                                                        invoice
                                                                                showInvoiceDeleteDialog =
                                                                                        true
                                                                        }
                                                                )
                                                                if (invoice != invoices.last()) {
                                                                        HorizontalDivider(
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .outlineVariant
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }

                if (showDeleteDialog && subscriber != null) {
                        AlertDialog(
                                onDismissRequest = {
                                        showDeleteDialog = false
                                        deleteConfirmationName = ""
                                },
                                title = { Text(text = "تأكيد الحذف") },
                                text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(text = "هل أنت متأكد من حذف هذا المشترك؟")
                                                Text(
                                                        text = "للتأكيد، يرجى كتابة اسم المشترك:",
                                                        style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                        text = subscriber.fullName,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold
                                                )
                                                OutlinedTextField(
                                                        value = deleteConfirmationName,
                                                        onValueChange = {
                                                                deleteConfirmationName = it
                                                        },
                                                        label = { Text("اسم المشترك") },
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        isError =
                                                                deleteConfirmationName
                                                                        .isNotEmpty() &&
                                                                        deleteConfirmationName !=
                                                                                subscriber.fullName
                                                )
                                        }
                                },
                                confirmButton = {
                                        Button(
                                                onClick = {
                                                        viewModel.deleteSubscriber(subscriberId)
                                                        showDeleteDialog = false
                                                        onNavigateBack()
                                                },
                                                enabled =
                                                        deleteConfirmationName ==
                                                                subscriber.fullName,
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .error
                                                        )
                                        ) { Text("حذف نهائي") }
                                },
                                dismissButton = {
                                        TextButton(
                                                onClick = {
                                                        showDeleteDialog = false
                                                        deleteConfirmationName = ""
                                                }
                                        ) { Text("إلغاء") }
                                }
                        )
                }
                if (showInvoiceDeleteDialog && invoiceToDelete != null) {
                        val targetText = formatDate(invoiceToDelete!!.issueDate)
                        AlertDialog(
                                onDismissRequest = {
                                        showInvoiceDeleteDialog = false
                                        deleteInvoiceConfirmationText = ""
                                        invoiceToDelete = null
                                },
                                title = { Text(text = "تأكيد حذف الفاتورة") },
                                text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(text = "هل أنت متأكد من حذف هذه الفاتورة؟")
                                                Text(
                                                        text = "للتأكيد، يرجى كتابة التاريخ:",
                                                        style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                        text = targetText,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold
                                                )
                                                OutlinedTextField(
                                                        value = deleteInvoiceConfirmationText,
                                                        onValueChange = {
                                                                deleteInvoiceConfirmationText = it
                                                        },
                                                        label = { Text("التاريخ") },
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        isError =
                                                                deleteInvoiceConfirmationText
                                                                        .isNotEmpty() &&
                                                                        deleteInvoiceConfirmationText !=
                                                                                targetText
                                                )
                                        }
                                },
                                confirmButton = {
                                        Button(
                                                onClick = {
                                                        invoiceToDelete?.let {
                                                                viewModel.deleteInvoice(it.id)
                                                        }
                                                        showInvoiceDeleteDialog = false
                                                        deleteInvoiceConfirmationText = ""
                                                        invoiceToDelete = null
                                                },
                                                enabled =
                                                        deleteInvoiceConfirmationText == targetText,
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .error
                                                        )
                                        ) { Text("حذف") }
                                },
                                dismissButton = {
                                        TextButton(
                                                onClick = {
                                                        showInvoiceDeleteDialog = false
                                                        deleteInvoiceConfirmationText = ""
                                                        invoiceToDelete = null
                                                }
                                        ) { Text("إلغاء") }
                                }
                        )
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
fun InvoiceHistoryRow(invoice: Invoice, onDelete: () -> Unit) {
        val dateStr = formatDate(invoice.issueDate)
        val isPaid = invoice.status == "PAID"

        Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
                                        modifier =
                                                Modifier.padding(
                                                        horizontal = 6.dp,
                                                        vertical = 2.dp
                                                ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color =
                                                if (isPaid)
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onErrorContainer
                                )
                        }
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                                text = "${invoice.totalAmount} درهم",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color =
                                        if (isPaid) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                        )
                        IconButton(onClick = onDelete) {
                                Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "حذف الفاتورة",
                                        tint = MaterialTheme.colorScheme.error
                                )
                        }
                }
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
