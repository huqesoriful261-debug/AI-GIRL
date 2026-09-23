package com.example.data

enum class ZoyaState {
    DISCONNECTED,
    CONNECTING,
    LISTENING,
    THINKING,
    SPEAKING
}

enum class ZoyaMood(val label: String, val emoji: String) {
    CONFIDENT("Confident & Sassy", "✨"),
    PLAYFUL("Playfully Teasing", "😏"),
    WITTY("Quick & Witty", "⚡"),
    LISTENING("All Ears, Babe", "🎧"),
    THINKING("Cooking a Comeback", "💭"),
    CARING("Sweet Beneath the Sass", "💖")
}

data class ToolCallInfo(
    val name: String,
    val arguments: Map<String, String>,
    val result: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user", "model", "system"
    val text: String,
    val toolCall: ToolCallInfo? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ZoyaVoiceOption(
    val id: String,
    val displayName: String,
    val description: String
)

val AVAILABLE_VOICES = listOf(
    ZoyaVoiceOption("Kore", "Kore (Sassy & Energetic)", "Default voice for Zoya - bold, lively, and teasing"),
    ZoyaVoiceOption("Aoede", "Aoede (Warm & Charismatic)", "Smooth, confident, and playful"),
    ZoyaVoiceOption("Puck", "Puck (Witty & Cheeky)", "High energy, fast and witty"),
    ZoyaVoiceOption("Fenrir", "Fenrir (Deep & Bold)", "Deeper, strong and confident")
)
