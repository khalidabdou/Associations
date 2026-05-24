# سير عمل تطبيق الغرامة (PenaltyApplied) على الفاتورة

## 1. الحقل في قاعدة البيانات

في ملف `AppDatabase.sq` يوجد عمود `isPenaltyApplied` من نوع `INTEGER` وقيمته الافتراضية `0`:

```sql
-- AppDatabase.sq:40
isPenaltyApplied INTEGER NOT NULL DEFAULT 0, -- 0 = لا غرامة, 1 = غرامة مطبقة
```

## 2. التحقق وتطبيق الغرامة

الدالة `checkAndApplyLateFees()` في `AppRepository.kt` تقوم بما يلي:

```kotlin
// AppRepository.kt:393-411
suspend fun checkAndApplyLateFees() {
    val settings = queries.getSettings().executeAsOneOrNull() ?: return
    if (settings.lateFeeAmount <= 0.0) return

    val unpaidInvoices = queries.getUnpaidInvoices().executeAsList()
    val currentTime = Clock.System.now().toEpochMilliseconds()

    unpaidInvoices.forEach { invoice ->
        if (invoice.isPenaltyApplied == 0L && currentTime > invoice.dueDate) {
            queries.applyPenalty(settings.lateFeeAmount, invoice.id)
        }
    }
}
```

**الخطوات:**
- تسترجع الإعدادات (`Settings`) وتتأكد أن قيمة `lateFeeAmount` أكبر من صفر.
- تجلب جميع الفواتير غير المدفوعة (`UNPAID`).
- تتحقق من كل فاتورة: إذا كان `isPenaltyApplied == 0` **و** تجاوز التاريخ الحالي (`currentTime`) تاريخ الاستحقاق (`dueDate`).
- تستدعي `applyPenalty` لتضيف مبلغ الغرامة إلى `totalAmount` وتضع `isPenaltyApplied = 1`.

## 3. استعلام تطبيق الغرامة في SQLDelight

```sql
-- AppDatabase.sq:174-175
applyPenalty:
UPDATE Invoice SET totalAmount = totalAmount + ?, isPenaltyApplied = 1 WHERE id = ?;
```

## 4. متى يتم التشغيل؟

يتم استدعاء `checkLateFees()` تلقائياً عند تهيئة `InvoicesViewModel`:

```kotlin
// InvoicesViewModel.kt:182-184
init {
    checkLateFees()
    loadInvoices()
}
```

أي عند فتح شاشة قائمة الفواتير (`InvoicesListScreen`).

## 5. العرض في الواجهة

عند عرض/طباعة الفاتورة، يُعرض مبلغ الغرامة كالتالي:

```kotlin
// InvoiceTemplate.kt:42-45
val penaltyApplied = invoice.isPenaltyApplied == 1L
val penaltyValue = if (penaltyApplied) lateFeeAmount else 0.0
val monthlyFeeValue = if (monthlyFixedFee > 0.0) monthlyFixedFee else 0.0
val waterChargeValue = (invoice.totalAmount - penaltyValue - monthlyFeeValue).coerceAtLeast(0.0)
```

**المنطق:**
- إذا كانت القيمة `1`، يُحسب `penaltyValue` = `lateFeeAmount` من الإعدادات.
- يُخصم مبلغ الغرامة والرسوم الشهرية من `totalAmount` لإظهار صافي استهلاك الماء.

---

## ملخص سريع

| المرحلة | القيمة | الشرح |
|---------|--------|-------|
| إنشاء الفاتورة | `isPenaltyApplied = 0` | لا غرامة افتراضياً |
| فتح شاشة الفواتير | فحص تلقائي | يُفحص كل فاتورة غير مدفوعة إذا تجاوز تاريخ الاستحقاق |
| تأخر الدفع | `isPenaltyApplied = 1` | يُضاف `lateFeeAmount` إلى `totalAmount` |
| عرض/طباعة الفاتورة | عرض منفصل | يُعرض مبلغ الغرامة منفصلاً عن رسوم الماء |
