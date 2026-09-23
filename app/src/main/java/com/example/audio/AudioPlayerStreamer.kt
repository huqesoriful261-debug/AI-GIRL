package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.sqrt

class AudioPlayerStreamer(
    private val context: Context,
    private val onAmplitude: (Float) -> Unit,
    private val onPlaybackStarted: () -> Unit,
    private val onPlaybackFinished: () -> Unit
) {
    companion object {
        const val DEFAULT_SAMPLE_RATE = 24000 // Gemini native audio format
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val playbackScope = CoroutineScope(Dispatchers.IO)
    private var isPlayingAudio = false

    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                textToSpeech?.language = Locale.US
                textToSpeech?.setPitch(1.15f) // Confident, young, sassy feminine pitch
                textToSpeech?.setSpeechRate(1.04f) // Snappy and energetic

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isPlayingAudio = true
                        onPlaybackStarted()
                        startTtsVisualizerSimulation()
                    }

                    override fun onDone(utteranceId: String?) {
                        isPlayingAudio = false
                        onAmplitude(0f)
                        onPlaybackFinished()
                    }

                    override fun onError(utteranceId: String?) {
                        isPlayingAudio = false
                        onAmplitude(0f)
                        onPlaybackFinished()
                    }
                })
            }
        }
    }

    fun playPcm16(pcmData: ByteArray, sampleRate: Int = DEFAULT_SAMPLE_RATE) {
        stopPlayback()
        if (pcmData.isEmpty()) {
            onPlaybackFinished()
            return
        }

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        ).coerceAtLeast(4096)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(CHANNEL_CONFIG)
            .setEncoding(AUDIO_FORMAT)
            .build()

        try {
            audioTrack = AudioTrack(
                attributes,
                format,
                minBufferSize * 2,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            audioTrack?.play()
            isPlayingAudio = true
            onPlaybackStarted()

            playbackJob = playbackScope.launch {
                val chunkSize = 2048
                var offset = 0
                val shortBuffer = ShortArray(chunkSize / 2)

                while (isActive && isPlayingAudio && offset < pcmData.size) {
                    val length = (pcmData.size - offset).coerceAtMost(chunkSize)
                    audioTrack?.write(pcmData, offset, length)

                    // Compute amplitude from bytes for visualizer
                    val shortsToRead = length / 2
                    val byteBuffer = ByteBuffer.wrap(pcmData, offset, length).order(ByteOrder.LITTLE_ENDIAN)
                    var sum = 0.0
                    for (i in 0 until shortsToRead) {
                        val s = byteBuffer.short.toDouble()
                        sum += s * s
                    }
                    val rms = if (shortsToRead > 0) sqrt(sum / shortsToRead) else 0.0
                    val amp = (rms / 26000.0).coerceIn(0.0, 1.0).toFloat()
                    onAmplitude(amp)

                    offset += length
                }

                // Allow track to drain buffer
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
                isPlayingAudio = false
                onAmplitude(0f)
                onPlaybackFinished()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayback()
            onPlaybackFinished()
        }
    }

    fun speakText(text: String) {
        stopPlayback()
        if (!isTtsInitialized || textToSpeech == null) {
            onPlaybackFinished()
            return
        }

        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "zoya_speech_${System.currentTimeMillis()}")
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "zoya_speech_${System.currentTimeMillis()}")
    }

    private fun startTtsVisualizerSimulation() {
        playbackJob?.cancel()
        playbackJob = playbackScope.launch {
            var step = 0f
            while (isActive && isPlayingAudio) {
                step += 0.35f
                val fakeAmp = (0.25f + 0.35f * kotlin.math.sin(step) + 0.15f * kotlin.math.cos(step * 1.8f)).coerceIn(0.1f, 0.85f)
                onAmplitude(fakeAmp)
                kotlinx.coroutines.delay(65)
            }
            onAmplitude(0f)
        }
    }

    fun stopPlayback() {
        isPlayingAudio = false
        playbackJob?.cancel()
        playbackJob = null

        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (isTtsInitialized) {
                textToSpeech?.stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onAmplitude(0f)
    }

    fun release() {
        stopPlayback()
        try {
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
