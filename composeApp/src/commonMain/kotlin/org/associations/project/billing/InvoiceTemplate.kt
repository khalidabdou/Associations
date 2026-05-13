package org.associations.project.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
import org.associations.project.ui.Strings

@Composable
fun InvoiceTemplate(
    invoice: Invoice,
    subscriber: Subscriber,
    associationName: String,
    associationAddress: String,
    associationPhone: String,
    printFormat: String, // "A4" or "RECEIPT"
    logoPath: String? = null,
    lateFeeAmount: Double = 0.0,
    monthlyFixedFee: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val isPaid = invoice.status == "PAID"
    val penaltyApplied = invoice.isPenaltyApplied == 1L
    val penaltyValue = if (penaltyApplied) lateFeeAmount else 0.0
    val monthlyFeeValue = if (monthlyFixedFee > 0.0) monthlyFixedFee else 0.0
    val waterChargeValue = (invoice.totalAmount - penaltyValue - monthlyFeeValue).coerceAtLeast(0.0)
    val isReceipt = printFormat == "RECEIPT"
    val isA5 = printFormat == "A5"
    val padding = when {
        isReceipt -> 12.dp
        isA5 -> 24.dp
        else -> 32.dp
    }
    val width = when {
        isReceipt -> 320.dp
        isA5 -> 420.dp
        else -> 595.dp // Approx A4 width in dp at 72dpi
    }
    val fontSizeSmall = when {
        isReceipt -> 11.sp
        isA5 -> 11.sp
        else -> 12.sp
    }
    val fontSizeMedium = when {
        isReceipt -> 13.sp
        isA5 -> 13.sp
        else -> 14.sp
    }
    val fontSizeLarge = when {
        isReceipt -> 17.sp
        isA5 -> 18.sp
        else -> 20.sp
    }
    val fontSizeTotal = when {
        isReceipt -> 20.sp
        isA5 -> 22.sp
        else -> 24.sp
    }
    val borderColor = Color(0xFF333333)
    val mutedColor = Color(0xFF555555)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isPaid) Strings.paymentReceiptTitle else Strings.waterInvoiceTitle,
                fontSize = fontSizeMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            if (isPaid) {
                Spacer(modifier = Modifier.width(8.dp))
                PaidBadge(fontSize = fontSizeSmall)
            }
        }

        // --- INVOICE INFO ---
        InfoRow(Strings.invoiceNumber, "${invoice.id}", fontSizeSmall)
        val date = Instant.fromEpochMilliseconds(invoice.issueDate)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        InfoRow(
            label = if (isPaid) Strings.paymentDate else Strings.date,
            value = date.date.toString(),
            fontSize = fontSizeSmall
        )
        InfoRow(Strings.subscriberLabel, subscriber.fullName, fontSizeSmall, valueBold = true)
        InfoRow(Strings.meterShort, subscriber.meterNumber, fontSizeSmall)

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
                TableHeaderCell(Strings.consumptionShort, fontSizeSmall, Modifier.weight(1f))
                TableHeaderCell(Strings.previousShort, fontSizeSmall, Modifier.weight(1f))
                TableHeaderCell(Strings.currentShort, fontSizeSmall, Modifier.weight(1f))
            }
            HorizontalDivider(color = borderColor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 10.dp)
            ) {
                TableCell("${invoice.consumption} ${Strings.m3}", fontSizeSmall, Modifier.weight(1f), bold = true)
                TableCell(invoice.previousReading.toString(), fontSizeSmall, Modifier.weight(1f))
                TableCell(invoice.currentReading.toString(), fontSizeSmall, Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- BREAKDOWN (charges, monthly fee, penalty) ---
        Column(modifier = Modifier.fillMaxWidth()) {
            BreakdownRow(Strings.waterCharges, waterChargeValue, fontSizeSmall)
            if (monthlyFeeValue > 0.0) {
                BreakdownRow(Strings.monthlyFeeLabel, monthlyFeeValue, fontSizeSmall)
            }
            if (penaltyValue > 0.0) {
                BreakdownRow(
                    label = Strings.penaltyLabel,
                    amount = penaltyValue,
                    fontSize = fontSizeSmall,
                    valueColor = Color(0xFFBF360C)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- TOTAL ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isPaid) Color(0xFFE8F5E9) else Color(0xFFF2F2F2),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Strings.total,
                fontSize = fontSizeLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "${formatAmount(invoice.totalAmount)} ${Strings.dirhamShort}",
                fontSize = fontSizeTotal,
                fontWeight = FontWeight.Bold,
                color = if (isPaid) Color(0xFF1B5E20) else Color.Black,
                maxLines = 1
            )
        }

        // --- PAYMENT STATUS BOX ---
        if (isPaid) {
            Spacer(modifier = Modifier.height(12.dp))
            val paidDate = Instant.fromEpochMilliseconds(invoice.issueDate)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFF2E7D32), RoundedCornerShape(6.dp))
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${Strings.paidStamp} ✓ ${Strings.paymentDate}: ${paidDate.date}",
                    fontSize = fontSizeMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    textAlign = TextAlign.Center
                )
            }
        } else if (invoice.dueDate > 0) {
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
                    text = "${Strings.paymentDeadline}: ${due.date}",
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
            text = Strings.thanksMessage,
            fontSize = fontSizeSmall,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = mutedColor,
            textAlign = TextAlign.Center
        )
    }
    }
}

@Composable
private fun PaidBadge(fontSize: androidx.compose.ui.unit.TextUnit) {
    Box(
        modifier = Modifier
            .border(2.dp, Color(0xFF2E7D32), RoundedCornerShape(4.dp))
            .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = Strings.paidStamp,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20)
        )
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    amount: Double,
    fontSize: androidx.compose.ui.unit.TextUnit,
    valueColor: Color = Color.Black
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = fontSize, color = Color(0xFF555555))
        Text(
            text = "${formatAmount(amount)} ${Strings.dirhamShort}",
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1
        )
    }
}

private fun formatAmount(value: Double): String {
    val rounded = (value * 100).toLong() / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
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
