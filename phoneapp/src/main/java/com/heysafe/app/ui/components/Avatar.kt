package com.heysafe.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Avatar(name: String, sizeDp: Int = 56) {
    val initials = name.trim().split(" ").filter { it.isNotEmpty() }
        .take(2).joinToString("") { it.take(1).uppercase() }
        .ifEmpty { "?" }
    val palette = listOf(0xFFE53935, 0xFF1976D2, 0xFF2E7D32, 0xFF6A1B9A, 0xFFEF6C00)
    val color = Color(palette[(name.hashCode() and 0x7fffffff) % palette.size])
    Box(
        modifier = Modifier.size(sizeDp.dp).background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}
