package com.heysafe.app.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heysafe.app.wear.presentation.theme.MyAppTheme
import com.heysafe.app.wear.transport.DataLayerSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlertActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sender = DataLayerSender(this)
        setContent {
            MyAppTheme {
                AlertScreen(
                    onSafe = {
                        scope.launch { runCatching { sender.sendAlertCanceled() } }
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
fun AlertScreen(onSafe: () -> Unit = {}) {
    val expandedSize = 90.dp
    val originalSize = 60.dp
    var isExpanded by remember { mutableStateOf(true) }

    val animatedSize by animateDpAsState(
        targetValue = if (isExpanded) expandedSize else originalSize,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "pulse-size",
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            isExpanded = !isExpanded
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(animatedSize)
                    .background(Color.Red, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "SOS", color = Color.White, fontSize = 18.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Alert sent",
                color = Color.White,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSafe,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
            ) {
                Text(text = "I'm Safe", fontSize = 14.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlertScreenPreview() {
    MyAppTheme {
        AlertScreen()
    }
}
