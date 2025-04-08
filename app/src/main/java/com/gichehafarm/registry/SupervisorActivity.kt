package com.gichehafarm.registry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.runtime.remember
import java.util.Calendar
class SupervisorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SupervisorCasualApp()
        }
    }
}

@Composable
fun SupervisorCasualApp() {
    val username = "Supervisor"

    // Use a Box to fill the entire screen with a gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E7D32), Color(0xFF81C784))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Company Logo
            Image(
                painter = painterResource(id = R.drawable.splash_screen), // Add your logo here
                contentDescription = "Company Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(20.dp))

            val greeting = remember {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                when {
                    hour in 5..11 -> "Good Morning"
                    hour in 12..17 -> "Good Afternoon"
                    else -> "Good Evening"
                }
            }

            // Greeting Message
            Text(
                text = "$greeting, $username",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle
            Text(
                text = "Welcome to Gicheha Farm Ltd Casual's Register, Supervisor Module",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Buttons with Icons
            val context = LocalContext.current
            SupervisorButton(
                text = "Record Today's Attendance",
                icon = R.drawable.ic_attendance, // Ensure this resource exists
                onClick = {
                    context.startActivity(Intent(context, AttendanceActivity::class.java))
                }
            )

            SupervisorButton(
                text = "View Reports",
                icon = R.drawable.ic_reports, // Ensure this resource exists
                onClick = {
                    context.startActivity(Intent(context, ReportsActivity::class.java))
                }
            )
        }
    }
}


@Composable
fun SupervisorButton(text: String, icon: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 10.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF2E7D32)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp)
    ) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, // Align items in row
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(10.dp)) // Correctly placed

            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}