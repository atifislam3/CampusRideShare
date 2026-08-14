package com.atif.campusrideshare.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.atif.campusrideshare.data.model.RideModel
import com.atif.campusrideshare.ui.components.EmptyState
import com.atif.campusrideshare.ui.components.RideCard
import com.atif.campusrideshare.ui.components.VehicleBadge
import com.atif.campusrideshare.ui.navigation.Screen
import com.atif.campusrideshare.ui.viewmodel.HomeViewModel
import com.atif.campusrideshare.util.Config
import com.atif.campusrideshare.util.VehicleUtils
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel
) {
    var viewMode by remember { mutableStateOf(ViewMode.Map) }
    val rides by homeViewModel.rides.collectAsState()
    val isRefreshing by homeViewModel.isRefreshing.collectAsState()
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val vehicleFilter by homeViewModel.vehicleFilter.collectAsState()

    var selectedRideForSheet by remember { mutableStateOf<RideModel?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Campus Ride Share") },
                    actions = {
                        IconButton(onClick = { 
                            viewMode = if (viewMode == ViewMode.Map) ViewMode.List else ViewMode.Map 
                        }) {
                            Icon(
                                imageVector = if (viewMode == ViewMode.Map) Icons.AutoMirrored.Filled.List else Icons.Default.Map,
                                contentDescription = "Toggle View"
                            )
                        }
                        IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                )
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { homeViewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search destination...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = vehicleFilter == null,
                        onClick = { homeViewModel.onVehicleFilterChanged(null) },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = vehicleFilter == Config.VEHICLE_CAR,
                        onClick = { homeViewModel.onVehicleFilterChanged(Config.VEHICLE_CAR) },
                        label = { Text("🚗 Cars") }
                    )
                    FilterChip(
                        selected = vehicleFilter == Config.VEHICLE_BIKE,
                        onClick = { homeViewModel.onVehicleFilterChanged(Config.VEHICLE_BIKE) },
                        label = { Text("🏍️ Bikes") }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.PostRide.route) }) {
                Icon(Icons.Default.Add, contentDescription = "Post Ride")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (viewMode == ViewMode.List) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { homeViewModel.refresh() }
                ) {
                    if (rides.isEmpty()) {
                        EmptyState(
                            message = "No Rides Found. Try adjusting your filters or post a new ride!",
                            icon = Icons.Default.DirectionsCar
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(rides) { ride ->
                                RideCard(ride = ride, onClick = {
                                    navController.navigate(Screen.RideDetail.createRoute(ride.rideId))
                                })
                            }
                        }
                    }
                }
            } else {
                MapViewContainer(
                    rides = rides,
                    onRideClick = { ride ->
                        selectedRideForSheet = ride
                        showSheet = true
                    }
                )
            }
        }

        if (showSheet && selectedRideForSheet != null) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            ) {
                RideBottomSheetContent(
                    ride = selectedRideForSheet!!,
                    onViewDetails = {
                        showSheet = false
                        navController.navigate(Screen.RideDetail.createRoute(selectedRideForSheet!!.rideId))
                    }
                )
            }
        }
    }
}

@Composable
fun MapViewContainer(
    rides: List<RideModel>,
    onRideClick: (RideModel) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    
    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    // Using a public OSM style URL
                    map.setStyle("https://demotiles.maplibre.org/style.json") { style: Style ->
                        // Add Car Icon
                        val carIcon = context.getDrawable(VehicleUtils.iconResFor(Config.VEHICLE_CAR))
                        carIcon?.let { style.addImage("car-icon", it) }
                        
                        // Add Bike Icon
                        val bikeIcon = context.getDrawable(VehicleUtils.iconResFor(Config.VEHICLE_BIKE))
                        bikeIcon?.let { style.addImage("bike-icon", it) }

                        val source = GeoJsonSource("rides-source")
                        style.addSource(source)

                        val layer = SymbolLayer("rides-layer", "rides-source")
                        layer.setProperties(
                            PropertyFactory.iconImage("{icon-id}"),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true)
                        )
                        style.addLayer(layer)

                        map.addOnMapClickListener { point ->
                            val features = map.queryRenderedFeatures(map.projection.toScreenLocation(point), "rides-layer")
                            if (features.isNotEmpty()) {
                                val rideId = features[0].getStringProperty("rideId")
                                val ride = rides.find { it.rideId == rideId }
                                if (ride != null) {
                                    onRideClick(ride)
                                }
                                true
                            } else false
                        }
                    }
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(33.6844, 73.0479)) // Default center
                        .zoom(12.0)
                        .build()
                }
            }
        },
        update = { view ->
            view.getMapAsync { map ->
                val style = map.style
                if (style != null && style.isFullyLoaded) {
                    val source = style.getSourceAs<GeoJsonSource>("rides-source")
                    source?.setGeoJson(createRidesGeoJson(rides))
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    // Lifecycle handling for MapView
    DisposableEffect(mapView) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
}

private fun createRidesGeoJson(rides: List<RideModel>): String {
    val featureCollection = JsonObject()
    featureCollection.addProperty("type", "FeatureCollection")
    val features = JsonArray()

    rides.forEach { ride ->
        val feature = JsonObject()
        feature.addProperty("type", "Feature")
        
        val geometry = JsonObject()
        geometry.addProperty("type", "Point")
        val coords = JsonArray()
        coords.add(ride.startLng)
        coords.add(ride.startLat)
        geometry.add("coordinates", coords)
        feature.add("geometry", geometry)

        val properties = JsonObject()
        properties.addProperty("rideId", ride.rideId)
        properties.addProperty("icon-id", if (ride.vehicleType == Config.VEHICLE_CAR) "car-icon" else "bike-icon")
        feature.add("properties", properties)

        features.add(feature)
    }
    featureCollection.add("features", features)
    return featureCollection.toString()
}

@Composable
fun RideBottomSheetContent(
    ride: RideModel,
    onViewDetails: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = ride.driverName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        VehicleBadge(
            vehicleType = ride.vehicleType,
            seatsLeft = ride.seatsLeft,
            costPerSeat = ride.costPerSeat
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = ride.destinationName,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onViewDetails,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Details")
        }
    }
}

enum class ViewMode {
    Map, List
}
