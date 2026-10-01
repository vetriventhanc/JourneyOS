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

val Ink = Color(0xFF172B3A)

val Canvas = Color(0xFFF5F7F5)
val Muted = Color(0xFF718096)

data class ItineraryActivity(
    val day: Int,
    val title: String,
    val time: String,
    val location: String
)

data class TripExpense(
    val amount: Double,
    val category: String,
    val date: String,
    val description: String
)

data class TravelDocument(val name: String, val uri: String)
data class PackingItem(val name: String, val packed: Boolean = false)

data class JourneyTrip(
    val destination: String,
    val dates: String,
    val note: String,
    val emoji: String,
    val photoUris: List<String> = emptyList(),
    val itinerary: List<ItineraryActivity> = emptyList(),
    val budget: Double = 0.0,
    val expenses: List<TripExpense> = emptyList(),
    val documents: List<TravelDocument> = emptyList(),
    val packingList: List<PackingItem> = emptyList()
)

private const val TRIPS_PREFS = "journeyos_trips"
private const val TRIPS_KEY = "saved_trips"

private fun defaultTrips() = listOf(
    JourneyTrip("Goa, India", "15–19 November", "5 days · Beach getaway", "🏝️"),
    JourneyTrip("Tokyo, Japan", "10–17 December", "8 days · City adventure", "🗼"),
    JourneyTrip("Munnar, India", "20–22 January", "3 days · Mountain escape", "⛰️")
)

fun loadTrips(context: android.content.Context): List<JourneyTrip> {
    val raw = context.getSharedPreferences(TRIPS_PREFS, android.content.Context.MODE_PRIVATE)
        .getString(TRIPS_KEY, null) ?: return defaultTrips()
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            val photos = item.optJSONArray("photos") ?: JSONArray()
            val itineraryJson = item.optJSONArray("itinerary") ?: JSONArray()
            val expensesJson = item.optJSONArray("expenses") ?: JSONArray()
            val documentsJson = item.optJSONArray("documents") ?: JSONArray()
            val packingJson = item.optJSONArray("packingList") ?: JSONArray()
            val documents = (0 until documentsJson.length()).map { j ->
                val document = documentsJson.getJSONObject(j)
                TravelDocument(
                    name = document.optString("name", "Travel document"),
                    uri = document.optString("uri", "")
                )
            }
            val packingList = (0 until packingJson.length()).map { j ->
                val packing = packingJson.getJSONObject(j)
                PackingItem(
                    name = packing.optString("name", ""),
                    packed = packing.optBoolean("packed", false)
                )
            }
            val expenses = (0 until expensesJson.length()).map { j ->
                val expense = expensesJson.getJSONObject(j)
                TripExpense(
                    amount = expense.optDouble("amount", 0.0),
                    category = expense.optString("category", "Other"),
                    date = expense.optString("date", ""),
                    description = expense.optString("description", "")
                )
            }
            val itinerary = (0 until itineraryJson.length()).map { j ->
                val activity = itineraryJson.getJSONObject(j)
                ItineraryActivity(
                    day = activity.optInt("day", 1).coerceAtLeast(1),
                    title = activity.optString("title"),
                    time = activity.optString("time"),
                    location = activity.optString("location")
                )
            }
            JourneyTrip(
                destination = item.optString("destination"),
                dates = item.optString("dates"),
                note = item.optString("note"),
                emoji = item.optString("emoji", "🧭"),
                photoUris = (0 until photos.length()).map { photos.getString(it) },
                itinerary = itinerary,
                budget = item.optDouble("budget", 0.0),
                expenses = expenses,
                documents = documents,
                packingList = packingList
            )
        }.ifEmpty { defaultTrips() }
    } catch (_: Exception) {
        defaultTrips()
    }
}

fun saveTrips(context: android.content.Context, trips: List<JourneyTrip>) {
    val array = JSONArray()
    trips.forEach { trip ->
        val photos = JSONArray()
        trip.photoUris.forEach { photos.put(it) }
        val expenses = JSONArray()
        trip.expenses.forEach { expense ->
            expenses.put(JSONObject().apply {
                put("amount", expense.amount)
                put("category", expense.category)
                put("date", expense.date)
                put("description", expense.description)
            })
        }
        val documents = JSONArray()
        trip.documents.forEach { document ->
            documents.put(JSONObject().apply {
                put("name", document.name)
                put("uri", document.uri)
            })
        }
        val packingList = JSONArray()
        trip.packingList.forEach { item ->
            packingList.put(JSONObject().apply {
                put("name", item.name)
                put("packed", item.packed)
            })
        }
        val itinerary = JSONArray()
        trip.itinerary.forEach { activity ->
            itinerary.put(
                JSONObject().apply {
                    put("day", activity.day)
                    put("title", activity.title)
                    put("time", activity.time)
                    put("location", activity.location)
                }
            )
        }
        array.put(
            JSONObject().apply {
                put("destination", trip.destination)
                put("dates", trip.dates)
                put("note", trip.note)
                put("emoji", trip.emoji)
                put("photos", photos)
                put("itinerary", itinerary)
                put("budget", trip.budget)
                put("expenses", expenses)
                put("documents", documents)
                put("packingList", packingList)
            }
        )
    }
    context.getSharedPreferences(TRIPS_PREFS, android.content.Context.MODE_PRIVATE)
        .edit().putString(TRIPS_KEY, array.toString()).apply()
}

