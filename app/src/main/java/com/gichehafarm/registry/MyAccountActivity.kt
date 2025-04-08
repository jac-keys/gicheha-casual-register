package com.gichehafarm.registry

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

class MyAccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAccountScreen()
        }
    }
}

@Composable
fun MyAccountScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    var preferredName by remember {
        mutableStateOf(sharedPreferences.getString("preferred_name", "HR") ?: "HR")
    }
    var nameInput by remember { mutableStateOf(TextFieldValue(preferredName)) }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour in 5..11 -> "Good Morning"
            hour in 12..17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

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
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_screen),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "$greeting, $preferredName", fontSize = 22.sp, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))

            CustomTextFields(value = nameInput.text, label = "Preferred Name") { newValue ->
                nameInput = TextFieldValue(newValue)
            }
            Spacer(modifier = Modifier.height(10.dp))

            MyAccountButton(text = "Save", icon = R.drawable.ic_save) {
                sharedPreferences.edit().putString("preferred_name", nameInput.text).apply()
                preferredName = nameInput.text
            }

            Spacer(modifier = Modifier.height(20.dp))

            MyAccountButton(text = "Logout", icon = R.drawable.ic_logout) {
                sharedPreferences.edit().clear().apply()
                val intent = Intent(context, HrLoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
            }
        }
    }
}

@Composable
fun CustomTextFields(value: String, label: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Black) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
fun MyAccountButton(text: String, icon: Int, onClick: () -> Unit) {
    val buttonColor = Color.White
    val textColor = Color(0xFF2E7D32)

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 10.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = textColor
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
            Text(
                text = text,
                fontSize = 18.sp,
                color = textColor
            )
        }
    }
}
