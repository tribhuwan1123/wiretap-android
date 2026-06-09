package com.wiretap.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wiretap.ui.ble.BleDetailScreen
import com.wiretap.ui.ble.BleLogScreen
import com.wiretap.ui.ble.ConnectionLifecycleScreen
import com.wiretap.ui.network.NetworkDetailScreen
import com.wiretap.ui.network.NetworkListScreen
import com.wiretap.ui.nfc.NfcLogScreen
import com.wiretap.ui.timeline.TimelineScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Timeline : Screen("timeline", "Timeline", Icons.Default.List)
    object Network : Screen("network", "Network", Icons.Default.Wifi)
    object Ble : Screen("ble", "BLE", Icons.Default.Bluetooth)
    object Nfc : Screen("nfc", "NFC", Icons.Default.Nfc)
}

private val tabs = listOf(Screen.Timeline, Screen.Network, Screen.Ble, Screen.Nfc)

@Composable
fun WireTapNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Timeline.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Timeline.route) { TimelineScreen(navController) }
            composable(Screen.Network.route) { NetworkListScreen(navController) }
            composable(
                "network/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { NetworkDetailScreen(it.arguments?.getString("id") ?: "") }
            composable(Screen.Ble.route) { BleLogScreen(navController) }
            composable(
                "ble/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { BleDetailScreen(it.arguments?.getString("id") ?: "") }
            composable("ble/lifecycle") { ConnectionLifecycleScreen() }
            composable(Screen.Nfc.route) { NfcLogScreen() }
        }
    }
}
