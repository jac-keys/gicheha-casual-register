package com.gichehafarm.registry

import android.content.Intent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gichehafarm.registry.data.DatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType

class WorkerLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkerLoginScreen { navigateToWorkerActivity() }
        }
    }

    private fun navigateToWorkerActivity() {
        val intent = Intent(this, WorkerActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
fun WorkerLoginScreen(onLoginSuccess: () -> Unit) {
    var workNumber by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
                painter = painterResource(id = R.drawable.splash_screen), // Replace with actual logo
                contentDescription = "Company Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Worker Login",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Work Number Input
            OutlinedTextField(
                value = workNumber,
                onValueChange = { workNumber = it },
                label = { Text("Work Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // ID Number Input
            OutlinedTextField(
                value = idNumber,
                onValueChange = { idNumber = it },
                label = { Text("ID Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // Login Button
            Button(
                onClick = {
                    keyboardController?.hide()
                    // Hide keyboard on button press
                    if (workNumber.isEmpty() || idNumber.isEmpty()) {
                        loginError = "Please fill in all fields"
                    } else {
                        authenticateWorker(workNumber, idNumber, context) { isValid, errorMessage ->
                            if (isValid) {
                                Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT)
                                    .show()
                                onLoginSuccess()
                            } else {
                                loginError = errorMessage ?: "Invalid Work Number or ID Number"
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Login", color = Color(0xFF2E7D32))

            }


            // Display login error
            loginError?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * Function to authenticate a worker using their work number and ID number.
 */
fun authenticateWorker(
    workNumber: String,
    idNumber: String,
    context: android.content.Context,
    onResult: (Boolean, String?) -> Unit
) {
    val dbHelper = DatabaseHelper.getInstance(context)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val db = dbHelper.readableDatabase
            val query = """
                SELECT COUNT(*) 
                FROM ${DatabaseHelper.TABLE_REGISTER_CASUALS} 
                WHERE ${DatabaseHelper.COLUMN_WORK_NUMBER} = ? 
                AND ${DatabaseHelper.COLUMN_ID_NUMBER} = ?
            """.trimIndent()

            db.rawQuery(query, arrayOf(workNumber, idNumber)).use { cursor ->
                cursor.moveToFirst()
                val count = cursor.getInt(0)
                withContext(Dispatchers.Main) {
                    onResult(count > 0, null)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(false, "Database error. Please try again.")
            }
        }
    }
}
