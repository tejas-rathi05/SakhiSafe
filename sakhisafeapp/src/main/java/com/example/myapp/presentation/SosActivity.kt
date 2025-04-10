package com.example.myapp.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.sp
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.example.myapp.presentation.theme.MyAppTheme
import kotlinx.coroutines.delay


class SosActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAppTheme {
                TimerScreen(onTimeOut = { navigateToAlertActivity() })
            }
        }
    }

    private fun navigateToAlertActivity() {
        val intent = Intent(this, AlertActivity::class.java)
        startActivity(intent)
    }
}
@Composable
fun TimerScreen(onTimeOut: () -> Unit) {
    var timer by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(true) }
    var buttonPressed by remember { mutableStateOf(false) }
    val timerUpdateInterval = 1000L // 1 second
    val countdownDuration = 15000L // 15 seconds

    // Handle timer updates
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(timerUpdateInterval)
            timer++
        }
    }

    // Handle countdown to navigate after 15 seconds if button is not pressed
    LaunchedEffect(buttonPressed) {
        if (!buttonPressed) {
            delay(countdownDuration)
            onTimeOut()
        }
    }

    // Calculate progress as a Float value between 0 and 1
    val progress = (timer % 60) / 60f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Set background color to black
        contentAlignment = Alignment.Center
    ) {
        // Outer CircularProgressIndicator
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier
                .size(200.dp) // Size of the progress indicator
                .padding(16.dp),
            color = Color.Red, // More vibrant color for the progress indicator
            trackColor = Color.Gray // Track color should contrast with the progress color
        )

        // Inner content: Box with text and button
        Box(
            modifier = Modifier
                .size(200.dp) // Match size of the progress indicator
                .background(Color.Black.copy(alpha = 0.7f)) // Slightly transparent black background
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ARE YOU SAFE?",
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(onClick = {
                    buttonPressed = true
                    // Handle YES button click
                }) {
                    Text(text = "YES", color = Color.White) // Change button text color to white
                }
            }
        }
    }
}

@Preview
@Composable
fun TimerScreenPreview() {
    MyAppTheme {
        TimerScreen { /* Handle navigation to new screen */ }
    }
}


