package com.atif.campusrideshare.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.atif.campusrideshare.ui.components.InitialsAvatar
import com.atif.campusrideshare.ui.components.LoadingOverlay
import com.atif.campusrideshare.ui.components.RatingBar
import com.atif.campusrideshare.ui.navigation.Screen
import com.atif.campusrideshare.ui.viewmodel.AuthViewModel
import com.atif.campusrideshare.ui.viewmodel.ProfileUiState
import com.atif.campusrideshare.ui.viewmodel.ProfileViewModel
import com.atif.campusrideshare.util.Config

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Form states
    var phone by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf(Config.VEHICLE_CAR) }
    var vehicleModel by remember { mutableStateOf("") }
    var vehicleColor by remember { mutableStateOf("") }
    var vehiclePlate by remember { mutableStateOf("") }

    // Update local state when profile loads
    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Success) {
            val user = (uiState as ProfileUiState.Success).user
            phone = user.phone
            vehicleType = if (user.vehicleType.isEmpty()) Config.VEHICLE_CAR else user.vehicleType
            vehicleModel = user.vehicleModel
            vehicleColor = user.vehicleColor
            vehiclePlate = user.vehiclePlate
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Home.route) },
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.MyRides.route) },
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
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Person, null) },
                    label = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Premium Header with Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                            )
                        )
                ) {
                    IconButton(
                        onClick = {
                            authViewModel.signOut()
                            navController.navigate(Screen.Login.route) { popUpTo(0) }
                        },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }

                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val user = (uiState as? ProfileUiState.Success)?.user
                        InitialsAvatar(
                            name = user?.fullName ?: "User", 
                            size = 100.dp,
                            modifier = Modifier.clip(CircleShape).background(Color.White).padding(4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = user?.fullName ?: "Loading...",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = user?.university ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    // Stats / Rating Section
                    val user = (uiState as? ProfileUiState.Success)?.user
                    if (user != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStat(label = "Rating", value = String.format("%.1f", user.averageRating), icon = Icons.Default.Star)
                            ProfileStat(label = "Trips", value = "${user.totalRatings}", icon = Icons.Default.Route)
                            ProfileStat(label = "Role", value = user.role.replaceFirstChar { it.uppercase() }, icon = Icons.Default.Shield)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text("Personal Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Vehicle Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = vehicleType == Config.VEHICLE_CAR,
                                    onClick = { vehicleType = Config.VEHICLE_CAR },
                                    label = { Text("Car") },
                                    leadingIcon = { Icon(Icons.Default.DirectionsCar, null) }
                                )
                                FilterChip(
                                    selected = vehicleType == Config.VEHICLE_BIKE,
                                    onClick = { vehicleType = Config.VEHICLE_BIKE },
                                    label = { Text("Bike") },
                                    leadingIcon = { Icon(Icons.Default.TwoWheeler, null) }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = vehicleModel, onValueChange = { vehicleModel = it }, label = { Text("Model (e.g. Civic)") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = vehicleColor, onValueChange = { vehicleColor = it }, label = { Text("Color") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = vehiclePlate, onValueChange = { vehiclePlate = it }, label = { Text("Plate #") }, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (user?.role == Config.ROLE_ADMIN) {
                        Button(
                            onClick = { navController.navigate(Screen.AdminDashboard.route) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("ADMIN DASHBOARD", fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = {
                            user?.let {
                                profileViewModel.saveProfile(it.fullName, phone, it.university)
                                profileViewModel.saveVehicleInfo(vehicleType, vehicleModel, vehicleColor, vehiclePlate)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            if (uiState is ProfileUiState.Loading) {
                LoadingOverlay(visible = true)
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}
