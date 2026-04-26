package com.heysafe.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PressAndHoldSos(
    onTriggered: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Long = 3_000,
) {
    var holding by remember { mutableStateOf(false) }
    var fired by remember { mutableStateOf(false) }

    LaunchedEffect(holding) {
        if (holding && !fired) {
            delay(holdMillis)
            if (holding) {
                fired = true
                onTriggered()
            }
        } else if (!holding) {
            fired = false
        }
    }

    Box(
        modifier = modifier
            .size(180.dp)
            .background(Color(0xFFE53935), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        tryAwaitRelease()
                        holding = false
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SOS", color = Color.White, style = MaterialTheme.typography.headlineLarge)
            Text(
                if (holding) "Hold…" else "Press and hold",
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}
