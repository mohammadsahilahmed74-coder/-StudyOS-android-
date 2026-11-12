package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AmbientSound(val displayName: String) {
    NONE("Mute / Off"),
    WHITE_NOISE("White Noise"),
    RAIN("Soft Rain"),
    BROWN_NOISE("Deep Ocean / Brown Noise"),
    FOREST("Breezy Leaves")
}

/**
 * AmbientSoundPlayer synthesizes lightweight ambient sound waves using AudioTrack.
 * Works offline, requires no asset downloads, and features full mute / volume control.
 */
class AmbientSoundPlayer(private val context: Context) {
    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    var currentSound: AmbientSound = AmbientSound.NONE
        private set

    var isMuted: Boolean = false
        private set

    fun playSound(sound: AmbientSound) {
        currentSound = sound
        if (sound == AmbientSound.NONE || isMuted) {
            stopSound()
            return
        }

        stopSound()

        try {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ) * 2

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            android.util.Log.e("AmbientSoundPlayer", "Failed to initialize AudioTrack: ${e.message}", e)
            stopSound()
            return
        }

        synthesisJob = scope.launch {
            val buffer = ShortArray(1024)
            var lastSample = 0.0f
            while (isActive) {
                for (i in buffer.indices) {
                    val rawSample = when (currentSound) {
                        AmbientSound.WHITE_NOISE -> {
                            (Random.nextFloat() * 2f - 1f) * 0.15f
                        }
                        AmbientSound.RAIN -> {
                            // Filtered noise with drops
                            val n = (Random.nextFloat() * 2f - 1f) * 0.12f
                            lastSample = (lastSample * 0.85f) + (n * 0.15f)
                            lastSample
                        }
                        AmbientSound.BROWN_NOISE -> {
                            // Integrated noise
                            val n = (Random.nextFloat() * 2f - 1f) * 0.05f
                            lastSample = (lastSample + (0.02f * n)).coerceIn(-0.3f, 0.3f)
                            lastSample
                        }
                        AmbientSound.FOREST -> {
                            // Gentle breathing sway
                            val n = (Random.nextFloat() * 2f - 1f) * 0.08f
                            lastSample = (lastSample * 0.95f) + (n * 0.05f)
                            lastSample
                        }
                        AmbientSound.NONE -> 0.0f
                    }
                    buffer[i] = (rawSample * Short.MAX_VALUE).toInt().toShort()
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }

    fun toggleMute() {
        isMuted = !isMuted
        if (isMuted) {
            audioTrack?.setVolume(0f)
        } else {
            audioTrack?.setVolume(1f)
            if (currentSound != AmbientSound.NONE && audioTrack == null) {
                playSound(currentSound)
            }
        }
    }

    fun stopSound() {
        synthesisJob?.cancel()
        synthesisJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
