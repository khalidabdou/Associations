package org.associations.project.billing

import org.associations.project.database.Invoice
import org.associations.project.database.Subscriber
import org.associations.project.database.AppSettings

interface PrintService {
    suspend fun printInvoice(invoice: Invoice, subscriber: Subscriber, settings: AppSettings)
}
