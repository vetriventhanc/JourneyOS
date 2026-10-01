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


@Composable
fun GalleryThumbnail(uriString: String, onClick: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uriString) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(Uri.parse(uriString))?.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) { null }
        }
    }
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFE8F2EF), RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap!!.asImageBitmap(), contentDescription = "Trip photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text("🖼️", fontSize = 28.sp)
        }
        Surface(
            color = Color(0xCC172B3A),
            shape = CircleShape,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(26.dp).clickable(onClick = onRemove)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("×", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PhotoPreviewDialog(uriString: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uriString) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(Uri.parse(uriString))?.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) { null }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        text = {
            if (bitmap != null) {
                Image(bitmap!!.asImageBitmap(), contentDescription = "Full-size trip photo", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(420.dp))
            } else {
                Text("This photo could not be opened.")
            }
        }
    )
}

@Composable
fun EmptyTripsCard(onCreateTrip: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🧭", fontSize = 40.sp)
            Spacer(Modifier.height(12.dp))
            Text("Your next adventure starts here", color = Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Create your first trip to get started.", color = Muted)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCreateTrip) { Text("Create trip") }
        }
    }
}

@Composable
fun SimpleInfoScreen(title: String, description: String, emoji: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 56.sp)
            Spacer(Modifier.height(18.dp))
            Text(title, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(description, color = Muted)
        }
    }
}

@Composable
fun CreateTripDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var destination by remember { mutableStateOf("") }
    var dates by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plan a new trip", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Where are you heading?", color = Muted)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = destination, onValueChange = { destination = it },
                    label = { Text("Destination") }, placeholder = { Text("e.g. Paris, France") },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = dates, onValueChange = { dates = it },
                    label = { Text("Travel dates (optional)") }, placeholder = { Text("e.g. 12–18 December") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(destination.trim(), dates.trim()) },
                enabled = destination.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
            ) { Text("Create trip") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


@Composable
fun EditTripDialog(
    trip: JourneyTrip,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var destination by remember(trip) { mutableStateOf(trip.destination) }
    var dates by remember(trip) { mutableStateOf(trip.dates) }
    var note by remember(trip) { mutableStateOf(trip.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit trip", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destination") },
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = dates,
                    onValueChange = { dates = it },
                    label = { Text("Travel dates") },
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Trip details") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(destination.trim(), dates.trim(), note.trim()) },
                enabled = destination.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
            ) { Text("Save changes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


@Composable
fun AddItineraryActivityDialog(
    onDismiss: () -> Unit,
    onAdd: (Int, String, String, String) -> Unit
) {
    var dayText by remember { mutableStateOf("1") }
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add itinerary activity", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = dayText,
                    onValueChange = { value -> dayText = value.filter(Char::isDigit).take(2) },
                    label = { Text("Trip day number") },
                    placeholder = { Text("e.g. 1") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Activity") },
                    placeholder = { Text("e.g. Visit the beach") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (optional)") },
                    placeholder = { Text("e.g. 10:00 AM") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (optional)") },
                    placeholder = { Text("e.g. Baga Beach") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        (dayText.toIntOrNull() ?: 1).coerceAtLeast(1),
                        title.trim(),
                        time.trim(),
                        location.trim()
                    )
                },
                enabled = title.isNotBlank() && (dayText.toIntOrNull() ?: 0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
            ) { Text("Add activity") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


@Composable
fun BudgetDialog(currentBudget: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var budgetText by remember(currentBudget) { mutableStateOf(if (currentBudget > 0) currentBudget.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set trip budget") },
        text = { OutlinedTextField(value = budgetText, onValueChange = { budgetText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) }, label = { Text("Budget (₹)") }, singleLine = true) },
        confirmButton = { Button(onClick = { onSave(budgetText.toDoubleOrNull() ?: 0.0) }, enabled = (budgetText.toDoubleOrNull() ?: -1.0) >= 0, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))) { Text("Save budget") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


