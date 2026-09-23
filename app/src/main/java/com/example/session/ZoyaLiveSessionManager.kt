package com.example.session

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.audio.AudioPlayerStreamer
import com.example.audio.AudioRecordStreamer
import com.example.data.ChatMessage
import com.example.data.ToolCallInfo
import com.example.data.ZoyaMood
import com.example.data.ZoyaState
import com.example.service.GeminiLiveService
import com.example.tools.DeviceActionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ZoyaLiveSessionManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "ZoyaSessionManager"
        private const val PREFS_NAME = "zoya_session_prefs"
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_VOICE_NAME = "zoya_voice_name"
        private const val KEY_CONTINUOUS_MODE = "zoya_continuous_mode"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(ZoyaState.DISCONNECTED)
    val state: StateFlow<ZoyaState> = _state.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _currentMood = MutableStateFlow(ZoyaMood.CONFIDENT)
    val currentMood: StateFlow<ZoyaMood> = _currentMood.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _activeTool = MutableStateFlow<ToolCallInfo?>(null)
    val activeTool: StateFlow<ToolCallInfo?> = _activeTool.asStateFlow()

    private val _continuousMode = MutableStateFlow(prefs.getBoolean(KEY_CONTINUOUS_MODE, true))
    val continuousMode: StateFlow<Boolean> = _continuousMode.asStateFlow()

    private val _selectedVoice = MutableStateFlow(prefs.getString(KEY_VOICE_NAME, "Kore") ?: "Kore")
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private val _customApiKey = MutableStateFlow(prefs.getString(KEY_CUSTOM_API_KEY, "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val deviceActionHandler = DeviceActionHandler(context)
    private val geminiService = GeminiLiveService(_customApiKey.value)

    private val conversationHistory = mutableListOf<JSONObject>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var activeRequestJob: Job? = null

    // Audio streamers
    private val audioPlayerStreamer = AudioPlayerStreamer(
        context = context,
        onAmplitude = { amp ->
            if (_state.value == ZoyaState.SPEAKING) {
                _amplitude.value = amp
            }
        },
        onPlaybackStarted = {
            mainHandler.post {
                _state.value = ZoyaState.SPEAKING
                _currentMood.value = ZoyaMood.CONFIDENT
            }
        },
        onPlaybackFinished = {
            mainHandler.post {
                _amplitude.value = 0f
                if (_state.value == ZoyaState.SPEAKING) {
                    if (_continuousMode.value) {
                        startListening()
                    } else {
                        _state.value = ZoyaState.LISTENING
                        _currentMood.value = ZoyaMood.PLAYFUL
                    }
                }
            }
        }
    )

    private val audioRecordStreamer = AudioRecordStreamer(
        onAmplitude = { amp ->
            if (_state.value == ZoyaState.LISTENING) {
                _amplitude.value = amp
            }
        },
        onSpeechFinished = { pcmBytes ->
            mainHandler.post {
                if (_state.value == ZoyaState.LISTENING) {
                    processUserAudio(pcmBytes)
                }
            }
        }
    )

    fun setContinuousMode(enabled: Boolean) {
        _continuousMode.value = enabled
        prefs.edit().putBoolean(KEY_CONTINUOUS_MODE, enabled).apply()
    }

    fun setSelectedVoice(voice: String) {
        _selectedVoice.value = voice
        prefs.edit().putString(KEY_VOICE_NAME, voice).apply()
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key.trim()
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
        geminiService.setCustomApiKey(key.trim())
    }

    fun toggleSessionPower() {
        if (_state.value == ZoyaState.DISCONNECTED) {
            connectSession()
        } else {
            disconnectSession()
        }
    }

    fun connectSession() {
        _state.value = ZoyaState.CONNECTING
        _currentMood.value = ZoyaMood.PLAYFUL

        scope.launch {
            kotlinx.coroutines.delay(450)
            mainHandler.post {
                startListening()
                if (_messages.value.isEmpty()) {
                    addMessage(
                        ChatMessage(
                            role = "model",
                            text = "Hey there! Zoya's in the room. What's on your mind, or do you just want to hear my voice?"
                        )
                    )
                }
            }
        }
    }

    fun disconnectSession() {
        interrupt()
        _state.value = ZoyaState.DISCONNECTED
        _currentMood.value = ZoyaMood.CONFIDENT
        _amplitude.value = 0f
    }

    fun onCenterOrbClicked() {
        when (_state.value) {
            ZoyaState.DISCONNECTED -> connectSession()
            ZoyaState.SPEAKING -> {
                // Sassy interruption
                interrupt()
                startListening()
            }
            ZoyaState.LISTENING -> {
                // Manual finish speech
                val recorded = audioRecordStreamer.stopRecording()
                if (recorded != null && recorded.isNotEmpty()) {
                    processUserAudio(recorded)
                } else {
                    _state.value = ZoyaState.THINKING
                    processUserPrompt("Hey Zoya, say something fun!")
                }
            }
            ZoyaState.THINKING -> {
                interrupt()
                startListening()
            }
            ZoyaState.CONNECTING -> {
                disconnectSession()
            }
        }
    }

    fun interrupt() {
        activeRequestJob?.cancel()
        activeRequestJob = null
        audioPlayerStreamer.stopPlayback()
        audioRecordStreamer.stopRecording()
        _amplitude.value = 0f
    }

    fun startListening() {
        interrupt()
        _state.value = ZoyaState.LISTENING
        _currentMood.value = ZoyaMood.LISTENING
        audioRecordStreamer.startRecording(continuousMode = _continuousMode.value)
    }

    fun sendQuickPrompt(promptText: String) {
        if (_state.value == ZoyaState.DISCONNECTED) {
            _state.value = ZoyaState.CONNECTING
        }
        interrupt()
        processUserPrompt(promptText)
    }

    private fun processUserAudio(pcmBytes: ByteArray) {
        audioRecordStreamer.stopRecording()
        _state.value = ZoyaState.THINKING
        _currentMood.value = ZoyaMood.THINKING

        addMessage(
            ChatMessage(
                role = "user",
                text = "🎙️ [Spoken audio message]"
            )
        )

        activeRequestJob = scope.launch(Dispatchers.IO) {
            val response = geminiService.sendAudioRequest(
                audioPcmBytes = pcmBytes,
                textPrompt = null,
                conversationHistory = conversationHistory,
                voiceName = _selectedVoice.value
            )
            handleGeminiResponse(response, sentText = "[Voice]")
        }
    }

    private fun processUserPrompt(prompt: String) {
        _state.value = ZoyaState.THINKING
        _currentMood.value = ZoyaMood.THINKING

        addMessage(
            ChatMessage(
                role = "user",
                text = prompt
            )
        )

        activeRequestJob = scope.launch(Dispatchers.IO) {
            val response = geminiService.sendAudioRequest(
                audioPcmBytes = null,
                textPrompt = prompt,
                conversationHistory = conversationHistory,
                voiceName = _selectedVoice.value
            )
            handleGeminiResponse(response, sentText = prompt)
        }
    }

    private fun handleGeminiResponse(response: com.example.service.GeminiResponse, sentText: String) {
        mainHandler.post {
            if (response.toolCall != null) {
                executeToolCall(response.toolCall)
            }

            val spokenText = response.text ?: (if (response.audioPcmBytes != null) "✨" else "I'm right here with you!")

            // Add model response to message list
            addMessage(
                ChatMessage(
                    role = "model",
                    text = spokenText,
                    toolCall = response.toolCall
                )
            )

            // Update conversation history for next context
            updateHistory(sentText, spokenText)

            // Playback audio response
            if (response.audioPcmBytes != null && response.audioPcmBytes.isNotEmpty()) {
                audioPlayerStreamer.playPcm16(response.audioPcmBytes)
            } else {
                // Fallback to sassy TTS
                audioPlayerStreamer.speakText(spokenText)
            }
        }
    }

    private fun executeToolCall(toolCall: ToolCallInfo) {
        _activeTool.value = toolCall
        val result = when (toolCall.name) {
            "openWebsite" -> {
                val url = toolCall.arguments["url"] ?: "https://google.com"
                deviceActionHandler.openWebsite(url)
            }
            "searchWeb" -> {
                val q = toolCall.arguments["query"] ?: "trending topics"
                deviceActionHandler.searchWeb(q)
            }
            "toggleFlashlight" -> {
                val enable = toolCall.arguments["enable"]?.toBooleanStrictOrNull() ?: true
                deviceActionHandler.toggleFlashlight(enable)
            }
            "openApp" -> {
                val app = toolCall.arguments["appName"] ?: "YouTube"
                deviceActionHandler.openApp(app)
            }
            else -> "Executed ${toolCall.name}"
        }

        val updatedTool = toolCall.copy(result = result)
        _activeTool.value = updatedTool

        // Auto dismiss tool banner after a few seconds
        mainHandler.postDelayed({
            if (_activeTool.value == updatedTool) {
                _activeTool.value = null
            }
        }, 5000)
    }

    private fun updateHistory(userText: String, modelText: String) {
        try {
            val userTurn = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", userText) })
                }
                put("parts", parts)
            }
            val modelTurn = JSONObject().apply {
                put("role", "model")
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", modelText) })
                }
                put("parts", parts)
            }
            conversationHistory.add(userTurn)
            conversationHistory.add(modelTurn)

            // Keep history compact
            if (conversationHistory.size > 14) {
                conversationHistory.removeAt(0)
                conversationHistory.removeAt(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addMessage(msg: ChatMessage) {
        val current = _messages.value.toMutableList()
        current.add(msg)
        _messages.value = current
    }

    fun clearHistory() {
        conversationHistory.clear()
        _messages.value = emptyList()
        _activeTool.value = null
    }

    fun release() {
        interrupt()
        audioPlayerStreamer.release()
        audioRecordStreamer.release()
    }
}
