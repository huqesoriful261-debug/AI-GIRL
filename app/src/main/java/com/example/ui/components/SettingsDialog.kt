package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.data.AVAILABLE_VOICES
import com.example.ui.theme.ZoyaCyan
import com.example.ui.theme.ZoyaGreen
import com.example.ui.theme.ZoyaMagenta
import com.example.ui.theme.ZoyaSurface
import com.example.ui.theme.ZoyaSurfaceVariant
import com.example.ui.theme.ZoyaTextMuted
import com.example.ui.theme.ZoyaTextWhite
import com.example.ui.theme.ZoyaViolet

@Composable
fun SettingsDialog(
    currentVoice: String,
    continuousMode: Boolean,
    customApiKey: String,
    onVoiceSelected: (String) -> Unit,
    onContinuousModeChanged: (Boolean) -> Unit,
    onSaveCustomApiKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf(customApiKey) }
    var keySavedSuccess by remember { mutableStateOf(false) }

    val hasBuildConfigKey = BuildConfig.GEMINI_API_KEY.isNotBlank() &&
            BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, ZoyaMagenta.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
            color = ZoyaSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Zoya Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZoyaTextWhite
                        )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ZoyaTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gemini API Key Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = ZoyaCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini API Key",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZoyaTextWhite
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Key status badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (hasBuildConfigKey || customApiKey.isNotBlank()) ZoyaGreen.copy(alpha = 0.12f) else ZoyaMagenta.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (hasBuildConfigKey || customApiKey.isNotBlank()) ZoyaGreen else ZoyaMagenta,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasBuildConfigKey) "AI Studio Key configured"
                            else if (customApiKey.isNotBlank()) "Custom API Key active"
                            else "No API key found. Enter below or use Secrets panel.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (hasBuildConfigKey || customApiKey.isNotBlank()) ZoyaGreen else ZoyaMagenta
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = {
                        keyInput = it
                        keySavedSuccess = false
                    },
                    placeholder = { Text("AIzaSy...", color = ZoyaTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZoyaTextWhite,
                        unfocusedTextColor = ZoyaTextWhite,
                        focusedBorderColor = ZoyaCyan,
                        unfocusedBorderColor = ZoyaSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            onSaveCustomApiKey(keyInput)
                            keySavedSuccess = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZoyaMagenta
                        ),
                        modifier = Modifier.testTag("save_api_key_button")
                    ) {
                        Text(if (keySavedSuccess) "Saved!" else "Save Key")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Voice Selection
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = ZoyaViolet,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice Tone & Persona",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZoyaTextWhite
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                AVAILABLE_VOICES.forEach { voice ->
                    val isSelected = voice.id.equals(currentVoice, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ZoyaViolet.copy(alpha = 0.2f) else ZoyaSurfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) ZoyaViolet else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onVoiceSelected(voice.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("voice_option_${voice.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = voice.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) ZoyaCyan else ZoyaTextWhite
                                    )
                                )
                                Text(
                                    text = voice.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = ZoyaTextMuted,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(ZoyaCyan),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Hands-free Continuous Conversation Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ZoyaSurfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hands-free Continuous Mode",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ZoyaTextWhite
                            )
                        )
                        Text(
                            text = "Zoya automatically listens after finishing her answer",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ZoyaTextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Switch(
                        checked = continuousMode,
                        onCheckedChange = onContinuousModeChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ZoyaMagenta,
                            uncheckedTrackColor = ZoyaSurface
                        ),
                        modifier = Modifier.testTag("continuous_mode_switch")
                    )
                }
            }
        }
    }
}
