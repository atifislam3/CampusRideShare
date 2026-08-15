package com.atif.campusrideshare.ui.screens.ride

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.atif.campusrideshare.data.model.RideRequestModel
import com.atif.campusrideshare.ui.components.EmptyState
import com.atif.campusrideshare.ui.components.InitialsAvatar
import com.atif.campusrideshare.ui.components.RatingBar
import com.atif.campusrideshare.ui.viewmodel.RequestViewModel
import com.atif.campusrideshare.util.Config

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(
    navController: NavController,
    viewModel: RequestViewModel
) {
    val requests by viewModel.incomingRequests.collectAsState()
    val error by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ride Requests") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding)
        ) {
            if (requests.isEmpty()) {
                EmptyState(
                    message = "No pending requests for this ride.",
                    icon = Icons.Default.Group
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(requests) { request ->
                        RequestItem(
                            request = request,
                            onRespond = { accept ->
                                viewModel.respondToRequest(request.rideId, request.requestId, accept)
                            }
                        )
                    }
                }
            }

            if (error != null) {
                SnackbarHost(hostState = remember { SnackbarHostState() }) {
                    // Simple error handling for brevity, ideally a Snackbar
                }
            }
        }
    }
}

@Composable
private fun RequestItem(
    request: RideRequestModel,
    onRespond: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsAvatar(name = request.passengerName, size = 48.dp)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.passengerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                RatingBar(rating = request.passengerRating)
            }

            if (request.status == Config.REQUEST_PENDING) {
                Row {
                    IconButton(
                        onClick = { onRespond(false) },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Reject")
                    }
                    IconButton(
                        onClick = { onRespond(true) },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Accept")
                    }
                }
            } else {
                Text(
                    text = request.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (request.status == Config.REQUEST_ACCEPTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
