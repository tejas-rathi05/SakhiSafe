package com.heysafe.app.wear.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heysafe.app.wear.presentation.theme.MyAppTheme
import com.heysafe.app.wear.transport.DataLayerSender
import com.heysafe.app.wear.transport.WearMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SosActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val triggerSource = intent.getStringExtra("triggerSource") ?: WearMessages.SOURCE_MANUAL
        val hrWindow = intent.getFloatArrayExtra("hrWindow") ?: floatArrayOf()
        val motionWindow = intent.getFloatArrayExtra("motionWindow") ?: floatArrayOf()
        val sender = DataLayerSender(this)

        setContent {
            MyAppTheme {
                TimerScreen(
                    onSafe = {
                        scope.launch { runCatching { sender.sendAlertCanceled() } }
                        finish()
                    },
                    onTimeout = {
                        scope.launch {
                            runCatching { sender.sendAlertConfirmed(triggerSource, hrWindow, motionWindow) }
                        }
                        startActivity(Intent(this@SosActivity, AlertActivity::class.java))
                        finish()
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

@Composable
fun TimerScreen(onSafe: () -> Unit, onTimeout: () -> Unit) {
    var timer by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(true) }
    var buttonPressed by remember { mutableStateOf(false) }
    val timerUpdateInterval = 1000L
    val countdownDuration = 15000L

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(timerUpdateInterval)
            timer++
        }
    }

    LaunchedEffect(buttonPressed) {
        if (!buttonPressed) {
            delay(countdownDuration)
            onTimeout()
        }
    }

    val progress = (timer % 60) / 60f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp),
            color = Color.Red,
            trackColor = Color.Gray,
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "ARE YOU SAFE?",
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Button(onClick = {
                    buttonPressed = true
                    onSafe()
                }) {
                    Text(text = "YES", color = Color.White)
                }
            }
        }
    }
}

@Preview
@Composable
fun TimerScreenPreview() {
    MyAppTheme {
        TimerScreen(onSafe = {}, onTimeout = {})
    }
}
