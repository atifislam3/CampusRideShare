package com.atif.campusrideshare.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.atif.campusrideshare.ui.components.LoadingOverlay
import com.atif.campusrideshare.ui.viewmodel.AdminViewModel
import com.atif.campusrideshare.util.Config
import com.atif.campusrideshare.util.TimeAgo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportDetailScreen(
    navController: NavController,
    viewModel: AdminViewModel,
    reportId: String
) {
    val reports by viewModel.reports.collectAsState()
    val report = reports.find { it.reportId == reportId }
    
    var adminNote by remember { mutableStateOf(report?.adminNote ?: "") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Report Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (report == null) {
            LoadingOverlay(visible = true)
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Reason", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(report.reason, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Description", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(report.description, style = MaterialTheme.typography.bodyLarge)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Metadata", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Reporter ID: ${report.reporterUid}")
                        Text("Reported ID: ${report.reportedUid}")
                        Text("Ride ID: ${report.rideId}")
                        Text("Created: ${TimeAgo.timeAgo(report.createdAt)}")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Admin Action", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = adminNote,
                    onValueChange = { adminNote = it },
                    label = { Text("Admin Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            viewModel.resolveReport(report.reportId, adminNote, Config.REPORT_RESOLVED)
                            navController.popBackStack()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Resolve")
                    }
                    OutlinedButton(
                        onClick = { 
                            viewModel.resolveReport(report.reportId, adminNote, Config.REPORT_DISMISSED)
                            navController.popBackStack()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}
