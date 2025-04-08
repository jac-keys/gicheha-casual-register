package com.gichehafarm.registry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gichehafarm.registry.data.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ViewRegisteredSupervisorsActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DatabaseHelper.getInstance(this)

        setContent {
            ViewSupervisorsScreen(dbHelper)
        }
    }
}

@Composable
fun ViewSupervisorsScreen(dbHelper: DatabaseHelper) {
    var supervisors by remember { mutableStateOf(emptyList<Supervisor>()) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var sortBy by remember { mutableStateOf("Name") }
    val coroutineScope = rememberCoroutineScope()

    // Function to reload supervisors from the database
    fun reloadSupervisors() {
        coroutineScope.launch {
            val loadedSupervisors = withContext(Dispatchers.IO) { dbHelper.loadSupervisors() }
            withContext(Dispatchers.Main) {
                supervisors = loadedSupervisors
            }
        }
    }

    // Function to delete a supervisor
    fun deleteSupervisor(worker: Supervisor) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) { dbHelper.deleteSupervisor(worker.id) }
            reloadSupervisors() // Refresh the list after deletion
        }
    }

    // Load supervisors when the screen is first displayed
    LaunchedEffect(Unit) { reloadSupervisors() }

    // Filter and sort the list of supervisors based on the search query and sort option
    val filteredWorkers = supervisors.filter {
        val query = searchQuery.text.lowercase()
        it.firstName.lowercase().contains(query) ||
                it.surname.lowercase().contains(query) ||
                it.workNumber.toString().contains(query) ||
                it.phone.contains(query) ||
                it.idNumber.contains(query)
    }.sortedWith(
        when (sortBy) {
            "Name" -> compareBy { it.firstName }
            "Age" -> compareBy { calculateAge(it.dateOfBirth) }
            "Work Number" -> compareBy { it.workNumber }
            else -> compareBy { it.firstName }
        }
    )

    // Main UI layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF2E7D32), Color(0xFF81C784)))),
        contentAlignment = Alignment.Center
    ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                // Company Logo
                Image(
                    painter = painterResource(id = R.drawable.splash_screen),
                    contentDescription = "Company Logo",
                    modifier = Modifier.size(100.dp).clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("Registered Supervisors", fontSize = 22.sp, color = Color.White)
                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search", color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Sort Dropdown
                SortSupervisorsDropdown(sortBy, onSortChange = { sortBy = it })

                Spacer(modifier = Modifier.height(8.dp))

                // List of Supervisors
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(filteredWorkers) { worker ->
                        SupervisorRow(worker, onDelete = { deleteSupervisor(worker) })
                    }
                }
            }
        }
    }


@Composable
fun SortSupervisorsDropdown(selectedSort: String, onSortChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Name", "Age", "Work Number")

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        OutlinedButton(onClick = { expanded = true },
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Blue, // Set the background color to blue
                contentColor = Color.Black // Set the text color to white
            )) {
            Text("Sort by: $selectedSort", color = Color.Black)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(onClick = {
                    onSortChange(option)
                    expanded = false
                }) {
                    Text(text = option, color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun SupervisorRow(worker: Supervisor, onDelete: () -> Unit) {
    val age = remember(worker.dateOfBirth) {
        calculateAge(worker.dateOfBirth)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Name: ${worker.firstName} ${worker.surname}")
            Text(text = "Work Number: ${worker.workNumber}")
            Text(text = "Phone: ${worker.phone}")
            Text(text = "ID Number: ${worker.idNumber}")
            Text(text = "Age: $age years")
            Text(text = "Payment Number: ${worker.paymentNumber}")

            Spacer(modifier = Modifier.height(8.dp))

            // Delete Button
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }

    // Confirm Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Supervisor") },
            text = { Text("Are you sure you want to delete ${worker.firstName} ${worker.surname}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Function to calculate age from a date string (e.g., "yyyy-MM-dd")
fun calculateAge(dateOfBirth: String): Int {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val birthDate = dateFormat.parse(dateOfBirth) ?: return -1
    val today = Calendar.getInstance()
    val birthCalendar = Calendar.getInstance().apply { time = birthDate }
    var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
    if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
        age--
    }
    return age
}

// Data class for Supervisor
data class Supervisor(
    val id: Int,
    val workNumber: String,
    val firstName: String,
    val surname: String,
    val dateOfBirth: String,
    val phone: String,
    val idNumber: String,
    val paymentNumber: String,
)