package org.associations.project.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.associations.project.dashboard.DashboardScreen
import org.associations.project.members.SubscriberListScreen
import org.associations.project.members.SubscriberEntryScreen
import org.associations.project.members.SubscriberDetailScreen
import org.associations.project.meter.MeterReadingScreen
import org.associations.project.billing.InvoicesListScreen
import org.associations.project.treasury.TreasuryScreen
import org.associations.project.settings.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard
    ) {
        composable<Screen.Dashboard> {
            DashboardScreen(
                onNavigateToMembers = { navController.navigate(Screen.SubscriberList) },
                onNavigateToMeter = { navController.navigate(Screen.MeterReading) },
                onNavigateToBilling = { navController.navigate(Screen.InvoicesList) }
            )
        }
        composable<Screen.SubscriberList> {
            SubscriberListScreen(
                onNavigateToDetail = { id -> navController.navigate(Screen.SubscriberDetail(id.toString())) },
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
                onNavigateToEdit = { id -> navController.navigate(Screen.SubscriberEntry(id.toString())) }
            )
        }
        composable<Screen.MeterReading> {
            MeterReadingScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<Screen.InvoicesList> {
            InvoicesListScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<Screen.Treasury> {
            TreasuryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<Screen.Settings> {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        // Add other routes as placeholders for now
    }
}
