package org.associations.project.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val padding = if (isReceipt) 12.dp else 32.dp
    val width = if (isReceipt) 320.dp else 595.dp // Approx A4 width in dp at 72dpi, Receipt 80mm
    val fontSizeSmall = if (isReceipt) 11.sp else 12.sp
    val fontSizeMedium = if (isReceipt) 13.sp else 14.sp
    val fontSizeLarge = if (isReceipt) 17.sp else 20.sp
    val fontSizeTotal = if (isReceipt) 20.sp else 24.sp
    val borderColor = Color(0xFF333333)
    val mutedColor = Color(0xFF555555)

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
                    .size(if (isReceipt) 64.dp else 96.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // --- HEADER ---
        Text(
            text = associationName,
            fontSize = fontSizeLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        if (associationAddress.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(associationAddress, fontSize = fontSizeSmall, color = mutedColor, textAlign = TextAlign.Center)
        }
        if (associationPhone.isNotBlank()) {
            Text(associationPhone, fontSize = fontSizeSmall, color = mutedColor, textAlign = TextAlign.Center)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = borderColor)

        Text(
            text = "فاتورة استهلاك الماء",
            fontSize = fontSizeMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // --- INVOICE INFO ---
        InfoRow("رقم الفاتورة", "#${invoice.id}", fontSizeSmall)
        val date = Instant.fromEpochMilliseconds(invoice.issueDate)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        InfoRow("التاريخ", date.date.toString(), fontSizeSmall)
        InfoRow("المشترك", subscriber.fullName, fontSizeSmall, valueBold = true)
        InfoRow("رقم العداد", subscriber.meterNumber, fontSizeSmall)

        Spacer(modifier = Modifier.height(14.dp))

        // --- READINGS TABLE (uniform for A4 and Receipt with weighted columns) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F2F2))
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                TableHeaderCell("الاستهلاك", fontSizeSmall, Modifier.weight(1f))
                TableHeaderCell("السابقة", fontSizeSmall, Modifier.weight(1f))
                TableHeaderCell("الحالية", fontSizeSmall, Modifier.weight(1f))
            }
            HorizontalDivider(color = borderColor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 10.dp)
            ) {
                TableCell("${invoice.consumption} m³", fontSizeSmall, Modifier.weight(1f), bold = true)
                TableCell(invoice.previousReading.toString(), fontSizeSmall, Modifier.weight(1f))
                TableCell(invoice.currentReading.toString(), fontSizeSmall, Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- TOTAL ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF2F2F2), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
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
                fontSize = fontSizeTotal,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1
            )
        }

        // --- PAYMENT DEADLINE (notification highlight) ---
        if (invoice.dueDate > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            val due = Instant.fromEpochMilliseconds(invoice.dueDate)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFE65100), RoundedCornerShape(6.dp))
                    .background(Color(0xFFFFF3E0), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اجل الدفع: ${due.date}",
                    fontSize = fontSizeMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFBF360C),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- FOOTER ---
        Text(
            text = "شكرا لالتزامكم بتسديد واجباتكم",
            fontSize = fontSizeSmall,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = mutedColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, fontSize: androidx.compose.ui.unit.TextUnit, valueBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$label:", fontSize = fontSize, color = Color(0xFF555555))
        Text(
            value,
            fontSize = fontSize,
            fontWeight = if (valueBold) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun TableHeaderCell(text: String, fontSize: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        textAlign = TextAlign.Center,
        modifier = modifier,
        maxLines = 1
    )
}

@Composable
private fun TableCell(text: String, fontSize: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier, bold: Boolean = false) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = Color.Black,
        textAlign = TextAlign.Center,
        modifier = modifier,
        maxLines = 1
    )
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
