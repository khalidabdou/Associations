package org.associations.project.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.associations.project.billing.InvoicesListScreen
import org.associations.project.dashboard.DashboardScreen
import org.associations.project.members.SubscriberDetailScreen
import org.associations.project.members.SubscriberEntryScreen
import org.associations.project.members.SubscriberListScreen
import org.associations.project.meter.MeterReadingScreen
import org.associations.project.settings.SettingsScreen
import org.associations.project.treasury.TreasuryScreen
import org.associations.project.ui.Strings
import org.associations.project.ui.activation.ActivationScreen

@Composable
fun NavGraph(
        navController: NavHostController,
        startDestination: Screen = Screen.Dashboard // Allow overriding start destination
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Screen.Activation> {
            ActivationScreen(onNavigateToDashboard = { navController.popBackStack() })
        }
        composable<Screen.Dashboard> {
            DashboardScreen(
                    onNavigateToMembers = { navController.navigate(Screen.SubscriberList) },
                    onNavigateToMeter = { navController.navigate(Screen.MeterReading) },
                    onNavigateToBilling = { navController.navigate(Screen.InvoicesList) }
            )
        }
        composable<Screen.SubscriberList> {
            SubscriberListScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.SubscriberDetail(id.toString()))
                    },
                    onNavigateToEntry = { navController.navigate(Screen.SubscriberEntry()) }
            )
        }
        composable<Screen.SubscriberEntry> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.SubscriberEntry>()
            SubscriberEntryScreen(
                    subscriberId = args.id?.toLongOrNull(),
                    onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<Screen.SubscriberDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.SubscriberDetail>()
            SubscriberDetailScreen(
                    subscriberId = args.id.toLongOrNull() ?: 0L,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate(Screen.SubscriberEntry(id.toString()))
                    }
            )
        }
        composable<Screen.MeterReading> {
            MeterReadingScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<Screen.ReadingsHistory> { PlaceholderScreen(title = "سجل القراءات") }
        composable<Screen.InvoicesList> {
            InvoicesListScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<Screen.InvoiceDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.InvoiceDetail>()
            PlaceholderScreen(title = "تفاصيل الفاتورة #${args.id}")
        }
        composable<Screen.Treasury> {
            TreasuryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<Screen.MaintenanceList> { PlaceholderScreen(title = Strings.maintenanceTickets) }
        composable<Screen.AddTicket> { PlaceholderScreen(title = Strings.addTicket) }
        composable<Screen.Settings> {
            SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToActivation = { navController.navigate(Screen.Activation) }
            )
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
        ) {
            Card(
                    modifier = Modifier.padding(32.dp),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                    shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(48.dp)
                ) {
                    // Icon
                    Icon(
                            imageVector = Icons.Outlined.Construction,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                            text = "قريباً",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                            text = "نعمل على تطوير هذه الميزة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
