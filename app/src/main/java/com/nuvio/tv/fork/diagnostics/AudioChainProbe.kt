package com.nuvio.tv.fork.diagnostics

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * G5c (feature 39): what the platform says the audio chain takes, captured once per sink build.
 * ALGORITHM_PORT of ysosrs `AudioCapabilityReport` @ 45e0984 (direct-support probe, surround mode,
 * per-encoding PCM channel read), reduced to the HUD snapshot.
 *
 * Read-only: [AudioTrack.isDirectPlaybackSupported] is the question media3 asks, so this reports
 * what the sink will decide. It never opens an AudioTrack or writes a setting, and any failure
 * leaves no snapshot rather than touching playback.
 */
object AudioChainProbe {

    @Volatile
    var latest: AudioChainSnapshot? = null
        private set

    fun capture(context: Context) {
        latest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { probe(context) }.getOrNull()
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun probe(context: Context): AudioChainSnapshot {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
        val direct = probes().filter { (_, encoding) ->
            val format = AudioFormat.Builder()
                .setEncoding(encoding)
                .setSampleRate(48_000)
                .setChannelMask(AudioFormat.CHANNEL_OUT_5POINT1)
                .build()
            runCatching { AudioTrack.isDirectPlaybackSupported(format, attributes) }.getOrDefault(false)
        }.map { it.first }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return AudioChainSnapshot(
            direct = direct,
            surroundMode = surroundMode(audioManager),
            maxPcmChannels = maxPcmChannels(audioManager),
        )
    }

    @SuppressLint("InlinedApi")
    private fun probes(): List<Pair<String, Int>> = buildList {
        add("AC3" to AudioFormat.ENCODING_AC3)
        add("E-AC3" to AudioFormat.ENCODING_E_AC3)
        add("JOC" to AudioFormat.ENCODING_E_AC3_JOC)
        add("TrueHD" to AudioFormat.ENCODING_DOLBY_TRUEHD)
        add("DTS" to AudioFormat.ENCODING_DTS)
        add("DTS-HD" to AudioFormat.ENCODING_DTS_HD)
    }

    /** Android TV surround setting; the getter exists from API 31. */
    private fun surroundMode(audioManager: AudioManager?): String? {
        if (audioManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return when (runCatching { audioManager.encodedSurroundMode }.getOrNull()) {
            AudioManager.ENCODED_SURROUND_OUTPUT_NEVER -> "NEVER"
            AudioManager.ENCODED_SURROUND_OUTPUT_ALWAYS -> "ALWAYS"
            AudioManager.ENCODED_SURROUND_OUTPUT_AUTO -> "AUTO"
            AudioManager.ENCODED_SURROUND_OUTPUT_MANUAL -> "MANUAL"
            else -> null
        }
    }

    /**
     * Highest PCM channel count the HDMI/ARC/eARC output negotiated, per encoding (API 31
     * `getAudioProfiles`); the port-wide channel union would over-report on plain ARC.
     */
    @SuppressLint("NewApi", "InlinedApi")
    private fun maxPcmChannels(audioManager: AudioManager?): Int? {
        if (audioManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val hdmi = setOf(AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_HDMI_EARC)
        val pcm = setOf(
            AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.ENCODING_PCM_24BIT_PACKED,
            AudioFormat.ENCODING_PCM_32BIT,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        return runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .filter { it.type in hdmi }
                .flatMap { it.audioProfiles }
                .filter { it.format in pcm }
                .flatMap { profile -> (profile.channelMasks.asList() + profile.channelIndexMasks.asList()).map(Integer::bitCount) }
                .filter { it > 0 }
                .maxOrNull()
        }.getOrNull()
    }
}
