package com.example.colorapp

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val firestore: FirebaseFirestore = Firebase.firestore

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ColorApp(firestore)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorApp(firestore: FirebaseFirestore) {
    var colors by remember { mutableStateOf(listOf<ColorItem>()) }
    var showDialog by remember { mutableStateOf(false) }
    var hexValue by remember { mutableStateOf(TextFieldValue()) }
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf("") }

    // Fetch colors from Firestore on startup
    LaunchedEffect(Unit) {
        fetchColors(firestore) { fetchedColors ->
            colors = fetchedColors
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Add a Color") },
            text = {
                TextField(
                    value = hexValue,
                    onValueChange = { hexValue = it },
                    label = { Text("Hex Value (e.g., #FF0000)") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isValidHex(hexValue.text)) {
                        // Get the current timestamp
                        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("DD-MM-YYYY HH:mm:ss"))
                        val newColor = ColorItem(hexValue.text, timestamp, false)
                        colors = colors + newColor
                        hexValue = TextFieldValue()
                    }
                    showDialog = false
                }) {
                    Text("Add Color")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (syncing) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Syncing...") },
            text = { Text(syncMessage) },
            confirmButton = {
                TextButton(onClick = { syncing = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ColorApp") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Show pending count
                        val pendingCount = colors.count { !it.synced }
                        Text(
                            text = "$pendingCount",
                            color = Color.White,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                        )
                        // Sync Icon
                        Image(
                            painter = painterResource(id = R.drawable.sync),
                            contentDescription = "Sync Icon",
                            modifier = Modifier
                                .size(50.dp)
                                .padding(4.dp)
                                .clickable {
                                    syncColors(firestore, colors) { syncedCount, failedCount ->
                                        // Handle sync results
                                        if (syncedCount == 0) {
                                            syncMessage = "No colors to sync."
                                        } else {
                                            colors = colors.map { colorItem ->
                                                if (!colorItem.synced) {
                                                    colorItem.copy(synced = true)
                                                } else {
                                                    colorItem
                                                }
                                            }
                                            syncMessage =
                                                "Successfully synced $syncedCount colors. Failed: $failedCount"
                                        }
                                        syncing = false
                                    }
                                    syncing = true
                                    syncMessage = "Sync in progress..."

                                }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Add Colors")
                    Image(
                        painter = painterResource(id = R.drawable.add),
                        contentDescription = "Add Icon",
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(padding)
        ) {
            items(colors) { colorItem ->
                ColorItemView(colorItem)
            }
        }
    }
}

@Composable
fun ColorItemView(colorItem: ColorItem) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .background(Color(android.graphics.Color.parseColor(colorItem.hex)))
            .height(100.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        HexValueDisplay(colorItem)
    }
}

@Composable
fun HexValueDisplay(colorItem: ColorItem) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(
            text = colorItem.hex.uppercase(),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight= FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Created at:",
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text(
                    text = colorItem.timestamp.split(" ")[0],
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
    }
}

data class ColorItem(val hex: String, val timestamp: String, val synced: Boolean)

fun isValidHex(hex: String): Boolean {
    return hex.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$"))
}

// Fetch colors from Firestore
fun fetchColors(firestore: FirebaseFirestore, onSuccess: (List<ColorItem>) -> Unit) {
    firestore.collection("colors")
        .get()
        .addOnSuccessListener { result ->
            val fetchedColors = result.map { document ->
                ColorItem(
                    hex = document.getString("hex") ?: "#FFFFFF",
                    timestamp = document.getString("timestamp") ?: "",
                    synced = true
                )
            }
            onSuccess(fetchedColors)
        }
        .addOnFailureListener { e ->
            Log.w("ColorApp", "Error fetching colors from Firestore", e)
            onSuccess(emptyList())
        }
}

// Sync pending colors with Firestore and handle sync success/failure
fun syncColors(
    firestore: FirebaseFirestore,
    colors: List<ColorItem>,
    onSyncComplete: (Int, Int) -> Unit
) {
    val unsyncedColors = colors.filter { !it.synced }

    if (unsyncedColors.isEmpty()) {
        onSyncComplete(0, 0)
        return
    }

    var syncedCount = 0
    var failedCount = 0
    for (color in unsyncedColors) {
        val colorData = hashMapOf(
            "hex" to color.hex,
            "timestamp" to color.timestamp
        )
        firestore.collection("colors")
            .add(colorData)
            .addOnSuccessListener {
                syncedCount++
                if (syncedCount + failedCount == unsyncedColors.size) {
                    onSyncComplete(
                        syncedCount,
                        failedCount
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.w("ColorApp", "Error adding color to Firestore", e)
                failedCount++
                if (syncedCount + failedCount == unsyncedColors.size) {
                    onSyncComplete(
                        syncedCount,
                        failedCount
                    )
                }
            }
    }
}
