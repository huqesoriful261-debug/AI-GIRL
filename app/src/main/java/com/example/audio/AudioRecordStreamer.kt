package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

class AudioRecordStreamer(
    private val onAmplitude: (Float) -> Unit,
    private val onSpeechFinished: (ByteArray) -> Unit
) {
    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val SILENCE_THRESHOLD = 0.035f
        private const val SILENCE_TIMEOUT_MS = 1400L
        private const val MIN_SPEECH_DURATION_MS = 600L
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val recordingScope = CoroutineScope(Dispatchers.IO)
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun startRecording(continuousMode: Boolean = true) {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        ).coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = recordingScope.launch {
                val buffer = ShortArray(1024)
                val byteStream = ByteArrayOutputStream()
                var speechStartTime = 0L
                var lastSpeechTime = 0L
                var speechDetected = false

                while (isActive && isRecording) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readCount > 0) {
                        // Write to byte stream (Little Endian)
                        for (i in 0 until readCount) {
                            val s = buffer[i].toInt()
                            byteStream.write(s and 0xFF)
                            byteStream.write((s shr 8) and 0xFF)
                        }

                        // Compute RMS amplitude
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            val v = buffer[i].toDouble()
                            sum += v * v
                        }
                        val rms = sqrt(sum / readCount)
                        val amp = (rms / 28000.0).coerceIn(0.0, 1.0).toFloat()
                        onAmplitude(amp)

                        val now = System.currentTimeMillis()

                        if (amp > SILENCE_THRESHOLD) {
                            if (!speechDetected) {
                                speechDetected = true
                                speechStartTime = now
                            }
                            lastSpeechTime = now
                        } else if (continuousMode && speechDetected) {
                            val speechDuration = lastSpeechTime - speechStartTime
                            val silenceDuration = now - lastSpeechTime
                            if (speechDuration >= MIN_SPEECH_DURATION_MS && silenceDuration >= SILENCE_TIMEOUT_MS) {
                                // User finished sentence
                                val recordedBytes = byteStream.toByteArray()
                                if (recordedBytes.isNotEmpty()) {
                                    isRecording = false
                                    onSpeechFinished(recordedBytes)
                                    break
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
        }
    }

    fun stopRecording(): ByteArray? {
        if (!isRecording && audioRecord == null) return null
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        return try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            onAmplitude(0f)
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun release() {
        stopRecording()
    }
}
