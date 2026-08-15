package com.atif.campusrideshare.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.atif.campusrideshare.ui.screens.admin.AdminDashboardScreen
import com.atif.campusrideshare.ui.screens.admin.AdminReportDetailScreen
import com.atif.campusrideshare.ui.screens.auth.LoginScreen
import com.atif.campusrideshare.ui.screens.auth.SignUpScreen
import com.atif.campusrideshare.ui.screens.home.HomeScreen
import com.atif.campusrideshare.ui.screens.notifications.NotificationsScreen
import com.atif.campusrideshare.ui.screens.profile.ProfileScreen
import com.atif.campusrideshare.ui.screens.ride.*
import com.atif.campusrideshare.ui.screens.splash.SplashScreen
import com.atif.campusrideshare.ui.viewmodel.*

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { fadeIn(tween(400)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(400)) },
        exitTransition = { fadeOut(tween(400)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(400)) },
        popEnterTransition = { fadeIn(tween(400)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(400)) },
        popExitTransition = { fadeOut(tween(400)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(400)) }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController, authViewModel)
        }

        composable(Screen.Login.route) {
            LoginScreen(navController, authViewModel)
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(navController, authViewModel)
        }

        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(navController, viewModel)
        }

        composable(Screen.PostRide.route) {
            val viewModel: PostRideViewModel = hiltViewModel()
            PostRideScreen(navController, viewModel)
        }

        composable(
            route = Screen.RideDetail.route,
            arguments = listOf(navArgument("rideId") { type = NavType.StringType })
        ) {
            val viewModel: RideDetailViewModel = hiltViewModel()
            RideDetailScreen(navController, viewModel)
        }

        composable(Screen.MyRides.route) {
            val viewModel: MyRidesViewModel = hiltViewModel()
            MyRidesScreen(navController, viewModel)
        }

        composable(
            route = Screen.Requests.route,
            arguments = listOf(navArgument("rideId") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            val viewModel: RequestViewModel = hiltViewModel()
            RequestsScreen(navController, viewModel)
        }

        composable(Screen.Notifications.route) {
            val viewModel: NotificationViewModel = hiltViewModel()
            NotificationsScreen(navController, viewModel)
        }

        composable(Screen.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(navController, authViewModel, viewModel)
        }

        composable(
            route = Screen.RateUser.route,
            arguments = listOf(
                navArgument("rideId") { type = NavType.StringType },
                navArgument("ratedUid") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rideId = backStackEntry.arguments?.getString("rideId") ?: ""
            val ratedUid = backStackEntry.arguments?.getString("ratedUid") ?: ""
            val raterRole = "passenger" 
            val viewModel: RatingViewModel = hiltViewModel()
            RateUserScreen(navController, viewModel, rideId, ratedUid, raterRole)
        }

        composable(Screen.AdminDashboard.route) {
            val viewModel: AdminViewModel = hiltViewModel()
            AdminDashboardScreen(navController, viewModel)
        }

        composable(
            route = Screen.AdminReportDetail.route,
            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId") ?: ""
            val viewModel: AdminViewModel = hiltViewModel()
            AdminReportDetailScreen(navController, viewModel, reportId)
        }
    }
}
