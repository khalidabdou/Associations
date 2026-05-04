package org.associations.project.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.associations.project.database.Invoice
import org.associations.project.database.Subscriber

@Composable
fun InvoiceTemplate(
    invoice: Invoice,
    subscriber: Subscriber,
    associationName: String,
    associationAddress: String,
    associationPhone: String,
    printFormat: String, // "A4" or "RECEIPT"
    logoPath: String? = null,
    modifier: Modifier = Modifier
) {
    val isReceipt = printFormat == "RECEIPT"
    val padding = if (isReceipt) 8.dp else 32.dp
    val width = if (isReceipt) 300.dp else 595.dp // Approx A4 width in dp at 72dpi, Receipt 80mm
    val fontSizeSmall = if (isReceipt) 10.sp else 12.sp
    val fontSizeMedium = if (isReceipt) 12.sp else 14.sp
    val fontSizeLarge = if (isReceipt) 16.sp else 20.sp

    Column(
        modifier = modifier
            .width(width)
            .background(Color.White)
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- LOGO ---
        if (!logoPath.isNullOrBlank()) {
            InvoiceLogoImage(
                logoPath = logoPath,
                modifier = Modifier
                    .size(if (isReceipt) 48.dp else 80.dp)
                    .padding(bottom = 8.dp)
            )
        }

        // --- HEADER ---
        Text(
            text = associationName,
            fontSize = fontSizeLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        if (associationAddress.isNotBlank()) {
            Text(associationAddress, fontSize = fontSizeSmall, color = Color.Black)
        }
        if (associationPhone.isNotBlank()) {
            Text(associationPhone, fontSize = fontSizeSmall, color = Color.Black)
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Black)
        
        Text(
            text = "فاتورة استهلاك الماء",
            fontSize = fontSizeMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // --- INVOICE INFO ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("رقم الفاتورة: #${invoice.id}", fontSize = fontSizeSmall, color = Color.Black)
                val date = Instant.fromEpochMilliseconds(invoice.issueDate)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                Text("التاريخ: ${date.date}", fontSize = fontSizeSmall, color = Color.Black)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(subscriber.fullName, fontSize = fontSizeMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("عداد رقم: ${subscriber.meterNumber}", fontSize = fontSizeSmall, color = Color.Black)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // --- READINGS TABLE ---
        if (isReceipt) {
            // Condensed for Receipt
            ReceiptRow("القراءة الحالية", invoice.currentReading.toString(), isReceipt = true)
            ReceiptRow("القراءة السابقة", invoice.previousReading.toString(), isReceipt = true)
            ReceiptRow("الاستهلاك", "${invoice.consumption} m3", isBold = true, isReceipt = true)
        } else {
            // Table for A4
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("القراءة الحالية", fontSize = fontSizeSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("القراءة السابقة", fontSize = fontSizeSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("الاستهلاك", fontSize = fontSizeSmall, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black) // Simple border for now
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(invoice.currentReading.toString(), fontSize = fontSizeSmall, color = Color.Black)
                Text(invoice.previousReading.toString(), fontSize = fontSizeSmall, color = Color.Black)
                Text("${invoice.consumption} m3", fontSize = fontSizeSmall, color = Color.Black)
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        // --- TOTAL ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "المجموع الكلي",
                fontSize = fontSizeLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "${invoice.totalAmount} DH",
                fontSize = fontSizeLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- FOOTER ---
        Text(
            text = "شكرا لالتزامكم بتسديد واجباتكم",
            fontSize = fontSizeSmall,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = Color.Black
        )
    }
}

@Composable
fun ReceiptRow(label: String, value: String, isBold: Boolean = false, isReceipt: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, color = Color.Black)
        Text(value, fontSize = 12.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, color = Color.Black)
    }
}

// Platform-specific image loading expect/actual
@Composable
expect fun InvoiceLogoImage(logoPath: String, modifier: Modifier = Modifier)
