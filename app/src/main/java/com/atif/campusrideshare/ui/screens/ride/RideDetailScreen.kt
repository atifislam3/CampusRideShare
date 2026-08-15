package com.atif.campusrideshare.ui.screens.ride

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.atif.campusrideshare.data.model.RideModel
import com.atif.campusrideshare.ui.components.DriverCard
import com.atif.campusrideshare.ui.components.LoadingOverlay
import com.atif.campusrideshare.ui.viewmodel.RideDetailViewModel
import com.atif.campusrideshare.util.Config
import com.atif.campusrideshare.util.PolylineDecoder
import com.atif.campusrideshare.util.VehicleUtils
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import com.google.gson.JsonObject
import com.google.gson.JsonArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    navController: NavController,
    viewModel: RideDetailViewModel
) {
    val context = LocalContext.current
    val ride by viewModel.ride.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val driverLoc by viewModel.driverLocation.collectAsState()
    val isSharing by viewModel.isLocationSharing.collectAsState()
    val requests by viewModel.requests.collectAsState()
    val uiMessage by viewModel.uiState.collectAsState()

    var showReportDialog by remember { mutableStateOf(false) }

    // Handle UI Messages (Errors or Success)
    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            if (msg.startsWith("SUCCESS")) {
                Toast.makeText(context, msg.replace("SUCCESS: ", ""), Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Trip Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Default.OutlinedFlag, contentDescription = "Report", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            ride?.let { currentRide ->
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Info
                    Box(modifier = Modifier.padding(16.dp)) {
                        DriverCard(ride = currentRide)
                    }

                    // Integrated Map
                    Box(modifier = Modifier.weight(1f)) {
                        MapViewContainer(currentRide, driverLoc)
                        
                        // Pickup/Drop Labels Overlay
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shadowElevation = 4.dp
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(currentRide.startAddress, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(currentRide.destinationName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                        }
                    }

                    // Premium Footer
                    Surface(
                        tonalElevation = 8.dp,
                        shadowElevation = 16.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                            if (user?.uid == currentRide.driverUid) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { viewModel.toggleLocationSharing() },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSharing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(if (isSharing) Icons.Default.LocationOff else Icons.Default.GpsFixed, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isSharing) "Stop GPS" else "Share GPS")
                                    }
                                    
                                    if (currentRide.status != Config.STATUS_COMPLETED) {
                                        Button(
                                            onClick = { viewModel.markCompleted() },
                                            modifier = Modifier.weight(1f).height(56.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Text("Finish Trip")
                                        }
                                    }
                                }
                            } else {
                                val isFull = currentRide.seatsLeft <= 0
                                val hasRequested = requests.any { it.passengerUid == user?.uid }
                                val requestStatus = requests.find { it.passengerUid == user?.uid }?.status
                                
                                Button(
                                    onClick = { viewModel.requestToJoin() },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    enabled = !isFull && !hasRequested && currentRide.status == Config.STATUS_OPEN,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = if (hasRequested) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) else ButtonDefaults.buttonColors()
                                ) {
                                    val buttonText = when {
                                        hasRequested -> "Requested (${requestStatus?.replaceFirstChar { it.uppercase() }})"
                                        isFull -> "Ride is Full"
                                        else -> "Request to Join • ${Config.CURRENCY_SYMBOL}${currentRide.costPerSeat.toInt()}"
                                    }
                                    Text(
                                        text = buttonText,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            } ?: LoadingOverlay(visible = true)

            // Error Dialog
            if (uiMessage != null && !uiMessage!!.startsWith("SUCCESS")) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    title = { Text("Notice") },
                    text = { Text(uiMessage!!) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
                    }
                )
            }
        }
    }

    if (showReportDialog) {
        ReportReasonDialog(
            onDismiss = { showReportDialog = false },
            onReport = { reason, desc -> showReportDialog = false }
        )
    }
}

@Composable
fun MapViewContainer(
    ride: RideModel,
    driverLocation: Pair<Double, Double>?
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    
    val animLat = remember { Animatable(ride.startLat.toFloat()) }
    val animLng = remember { Animatable(ride.startLng.toFloat()) }

    LaunchedEffect(driverLocation) {
        driverLocation?.let { (lat, lng) ->
            animLat.animateTo(lat.toFloat(), tween(1000))
            animLng.animateTo(lng.toFloat(), tween(1000))
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    // Set initial center on pickup point so it doesn't show the whole world first
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(ride.startLat, ride.startLng))
                        .zoom(15.0)
                        .build()

                    map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                        // 1. Setup Route
                        val routePoints = PolylineDecoder.decode(ride.routePolyline)
                        val validPoints = routePoints.filter { it.latitude != 0.0 && it.longitude != 0.0 }
                        
                        val routeFeature = JsonObject().apply {
                            addProperty("type", "Feature")
                            add("geometry", JsonObject().apply {
                                addProperty("type", "LineString")
                                add("coordinates", JsonArray().apply {
                                    validPoints.forEach {
                                        add(JsonArray().apply { add(it.longitude); add(it.latitude) })
                                    }
                                })
                            })
                        }
                        style.addSource(GeoJsonSource("route-source", routeFeature.toString()))
                        style.addLayer(LineLayer("route-layer", "route-source").apply {
                            setProperties(
                                PropertyFactory.lineColor(Color(0xFF2196F3).hashCode()),
                                PropertyFactory.lineWidth(6f),
                                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                            )
                        })

                        // 2. Setup Markers
                        val driverIcon = ContextCompat.getDrawable(context, VehicleUtils.iconResFor(ride.vehicleType))
                        driverIcon?.let { style.addImage("driver-icon", drawableToBitmap(it)) }
                        
                        ContextCompat.getDrawable(context, com.atif.campusrideshare.R.drawable.ic_car_pin)?.let { style.addImage("start-pin", drawableToBitmap(it)) }
                        ContextCompat.getDrawable(context, com.atif.campusrideshare.R.drawable.ic_dest_pin)?.let { style.addImage("dest-pin", drawableToBitmap(it)) }

                        style.addSource(GeoJsonSource("driver-source"))
                        style.addLayer(SymbolLayer("driver-layer", "driver-source").apply {
                            setProperties(
                                PropertyFactory.iconImage("driver-icon"),
                                PropertyFactory.iconSize(1.4f),
                                PropertyFactory.iconAllowOverlap(true)
                            )
                        })

                        // 3. Robust Zoom to fit route
                        if (validPoints.size >= 2) {
                            val bounds = LatLngBounds.Builder().includes(validPoints).build()
                            mapView.post {
                                map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150), 1500)
                            }
                        }
                    }
                }
            }
        },
        update = { view ->
            view.getMapAsync { map ->
                val style = map.style
                if (style != null && style.isFullyLoaded) {
                    val source = style.getSourceAs<GeoJsonSource>("driver-source")
                    val point = JsonObject().apply {
                        addProperty("type", "Point")
                        add("coordinates", JsonArray().apply {
                            add(animLng.value.toDouble())
                            add(animLat.value.toDouble())
                        })
                    }
                    val feature = JsonObject().apply {
                        addProperty("type", "Feature")
                        add("geometry", point)
                    }
                    source?.setGeoJson(feature.toString())
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

@Composable
fun ReportReasonDialog(
    onDismiss: () -> Unit,
    onReport: (String, String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(Config.REPORT_REASONS.first()) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Issue") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Config.REPORT_REASONS.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(selected = (reason == selectedReason), onClick = { selectedReason = reason })
                        Text(text = reason, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onReport(selectedReason, description) }) { Text("Submit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
