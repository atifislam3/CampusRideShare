package com.atif.campusrideshare.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.atif.campusrideshare.data.model.ReportModel
import com.atif.campusrideshare.data.model.UserModel
import com.atif.campusrideshare.ui.components.EmptyState
import com.atif.campusrideshare.ui.components.InitialsAvatar
import com.atif.campusrideshare.ui.navigation.Screen
import com.atif.campusrideshare.ui.viewmodel.AdminViewModel
import com.atif.campusrideshare.util.Config
import com.atif.campusrideshare.util.TimeAgo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AdminViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val reports by viewModel.reports.collectAsState()
    val users by viewModel.users.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    var userSearchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Admin Dashboard") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Reports") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Users") })
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (selectedTab == 0) {
                    // Reports Tab
                    ReportsTab(
                        reports = reports,
                        currentFilter = statusFilter,
                        onFilterChange = { viewModel.setStatusFilter(it) },
                        onReportClick = { navController.navigate(Screen.AdminReportDetail.createRoute(it.reportId)) }
                    )
                } else {
                    // Users Tab
                    UsersTab(
                        users = users.filter { it.fullName.contains(userSearchQuery, ignoreCase = true) },
                        searchQuery = userSearchQuery,
                        onSearchChange = { userSearchQuery = it },
                        onToggleBan = { user ->
                            if (user.banned) viewModel.unbanUser(user.uid)
                            else viewModel.banUser(user.uid)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportsTab(
    reports: List<ReportModel>,
    currentFilter: String?,
    onFilterChange: (String?) -> Unit,
    onReportClick: (ReportModel) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = currentFilter == null, onClick = { onFilterChange(null) }, label = { Text("All") })
            FilterChip(selected = currentFilter == Config.REPORT_PENDING, onClick = { onFilterChange(Config.REPORT_PENDING) }, label = { Text("Pending") })
            FilterChip(selected = currentFilter == Config.REPORT_RESOLVED, onClick = { onFilterChange(Config.REPORT_RESOLVED) }, label = { Text("Resolved") })
        }

        if (reports.isEmpty()) {
            EmptyState(message = "No reports found.", icon = Icons.Default.Flag)
        } else {
            LazyColumn {
                items(reports) { report ->
                    ListItem(
                        headlineContent = { Text(report.reason) },
                        supportingContent = { Text("Reported ${TimeAgo.timeAgo(report.createdAt)}") },
                        trailingContent = {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(report.status.uppercase()) }
                            )
                        },
                        modifier = Modifier.clickable { onReportClick(report) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun UsersTab(
    users: List<UserModel>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onToggleBan: (UserModel) -> Unit
) {
    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search users...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = MaterialTheme.shapes.medium
        )

        if (users.isEmpty()) {
            EmptyState(message = "No users found.", icon = Icons.Default.Person)
        } else {
            LazyColumn {
                items(users) { user ->
                    ListItem(
                        leadingContent = { InitialsAvatar(name = user.fullName, size = 40.dp) },
                        headlineContent = { Text(user.fullName) },
                        supportingContent = { Text(user.email) },
                        trailingContent = {
                            IconButton(onClick = { onToggleBan(user) }) {
                                Icon(
                                    imageVector = if (user.banned) Icons.Default.CheckCircle else Icons.Default.Block,
                                    contentDescription = if (user.banned) "Unban" else "Ban",
                                    tint = if (user.banned) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
