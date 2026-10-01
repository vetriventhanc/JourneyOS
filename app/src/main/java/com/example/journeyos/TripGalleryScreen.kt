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
fun TripGalleryScreen(
    trip: JourneyTrip,
    onBack: () -> Unit,
    onAddPhotos: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onAddActivity: () -> Unit,
    onDeleteActivity: (ItineraryActivity) -> Unit,
    onAddExpense: () -> Unit,
    onSetBudget: () -> Unit,
    onDeleteExpense: (TripExpense) -> Unit,
    onAddDocument: () -> Unit,
    onOpenDocument: (TravelDocument) -> Unit,
    onDeleteDocument: (TravelDocument) -> Unit,
    onAddPackingItem: () -> Unit,
    onTogglePackingItem: (PackingItem) -> Unit,
    onDeletePackingItem: (PackingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("‹ Back", color = Color(0xFF009688)) }
                Column(Modifier.weight(1f)) {
                    Text(trip.destination, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${trip.photoUris.size} photos", color = Muted, fontSize = 13.sp)
                }
            }
            Button(
                onClick = onAddPhotos,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)),
                shape = RoundedCornerShape(14.dp)
            ) { Text("＋  Add photos") }
            Spacer(Modifier.height(6.dp))
            Text("Photo gallery", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        if (trip.photoUris.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", fontSize = 40.sp)
                        Text("Your memories start here", color = Ink, fontWeight = FontWeight.Bold)
                        Text("Add photos from this trip to create your gallery.", color = Muted)
                    }
                }
            }
        } else {
            items(trip.photoUris.chunked(3)) { photoRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    photoRow.forEach { uri ->
                        Box(Modifier.weight(1f)) {
                            GalleryThumbnail(
                                uriString = uri,
                                onClick = { onOpenPhoto(uri) },
                                onRemove = { onRemovePhoto(uri) }
                            )
                        }
                    }
                    repeat(3 - photoRow.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        item {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Itinerary", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("${trip.itinerary.size} planned activities", color = Muted, fontSize = 13.sp)
                }
                Button(
                    onClick = onAddActivity,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("＋ Add") }
            }
        }

        if (trip.itinerary.isEmpty()) {
            item {
                Text("No plans yet. Add activities for each day of your trip.", color = Muted, fontSize = 13.sp)
            }
        } else {
            items(trip.itinerary.sortedWith(compareBy<ItineraryActivity> { it.day }.thenBy { it.time })) { activity ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Color(0xFFE8F2EF), shape = RoundedCornerShape(10.dp)) {
                            Text("Day ${activity.day}", modifier = Modifier.padding(10.dp), color = Color(0xFF009688), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(activity.title, color = Ink, fontWeight = FontWeight.SemiBold)
                            if (activity.time.isNotBlank()) Text(activity.time, color = Color(0xFF009688), fontSize = 12.sp)
                            if (activity.location.isNotBlank()) Text("📍 ${activity.location}", color = Muted, fontSize = 12.sp)
                        }
                        TextButton(onClick = { onDeleteActivity(activity) }) {
                            Text("×", color = Color(0xFFB42318), fontSize = 20.sp)
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Trip expenses", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("${trip.expenses.size} expenses", color = Muted, fontSize = 13.sp)
                }
                Button(onClick = onAddExpense, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)), shape = RoundedCornerShape(12.dp)) { Text("＋ Add") }
            }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    val spent = trip.expenses.sumOf { it.amount }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Budget", color = Muted, fontSize = 12.sp); Text(if (trip.budget > 0) "₹${"%.2f".format(trip.budget)}" else "Not set", color = Ink, fontWeight = FontWeight.Bold) }
                        Column(horizontalAlignment = Alignment.End) { Text("Spent", color = Muted, fontSize = 12.sp); Text("₹${"%.2f".format(spent)}", color = Color(0xFF009688), fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (trip.budget > 0) {
                        val progress = (spent / trip.budget).toFloat().coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = when {
                                spent > trip.budget -> Color(0xFFB42318)
                                spent >= trip.budget * 0.8 -> Color(0xFFE59B25)
                                else -> Color(0xFF009688)
                            },
                            trackColor = Color(0xFFE8EFED)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("${(spent / trip.budget * 100).toInt()}% of budget used", color = Muted, fontSize = 12.sp)
                        if (spent > trip.budget) {
                            Text("⚠ Over budget by ₹${"%.2f".format(spent - trip.budget)}", color = Color(0xFFB42318), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else if (spent >= trip.budget * 0.8) {
                            Text("⚠ You’ve used at least 80% of this budget.", color = Color(0xFF9A6700), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (trip.budget > 0) {
                                if (trip.budget - spent >= 0) "Remaining: ₹${"%.2f".format(trip.budget - spent)}"
                                else "Over budget: ₹${"%.2f".format(spent - trip.budget)}"
                            } else "Set a budget to track remaining funds",
                            color = if (trip.budget > 0 && spent > trip.budget) Color(0xFFB42318) else Muted,
                            fontSize = 12.sp
                        )
                        TextButton(onClick = onSetBudget) { Text("${if (trip.budget > 0) "Edit" else "Set"} budget") }
                    }
                }
            }
        }
        if (trip.expenses.isEmpty()) {
            item { Text("No expenses yet. Add your first trip expense.", color = Muted, fontSize = 13.sp) }
        } else {
            items(trip.expenses.reversed()) { expense ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(expense.description.ifBlank { expense.category }, color = Ink, fontWeight = FontWeight.SemiBold)
                            Text("${expense.category} • ${expense.date}", color = Muted, fontSize = 12.sp)
                        }
                        Text("₹${"%.2f".format(expense.amount)}", color = Ink, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { onDeleteExpense(expense) }) { Text("×", color = Color(0xFFB42318), fontSize = 20.sp) }
                    }
                }
            }
            item {
                Text("Daily spending", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                trip.expenses
                    .groupBy { it.date.ifBlank { "Date not set" } }
                    .toSortedMap()
                    .forEach { (date, entries) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(date, color = Muted, fontSize = 13.sp)
                            Text("₹${"%.2f".format(entries.sumOf { it.amount })}", color = Ink, fontWeight = FontWeight.SemiBold)
                        }
                    }
            }
            item {
                Text("Spending by category", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                trip.expenses.groupBy { it.category }.toSortedMap().forEach { (category, entries) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category, color = Muted); Text("₹${"%.2f".format(entries.sumOf { it.amount })}", color = Ink, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Travel documents", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("${trip.documents.size} saved files", color = Muted, fontSize = 13.sp)
                }
                Button(
                    onClick = onAddDocument,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("＋ Add file") }
            }
        }
        if (trip.documents.isEmpty()) {
            item { Text("Keep tickets, bookings, and other trip files here.", color = Muted, fontSize = 13.sp) }
        } else {
            items(trip.documents) { document ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenDocument(document) }
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📄", fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(document.name, Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Medium, maxLines = 2)
                        TextButton(onClick = { onDeleteDocument(document) }) {
                            Text("Remove", color = Color(0xFFB42318), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Packing checklist", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("${trip.packingList.count { it.packed }} of ${trip.packingList.size} packed", color = Muted, fontSize = 13.sp)
                }
                Button(
                    onClick = onAddPackingItem,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("＋ Add item") }
            }
        }
        if (trip.packingList.isEmpty()) {
            item { Text("Add essentials to your checklist before you leave.", color = Muted, fontSize = 13.sp) }
        } else {
            items(trip.packingList) { packingItem ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = packingItem.packed,
                            onCheckedChange = { onTogglePackingItem(packingItem) }
                        )
                        Text(
                            packingItem.name,
                            Modifier.weight(1f),
                            color = if (packingItem.packed) Muted else Ink,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(onClick = { onDeletePackingItem(packingItem) }) {
                            Text("×", color = Color(0xFFB42318), fontSize = 20.sp)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}


