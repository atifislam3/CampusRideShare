package com.atif.campusrideshare.util

object Config {
    // Firebase
    const val FIREBASE_DB_URL = "https://your-project-id-default-rtdb.firebaseio.com"
    
    // Networking
    const val OSRM_BASE_URL = "https://router.project-osrm.org"
    const val RETROFIT_TIMEOUT_SECONDS = 30L

    // Pricing & Limits
    const val RS_PER_KM = 20.0
    const val MAX_CAR_SEATS = 4
    const val MIN_SEATS = 1
    const val RATING_MAX_STARS = 5

    // Vehicle Types
    const val VEHICLE_CAR = "car"
    const val VEHICLE_BIKE = "bike"

    // Time & Intervals
    const val LOCATION_UPDATE_INTERVAL_MS = 5000L
    const val DEBOUNCE_SEARCH_MS = 500L
    const val DATE_FORMAT_FULL = "EEE, dd MMM yyyy, hh:mm a"
    const val TIME_FORMAT_ONLY = "hh:mm a"

    // Ride Status
    const val STATUS_OPEN = "open"
    const val STATUS_FULL = "full"
    const val STATUS_COMPLETED = "completed"
    const val STATUS_CANCELLED = "cancelled"

    // Request Status
    const val REQUEST_PENDING = "pending"
    const val REQUEST_ACCEPTED = "accepted"
    const val REQUEST_REJECTED = "rejected"
    const val REQUEST_CANCELLED = "cancelled"

    // User Roles
    const val ROLE_USER = "user"
    const val ROLE_ADMIN = "admin"

    // Report Status
    const val REPORT_PENDING = "pending"
    const val REPORT_RESOLVED = "resolved"
    const val REPORT_DISMISSED = "dismissed"

    // Notifications
    const val NOTIF_TYPE_RIDE_REQUEST = "ride_request"
    const val NOTIF_TYPE_REQUEST_ACCEPTED = "request_accepted"
    const val NOTIF_TYPE_REQUEST_REJECTED = "request_rejected"
    const val NOTIF_TYPE_RIDE_CANCELLED = "ride_cancelled"
    const val NOTIF_TYPE_RIDE_COMPLETED = "ride_completed"
    const val NOTIF_TYPE_NEW_RATING = "new_rating"

    // Map Configuration
    const val DEFAULT_MAP_ZOOM = 14.0
    const val ROUTE_LINE_WIDTH = 5f

    // Report Reasons
    val REPORT_REASONS = listOf(
        "Rude behavior",
        "Unsafe driving",
        "No-show",
        "Harassment",
        "Vehicle condition",
        "Incorrect route",
        "Other"
    )
}
