package com.edgeglow.aod

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun EdgeGlowBorder(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    strokeWidth: Dp = 14.dp,
    cornerRadius: Dp = 42.dp,
    animationDurationMs: Int = 3000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "edge_glow_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Efeito de pulso de brilho suave
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)

        // Assegura ciclo perfeito no SweepGradient
        val gradientColors = if (colors.isNotEmpty()) {
            colors + colors.first()
        } else {
            listOf(Color.Cyan, Color.Magenta, Color.Yellow, Color.Cyan)
        }

        val sweepBrush = Brush.sweepGradient(
            colors = gradientColors.map { it.copy(alpha = it.alpha * pulseAlpha) },
            center = center
        )

        val halfStroke = strokeWidthPx / 2f
        val roundRect = RoundRect(
            left = halfStroke,
            top = halfStroke,
            right = width - halfStroke,
            bottom = height - halfStroke,
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )

        val path = Path().apply {
            addRoundRect(roundRect)
        }

        // Camada 1: Glow exterior (brilho difuso mais largo)
        rotate(degrees = rotationAngle, pivot = center) {
            drawPath(
                path = path,
                brush = sweepBrush,
                style = Stroke(width = strokeWidthPx * 1.8f)
            )
        }

        // Camada 2: Linha de néon central super nítida
        rotate(degrees = rotationAngle, pivot = center) {
            drawPath(
                path = path,
                brush = sweepBrush,
                style = Stroke(width = strokeWidthPx)
            )
        }
    }
}
