package com.game.maths

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

enum class VoicePlaybackMode(val label: String) {
    DIRECT_PCM("软件 PCM · 48 kHz"),
    SYSTEM_MEDIA("系统媒体播放器");

    companion object {
        fun fromStored(value: String?): VoicePlaybackMode =
            entries.firstOrNull { it.name == value } ?: DIRECT_PCM
    }
}

/**
 * Two testable offline narration routes:
 * DIRECT_PCM: app-side PCM conversion to 48 kHz stereo, then a SINGLE complete
 *             AudioTrack.MODE_STATIC write, with no streaming, no media codec.
 * SYSTEM_MEDIA: the older whole-utterance WAV MediaPlayer route for comparison.
 *
 * Playback/cancel/release all run on ONE worker thread. UI calls only change the
 * generation, preventing the old cross-thread stop/write race and hard cut pops.
 */
class VoicePlayback(private val context: Context, private val onSpeaking: (Boolean) -> Unit) {
    private data class Clip(val offset: Int, val length: Int)
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "MathPlanetVoiceWorker").apply { isDaemon = true }
    }
    private val pcm = context.assets.open("voicepack.pcm").use { it.readBytes() }
    private val clips = context.assets.open("voicepack.tsv").bufferedReader().use { reader ->
        reader.lineSequence().mapNotNull { row ->
            val parts = row.split('\t')
            if (parts.size != 3) return@mapNotNull null
            val offset = parts[1].toIntOrNull() ?: return@mapNotNull null
            val length = parts[2].toIntOrNull() ?: return@mapNotNull null
            if (offset < 0 || length <= 0 || length % 2 != 0 ||
                offset.toLong() + length > pcm.size) return@mapNotNull null
            parts[0] to Clip(offset, length)
        }.toMap()
    }
    private val lock = Any()
    @Volatile private var generation = 0
    @Volatile private var closed = false
    @Volatile private var playbackMode = VoicePlaybackMode.DIRECT_PCM
    @Volatile private var pcmPrepared = false

    var status by mutableStateOf("尚未试听，请检查媒体音量")
        private set
    val ready get() = pcm.isNotEmpty() && clips.size >= 100
    val clipCount get() = clips.size

    /**
     * Pre-initialize the output route on the serialized worker. A silent 50ms
     * static track is played before the first real utterance so Android 15
     * vendor audio initialization cannot swallow the opening narration.
     * This runs only once and never blocks Compose's UI thread.
     */
    fun prepare() {
        val needsWarmup = synchronized(lock) {
            if (closed || pcmPrepared || playbackMode != VoicePlaybackMode.DIRECT_PCM) false
            else {
                pcmPrepared = true
                true
            }
        }
        if (!needsWarmup) return
        worker.execute {
            if (closed) return@execute
            var track: AudioTrack? = null
            try {
                val silence = ByteArray(9600) // 50ms at 48 kHz, PCM16 stereo
                track = AudioTrack.Builder()
                    .setAudioAttributes(attributes())
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(PcmResampler.OUTPUT_SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(silence.size)
                    .build()
                check(track.state == AudioTrack.STATE_INITIALIZED) {
                    "PCM output warmup track failed to initialize"
                }
                check(track.write(silence, 0, silence.size) == silence.size) {
                    "PCM output warmup write incomplete"
                }
                track.play()
                Thread.sleep(65L)
                Log.i(TAG, "PCM output warmed up before first narration")
            } catch (error: Exception) {
                Log.w(TAG, "PCM output warmup failed; real playback will still be attempted", error)
                synchronized(lock) { pcmPrepared = false }
            } finally {
                runCatching { track?.stop() }
                runCatching { track?.release() }
            }
        }
    }

    fun setMode(mode: VoicePlaybackMode) {
        if (playbackMode != mode) {
            synchronized(lock) {
                playbackMode = mode
                generation++
            }
            onSpeaking(false)
        }
        reportOn()
    }

    fun reportOff() = setStatus("中文配音已关闭")
    fun reportOn() = setStatus(
        if (playbackMode == VoicePlaybackMode.DIRECT_PCM)
            "中文配音：软件 PCM · 48 kHz 静态缓冲"
        else "中文配音：系统媒体播放器"
    )

    private fun setStatus(message: String, request: Int? = null) {
        main.post {
            if (!closed && (request == null || request == generation)) status = message
        }
    }

    private fun current(request: Int) = !closed && generation == request

    fun play(tokens: List<String>) {
        if (!ready) {
            Log.e(TAG, "Narration unavailable: bundled PCM or voice index is incomplete")
            setStatus("内置中文配音缺失，请重新安装")
            return
        }
        if (tokens.isEmpty()) return
        // Also guard direct callers that arrive before the normal ON_RESUME path.
        if (playbackMode == VoicePlaybackMode.DIRECT_PCM) prepare()
        val request: Int
        val mode: VoicePlaybackMode
        synchronized(lock) {
            if (closed) return
            generation++
            request = generation
            mode = playbackMode
        }
        onSpeaking(true)
        setStatus(
            if (mode == VoicePlaybackMode.DIRECT_PCM)
                "播放中：软件 PCM 48 kHz（无需媒体解码器）"
            else "播放中：系统媒体播放器",
            request
        )
        worker.execute {
            if (!current(request)) return@execute
            val stream = ByteArrayOutputStream()
            for (token in tokens) {
                val clip = clips[token]
                if (clip == null) {
                    Log.w(TAG, "Unknown voice token: " + token)
                    continue
                }
                stream.write(pcm, clip.offset, clip.length)
                stream.write(ByteArray(256))
            }
            if (!current(request)) return@execute
            var success = false
            try {
                val bytes = stream.toByteArray()
                if (bytes.isNotEmpty()) {
                    val startedAt = SystemClock.uptimeMillis()
                    success = when (mode) {
                        VoicePlaybackMode.DIRECT_PCM -> playStaticPcm(bytes, request)
                        VoicePlaybackMode.SYSTEM_MEDIA -> playWav(bytes, request)
                    }
                    Log.i(TAG, "backend=" + mode.name + " samples=" + bytes.size / 2 +
                        " success=" + success + " elapsedMs=" +
                        (SystemClock.uptimeMillis() - startedAt))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Narration failure with backend=" + mode.name, e)
            } finally {
                if (current(request)) {
                    setStatus(
                        if (success) "朗读完成 · " + mode.label
                        else "语音播放失败，请提供 MathPlanetVoice 日志",
                        request
                    )
                    onSpeaking(false)
                }
            }
        }
    }

    private fun attributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

    private fun playStaticPcm(sourcePcm16k: ByteArray, request: Int): Boolean {
        val data = PcmResampler.toStereo48k(sourcePcm16k)
        if (data.isEmpty() || !current(request)) return false
        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(attributes())
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(PcmResampler.OUTPUT_SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(data.size)
                .build()
            check(track.state == AudioTrack.STATE_INITIALIZED) { "Static AudioTrack init failed" }
            var written = 0
            // Pre-fill entire utterance BEFORE play. No buffer starvation during playback.
            while (written < data.size && current(request)) {
                val amount = track.write(
                    data, written, data.size - written, AudioTrack.WRITE_BLOCKING
                )
                check(amount > 0) { "Static AudioTrack write failed: " + amount }
                written += amount
            }
            if (!current(request) || written != data.size) return false
            track.setVolume(1f) // amplitude was attenuated in software
            track.play()
            val totalFrames = data.size / 4L
            val deadline = SystemClock.uptimeMillis() +
                totalFrames * 1000L / PcmResampler.OUTPUT_SAMPLE_RATE + 2500L
            while (current(request) && SystemClock.uptimeMillis() < deadline) {
                val head = track.playbackHeadPosition.toLong() and 0xffffffffL
                if (head >= (totalFrames - 240L).coerceAtLeast(0L)) return true
                Thread.sleep(20L)
            }
            if (current(request)) Log.w(TAG, "Static PCM timeout: frames=" + totalFrames +
                " head=" + (track.playbackHeadPosition.toLong() and 0xffffffffL))
            return false
        } finally {
            // A quick "next" or repeat previously hard-stopped the static track
            // mid-waveform, which can create an audible click. Let this same
            // worker soften the end before stopping (never cross-thread stop).
            if (!current(request) && track?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                runCatching {
                    for (volume in floatArrayOf(.65f, .30f, 0f)) {
                        track.setVolume(volume)
                        Thread.sleep(8L)
                    }
                }
            }
            runCatching { track?.stop() }
            runCatching { track?.release() }
        }
    }

    private fun playWav(sourcePcm16k: ByteArray, request: Int): Boolean {
        val file = File(context.cacheDir, "mathplanet_voice_" + request + ".wav")
        var player: MediaPlayer? = null
        try {
            FileOutputStream(file).use { output ->
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                header.put("RIFF".toByteArray(Charsets.US_ASCII))
                header.putInt(sourcePcm16k.size + 36)
                header.put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
                header.putInt(16)
                header.putShort(1.toShort())
                header.putShort(1.toShort())
                header.putInt(16000)
                header.putInt(32000)
                header.putShort(2.toShort())
                header.putShort(16.toShort())
                header.put("data".toByteArray(Charsets.US_ASCII))
                header.putInt(sourcePcm16k.size)
                output.write(header.array())
                output.write(sourcePcm16k)
            }
            if (!current(request)) return false
            player = MediaPlayer().apply {
                setAudioAttributes(attributes())
                setVolume(.78f, .78f)
                setDataSource(file.absolutePath)
                prepare()
            }
            if (!current(request)) return false
            player.start()
            val deadline = SystemClock.uptimeMillis() +
                sourcePcm16k.size / 32L + 2500L
            while (current(request) && SystemClock.uptimeMillis() < deadline) {
                if (!player.isPlaying) return true
                Thread.sleep(25L)
            }
            if (current(request)) Log.w(TAG, "MediaPlayer timeout")
            return false
        } finally {
            runCatching { player?.release() }
            runCatching { file.delete() }
        }
    }

    fun stop() {
        synchronized(lock) { generation++ }
        // The running worker notices cancellation within 20-25ms and releases its
        // own track. No cross-thread AudioTrack/MediaPlayer operations occur.
        onSpeaking(false)
    }

    fun close() {
        synchronized(lock) { closed = true; generation++ }
        onSpeaking(false)
        worker.shutdown()
    }

    companion object {
        private const val TAG = "MathPlanetVoice"
    }
}
