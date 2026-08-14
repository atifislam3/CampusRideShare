package com.atif.campusrideshare.service

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OSRM API Response Data Classes
 */
data class OsrmResponse(
    @SerializedName("routes") val routes: List<OsrmRoute>,
    @SerializedName("code") val code: String
)

data class OsrmRoute(
    @SerializedName("distance") val distance: Double, // in meters
    @SerializedName("geometry") val geometry: String  // encoded polyline
)

/**
 * Retrofit Interface for OSRM Public Routing API
 */
interface OsrmApi {
    @GET("/route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path("coordinates") coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline"
    ): OsrmResponse
}

/**
 * Repository for handling routing and distance calculation via OSRM.
 */
@Singleton
class OsrmRepository @Inject constructor(
    private val api: OsrmApi
) {
    /**
     * Fetches route data from start to destination.
     * Returns a Result containing a Pair of (Distance in KM, Encoded Polyline).
     */
    suspend fun getRouteDistanceAndPolyline(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double
    ): Result<Pair<Double, String>> = try {
        // OSRM expects coordinates in {longitude},{latitude} format
        val coordinates = "$startLng,$startLat;$destLng,$destLat"
        
        val response = api.getRoute(coordinates)
        
        if (response.code == "Ok" && response.routes.isNotEmpty()) {
            val primaryRoute = response.routes[0]
            val distanceKm = primaryRoute.distance / 1000.0
            Result.success(Pair(distanceKm, primaryRoute.geometry))
        } else {
            Result.failure(Exception("OSRM routing failed with code: ${response.code}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
