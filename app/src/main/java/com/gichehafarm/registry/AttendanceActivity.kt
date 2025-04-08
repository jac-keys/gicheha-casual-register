package com.gichehafarm.registry

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import java.io.File
import androidx.compose.ui.graphics.Brush
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gichehafarm.registry.data.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AttendanceActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DatabaseHelper.getInstance(this)
        setContent {
            AttendanceScreen(dbHelper)
        }
    }
}

@Composable
fun AttendanceScreen(dbHelper: DatabaseHelper) {
    val context = LocalContext.current
    // Assuming CasualWorker is defined elsewhere in your project.
    val casualWorkers = remember { mutableStateListOf<CasualWorker>() }
    val attendanceStatus = remember { mutableStateMapOf<String, Boolean>() }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSaveConfirmation by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val currentDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Load workers and initialize attendance status.
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val workers = withContext(Dispatchers.IO) { dbHelper.loadCasualWorkers() }
            casualWorkers.clear()
            casualWorkers.addAll(workers)

            // Initialize attendance status for each worker.
            workers.forEach { worker ->
                val isPresent = withContext(Dispatchers.IO) {
                    dbHelper.getWorkerAttendanceStatus(worker.workNumber, currentDate)
                }
                attendanceStatus[worker.workNumber] = isPresent
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load workers: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
            .background(
                Brush.verticalGradient(listOf(Color(0xFF2E7D32), Color(0xFF81C784)))
            )
    ) {
        // Company Logo
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.splash_screen),
                contentDescription = "Company Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Mark Today's Attendance",
            fontSize = 22.sp,
            color = Color.White,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by Name or Work No.") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = {
                coroutineScope.launch {
                    casualWorkers.forEach { worker ->
                        withContext(Dispatchers.IO) {
                            dbHelper.recordAttendance(
                                workNumber = worker.workNumber,
                                date = currentDate,
                                attended = true
                            )
                        }
                        attendanceStatus[worker.workNumber] = true
                    }
                }
            }) {
                Text("Mark All Present")
            }

            TextButton(onClick = {
                coroutineScope.launch {
                    casualWorkers.forEach { worker ->
                        withContext(Dispatchers.IO) {
                            dbHelper.recordAttendance(
                                workNumber = worker.workNumber,
                                date = currentDate,
                                attended = false
                            )
                        }
                        attendanceStatus[worker.workNumber] = false
                    }
                }
            }) {
                Text("Clear All")
            }

            TextButton(onClick = { showSaveConfirmation = true }) {
                Text("Save to HR")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Name",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(3f)
            )
            Text(
                "Work No",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(2f)
            )
            Text(
                "ID No",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(2f)
            )
            Text(
                "Present",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
        Divider(color = Color.Black, thickness = 1.dp)

        // Worker List
        LazyColumn {
            items(
                items = casualWorkers.filter { worker ->
                    worker.firstName.contains(searchQuery, ignoreCase = true) ||
                            worker.surname.contains(searchQuery, ignoreCase = true) ||
                            worker.workNumber.contains(searchQuery)
                }
            ) { worker ->
                // Derived state for attendance status of this worker.
                val isPresent by remember(worker.workNumber) {
                    derivedStateOf { attendanceStatus[worker.workNumber] ?: false }
                }
                WorkerAttendanceRow(
                    worker = worker,
                    isPresent = isPresent,
                    onAttendanceChanged = { present ->
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                dbHelper.recordAttendance(
                                    workNumber = worker.workNumber,
                                    date = currentDate,
                                    attended = present
                                )
                            }
                            attendanceStatus[worker.workNumber] = present
                        }
                    }
                )
                Divider(color = Color.LightGray, thickness = 0.5.dp)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Export Button
        Button(
            onClick = {
                coroutineScope.launch {
                    exportAttendanceToCSV(
                        context = context,
                        workers = casualWorkers,
                        attendanceStatus = attendanceStatus
                    )
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Export to CSV")
        }
    }

    // Save Confirmation Dialog
    if (showSaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmation = false },
            title = { Text("Save Attendance") },
            text = { Text("Are you sure you want to save today's attendance to HR?") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        try {
                            val presentWorkers = casualWorkers.filter {
                                attendanceStatus[it.workNumber] == true
                            }
                            if (presentWorkers.isEmpty()) {
                                errorMessage = "No workers marked as present"
                                return@launch
                            }
                            withContext(Dispatchers.IO) {
                                dbHelper.saveAttendanceToHr(presentWorkers, currentDate)
                            }
                            Toast.makeText(context, "Attendance saved to HR", Toast.LENGTH_SHORT).show()
                            context.sendBroadcast(Intent("ATTENDANCE_SAVED_TO_HR"))
                        } catch (e: Exception) {
                            errorMessage = "Failed to save: ${e.message}"
                        }
                    }
                    showSaveConfirmation = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Show error message as a Toast if any error occurs.
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            errorMessage = null
        }
    }
}

@Composable
fun WorkerAttendanceRow(
    worker: CasualWorker,
    isPresent: Boolean,
    onAttendanceChanged: (Boolean) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Attendance") },
            text = {
                Text("Mark ${worker.firstName} as ${if (!isPresent) "Present" else "Absent"}?")
            },
            confirmButton = {
                TextButton(onClick = {
                    onAttendanceChanged(!isPresent)
                    showDialog = false
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${worker.firstName} ${worker.surname}",
            modifier = Modifier.weight(3f),
            color = Color.White
        )
        Text(
            text = worker.workNumber,
            modifier = Modifier.weight(2f),
            color = Color.White
        )
        Text(
            text = worker.idNumber,
            modifier = Modifier.weight(2f),
            color = Color.White
        )
        Checkbox(
            checked = isPresent,
            onCheckedChange = { showDialog = true },
            modifier = Modifier.weight(1f)
        )
    }
}

private suspend fun exportAttendanceToCSV(
    context: Context,
    workers: List<CasualWorker>,
    attendanceStatus: Map<String, Boolean>
) {
    withContext(Dispatchers.IO) {
        val fileName = "Attendance_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.csv"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        try {
            FileWriter(file).use { writer ->
                writer.append("Name,Work Number,ID Number,Attendance Status\n")
                workers.forEach { worker ->
                    val status = if (attendanceStatus[worker.workNumber] == true) "Present" else "Absent"
                    writer.append("${worker.firstName} ${worker.surname},${worker.workNumber},${worker.idNumber},$status\n")
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

