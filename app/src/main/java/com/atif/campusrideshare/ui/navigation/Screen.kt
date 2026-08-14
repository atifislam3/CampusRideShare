package com.atif.campusrideshare.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object PostRide : Screen("post_ride")
    object MyRides : Screen("my_rides")
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")
    object AdminDashboard : Screen("admin_dashboard")

    object RideDetail : Screen("ride_detail/{rideId}") {
        fun createRoute(rideId: String) = "ride_detail/$rideId"
    }

    object Requests : Screen("requests?rideId={rideId}") {
        fun createRoute(rideId: String? = null) = if (rideId != null) "requests?rideId=$rideId" else "requests"
    }

    object RateUser : Screen("rate_user/{rideId}/{ratedUid}") {
        fun createRoute(rideId: String, ratedUid: String) = "rate_user/$rideId/$ratedUid"
    }

    object AdminReportDetail : Screen("admin_report_detail/{reportId}") {
        fun createRoute(reportId: String) = "admin_report_detail/$reportId"
    }
}
