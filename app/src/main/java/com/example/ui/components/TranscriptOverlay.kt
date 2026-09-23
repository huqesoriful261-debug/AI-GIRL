package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.data.ToolCallInfo
import com.example.data.ZoyaState
import com.example.ui.theme.ZoyaAmber
import com.example.ui.theme.ZoyaCyan
import com.example.ui.theme.ZoyaGreen
import com.example.ui.theme.ZoyaMagenta
import com.example.ui.theme.ZoyaSurface
import com.example.ui.theme.ZoyaTextMuted
import com.example.ui.theme.ZoyaTextWhite

@Composable
fun TranscriptOverlay(
    state: ZoyaState,
    latestMessage: ChatMessage?,
    activeTool: ToolCallInfo?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tool Action Notification Card
        AnimatedVisibility(
            visible = activeTool != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            if (activeTool != null) {
                ToolExecutionCard(toolCall = activeTool)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // State Guidance or Live Subtitle Card
        val subtitleContent = when (state) {
            ZoyaState.DISCONNECTED -> "Tap the orb to activate Zoya"
            ZoyaState.CONNECTING -> "Synchronizing live frequency..."
            ZoyaState.LISTENING -> "I'm listening, tell me everything..."
            ZoyaState.THINKING -> "Hmm... cooking up a snappy response..."
            ZoyaState.SPEAKING -> latestMessage?.text?.takeIf { latestMessage.role == "model" }
                ?: "..."
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ZoyaSurface.copy(alpha = 0.92f),
                            Color(0xFF0F0A1F).copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            ZoyaMagenta.copy(alpha = if (state == ZoyaState.SPEAKING) 0.6f else 0.2f),
                            ZoyaCyan.copy(alpha = if (state == ZoyaState.LISTENING) 0.6f else 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("transcript_caption_card"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state == ZoyaState.SPEAKING) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ZoyaMagenta,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ZOYA",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ZoyaMagenta,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                    text = subtitleContent,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (state == ZoyaState.DISCONNECTED) ZoyaTextMuted else ZoyaTextWhite,
                        fontSize = if (state == ZoyaState.SPEAKING) 16.sp else 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = if (state == ZoyaState.SPEAKING) FontWeight.Medium else FontWeight.Normal,
                        fontStyle = if (state == ZoyaState.LISTENING || state == ZoyaState.THINKING) FontStyle.Italic else FontStyle.Normal,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

@Composable
private fun ToolExecutionCard(toolCall: ToolCallInfo) {
    val (icon, title, color) = when (toolCall.name) {
        "openWebsite" -> Triple(Icons.Default.OpenInBrowser, "Browser Action", ZoyaCyan)
        "searchWeb" -> Triple(Icons.Default.Search, "Web Search", ZoyaAmber)
        "toggleFlashlight" -> Triple(Icons.Default.FlashOn, "Flashlight Tool", ZoyaMagenta)
        "openApp" -> Triple(Icons.AutoMirrored.Filled.Launch, "App Launcher", ZoyaGreen)
        else -> Triple(Icons.Default.AutoAwesome, "Action Executed", ZoyaCyan)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("tool_call_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = color,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = toolCall.result ?: "Executing ${toolCall.name}...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ZoyaTextWhite,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
