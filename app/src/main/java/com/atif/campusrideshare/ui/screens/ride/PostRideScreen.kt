package com.atif.campusrideshare.ui.screens.ride

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.atif.campusrideshare.ui.components.LoadingOverlay
import com.atif.campusrideshare.ui.navigation.Screen
import com.atif.campusrideshare.ui.viewmodel.PostRideUiState
import com.atif.campusrideshare.ui.viewmodel.PostRideViewModel
import com.atif.campusrideshare.util.Config
import com.atif.campusrideshare.util.PolylineDecoder
import com.atif.campusrideshare.util.VehicleUtils
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import java.net.URL
import java.net.URLEncoder
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRideScreen(
    navController: NavController,
    viewModel: PostRideViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val vehicleType by viewModel.vehicleType.collectAsState()
    val seats by viewModel.seats.collectAsState()
    
    val scrollState = rememberScrollState()
    val calendar = remember { Calendar.getInstance() }
    
    var pickingMode by remember { mutableStateOf(PickingMode.START) }
    var selectedDateText by remember { mutableStateOf("Select Date") }
    var selectedTimeText by remember { mutableStateOf("Select Time") }

    // Map control state
    var mapTarget by remember { mutableStateOf<LatLng?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is PostRideUiState.Success) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.PostRide.route) { inclusive = true }
            }
        }
    }

    // Trigger route preview when both points are set
    LaunchedEffect(viewModel.startLat, viewModel.destLat) {
        if (viewModel.startLat != 0.0 && viewModel.destLat != 0.0) {
            viewModel.updateRoutePreview()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Post a Ride") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .imePadding()
            ) {
                // Interactive Selection Guide
                Text(
                    text = if (pickingMode == PickingMode.START) "📍 Step 1: Set Start Point" else "🏁 Step 2: Set Destination",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(Color.LightGray, MaterialTheme.shapes.medium)
                ) {
                    MapViewContainer(
                        viewModel = viewModel, 
                        pickingMode = pickingMode, 
                        targetLocation = mapTarget
                    )
                    
                    // Floating Map Controls
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            tonalElevation = 6.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column {
                                IconButton(onClick = { pickingMode = PickingMode.START }) {
                                    Icon(
                                        Icons.Default.MyLocation, 
                                        contentDescription = "Pick Start",
                                        tint = if (pickingMode == PickingMode.START) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                                IconButton(onClick = { pickingMode = PickingMode.DESTINATION }) {
                                    Icon(
                                        Icons.Default.Place, 
                                        contentDescription = "Pick Destination",
                                        tint = if (pickingMode == PickingMode.DESTINATION) MaterialTheme.colorScheme.error else Color.Gray
                                    )
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick = {
                                val locationClient = LocationServices.getFusedLocationProviderClient(context)
                                scope.launch {
                                    try {
                                        @SuppressLint("MissingPermission")
                                        val location = locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                                        location?.let {
                                            mapTarget = LatLng(it.latitude, it.longitude)
                                        }
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Center on me")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Start Address with Text Search
                OutlinedTextField(
                    value = viewModel.startAddress,
                    onValueChange = { viewModel.startAddress = it },
                    label = { Text("Start Address") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton(onClick = { 
                            scope.launch { 
                                isSearching = true
                                searchLocation(viewModel.startAddress, context) { latLng ->
                                    viewModel.startLat = latLng.latitude
                                    viewModel.startLng = latLng.longitude
                                    mapTarget = latLng
                                }
                                isSearching = false
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search Start")
                        }
                    },
                    placeholder = { Text("Type and tap search icon...") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dest Address with Text Search
                OutlinedTextField(
                    value = viewModel.destinationName,
                    onValueChange = { viewModel.destinationName = it },
                    label = { Text("Destination Name") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    trailingIcon = {
                        IconButton(onClick = { 
                            scope.launch { 
                                isSearching = true
                                searchLocation(viewModel.destinationName, context) { latLng ->
                                    viewModel.destLat = latLng.latitude
                                    viewModel.destLng = latLng.longitude
                                    mapTarget = latLng
                                }
                                isSearching = false
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search Destination")
                        }
                    },
                    placeholder = { Text("Type and tap search icon...") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time Cost Display
                if (viewModel.estimatedCost > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Estimated Trip Cost",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "${Config.CURRENCY_SYMBOL} ${viewModel.estimatedCost.toInt()} per seat",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Distance",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = String.format("%.1f km", viewModel.estimatedDistance),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Vehicle Type Choice
                Text("Vehicle Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = vehicleType == Config.VEHICLE_CAR,
                        onClick = { viewModel.setVehicleType(Config.VEHICLE_CAR) },
                        label = { Text("🚗 Car") }
                    )
                    FilterChip(
                        selected = vehicleType == Config.VEHICLE_BIKE,
                        onClick = { viewModel.setVehicleType(Config.VEHICLE_BIKE) },
                        label = { Text("🏍️ Bike") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Seats Stepper
                Text("Available Seats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (vehicleType == Config.VEHICLE_CAR) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(onClick = { viewModel.setSeats(seats - 1) }) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                        }
                        Text(text = seats.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.setSeats(seats + 1) }) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                        }
                    }
                } else {
                    Text("1 seat (pillion)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date/Time Selection
                Text("Departure Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(context, { _, y, m, d ->
                                calendar.set(y, m, d)
                                selectedDateText = String.format("%02d/%02d/%d", d, m + 1, y)
                                viewModel.departureTime = calendar.timeInMillis
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedDateText)
                    }

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(context, { _, h, min ->
                                calendar.set(Calendar.HOUR_OF_DAY, h)
                                calendar.set(Calendar.MINUTE, min)
                                selectedTimeText = String.format("%02d:%02d", h, min)
                                viewModel.departureTime = calendar.timeInMillis
                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedTimeText)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Optional Note
                OutlinedTextField(
                    value = viewModel.note,
                    onValueChange = { viewModel.note = it },
                    label = { Text("Ride Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Near gate 2...") }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.submitRide() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = viewModel.startLat != 0.0 && viewModel.destLat != 0.0 && viewModel.departureTime != 0L
                ) {
                    Text("Post Ride", style = MaterialTheme.typography.titleMedium)
                }

                if (uiState is PostRideUiState.Error) {
                    Text(
                        text = (uiState as PostRideUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        LoadingOverlay(visible = uiState is PostRideUiState.Loading || isSearching)
    }
}

@Composable
private fun MapViewContainer(
    viewModel: PostRideViewModel,
    pickingMode: PickingMode,
    targetLocation: LatLng?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mapView = remember { MapView(context) }

    LaunchedEffect(targetLocation) {
        targetLocation?.let {
            mapView.getMapAsync { map ->
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 16.0), 1000)
                if (pickingMode == PickingMode.START) {
                    viewModel.startLat = it.latitude
                    viewModel.startLng = it.longitude
                    scope.launch {
                        reverseGeocode(it.latitude, it.longitude) { addr -> viewModel.startAddress = addr }
                    }
                } else if (pickingMode == PickingMode.DESTINATION) {
                    viewModel.destLat = it.latitude
                    viewModel.destLng = it.longitude
                    scope.launch {
                        reverseGeocode(it.latitude, it.longitude) { addr -> viewModel.destinationName = addr }
                    }
                }
                updateMapMarkers(map, viewModel)
            }
        }
    }

    // Update Polyline when viewModel.routePolyline changes
    LaunchedEffect(viewModel.routePolyline) {
        mapView.getMapAsync { map ->
            updateRouteOnMap(map, viewModel)
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                        ContextCompat.getDrawable(context, com.atif.campusrideshare.R.drawable.ic_car_pin)?.let { style.addImage("start-pin", drawableToBitmap(it)) }
                        ContextCompat.getDrawable(context, com.atif.campusrideshare.R.drawable.ic_dest_pin)?.let { style.addImage("dest-pin", drawableToBitmap(it)) }

                        style.addSource(GeoJsonSource("points-source"))
                        style.addLayer(SymbolLayer("points-layer", "points-source").apply {
                            setProperties(
                                PropertyFactory.iconImage("{icon-id}"),
                                PropertyFactory.iconSize(1.5f),
                                PropertyFactory.iconAllowOverlap(true)
                            )
                        })
                        
                        style.addSource(GeoJsonSource("route-source"))
                        style.addLayerBelow(LineLayer("route-layer", "route-source").apply {
                            setProperties(
                                PropertyFactory.lineColor(Color(0xFF2196F3).hashCode()),
                                PropertyFactory.lineWidth(5f),
                                PropertyFactory.lineJoin("round"),
                                PropertyFactory.lineCap("round")
                            )
                        }, "points-layer")

                        map.addOnMapClickListener { point ->
                            scope.launch {
                                if (pickingMode == PickingMode.START) {
                                    viewModel.startLat = point.latitude
                                    viewModel.startLng = point.longitude
                                    reverseGeocode(point.latitude, point.longitude) { addr -> viewModel.startAddress = addr }
                                } else {
                                    viewModel.destLat = point.latitude
                                    viewModel.destLng = point.longitude
                                    reverseGeocode(point.latitude, point.longitude) { addr -> viewModel.destinationName = addr }
                                }
                                updateMapMarkers(map, viewModel)
                            }
                            true
                        }
                    }
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(Config.DEFAULT_LAT, Config.DEFAULT_LNG))
                        .zoom(Config.DEFAULT_MAP_ZOOM)
                        .build()
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.parent?.requestDisallowInterceptTouchEvent(true)
        }
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

private fun updateRouteOnMap(map: org.maplibre.android.maps.MapLibreMap, viewModel: PostRideViewModel) {
    val style = map.style ?: return
    val source = style.getSourceAs<GeoJsonSource>("route-source") ?: return
    
    if (viewModel.routePolyline.isEmpty()) {
        source.setGeoJson("{}")
        return
    }

    val points = PolylineDecoder.decode(viewModel.routePolyline)
    val feature = JsonObject()
    feature.addProperty("type", "Feature")
    val geometry = JsonObject()
    geometry.addProperty("type", "LineString")
    val coords = JsonArray()
    points.forEach { 
        val c = JsonArray()
        c.add(it.longitude)
        c.add(it.latitude)
        coords.add(c)
    }
    geometry.add("coordinates", coords)
    feature.add("geometry", geometry)
    
    source.setGeoJson(feature.toString())

    // Zoom to fit both points
    if (points.isNotEmpty()) {
        val bounds = LatLngBounds.Builder()
            .include(LatLng(viewModel.startLat, viewModel.startLng))
            .include(LatLng(viewModel.destLat, viewModel.destLng))
            .build()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100), 1000)
    }
}

private suspend fun searchLocation(query: String, context: android.content.Context, onFound: (LatLng) -> Unit) {
    if (query.isBlank()) return
    
    withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=1")
            val connection = url.openConnection()
            connection.setRequestProperty("User-Agent", "CampusRideShareApp")
            val text = connection.getInputStream().bufferedReader().readText()
            val jsonArray = JsonParser.parseString(text).asJsonArray
            
            if (jsonArray.size() > 0) {
                val first = jsonArray[0].asJsonObject
                val lat = first.get("lat").asDouble
                val lon = first.get("lon").asDouble
                withContext(Dispatchers.Main) {
                    onFound(LatLng(lat, lon))
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Search failed: Check connection", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private suspend fun reverseGeocode(lat: Double, lng: Double, onResult: (String) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng")
            val connection = url.openConnection()
            connection.setRequestProperty("User-Agent", "CampusRideShareApp")
            val text = connection.getInputStream().bufferedReader().readText()
            val json = JsonParser.parseString(text).asJsonObject
            val address = json.get("display_name").asString.split(",").take(2).joinToString(",")
            withContext(Dispatchers.Main) { onResult(address) }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onResult("Location ($lat, $lng)") }
        }
    }
}

private fun updateMapMarkers(map: org.maplibre.android.maps.MapLibreMap, viewModel: PostRideViewModel) {
    val style = map.style ?: return
    val source = style.getSourceAs<GeoJsonSource>("points-source") ?: return
    
    val featureCollection = JsonObject()
    featureCollection.addProperty("type", "FeatureCollection")
    val features = JsonArray()

    if (viewModel.startLat != 0.0) {
        val f = JsonObject()
        f.addProperty("type", "Feature")
        val geom = JsonObject()
        geom.addProperty("type", "Point")
        val coords = JsonArray()
        coords.add(viewModel.startLng)
        coords.add(viewModel.startLat)
        geom.add("coordinates", coords)
        f.add("geometry", geom)
        val props = JsonObject()
        props.addProperty("icon-id", "start-pin")
        f.add("properties", props)
        features.add(f)
    }

    if (viewModel.destLat != 0.0) {
        val f = JsonObject()
        f.addProperty("type", "Feature")
        val geom = JsonObject()
        geom.addProperty("type", "Point")
        val coords = JsonArray()
        coords.add(viewModel.destLng)
        coords.add(viewModel.destLat)
        geom.add("coordinates", coords)
        f.add("geometry", geom)
        val props = JsonObject()
        props.addProperty("icon-id", "dest-pin")
        f.add("properties", props)
        features.add(f)
    }

    featureCollection.add("features", features)
    source.setGeoJson(featureCollection.toString())
}

enum class PickingMode {
    START, DESTINATION
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
