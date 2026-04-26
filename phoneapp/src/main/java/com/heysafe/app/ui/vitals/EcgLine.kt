package com.heysafe.app.ui.vitals

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun EcgLine(
    samples: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFE53935),
) {
    Canvas(modifier = modifier) {
        if (samples.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val mn = samples.min()
        val mx = samples.max().coerceAtLeast(mn + 1f)
        val stepX = w / (samples.size - 1)
        val path = Path()
        samples.forEachIndexed { i, v ->
            val x = i * stepX
            val y = h - ((v - mn) / (mx - mn)) * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 6f, cap = StrokeCap.Round))
    }
}
