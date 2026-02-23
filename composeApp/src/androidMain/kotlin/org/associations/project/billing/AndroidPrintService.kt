package org.associations.project.billing

import org.associations.project.database.AppSettings
import org.associations.project.database.Invoice
import org.associations.project.database.Subscriber

class AndroidPrintService : PrintService {
    override suspend fun printInvoice(
            invoice: Invoice,
            subscriber: Subscriber,
            settings: AppSettings
    ) {
        // TODO: Implement using Android PrintManager API
        // For now, this is a no-op placeholder
        println("Android print not yet implemented for invoice #${invoice.id}")
    }
}
