package com.heysafe.app.wear.presentation

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heysafe.app.wear.presentation.theme.MyAppTheme
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.tooling.preview.Preview
import com.heysafe.app.wear.presentation.theme.MyAppTheme
import kotlinx.coroutines.delay


class AlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAppTheme {
                AlertScreen()
            }
        }
    }
}

@Composable
fun AlertScreen() {
    // Define the animation state
    val expandedSize = 200.dp
    val originalSize = 100.dp
    var isExpanded by remember { mutableStateOf(true) }

    // Use animateDpAsState to animate between sizes
    val animatedSize by animateDpAsState(
        targetValue = if (isExpanded) expandedSize else originalSize,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
    )

    // Toggle the animation state
    LaunchedEffect(Unit) {
        while (true) {
            delay(500) // Delay before toggling state
            isExpanded = !isExpanded
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Outer CircularProgressIndicator
        Box(
            modifier = Modifier
                .size(animatedSize)
                .background(Color.Red, CircleShape) // Make the box circular
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SOS Alert",
                color = Color.White,
                fontSize = 20.sp
            )

        }
    }
}

@Preview(showBackground = true)
@Composable
fun CircleAnimationScreenPreview() {
    MyAppTheme {
        AlertScreen()
    }
}