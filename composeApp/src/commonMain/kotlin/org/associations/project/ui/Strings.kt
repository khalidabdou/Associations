package org.associations.project.ui

/**
 * Arabic strings for the application
 * النصوص العربية للتطبيق
 */
object Strings {
    // App
    const val appName = "إدارة جمعية الماء"
    
    // Navigation
    const val navDashboard = "الرئيسية"
    const val navMembers = "المشتركون"
    const val navReadings = "القراءات"
    const val navInvoices = "الفواتير"
    const val navTreasury = "الخزينة"
    const val navMaintenance = "الصيانة"
    const val navSettings = "الإعدادات"
    const val navReports = "التقارير"

    
    // Common Actions
    const val add = "إضافة"
    const val edit = "تعديل"
    const val delete = "حذف"
    const val save = "حفظ"
    const val cancel = "إلغاء"
    const val search = "بحث"
    const val filter = "تصفية"
    const val back = "رجوع"
    const val confirm = "تأكيد"
    const val close = "إغلاق"
    const val print = "طباعة"
    const val export = "تصدير"
    
    // Dashboard
    const val dashboard = "لوحة التحكم"
    const val totalIncome = "إجمالي الدخل"
    const val unpaidAmount = "المبلغ غير المدفوع"
    const val totalMembers = "عدد المشتركين"
    const val waterConsumption = "استهلاك الماء"
    const val quickActions = "إجراءات سريعة"
    const val addReading = "إضافة قراءة"
    const val newPayment = "دفعة جديدة"
    const val addMember = "إضافة مشترك"
    const val recentActivity = "النشاط الأخير"
    const val dhs = "درهم"
    const val tons = "طن"
    
    // Members
    const val members = "المشتركون"
    const val membersList = "قائمة المشتركين"
    const val addNewMember = "إضافة مشترك جديد"
    const val editMember = "تعديل المشترك"
    const val memberDetails = "تفاصيل المشترك"
    const val fullName = "الاسم الكامل"
    const val phone = "رقم الهاتف"
    const val meterNumber = "رقم العداد"
    const val address = "العنوان"
    const val zone = "المنطقة"
    const val selectZone = "اختر المنطقة"
    const val status = "الحالة"
    const val active = "نشط"
    const val suspended = "موقوف"
    const val createdAt = "تاريخ التسجيل"
    const val allZones = "جميع المناطق"
    const val noMembers = "لا يوجد مشتركون"
    const val searchMembers = "البحث عن مشترك..."
    
    // Meter Readings
    const val meterReadings = "قراءات العدادات"
    const val addReadings = "إضافة قراءات"
    const val readingsHistory = "سجل القراءات"
    const val previousReading = "القراءة السابقة"
    const val currentReading = "القراءة الحالية"
    const val consumption = "الاستهلاك"
    const val readingDate = "تاريخ القراءة"
    const val enterReading = "أدخل القراءة"
    const val saveReadings = "حفظ القراءات"
    
    // Invoices
    const val invoices = "الفواتير"
    const val invoicesList = "قائمة الفواتير"
    const val invoiceDetails = "تفاصيل الفاتورة"
    const val generateInvoices = "إنشاء الفواتير"
    const val paid = "مدفوعة"
    const val unpaid = "غير مدفوعة"
    const val paidInvoices = "الفواتير المدفوعة"
    const val unpaidInvoices = "الفواتير غير المدفوعة"
    const val allInvoices = "جميع الفواتير"
    const val markAsPaid = "تحديد كمدفوعة"
    const val totalAmount = "المبلغ الإجمالي"
    const val issueDate = "تاريخ الإصدار"
    const val dueDate = "تاريخ الاستحقاق"
    const val pricingTiers = "شرائح التسعير"
    const val noInvoices = "لا توجد فواتير"
    
    // Treasury
    const val treasury = "الخزينة"
    const val balance = "الرصيد"
    const val income = "الدخل"
    const val expenses = "المصروفات"
    const val addIncome = "إضافة دخل"
    const val addExpense = "إضافة مصروف"
    const val transactionsList = "قائمة المعاملات"
    const val transactionHistory = "سجل المعاملات"
    const val amount = "المبلغ"
    const val category = "الفئة"
    const val description = "الوصف"
    const val date = "التاريخ"
    const val noTransactions = "لا توجد معاملات"
    
    // Categories
    const val categoryBillPayment = "دفع فاتورة"
    const val categoryMaintenance = "صيانة"
    const val categorySalaries = "رواتب"
    const val categorySupplies = "مستلزمات"
    const val categoryOther = "أخرى"
    
    // Maintenance
    const val maintenance = "الصيانة"
    const val maintenanceTickets = "تذاكر الصيانة"
    const val addTicket = "إضافة تذكرة"
    const val ticketDetails = "تفاصيل التذكرة"
    const val issueType = "نوع المشكلة"
    const val priority = "الأولوية"
    const val ticketStatus = "حالة التذكرة"
    const val open = "مفتوحة"
    const val inProgress = "قيد المعالجة"
    const val resolved = "محلولة"
    const val reportedDate = "تاريخ الإبلاغ"
    const val noTickets = "لا توجد تذاكر"
    
    // Issue Types
    const val issueLeak = "تسرب"
    const val issueBrokenMeter = "عداد معطل"
    const val issueBrokenPipe = "أنبوب مكسور"
    const val issueOther = "أخرى"
    
    // Priority
    const val priorityLow = "منخفضة"
    const val priorityMedium = "متوسطة"
    const val priorityHigh = "عالية"
    
    // Settings
    const val settings = "الإعدادات"
    const val associationProfile = "معلومات الجمعية"
    const val associationName = "اسم الجمعية"
    const val associationAddress = "عنوان الجمعية"
    const val zoneManagement = "إدارة المناطق"
    const val addZone = "إضافة منطقة"
    const val editZone = "تعديل المنطقة"
    const val deleteZone = "حذف المنطقة"
    const val zoneName = "اسم المنطقة"
    const val zoneDescription = "وصف المنطقة"
    const val noZones = "لا توجد مناطق"
    
    // Pricing & Billing Settings
    const val pricingConfiguration = "إعدادات التسعير"
    const val pricingTranches = "شرائح التسعير"
    const val addTranche = "إضافة شريحة"
    const val editTranche = "تعديل الشريحة"
    const val minUsage = "من (م³)"
    const val maxUsage = "إلى (م³)"
    const val pricePerUnit = "السعر للمتر المكعب"
    const val pricePerM3 = "سعر م³"
    const val lateFee = "غرامة التأخير"
    const val lateFeePercent = "غرامة التأخير (درهم)"
    const val monthFilter = "تصفية حسب الشهر"
    const val allMonths = "جميع الشهور"
    const val applyFilter = "تطبيق"
    const val lateFeeAmount = "مبلغ غرامة التأخير"
    const val fixedFee = "رسوم ثابتة"
    const val monthlyFixedFee = "الرسوم الشهرية الثابتة"
    const val gracePeriod = "فترة السماح (أيام)"
    const val billingSettings = "إعدادات الفوترة"
    const val dueDateDays = "أيام الاستحقاق"
    
    // Database
    const val database = "قاعدة البيانات"
    const val backup = "نسخ احتياطي"
    const val restore = "استعادة"
    
    // Messages
    const val confirmDelete = "هل أنت متأكد من الحذف؟"
    const val savedSuccessfully = "تم الحفظ بنجاح"
    const val deletedSuccessfully = "تم الحذف بنجاح"
    const val errorOccurred = "حدث خطأ"
    const val requiredField = "هذا الحقل مطلوب"
    const val m3 = "م³"
    const val optional = "اختياري"

    // Invoice template / printing
    const val invoiceNumber = "رقم الفاتورة"
    const val waterInvoiceTitle = "فاتورة استهلاك الماء"
    const val paymentReceiptTitle = "وصل دفع فاتورة الماء"
    const val subscriberLabel = "المشترك"
    const val meterShort = "العداد"
    const val previousShort = "السابقة"
    const val currentShort = "الحالية"
    const val consumptionShort = "الاستهلاك"
    const val total = "المجموع الكلي"
    const val waterCharges = "استهلاك الماء"
    const val penaltyLabel = "غرامة التأخير"
    const val monthlyFeeLabel = "الرسوم الشهرية"
    const val paymentDeadline = "اجل الدفع"
    const val paidStamp = "مدفوعة"
    const val paymentDate = "تاريخ الدفع"
    const val thanksMessage = "شكرا لالتزامكم بتسديد واجباتكم"
    const val printPreview = "معاينة الطباعة"
    const val share = "مشاركة"
    const val dirhamShort = "درهم"

    // Multi-select / bulk print
    const val selectAll = "تحديد الكل"
    const val clearSelection = "إلغاء التحديد"
    const val printSelected = "طباعة المحددة"
    const val itemsSelected = "محددة"

    // Mark as paid dialog
    const val markPaidTitle = "تأكيد الدفع"
    const val markPaidQuestion = "هل تم استلام الدفع لهذه الفاتورة؟"
    const val markUnpaidTitle = "تأكيد الإلغاء"
    const val markUnpaidQuestion = "هل تريد إلغاء الدفع واستعادة الفاتورة كغير مدفوعة؟"
    const val yes = "نعم"
    const val no = "لا"

    // Delete with code confirmation
    const val deleteInvoiceTitle = "حذف الفاتورة"
    const val deleteInvoiceMessage = "لتأكيد الحذف، أدخل الرمز التالي:"
    const val deleteCodeLabel = "رمز التأكيد"
    const val deleteCodeMismatch = "الرمز غير صحيح"

    // Print copies
    const val printCopiesTitle = "عدد النسخ"
    const val copies = "نسخة"
    const val printCountInvoices = "طباعة {count} فاتورة × {copies} نسخة"

    // Reports
    const val monthlyReport = "التقرير الشهري"
    const val totalWaterConsumed = "إجمالي الماء المستهلك"
    const val totalBillsGenerated = "إجمالي الفواتير الصادرة"
    const val totalPaidAmount = "إجمالي المبالغ المحصلة"
    const val totalUnpaidAmount = "إجمالي المبالغ غير المحصلة"
    const val totalIncomeReport = "إجمالي مداخيل الخزينة"
    const val totalExpensesReport = "إجمالي مصاريف الخزينة"
    const val netBalanceReport = "صافي الرصيد المالي"
    const val monthlyReportFor = "التقرير المالي والتشغيلي لشهر"
    const val summaryStats = "ملخص المؤشرات"
    const val detailedInvoices = "تفاصيل الفواتير"
    const val detailedTransactions = "تفاصيل المعاملات المالية"
    const val invoiceCount = "عدد الفواتير"
    const val paidCount = "المدفوعة"
    const val unpaidCount = "غير المدفوعة"
}

