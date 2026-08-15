package com.atif.campusrideshare.ui.screens.ride

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.atif.campusrideshare.data.model.RideModel
import com.atif.campusrideshare.ui.components.EmptyState
import com.atif.campusrideshare.ui.components.RideCard
import com.atif.campusrideshare.ui.navigation.Screen
import com.atif.campusrideshare.ui.viewmodel.MyRidesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRidesScreen(
    navController: NavController,
    viewModel: MyRidesViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Offering", "Joined")
    
    val ridesAsDriver by viewModel.ridesAsDriver.collectAsState()
    val ridesAsPassenger by viewModel.ridesAsPassenger.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("My Trips", fontWeight = FontWeight.Bold) }
                )
                SecondaryTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Home.route) },
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already here */ },
                    icon = { Icon(Icons.Default.DirectionsCar, null) },
                    label = { Text("My Rides") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Notifications.route) },
                    icon = { Icon(Icons.Default.Notifications, null) },
                    label = { Text("Alerts") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Profile.route) },
                    icon = { Icon(Icons.Default.Person, null) },
                    label = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding)
        ) {
            val currentRides = if (selectedTab == 0) ridesAsDriver else ridesAsPassenger
            
            if (currentRides.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    EmptyState(
                        message = "You haven't ${if (selectedTab == 0) "offered" else "joined"} any rides yet.",
                        icon = Icons.Default.EventBusy
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentRides) { ride ->
                        RideCard(
                            ride = ride,
                            onClick = {
                                navController.navigate(Screen.RideDetail.createRoute(ride.rideId))
                            }
                        )
                    }
                }
            }
        }
    }
}
