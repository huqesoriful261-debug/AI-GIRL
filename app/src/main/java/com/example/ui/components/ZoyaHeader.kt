package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HearingDisabled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ZoyaMood
import com.example.data.ZoyaState
import com.example.ui.theme.ZoyaCyan
import com.example.ui.theme.ZoyaGreen
import com.example.ui.theme.ZoyaMagenta
import com.example.ui.theme.ZoyaRed
import com.example.ui.theme.ZoyaSurfaceVariant
import com.example.ui.theme.ZoyaTextMuted
import com.example.ui.theme.ZoyaTextWhite
import com.example.ui.theme.ZoyaViolet

@Composable
fun ZoyaHeader(
    state: ZoyaState,
    mood: ZoyaMood,
    continuousMode: Boolean,
    onToggleContinuousMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Title with futuristic glowing gradient
            Column {
                Text(
                    text = "ZOYA AI",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.5.sp,
                        brush = Brush.horizontalGradient(
                            listOf(ZoyaMagenta, ZoyaCyan)
                        )
                    ),
                    modifier = Modifier.testTag("app_brand_title")
                )
                Text(
                    text = "Voice Assistant • Sassy Companion",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ZoyaTextMuted,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            // Top action buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Hands-free continuous listening toggle
                IconButton(
                    onClick = onToggleContinuousMode,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (continuousMode) ZoyaMagenta.copy(alpha = 0.15f) else Color.Transparent)
                        .testTag("toggle_continuous_mode")
                ) {
                    Icon(
                        imageVector = if (continuousMode) Icons.Default.Hearing else Icons.Default.HearingDisabled,
                        contentDescription = "Toggle Hands-free Voice Mode",
                        tint = if (continuousMode) ZoyaCyan else ZoyaTextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Clear / reset session
                IconButton(
                    onClick = onClearSession,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("clear_session_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Conversation History",
                        tint = ZoyaTextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Settings
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open Settings",
                        tint = ZoyaTextWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Status & Sassy Mood Pill Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Status Indicator
            val (statusText, statusColor) = when (state) {
                ZoyaState.DISCONNECTED -> "STANDBY" to Color(0xFF6B6282)
                ZoyaState.CONNECTING -> "LINKING..." to ZoyaCyan
                ZoyaState.LISTENING -> "LISTENING" to ZoyaGreen
                ZoyaState.THINKING -> "THINKING" to ZoyaViolet
                ZoyaState.SPEAKING -> "SPEAKING" to ZoyaMagenta
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = statusColor
                        )
                    )
                }
            }

            // Sassy Mood Badge
            AnimatedVisibility(
                visible = state != ZoyaState.DISCONNECTED,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ZoyaSurfaceVariant)
                        .border(1.dp, ZoyaMagenta.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = mood.emoji,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mood.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = ZoyaTextWhite
                            )
                        )
                    }
                }
            }
        }
    }
}
