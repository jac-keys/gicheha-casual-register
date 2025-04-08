package com.gichehafarm.registry

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlinx.coroutines.flow.collectLatest

class HrLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("HR_PREFS", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("HR_LOGGED_IN", false)

        if (isLoggedIn) {
            startActivity(Intent(this, HRActivity::class.java))
            finish()
        } else {
            setContent {
                HrLoginScreen(this)
            }
        }
    }
}

@Composable
fun HrLoginScreen(context: Context) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val sharedPreferences = context.getSharedPreferences("HR_PREFS", Context.MODE_PRIVATE)

    val predefinedHRUsername = "admin_hr"
    val predefinedHRPassword = "secureHRpass"

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

            // Greeting Message
            Text(
                text = "$greeting, HR",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle
            Text(
                text = "Please Login to Continue",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
               // colors = OutlinedTextFieldDefaults.colors(
                    //focusedBorderColor = Color.White,
                    //unfocusedBorderColor = Color.White,
                   // cursorColor = Color.White
               // ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"

                        )
                    }
                },

                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (username == predefinedHRUsername && password == predefinedHRPassword) {
                        sharedPreferences.edit().putBoolean("HR_LOGGED_IN", true).apply()
                        Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                        username = ""
                        password = ""
                        context.startActivity(Intent(context, HRActivity::class.java))
                        (context as? ComponentActivity)?.finish()
                    } else {
                        Toast.makeText(context, "Invalid HR Credentials!", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login", color = Color(0xFF2E7D32)) // Green text on white button
            }
        }
    }
}

@Composable
fun GichehaCasualsApp() {
    val context = LocalContext.current
    val userManager = remember { UserManager(context) }

    var preferredName by remember { mutableStateOf("HR") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            userManager.preferredNameFlow.collectLatest { name ->
                preferredName = name
            }
        }
    }

    // Display the preferred name
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Welcome, $preferredName!",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
