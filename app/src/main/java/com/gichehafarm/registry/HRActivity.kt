package com.gichehafarm.registry

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.gichehafarm.registry.PayrollActivity

class HRActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GichehaCasualApp()
        }
    }
}

@Composable
fun GichehaCasualApp() {
    val context = LocalContext.current
    val userManager = remember { UserManager(context) }

    // Fix: Explicitly specify type for mutableStateOf
    var preferredName by remember { mutableStateOf<String>("HR") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            userManager.getPreferredNameFlow().collectLatest { name ->
                preferredName = name
            }
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

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Company Logo
                Image(
                    painter = painterResource(id = R.drawable.splash_screen),
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

                // Greeting Message with Preferred Name
                Text(
                    text = "$greeting, $preferredName",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Subtitle
                Text(
                    text = "Welcome to Gicheha Farm Ltd Casual's Register, HR Module",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                HrButton("Register New Casuals", R.drawable.ic_add) {
                    context.startActivity(Intent(context, RegisterWorkersActivity::class.java))
                }
                Spacer(modifier = Modifier.height(10.dp))

                HrButton("View Registered Casuals", R.drawable.ic_view) {
                    context.startActivity(Intent(context, ViewCasualsActivity::class.java))
                }
                Spacer(modifier = Modifier.height(10.dp))

                HrButton("View Reports", R.drawable.ic_reports) {
                    context.startActivity(Intent(context, ReportsActivity::class.java))
                }

                HrButton("Compute Payroll", R.drawable.ic_payroll) {
                    context.startActivity(Intent(context, PayrollActivity::class.java))
                }
            }
        }
    }
}

@Composable
fun HrButton(text: String, icon: Int, onClick: () -> Unit) {
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
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
