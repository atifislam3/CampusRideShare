package com.atif.campusrideshare.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    // Initialize form when user data is loaded
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
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0)
                        }
                    }) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState is ProfileUiState.Success || uiState is ProfileUiState.Loading || uiState is ProfileUiState.UpdateSuccess) {
                    val user = (uiState as? ProfileUiState.Success)?.user 
                        ?: (authViewModel.currentUser.collectAsState().value)

                    if (user != null) {
                        InitialsAvatar(name = user.fullName, size = 80.dp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = user.university,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        RatingBar(rating = user.averageRating, starSize = 20.dp)
                        
                        Text(
                            text = "${String.format("%.1f", user.averageRating)} (${user.totalRatings} ratings)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Editable Phone
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Vehicle Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "My Vehicle",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = vehicleType == Config.VEHICLE_CAR,
                                        onClick = { vehicleType = Config.VEHICLE_CAR },
                                        label = { Text("🚗 Car") },
                                        leadingIcon = {
                                            if (vehicleType == Config.VEHICLE_CAR) {
                                                Icon(
                                                    Icons.Default.DirectionsCar,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            }
                                        }
                                    )
                                    FilterChip(
                                        selected = vehicleType == Config.VEHICLE_BIKE,
                                        onClick = { vehicleType = Config.VEHICLE_BIKE },
                                        label = { Text("🏍️ Bike") },
                                        leadingIcon = {
                                            if (vehicleType == Config.VEHICLE_BIKE) {
                                                Icon(
                                                    Icons.Default.DirectionsBike,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            }
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = vehicleModel,
                                    onValueChange = { vehicleModel = it },
                                    label = { Text("Model (e.g. Toyota Corolla)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = vehicleColor,
                                    onValueChange = { vehicleColor = it },
                                    label = { Text("Color") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = vehiclePlate,
                                    onValueChange = { vehiclePlate = it },
                                    label = { Text("License Plate") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                profileViewModel.saveProfile(user.fullName, phone, user.university)
                                profileViewModel.saveVehicleInfo(
                                    vehicleType,
                                    vehicleModel,
                                    vehicleColor,
                                    vehiclePlate
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Save Profile Changes", modifier = Modifier.padding(vertical = 4.dp))
                        }

                        if (uiState is ProfileUiState.Error) {
                            Text(
                                text = (uiState as ProfileUiState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            LoadingOverlay(visible = uiState is ProfileUiState.Loading)
        }
    }
}
