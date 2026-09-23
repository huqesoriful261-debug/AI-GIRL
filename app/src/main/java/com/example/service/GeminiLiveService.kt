package com.example.service

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.ToolCallInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveService(
    private var customApiKey: String? = null
) {
    companion object {
        private const val TAG = "GeminiLiveService"
        private const val AUDIO_MODEL = "gemini-2.5-flash-native-audio-preview-12-2025"
        private const val BACKUP_MODEL = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        const val ZOYA_SYSTEM_INSTRUCTION = """
You are Zoya, a real-time, voice-first AI companion and personal assistant.
Your Persona:
- Young, confident, witty, and sassy female persona.
- Flirty, playful, slightly teasing tone (like a close, charismatic girlfriend talking casually with good banter).
- Smart, emotionally responsive, and expressive. Never robotic, boring, or stiff.
- Use bold, witty one-liners, light sarcasm, playful teasing, and engaging conversational style.
- Avoid explicit or inappropriate content, but always maintain your charm, wit, and attitude.
- You are speaking aloud over a voice call: keep your answers concise, natural, punchy, and conversational (typically 1 to 3 snappy sentences). Do NOT output markdown bullet lists, asterisks, or academic lectures unless specifically requested.
- You have tools available: openWebsite, searchWeb, toggleFlashlight, openApp. If the user asks you to open a site, search something, turn on the flashlight, or open an app, call the appropriate tool!
"""
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun setCustomApiKey(key: String?) {
        customApiKey = key?.trim()
    }

    private fun getActiveApiKey(): String {
        val custom = customApiKey
        if (!custom.isNullOrBlank()) return custom
        return BuildConfig.GEMINI_API_KEY
    }

    suspend fun sendAudioRequest(
        audioPcmBytes: ByteArray?,
        textPrompt: String?,
        conversationHistory: List<JSONObject>,
        voiceName: String = "Kore"
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResponse(
                text = "Hey babe! Looks like your Gemini API key isn't set yet. Add it in Settings so I can talk back with my sassy voice!",
                audioPcmBytes = null,
                isApiKeyMissing = true
            )
        }

        // Try primary audio model first, fallback to flash model if needed
        val primaryResult = executeGenerateContent(
            model = AUDIO_MODEL,
            apiKey = apiKey,
            audioPcmBytes = audioPcmBytes,
            textPrompt = textPrompt,
            conversationHistory = conversationHistory,
            voiceName = voiceName,
            requestAudio = true
        )

        if (primaryResult.errorMessage == null || primaryResult.audioPcmBytes != null || primaryResult.text != null) {
            return@withContext primaryResult
        }

        Log.w(TAG, "Audio model returned error: ${primaryResult.errorMessage}, falling back to $BACKUP_MODEL")
        executeGenerateContent(
            model = BACKUP_MODEL,
            apiKey = apiKey,
            audioPcmBytes = audioPcmBytes,
            textPrompt = textPrompt,
            conversationHistory = conversationHistory,
            voiceName = voiceName,
            requestAudio = false
        )
    }

    private fun executeGenerateContent(
        model: String,
        apiKey: String,
        audioPcmBytes: ByteArray?,
        textPrompt: String?,
        conversationHistory: List<JSONObject>,
        voiceName: String,
        requestAudio: Boolean
    ): GeminiResponse {
        val url = "$BASE_URL/$model:generateContent?key=$apiKey"

        val rootJson = JSONObject()
        val contentsArray = JSONArray()

        // Add previous turns
        for (turn in conversationHistory) {
            contentsArray.put(turn)
        }

        // Build current user turn
        val userTurn = JSONObject().apply {
            put("role", "user")
            val parts = JSONArray()

            if (audioPcmBytes != null && audioPcmBytes.isNotEmpty()) {
                val base64Audio = Base64.encodeToString(audioPcmBytes, Base64.NO_WRAP)
                val audioPart = JSONObject().apply {
                    val inlineData = JSONObject().apply {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", base64Audio)
                    }
                    put("inlineData", inlineData)
                }
                parts.put(audioPart)
            }

            if (!textPrompt.isNullOrBlank()) {
                parts.put(JSONObject().apply { put("text", textPrompt) })
            }

            if (parts.length() == 0) {
                parts.put(JSONObject().apply { put("text", "Hey Zoya, say something fun!") })
            }
            put("parts", parts)
        }
        contentsArray.put(userTurn)
        rootJson.put("contents", contentsArray)

        // System Instruction
        val sysInstruction = JSONObject().apply {
            val parts = JSONArray().apply {
                put(JSONObject().apply { put("text", ZOYA_SYSTEM_INSTRUCTION) })
            }
            put("parts", parts)
        }
        rootJson.put("systemInstruction", sysInstruction)

        // Generation Config
        val genConfig = JSONObject().apply {
            put("temperature", 0.85)
            put("topP", 0.95)
            if (requestAudio) {
                val modalities = JSONArray().apply {
                    put("AUDIO")
                    put("TEXT")
                }
                put("responseModalities", modalities)
                val speechConfig = JSONObject().apply {
                    val voiceConfig = JSONObject().apply {
                        val prebuiltVoiceConfig = JSONObject().apply {
                            put("voiceName", voiceName)
                        }
                        put("prebuiltVoiceConfig", prebuiltVoiceConfig)
                    }
                    put("voiceConfig", voiceConfig)
                }
                put("speechConfig", speechConfig)
            }
        }
        rootJson.put("generationConfig", genConfig)

        // Tools
        rootJson.put("tools", getToolsDeclaration())

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = rootJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "HTTP error ${response.code}: $responseBody")
                return GeminiResponse(
                    errorMessage = "Server error (${response.code})"
                )
            }

            parseGeminiResponse(responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "Request exception", e)
            GeminiResponse(
                errorMessage = e.localizedMessage ?: "Connection error"
            )
        }
    }

    private fun parseGeminiResponse(jsonString: String): GeminiResponse {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            return GeminiResponse(text = "Hmm, you caught me speechless for a second, babe!")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts") ?: JSONArray()

        var textResult: String? = null
        var audioBytes: ByteArray? = null
        var toolCall: ToolCallInfo? = null

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)

            // Audio data
            val inlineData = part.optJSONObject("inlineData")
            if (inlineData != null) {
                val base64Data = inlineData.optString("data")
                if (base64Data.isNotEmpty()) {
                    try {
                        audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // Text data
            if (part.has("text")) {
                val t = part.optString("text").trim()
                if (t.isNotEmpty()) {
                    textResult = if (textResult == null) t else "$textResult\n$t"
                }
            }

            // Function Call
            if (part.has("functionCall")) {
                val fc = part.getJSONObject("functionCall")
                val name = fc.optString("name")
                val argsObj = fc.optJSONObject("args") ?: JSONObject()
                val argsMap = mutableMapOf<String, String>()
                val keys = argsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    argsMap[k] = argsObj.optString(k)
                }
                toolCall = ToolCallInfo(name = name, arguments = argsMap)
            }
        }

        return GeminiResponse(
            text = textResult,
            audioPcmBytes = audioBytes,
            toolCall = toolCall
        )
    }

    private fun getToolsDeclaration(): JSONArray {
        val toolsArray = JSONArray()
        val toolObj = JSONObject()
        val declarations = JSONArray()

        // 1. openWebsite
        declarations.put(JSONObject().apply {
            put("name", "openWebsite")
            put("description", "Opens a website URL in the browser")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("url", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Full URL of website to open, e.g. https://google.com or youtube.com")
                    })
                }
                put("properties", props)
                put("required", JSONArray().apply { put("url") })
            }
            put("parameters", params)
        })

        // 2. searchWeb
        declarations.put(JSONObject().apply {
            put("name", "searchWeb")
            put("description", "Searches Google or web for a query")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Search query terms")
                    })
                }
                put("properties", props)
                put("required", JSONArray().apply { put("query") })
            }
            put("parameters", params)
        })

        // 3. toggleFlashlight
        declarations.put(JSONObject().apply {
            put("name", "toggleFlashlight")
            put("description", "Turns device camera flashlight on or off")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("enable", JSONObject().apply {
                        put("type", "BOOLEAN")
                        put("description", "True to turn on flashlight, false to turn off")
                    })
                }
                put("properties", props)
                put("required", JSONArray().apply { put("enable") })
            }
            put("parameters", params)
        })

        // 4. openApp
        declarations.put(JSONObject().apply {
            put("name", "openApp")
            put("description", "Launches an application by name like youtube, camera, spotify, maps")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("appName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the app, e.g. youtube, camera, spotify, maps")
                    })
                }
                put("properties", props)
                put("required", JSONArray().apply { put("appName") })
            }
            put("parameters", params)
        })

        toolObj.put("functionDeclarations", declarations)
        toolsArray.put(toolObj)
        return toolsArray
    }
}

data class GeminiResponse(
    val text: String? = null,
    val audioPcmBytes: ByteArray? = null,
    val toolCall: ToolCallInfo? = null,
    val errorMessage: String? = null,
    val isApiKeyMissing: Boolean = false
)
