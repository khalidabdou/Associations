package org.associations.project.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Dashboard : Screen

    @Serializable
    data object SubscriberList : Screen

    @Serializable
    data class SubscriberEntry(val id: Long? = null) : Screen

    @Serializable
    data class SubscriberDetail(val id: Long) : Screen

    @Serializable
    data object MeterReading : Screen

    @Serializable
    data object ReadingsHistory : Screen

    @Serializable
    data object InvoicesList : Screen

    @Serializable
    data class InvoiceDetail(val id: Long) : Screen

    @Serializable
    data object Treasury : Screen

    @Serializable
    data object MaintenanceList : Screen

    @Serializable
    data object AddTicket : Screen

    @Serializable
    data object Settings : Screen
}
