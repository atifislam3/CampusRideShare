package com.atif.campusrideshare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.atif.campusrideshare.ui.viewmodel.AuthViewModel

// Placeholder imports for screens that will be generated next
// These functions are expected to exist in the ui.screens package
/*
import com.atif.campusrideshare.ui.screens.splash.SplashScreen
import com.atif.campusrideshare.ui.screens.auth.LoginScreen
import com.atif.campusrideshare.ui.screens.auth.SignUpScreen
import com.atif.campusrideshare.ui.screens.home.HomeScreen
import com.atif.campusrideshare.ui.screens.ride.PostRideScreen
import com.atif.campusrideshare.ui.screens.ride.RideDetailScreen
import com.atif.campusrideshare.ui.screens.ride.MyRidesScreen
import com.atif.campusrideshare.ui.screens.ride.RequestsScreen
import com.atif.campusrideshare.ui.screens.notifications.NotificationsScreen
import com.atif.campusrideshare.ui.screens.profile.ProfileScreen
import com.atif.campusrideshare.ui.screens.ride.RateUserScreen
import com.atif.campusrideshare.ui.screens.admin.AdminDashboardScreen
import com.atif.campusrideshare.ui.screens.admin.AdminReportDetailScreen
*/

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            // SplashScreen logic will use authViewModel to decide where to navigate
            // But we keep the UI logic in the Screen itself.
            // For now, we reference the expected function name.
            // SplashScreen(navController, authViewModel)
        }

        composable(Screen.Login.route) {
            // LoginScreen(navController, authViewModel)
        }

        composable(Screen.SignUp.route) {
            // SignUpScreen(navController, authViewModel)
        }

        composable(Screen.Home.route) {
            // HomeScreen(navController)
        }

        composable(Screen.PostRide.route) {
            // PostRideScreen(navController)
        }

        composable(
            route = Screen.RideDetail.route,
            arguments = listOf(navArgument("rideId") { type = NavType.StringType })
        ) {
            // RideDetailScreen(navController)
        }

        composable(Screen.MyRides.route) {
            // MyRidesScreen(navController)
        }

        composable(
            route = Screen.Requests.route,
            arguments = listOf(navArgument("rideId") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            // RequestsScreen(navController)
        }

        composable(Screen.Notifications.route) {
            // NotificationsScreen(navController)
        }

        composable(Screen.Profile.route) {
            // ProfileScreen(navController, authViewModel)
        }

        composable(
            route = Screen.RateUser.route,
            arguments = listOf(
                navArgument("rideId") { type = NavType.StringType },
                navArgument("ratedUid") { type = NavType.StringType }
            )
        ) {
            // RateUserScreen(navController)
        }

        // Admin Routes
        composable(Screen.AdminDashboard.route) {
            // AdminDashboardScreen(navController)
        }

        composable(
            route = Screen.AdminReportDetail.route,
            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
        ) {
            // AdminReportDetailScreen(navController)
        }
    }
}
