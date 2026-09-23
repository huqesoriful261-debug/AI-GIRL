package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ZoyaState
import com.example.ui.theme.ZoyaCyan
import com.example.ui.theme.ZoyaMagenta
import com.example.ui.theme.ZoyaPurpleGlow
import com.example.ui.theme.ZoyaViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FuturisticVisualizer(
    state: ZoyaState,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_infinite")

    // Rotation angle for outer holographic cyber ring
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == ZoyaState.THINKING) 3000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Reverse rotation for second ring
    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverse_rotation"
    )

    // Pulse scale for idle / speaking
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == ZoyaState.SPEAKING || state == ZoyaState.LISTENING) 1.14f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == ZoyaState.SPEAKING) 500 else 1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Animated smoothed amplitude
    val smoothAmp = remember { Animatable(0f) }
    LaunchedEffect(amplitude) {
        smoothAmp.animateTo(
            targetValue = amplitude.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 60)
        )
    }

    val primaryColor = when (state) {
        ZoyaState.DISCONNECTED -> Color(0xFF33294D)
        ZoyaState.CONNECTING -> ZoyaCyan
        ZoyaState.LISTENING -> ZoyaMagenta
        ZoyaState.THINKING -> ZoyaViolet
        ZoyaState.SPEAKING -> ZoyaCyan
    }

    val secondaryColor = when (state) {
        ZoyaState.DISCONNECTED -> Color(0xFF1E1733)
        ZoyaState.CONNECTING -> ZoyaMagenta
        ZoyaState.LISTENING -> ZoyaCyan
        ZoyaState.THINKING -> ZoyaMagenta
        ZoyaState.SPEAKING -> ZoyaMagenta
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(310.dp)
            .testTag("futuristic_visualizer")
    ) {
        // Futuristic Canvas with reactive radial waves, cyber rings, and glowing pulse
        Canvas(modifier = Modifier.size(310.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.width * 0.28f
            val reactiveBonus = smoothAmp.value * 45f

            // Outer pulse glow waves
            if (state == ZoyaState.LISTENING || state == ZoyaState.SPEAKING) {
                for (i in 1..3) {
                    val waveRadius = baseRadius + (i * 24f * pulseScale) + (reactiveBonus * i * 0.6f)
                    val alpha = (0.28f / i) * (1f - (waveRadius / (size.width / 2f)).coerceIn(0f, 1f))
                    drawCircle(
                        color = primaryColor.copy(alpha = alpha),
                        radius = waveRadius,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                }
            }

            // Segmented cyber ring with rotating ticks
            val ringRadius = baseRadius + 32f
            val segments = 48
            for (i in 0 until segments) {
                val angleDeg = (i * (360f / segments) + rotationAngle) % 360f
                val rad = Math.toRadians(angleDeg.toDouble())
                val isMajor = i % 4 == 0
                val tickLen = if (isMajor) 12f + (reactiveBonus * 0.2f) else 5f
                val r1 = ringRadius
                val r2 = ringRadius + tickLen

                val p1 = Offset((center.x + r1 * cos(rad)).toFloat(), (center.y + r1 * sin(rad)).toFloat())
                val p2 = Offset((center.x + r2 * cos(rad)).toFloat(), (center.y + r2 * sin(rad)).toFloat())

                val tickAlpha = if (state == ZoyaState.DISCONNECTED) 0.15f else if (isMajor) 0.85f else 0.4f
                val tickColor = if (i % 2 == 0) primaryColor else secondaryColor
                drawLine(
                    color = tickColor.copy(alpha = tickAlpha),
                    start = p1,
                    end = p2,
                    strokeWidth = if (isMajor) 2.5f else 1.2f,
                    cap = StrokeCap.Round
                )
            }

            // Radial audio waveform bars (visible in LISTENING & SPEAKING)
            if (state == ZoyaState.LISTENING || state == ZoyaState.SPEAKING) {
                val barCount = 36
                for (b in 0 until barCount) {
                    val angle = (b * (360.0 / barCount) + reverseRotation)
                    val rad = Math.toRadians(angle)
                    // Bar height based on angle sine pattern and real amplitude
                    val waveFactor = (sin(b * 0.8 + rotationAngle * 0.1).toFloat() + 1f) * 0.5f
                    val barHeight = 8f + (smoothAmp.value * 52f * waveFactor)

                    val startOffset = Offset(
                        (center.x + (baseRadius + 6f) * cos(rad)).toFloat(),
                        (center.y + (baseRadius + 6f) * sin(rad)).toFloat()
                    )
                    val endOffset = Offset(
                        (center.x + (baseRadius + 6f + barHeight) * cos(rad)).toFloat(),
                        (center.y + (baseRadius + 6f + barHeight) * sin(rad)).toFloat()
                    )

                    drawLine(
                        brush = Brush.linearGradient(
                            listOf(primaryColor, secondaryColor),
                            start = startOffset,
                            end = endOffset
                        ),
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Inner holographic aura glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (state == ZoyaState.DISCONNECTED) 0.1f else 0.45f),
                        secondaryColor.copy(alpha = if (state == ZoyaState.DISCONNECTED) 0.05f else 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.3f
                ),
                radius = baseRadius * 1.3f,
                center = center
            )
        }

        // Central Orb Touch Target
        val orbGradient = Brush.radialGradient(
            colors = when (state) {
                ZoyaState.DISCONNECTED -> listOf(
                    Color(0xFF261D3D),
                    Color(0xFF130E24)
                )
                ZoyaState.CONNECTING -> listOf(
                    ZoyaCyan.copy(alpha = 0.8f),
                    ZoyaPurpleGlow
                )
                ZoyaState.LISTENING -> listOf(
                    ZoyaMagenta,
                    Color(0xFF5A0B3F),
                    Color(0xFF17051F)
                )
                ZoyaState.THINKING -> listOf(
                    ZoyaViolet,
                    ZoyaMagenta,
                    Color(0xFF1D0530)
                )
                ZoyaState.SPEAKING -> listOf(
                    ZoyaCyan,
                    ZoyaMagenta,
                    Color(0xFF0F0724)
                )
            }
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(136.dp)
                .shadow(
                    elevation = if (state == ZoyaState.DISCONNECTED) 6.dp else 24.dp,
                    shape = CircleShape,
                    ambientColor = primaryColor,
                    spotColor = secondaryColor
                )
                .clip(CircleShape)
                .background(orbGradient)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = primaryColor),
                    onClick = onClick
                )
                .testTag("zoya_orb_button")
        ) {
            val icon = when (state) {
                ZoyaState.DISCONNECTED -> Icons.Default.PowerSettingsNew
                ZoyaState.CONNECTING -> Icons.Default.Mic
                ZoyaState.LISTENING -> Icons.Default.Mic
                ZoyaState.THINKING -> Icons.Default.Stop
                ZoyaState.SPEAKING -> Icons.Default.Stop
            }

            Icon(
                imageVector = icon,
                contentDescription = "Zoya AI Interaction Button",
                tint = if (state == ZoyaState.DISCONNECTED) Color(0xFF9E92B8) else Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
