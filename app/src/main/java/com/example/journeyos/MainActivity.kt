package com.example.journeyos

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



private const val TRIPS_PREFS = "journeyos_trips"
private const val TRIPS_KEY = "saved_trips"

private fun defaultTrips() = listOf(
    JourneyTrip("Goa, India", "15–19 November", "5 days · Beach getaway", "🏝️"),
    JourneyTrip("Tokyo, Japan", "10–17 December", "8 days · City adventure", "🗼"),
    JourneyTrip("Munnar, India", "20–22 January", "3 days · Mountain escape", "⛰️")
)

@Composable
fun JourneyOSApp() {
    val context = LocalContext.current
    val trips = remember {
        mutableStateListOf<JourneyTrip>().apply {
            addAll(loadTrips(context))
        }
    }

    var selectedTab by remember { mutableStateOf("Home") }
    var showCreateTrip by remember { mutableStateOf(false) }
    var editingTripIndex by remember { mutableStateOf<Int?>(null) }
    var deletingTripIndex by remember { mutableStateOf<Int?>(null) }
    var selectedTripIndex by remember { mutableStateOf<Int?>(null) }
    var photoTargetIndex by remember { mutableStateOf<Int?>(null) }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    var showItineraryDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showPackingDialog by remember { mutableStateOf(false) }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val index = selectedTripIndex
        if (uri != null && index != null && index in trips.indices) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Some document providers do not support persistable permissions.
            }
            val displayName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
                ?.ifBlank { "Travel document" } ?: "Travel document"
            val current = trips[index]
            trips[index] = current.copy(
                documents = current.documents + TravelDocument(displayName, uri.toString())
            )
            saveTrips(context, trips)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        val index = photoTargetIndex
        if (index != null && index in trips.indices && uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Some picker providers do not support persistable permissions.
                }
            }
            val existing = trips[index].photoUris
            trips[index] = trips[index].copy(
                photoUris = (existing + uris.map { it.toString() }).distinct()
            )
            saveTrips(context, trips)
        }
        photoTargetIndex = null
    }

    fun addPhotos(index: Int) {
        photoTargetIndex = index
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf("Home" to "⌂", "My Trips" to "✈", "Explore" to "◎", "Profile" to "○")
                    .forEach { (label, symbol) ->
                        NavigationBarItem(
                            selected = selectedTab == label,
                            onClick = { selectedTab = label; selectedTripIndex = null },
                            icon = { Text(symbol, fontSize = 21.sp) },
                            label = { Text(label) }
                        )
                    }
            }
        }
    ) { innerPadding ->
        val galleryIndex = selectedTripIndex
        if (galleryIndex != null && galleryIndex in trips.indices) {
            TripGalleryScreen(
                trip = trips[galleryIndex],
                onBack = { selectedTripIndex = null },
                onAddPhotos = { addPhotos(galleryIndex) },
                onOpenPhoto = { selectedPhotoUri = it },
                onRemovePhoto = { uri ->
                    val current = trips[galleryIndex]
                    trips[galleryIndex] = current.copy(photoUris = current.photoUris.filterNot { it == uri })
                    saveTrips(context, trips)
                },
                onAddActivity = { showItineraryDialog = true },
                onDeleteActivity = { activity ->
                    val current = trips[galleryIndex]
                    trips[galleryIndex] = current.copy(
                        itinerary = current.itinerary.filterNot { it == activity }
                    )
                    saveTrips(context, trips)
                },
                onAddExpense = { showExpenseDialog = true },
                onSetBudget = { showBudgetDialog = true },
                onDeleteExpense = { expense ->
                    val current = trips[galleryIndex]
                    trips[galleryIndex] = current.copy(expenses = current.expenses.filterNot { it == expense })
                    saveTrips(context, trips)
                },
                onAddDocument = { documentPicker.launch(arrayOf("*/*")) },
                onOpenDocument = { document ->
                    try {
                        val uri = Uri.parse(document.uri)
                        val mimeType = context.contentResolver.getType(uri) ?: "*/*"
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        )
                    } catch (_: Exception) {
                        android.widget.Toast.makeText(context, "No app found to open this file.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onDeleteDocument = { document ->
                    val current = trips[galleryIndex]
                    trips[galleryIndex] = current.copy(documents = current.documents.filterNot { it == document })
                    saveTrips(context, trips)
                },
                onAddPackingItem = { showPackingDialog = true },
                onTogglePackingItem = { item ->
                    val current = trips[galleryIndex]
                    trips[galleryIndex] = current.copy(
                        packingList = current.packingList.map {
                            if (it == item) it.copy(packed = !it.packed) else it
                        }
                    )
                    saveTrips(context, trips)
                },
                onDeletePackingItem = { item ->
                    val current = trips[galleryIndex]
                    trips[galleryIndex] = current.copy(packingList = current.packingList.filterNot { it == item })
                    saveTrips(context, trips)
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            when (selectedTab) {
                "Home" -> HomeScreen(
                    trips = trips,
                    onCreateTrip = { showCreateTrip = true },
                    onOpenGallery = { selectedTripIndex = it },
                    onEditTrip = { editingTripIndex = it },
                    onDeleteTrip = { deletingTripIndex = it },
                    modifier = Modifier.padding(innerPadding)
                )
                "My Trips" -> TripsScreen(
                    trips = trips,
                    onCreateTrip = { showCreateTrip = true },
                    onOpenGallery = { selectedTripIndex = it },
                    onEditTrip = { editingTripIndex = it },
                    onDeleteTrip = { deletingTripIndex = it },
                    modifier = Modifier.padding(innerPadding)
                )
                "Explore" -> SimpleInfoScreen(
                    "Explore the world", "Your next adventure starts here.", "🌍",
                    Modifier.padding(innerPadding)
                )
                else -> SimpleInfoScreen(
                    "Your profile", "Your travel profile will live here.", "👤",
                    Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (showCreateTrip) {
        CreateTripDialog(
            onDismiss = { showCreateTrip = false },
            onCreate = { destination, dates ->
                trips.add(
                    0, JourneyTrip(
                        destination, dates.ifBlank { "Dates not set" },
                        "New adventure", "🧭"
                    )
                )
                saveTrips(context, trips)
                selectedTab = "My Trips"
                showCreateTrip = false
            }
        )
    }

    val editIndex = editingTripIndex
    if (editIndex != null && editIndex in trips.indices) {
        EditTripDialog(
            trip = trips[editIndex],
            onDismiss = { editingTripIndex = null },
            onSave = { destination, dates, note ->
                trips[editIndex] = trips[editIndex].copy(
                    destination = destination,
                    dates = dates.ifBlank { "Dates not set" },
                    note = note
                )
                saveTrips(context, trips)
                editingTripIndex = null
            }
        )
    }

    val deleteIndex = deletingTripIndex
    if (deleteIndex != null && deleteIndex in trips.indices) {
        AlertDialog(
            onDismissRequest = { deletingTripIndex = null },
            title = { Text("Delete trip?") },
            text = { Text("Delete ${trips[deleteIndex].destination} and remove it from My Trips? Its saved gallery references will also be removed from JourneyOS.") },
            confirmButton = {
                Button(
                    onClick = {
                        trips.removeAt(deleteIndex)
                        saveTrips(context, trips)
                        selectedTripIndex = null
                        deletingTripIndex = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB42318))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingTripIndex = null }) { Text("Cancel") }
            }
        )
    }

    val itineraryIndex = selectedTripIndex
    if (showItineraryDialog && itineraryIndex != null && itineraryIndex in trips.indices) {
        AddItineraryActivityDialog(
            onDismiss = { showItineraryDialog = false },
            onAdd = { day, title, time, location ->
                val current = trips[itineraryIndex]
                trips[itineraryIndex] = current.copy(
                    itinerary = current.itinerary + ItineraryActivity(day, title, time, location)
                )
                saveTrips(context, trips)
                showItineraryDialog = false
            }
        )
    }

    val expenseTripIndex = selectedTripIndex
    if (showExpenseDialog && expenseTripIndex != null && expenseTripIndex in trips.indices) {
        AddExpenseDialog(
            onDismiss = { showExpenseDialog = false },
            onAdd = { amount, category, date, description ->
                val current = trips[expenseTripIndex]
                trips[expenseTripIndex] = current.copy(
                    expenses = current.expenses + TripExpense(amount, category, date, description)
                )
                saveTrips(context, trips)
                showExpenseDialog = false
            }
        )
    }
    if (showBudgetDialog && expenseTripIndex != null && expenseTripIndex in trips.indices) {
        BudgetDialog(
            currentBudget = trips[expenseTripIndex].budget,
            onDismiss = { showBudgetDialog = false },
            onSave = { budget ->
                trips[expenseTripIndex] = trips[expenseTripIndex].copy(budget = budget)
                saveTrips(context, trips)
                showBudgetDialog = false
            }
        )
    }

    val packingTripIndex = selectedTripIndex

    }

@Composable
fun HomeScreen(
    trips: List<JourneyTrip>,
    onCreateTrip: () -> Unit,
    onOpenGallery: (Int) -> Unit,
    onEditTrip: (Int) -> Unit,
    onDeleteTrip: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("JOURNEYOS", color = Color(0xFF009688), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Your world,", fontSize = 30.sp, color = Ink, fontWeight = FontWeight.Bold)
                    Text("your journey.", fontSize = 30.sp, color = Color(0xFF009688), fontWeight = FontWeight.Bold)
                }
                Surface(shape = CircleShape, color = Color(0xFFDDEEE9), modifier = Modifier.size(54.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("✈️", fontSize = 25.sp) }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Text("THE WORLD IS WAITING", color = Color(0xFF9ADBD1), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Where will you\ngo next?", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = onCreateTrip, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)), shape = RoundedCornerShape(14.dp)) {
                        Text("＋  Plan a trip")
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Your journeys", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("${trips.size} trips", color = Muted, fontSize = 13.sp)
            }
        }
        item {
            val totalSpent = trips.sumOf { trip -> trip.expenses.sumOf { it.amount } }
            val totalBudget = trips.sumOf { it.budget }
            val tripsOverBudget = trips.count { trip ->
                trip.budget > 0 && trip.expenses.sumOf { it.amount } > trip.budget
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Budget insights · All trips", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total spent", color = Muted, fontSize = 12.sp)
                            Text("₹${"%.2f".format(totalSpent)}", color = Color(0xFF009688), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Combined budgets", color = Muted, fontSize = 12.sp)
                            Text(if (totalBudget > 0) "₹${"%.2f".format(totalBudget)}" else "Not set", color = Ink, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (tripsOverBudget > 0) "$tripsOverBudget trip(s) over budget"
                        else "No trips currently over budget",
                        color = if (tripsOverBudget > 0) Color(0xFFB42318) else Color(0xFF009688),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        if (trips.isEmpty()) {
            item { EmptyTripsCard(onCreateTrip) }
        } else {
            items(trips.take(2)) { trip ->
                val index = trips.indexOf(trip)
                TripCard(
                    trip,
                    onOpenGallery = { onOpenGallery(index) },
                    onEdit = { onEditTrip(index) },
                    onDelete = { onDeleteTrip(index) }
                )
            }
        }
        item {
            Button(onClick = onCreateTrip, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)), shape = RoundedCornerShape(14.dp)) {
                Text("＋  Create a new trip")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}






class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { JourneyOSApp() }
        }
    }
}
