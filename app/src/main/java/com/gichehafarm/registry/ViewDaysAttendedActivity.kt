package com.gichehafarm.registry

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.util.Calendar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gichehafarm.registry.data.DatabaseHelper
import androidx.compose.ui.unit.sp

class ViewDaysAttendedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ViewAttendanceScreen()
        }
    }
}

@Composable
fun ViewAttendanceScreen() {
    val context = LocalContext.current
    var workNumber by remember { mutableStateOf("") }
    var daysAttended by remember { mutableStateOf<Int?>(null) }
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH) + 1  // Months are 0-based in Calendar
    val currentYear = calendar.get(Calendar.YEAR)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2E7D32), Color(0xFF81C784))))
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) { Image(
            painter = painterResource(id = R.drawable.splash_screen),
            contentDescription = "Logo",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
        )
            Spacer(modifier = Modifier.height(20.dp))
            Text("Check Days Attended", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = workNumber,
                onValueChange = { workNumber = it },
                label = { Text("Enter Your Work Number") }
            )

            Spacer(modifier = Modifier.height(20.dp))

            HrButton("Fetch Attendance", R.drawable.ic_view) {
                if (workNumber.isNotEmpty()) {
                    val dbHelper = DatabaseHelper.getInstance(context)

                    // First get worker ID from work number
                    val workerId = dbHelper.getWorkerIdByWorkNumber(workNumber)

                    if (workerId != -1) {
                        val attendance = dbHelper.getWorkerMonthlyAttendance(
                            workerId = workerId,
                            month = currentMonth,
                            year = currentYear
                        )
                        daysAttended = attendance
                    } else {
                        Toast.makeText(context, "Worker not found", Toast.LENGTH_SHORT).show()
                        daysAttended = null
                    }
                } else {
                    Toast.makeText(context, "Please enter work number", Toast.LENGTH_SHORT).show()
                }
            }

            daysAttended?.let {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "You attended $it day(s) this month.",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}