package com.gichehafarm.registry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gichehafarm.registry.data.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : ComponentActivity() {
    private var receiverRegistered = false

    private val attendanceUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // When a broadcast is received, reload the ReportsScreen.
            runOnUiThread {
                setContent { ReportsScreen(this@ReportsActivity) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReportsScreen(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!receiverRegistered) {
            registerReceiver(attendanceUpdateReceiver, IntentFilter("ATTENDANCE_SAVED_TO_HR"))
            receiverRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (receiverRegistered) {
            unregisterReceiver(attendanceUpdateReceiver)
            receiverRegistered = false
        }
    }
}

@Composable
fun ReportsScreen(context: Context) {
    // Get current month index based on custom logic (day 21 to next month)
    val currentMonth = remember { getCustomMonth() }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    val databaseHelper = remember { DatabaseHelper.getInstance(context) }
    var registeredCasuals by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var attendanceRecords by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var selectedReportType by remember { mutableStateOf("Registrations") }

    val months = listOf(
        "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
        "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
    )

    LaunchedEffect(selectedMonth, selectedYear, selectedReportType) {
        Log.d("ReportsScreen", "Loading data for $selectedReportType")
        try {
            if (selectedReportType == "Registrations") {
                registeredCasuals = loadRegisteredCasuals(databaseHelper, selectedMonth, selectedYear)
                Log.d("ReportsScreen", "Loaded ${registeredCasuals.size} registration records")
            } else {
                // For "Attendance" we load the attended workers exported via the Save to HR process.
                attendanceRecords = loadAttendanceRecords(databaseHelper, selectedMonth, selectedYear)
                Log.d("ReportsScreen", "Loaded ${attendanceRecords.size} attendance records")
            }
        } catch (e: Exception) {
            Log.e("ReportsScreen", "Error loading data", e)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E7D32), Color(0xFF81C784))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                selectedYear = selectedYear,
                selectedReportType = selectedReportType,
                onYearSelected = { year -> selectedYear = year },
                onReportTypeSelected = { type -> selectedReportType = type }
            )

            Image(
                painter = painterResource(id = R.drawable.splash_screen),
                contentDescription = "Company Logo",
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 16.dp)
            )

            // Report type selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Registrations", "Attendance").forEach { type ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedReportType == type) Color.Green else Color.LightGray)
                            .clickable { selectedReportType = type }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = type,
                            color = if (selectedReportType == type) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Month selector
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(months) { month ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (months[selectedMonth] == month) Color.Green else Color.LightGray)
                            .clickable { selectedMonth = months.indexOf(month) }
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            text = month,
                            color = if (months[selectedMonth] == month) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedReportType == "Registrations") {
                RegistrationTable(registeredCasuals)
            } else {
                AttendanceTable(attendanceRecords)
            }
        }
    }
}
fun loadAttendanceRecords(databaseHelper: DatabaseHelper, month: Int, year: Int): List<Pair<String, Int>> {
    val calendar = Calendar.getInstance()

    // Set the calendar to the 21st of the previous month
    if (month == 0) { // January
        calendar.set(year - 1, 11, 21) // December of the previous year
    } else {
        calendar.set(year, month - 1, 21) // 21st of the previous month
    }
    val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

    // Set the calendar to the 20th of the current month
    calendar.set(year, month, 20)
    val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

    val attendanceMap = mutableMapOf<String, Int>()
    val db = databaseHelper.readableDatabase
    val query = """
        SELECT date, COUNT(*) 
        FROM hr_attendance 
        WHERE date BETWEEN ? AND ? 
        GROUP BY date
        ORDER BY date DESC
    """.trimIndent()

    val cursor = db.rawQuery(query, arrayOf(startDate, endDate))
    Log.d("ReportsActivity", "Querying attendance from $startDate to $endDate")

    while (cursor.moveToNext()) {
        val date = cursor.getString(0)
        val count = cursor.getInt(1)
        attendanceMap[date] = count
    }
    cursor.close()
    return attendanceMap.toList()
}

@Composable
fun AttendanceTable(data: List<Pair<String, Int>>) {
    if (data.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No attendance records found", style = MaterialTheme.typography.body2)
        }
        return
    }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "${data.sumOf { it.second }} Total Attendance Records",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Date", fontWeight = FontWeight.Bold)
                Text("Workers Present", fontWeight = FontWeight.Bold)
            }
            Divider()
            data.forEach { (date, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = date)
                    Text(text = count.toString())
                }
                Divider()
            }
        }
    }
}

@Composable
fun TopBar(
    selectedYear: Int,
    selectedReportType: String,
    onYearSelected: (Int) -> Unit,
    onReportTypeSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E7D32), Color(0xFF81C784))
                )
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "Reports", fontSize = 18.sp, color = Color.White)
        Spacer(modifier = Modifier.weight(1f))
        YearDropdownButton(selectedYear, onYearSelected)
    }
}

@Composable
fun YearDropdownButton(selectedYear: Int, onYearSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear downTo (currentYear - 10)).toList()

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.DateRange, contentDescription = "Select Year", tint = Color.White)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            years.forEach { year ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onYearSelected(year)
                    }
                ) {
                    Text(text = year.toString())
                }
            }
        }
    }
}

fun getCustomMonth(): Int {
    val calendar = Calendar.getInstance()
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    var month = calendar.get(Calendar.MONTH)
    if (day >= 21) month = (month + 1 + 12) % 12
    return month
}

fun loadRegisteredCasuals(databaseHelper: DatabaseHelper, month: Int, year: Int): List<Pair<String, Int>> {
    val calendar = Calendar.getInstance()

    // Set the calendar to the 21st of the previous month
    if (month == 0) { // January
        calendar.set(year - 1, 11, 21) // December of the previous year
    } else {
        calendar.set(year, month - 1, 21) // 21st of the previous month
    }
    val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

    // Set the calendar to the 20th of the current month
    calendar.set(year, month, 20)
    val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

    val casualsMap = mutableMapOf<String, Int>()
    val db = databaseHelper.readableDatabase
    val query = """
        SELECT date, COUNT(*) 
        FROM register_casuals 
        WHERE date BETWEEN ? AND ? 
        GROUP BY date
    """.trimIndent()

    val cursor = db.rawQuery(query, arrayOf(startDate, endDate))
    Log.d("ReportsActivity", "Querying registrations from $startDate to $endDate")

    while (cursor.moveToNext()) {
        val date = cursor.getString(0)
        val count = cursor.getInt(1)
        casualsMap[date] = count
    }
    cursor.close()
    return casualsMap.toList()
}


@Composable
fun RegistrationTable(data: List<Pair<String, Int>>) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "${data.sumOf { it.second }} Total Registrations",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Date", fontWeight = FontWeight.Bold)
                Text("Casuals Registered", fontWeight = FontWeight.Bold)
            }
            Divider()
            data.forEach { (date, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = date)
                    Text(text = count.toString())
                }
                Divider()
            }
        }
    }
}
