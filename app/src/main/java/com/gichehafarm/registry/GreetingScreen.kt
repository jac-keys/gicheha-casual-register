package com.gichehafarm.registry  // Ensure this matches your app’s package

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

// Function to determine the greeting
fun getGreeting(): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 0..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        in 18..23 -> "Good evening"
        else -> "Hello"
    }
}

// Composable function to display greeting
@Composable
fun GreetingText() {
    val greeting = getGreeting()
    Text(
        text = greeting,
        fontSize = 24.sp,
        modifier = Modifier.padding(16.dp)
    )
}
