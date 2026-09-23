package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.ZoyaState
import com.example.session.ZoyaLiveSessionManager
import com.example.ui.components.ConversationHistorySheet
import com.example.ui.components.FuturisticVisualizer
import com.example.ui.components.QuickPromptsBar
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TranscriptOverlay
import com.example.ui.components.ZoyaHeader
import com.example.ui.theme.ZoyaCyan
import com.example.ui.theme.ZoyaDarkBg
import com.example.ui.theme.ZoyaMagenta
import com.example.ui.theme.ZoyaSurface
import com.example.ui.theme.ZoyaTextMuted
import com.example.ui.theme.ZoyaTextWhite

@Composable
fun ZoyaMainScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sessionManager = remember {
        ZoyaLiveSessionManager(context, coroutineScope)
    }

    DisposableEffect(Unit) {
        onDispose {
            sessionManager.release()
        }
    }

    val state by sessionManager.state.collectAsState()
    val amplitude by sessionManager.amplitude.collectAsState()
    val mood by sessionManager.currentMood.collectAsState()
    val messages by sessionManager.messages.collectAsState()
    val activeTool by sessionManager.activeTool.collectAsState()
    val continuousMode by sessionManager.continuousMode.collectAsState()
    val selectedVoice by sessionManager.selectedVoice.collectAsState()
    val customApiKey by sessionManager.customApiKey.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    // Microphone Permission Check
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            sessionManager.connectSession()
        }
    }

    Scaffold(
        containerColor = ZoyaDarkBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ZoyaDarkBg,
                            Color(0xFF0F0B1E),
                            Color(0xFF080512)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header with Title, Status & Mood
                ZoyaHeader(
                    state = state,
                    mood = mood,
                    continuousMode = continuousMode,
                    onToggleContinuousMode = {
                        sessionManager.setContinuousMode(!continuousMode)
                    },
                    onOpenSettings = { showSettings = true },
                    onClearSession = { sessionManager.clearHistory() }
                )

                // Mic Permission Banner if missing
                AnimatedVisibility(
                    visible = !hasMicPermission,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ZoyaMagenta.copy(alpha = 0.15f))
                            .border(1.dp, ZoyaMagenta.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = ZoyaMagenta,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Microphone Access Required",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = ZoyaTextWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Allow mic to talk with Zoya in real time",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = ZoyaTextMuted
                                        )
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ZoyaMagenta),
                                modifier = Modifier.testTag("grant_mic_button")
                            ) {
                                Text("Allow")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.1f))

                // Center Holographic Visualizer Orb
                FuturisticVisualizer(
                    state = state,
                    amplitude = amplitude,
                    onClick = {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            sessionManager.onCenterOrbClicked()
                        }
                    }
                )

                Spacer(modifier = Modifier.weight(0.1f))

                // Floating Subtitle Transcript Card / Tool Execution Badge
                TranscriptOverlay(
                    state = state,
                    latestMessage = messages.lastOrNull(),
                    activeTool = activeTool
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Sassy Banter Prompts Bar
                QuickPromptsBar(
                    onPromptSelected = { prompt ->
                        sessionManager.sendQuickPrompt(prompt)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Footer Bar with Interaction Hint and Dialogue Log Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Interaction guidance
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ZoyaCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (state) {
                                ZoyaState.DISCONNECTED -> "Tap orb to wake Zoya"
                                ZoyaState.LISTENING -> "Speaking... tap orb to finish"
                                ZoyaState.SPEAKING -> "Tap orb to interrupt"
                                ZoyaState.THINKING -> "Thinking..."
                                ZoyaState.CONNECTING -> "Connecting..."
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ZoyaTextMuted,
                                fontSize = 12.sp
                            )
                        )
                    }

                    // Dialogue Log Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ZoyaSurface)
                            .border(1.dp, ZoyaCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .clickable { showHistory = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("open_dialogue_log_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "Open Dialogue Log",
                                tint = ZoyaCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Log (${messages.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ZoyaTextWhite,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }

        // Settings Dialog
        if (showSettings) {
            SettingsDialog(
                currentVoice = selectedVoice,
                continuousMode = continuousMode,
                customApiKey = customApiKey,
                onVoiceSelected = { voice ->
                    sessionManager.setSelectedVoice(voice)
                },
                onContinuousModeChanged = { enabled ->
                    sessionManager.setContinuousMode(enabled)
                },
                onSaveCustomApiKey = { key ->
                    sessionManager.setCustomApiKey(key)
                },
                onDismiss = { showSettings = false }
            )
        }

        // Conversation History Sheet
        if (showHistory) {
            ConversationHistorySheet(
                messages = messages,
                onDismiss = { showHistory = false }
            )
        }
    }
}
