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
import androidx.compose.ui.unit.Dp
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
        // --- HEADER ---
        Text(
            text = associationName,
            fontSize = fontSizeLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        if (associationAddress.isNotBlank()) {
            Text(associationAddress, fontSize = fontSizeSmall, color = Color.DarkGray)
        }
        if (associationPhone.isNotBlank()) {
            Text(associationPhone, fontSize = fontSizeSmall, color = Color.DarkGray)
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Black)
        
        Text(
            text = "فاتورة استهلاك الماء",
            fontSize = fontSizeMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // --- INVOICE INFO ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("رقم الفاتورة: #${invoice.id}", fontSize = fontSizeSmall)
                val date = Instant.fromEpochMilliseconds(invoice.issueDate)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                Text("التاريخ: ${date.date}", fontSize = fontSizeSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(subscriber.fullName, fontSize = fontSizeMedium, fontWeight = FontWeight.Bold)
                Text("عداد رقم: ${subscriber.meterNumber}", fontSize = fontSizeSmall)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // --- READINGS TABLE ---
        if (isReceipt) {
            // Condensed for Receipt
            ReceiptRow("القراءة الحالية", invoice.currentReading.toString())
            ReceiptRow("القراءة السابقة", invoice.previousReading.toString())
            ReceiptRow("الاستهلاك", "${invoice.consumption} m3", isBold = true)
        } else {
            // Table for A4
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("القراءة الحالية", fontSize = fontSizeSmall, fontWeight = FontWeight.Bold)
                Text("القراءة السابقة", fontSize = fontSizeSmall, fontWeight = FontWeight.Bold)
                Text("الاستهلاك", fontSize = fontSizeSmall, fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black) // Simple border for now
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(invoice.currentReading.toString(), fontSize = fontSizeSmall)
                Text(invoice.previousReading.toString(), fontSize = fontSizeSmall)
                Text("${invoice.consumption} m3", fontSize = fontSizeSmall)
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
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${invoice.totalAmount} DH",
                fontSize = fontSizeLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- FOOTER ---
        Text(
            text = "شكرا لالتزامكم بتسديد واجباتكم",
            fontSize = fontSizeSmall,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}

@Composable
fun ReceiptRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontSize = 12.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}
