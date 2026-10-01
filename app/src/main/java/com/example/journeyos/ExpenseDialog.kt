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
fun AddExpenseDialog(onDismiss: () -> Unit, onAdd: (Double, String, String, String) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var date by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val categories = listOf("Food", "Transport", "Stay", "Activities", "Shopping", "Other")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add trip expense") },
        text = {
            Column {
                OutlinedTextField(value = amountText, onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) }, label = { Text("Amount (₹)") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Text("Category", color = Color.Gray, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.take(3).forEach { item ->
                        OutlinedButton(onClick = { category = item }, colors = ButtonDefaults.outlinedButtonColors(containerColor = if (category == item) Color(0xFFDDEEE9) else Color.Transparent)) { Text(item, fontSize = 11.sp) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.drop(3).forEach { item ->
                        OutlinedButton(onClick = { category = item }, colors = ButtonDefaults.outlinedButtonColors(containerColor = if (category == item) Color(0xFFDDEEE9) else Color.Transparent)) { Text(item, fontSize = 11.sp) }
                    }
                }
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (e.g. 01 Oct 2026)") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        amountText.toDoubleOrNull() ?: 0.0,
                        category,
                        date.ifBlank { "Date not set" },
                        description.trim()
                    )
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF009688)
                )
            ) {
                Text("Add expense")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

