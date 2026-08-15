package com.atif.campusrideshare.ui.screens.home

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.atif.campusrideshare.data.model.RideModel
import com.atif.campusrideshare.ui.components.EmptyState
import com.atif.campusrideshare.ui.components.RideCard
import com.atif.campusrideshare.ui.navigation.Screen
import com.atif.campusrideshare.ui.viewmodel.HomeViewModel
import com.atif.campusrideshare.util.Config
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.maplibre.android.camera.CameraUpdateFactory
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
    val rides by homeViewModel.rides.collectAsState()
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val vehicleFilter by homeViewModel.vehicleFilter.collectAsState()

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    )
    val scope = rememberCoroutineScope()
    
    var targetLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedRideForSheet by remember { mutableStateOf<RideModel?>(null) }

    val isSheetExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.MyRides.route) },
                    icon = { Icon(Icons.Default.DirectionsCar, null) },
                    label = { Text("My Rides") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Notifications.route) },
                    icon = { Icon(Icons.Default.Notifications, null) },
                    label = { Text("Alerts") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Profile.route) },
                    icon = { Icon(Icons.Default.Person, null) },
                    label = { Text("Profile") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = 260.dp,
                sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                sheetContainerColor = MaterialTheme.colorScheme.surface,
                sheetShadowElevation = 16.dp,
                sheetContent = {
                    Column(modifier = Modifier.fillMaxHeight(0.85f)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(modifier = Modifier.size(40.dp, 4.dp), color = MaterialTheme.colorScheme.outlineVariant, shape = CircleShape) {}
                        }

                        Text(
                            text = if (searchQuery.isEmpty()) "Suggested Rides" else "Search Results",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )

                        if (rides.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                EmptyState(
                                    message = if (searchQuery.isEmpty()) "No rides nearby." else "No matches found.",
                                    icon = Icons.Default.DirectionsCar
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                items(rides) { ride ->
                                    RideCard(ride = ride, onClick = {
                                        targetLocation = LatLng(ride.startLat, ride.startLng)
                                        selectedRideForSheet = ride
                                        scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                                    })
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    MapViewContainer(
                        rides = rides,
                        targetLocation = targetLocation,
                        onRideClick = { ride ->
                            selectedRideForSheet = ride
                            targetLocation = LatLng(ride.startLat, ride.startLng)
                        }
                    )
                }
            }

            // --- OVERLAYS ---

            // 1. Fully Integrated Search Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.statusBarsPadding().padding(bottom = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { homeViewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search location or driver") },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                    
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = vehicleFilter == null,
                            onClick = { homeViewModel.onVehicleFilterChanged(null) },
                            label = { Text("All") },
                            shape = CircleShape
                        )
                        FilterChip(
                            selected = vehicleFilter == Config.VEHICLE_CAR,
                            onClick = { homeViewModel.onVehicleFilterChanged(Config.VEHICLE_CAR) },
                            label = { Text("Cars") },
                            shape = CircleShape
                        )
                        FilterChip(
                            selected = vehicleFilter == Config.VEHICLE_BIKE,
                            onClick = { homeViewModel.onVehicleFilterChanged(Config.VEHICLE_BIKE) },
                            label = { Text("Bikes") },
                            shape = CircleShape
                        )
                    }
                }
            }

            // 2. Animated Floating Buttons
            AnimatedVisibility(
                visible = !isSheetExpanded,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 280.dp, end = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val context = LocalContext.current
                    FloatingActionButton(
                        onClick = {
                            val locationClient = LocationServices.getFusedLocationProviderClient(context)
                            scope.launch {
                                try {
                                    @SuppressLint("MissingPermission")
                                    val location = locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                                    location?.let { targetLocation = LatLng(it.latitude, it.longitude) }
                                } catch (e: Exception) { }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, "My Location")
                    }

                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate(Screen.PostRide.route) },
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text("Offer Ride") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // 3. Selection Card Overlay
            if (selectedRideForSheet != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 280.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(12.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedRideForSheet!!.driverName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("To: ${selectedRideForSheet!!.destinationName}", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = { navController.navigate(Screen.RideDetail.createRoute(selectedRideForSheet!!.rideId)) },
                                shape = CircleShape
                            ) {
                                Text("Book Now")
                            }
                            IconButton(onClick = { selectedRideForSheet = null }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapViewContainer(
    rides: List<RideModel>,
    targetLocation: LatLng?,
    onRideClick: (RideModel) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    
    LaunchedEffect(targetLocation) {
        targetLocation?.let {
            mapView.getMapAsync { map ->
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 16.0), 1200)
            }
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style: Style ->
                        ContextCompat.getDrawable(context, com.atif.campusrideshare.R.drawable.ic_car_pin)?.let { 
                            style.addImage("car-icon", drawableToBitmap(it)) 
                        }
                        ContextCompat.getDrawable(context, com.atif.campusrideshare.R.drawable.ic_bike_pin)?.let { 
                            style.addImage("bike-icon", drawableToBitmap(it)) 
                        }
                        style.addSource(GeoJsonSource("rides-source"))
                        style.addLayer(SymbolLayer("rides-layer", "rides-source").apply {
                            setProperties(
                                PropertyFactory.iconImage("{icon-id}"),
                                PropertyFactory.iconAllowOverlap(true),
                                PropertyFactory.iconIgnorePlacement(true),
                                PropertyFactory.iconSize(1.6f)
                            )
                        })
                        map.addOnMapClickListener { point ->
                            val features = map.queryRenderedFeatures(map.projection.toScreenLocation(point), "rides-layer")
                            if (features.isNotEmpty()) {
                                val rideId = features[0].getStringProperty("rideId")
                                val ride = rides.find { it.rideId == rideId }
                                if (ride != null) onRideClick(ride)
                                true
                            } else false
                        }
                    }
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(Config.DEFAULT_LAT, Config.DEFAULT_LNG))
                        .zoom(Config.DEFAULT_MAP_ZOOM)
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
        val geometry = JsonObject().apply {
            addProperty("type", "Point")
            add("coordinates", JsonArray().apply { add(ride.startLng); add(ride.startLat) })
        }
        feature.add("geometry", geometry)
        val properties = JsonObject().apply {
            addProperty("rideId", ride.rideId)
            addProperty("icon-id", if (ride.vehicleType == Config.VEHICLE_CAR) "car-icon" else "bike-icon")
        }
        feature.add("properties", properties)
        features.add(feature)
    }
    featureCollection.add("features", features)
    return featureCollection.toString()
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth.coerceAtLeast(1),
        drawable.intrinsicHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
